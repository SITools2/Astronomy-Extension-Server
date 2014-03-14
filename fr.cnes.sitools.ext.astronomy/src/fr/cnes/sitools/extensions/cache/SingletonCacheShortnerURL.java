 /*******************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SITools2.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.extensions.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.restlet.data.LocalReference;
import org.restlet.engine.Engine;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * Singleton for server cache based on Shortner URL.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SingletonCacheShortnerURL {
    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(SingletonCacheShortnerURL.class.getName());

    /**
     * Maximum number of URLS that the service can manage.
     */
    private static final int NUMBER_URLS = 238327;
    /**
     * Cache name for this service.
     */
    private static final String CACHE_NAME = "ShortenerUrl";
    /**
     * Alpha numeric for transformation.
     */
    private static final String ALPHANUMERIC
            = "0123456789"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz";

    /**
     * Any letters or numbers which are similar in appearance should be avoided
     * (like “I”, “1”, “O”, “0”,"Q", "i", "l", "o").
     */
    private static final String ALPHANUMERICALT
            = "23456789"
            + "ABCDEFGHJKLMNPRSTUVWXYZ"
            + "abcdefghjkmnpqrstuvwxyz";

    /**
     * Cache configuration.
     */
    private final transient Representation cacheConf = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/ehcache.xml").get();

    /**
     * Constructsthe singleton.
     */
    protected SingletonCacheShortnerURL() {
        try {
            CacheManager.create(cacheConf.getStream());
        } catch (IOException ex) {
            Engine.getLogger(SingletonCacheShortnerURL.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Creates the cache.
     */
    public static void create() {
        new SingletonCacheShortnerURL();
    }

    /**
     * Returns the cache instance.
     *
     * @return the cache instance
     */
    public static CacheManager getInstance() {
        return CacheManager.getInstance();
    }

    /**
     * Creates an Id with an uniform distribution.
     *
     * @return the Id
     */
    private static int generateId() {
        final Random rand = new Random();
        return rand.nextInt(NUMBER_URLS + 1);
    }

    /**
     * Converts the input to the base.
     *
     * @param input Identifier to convert
     * @param baseChars the base that is used for the conversion (alphanumeric
     * or alphanumeric_alt)
     * @return the input that is converted to the base
     */
    private static String toBase(final long input, final String baseChars) {
        long inputVal = input;
        String shorteningId = "";
        final int targetBase = baseChars.length();
        do {
            shorteningId = String.format("%s%s", String.valueOf(baseChars.charAt((int) (inputVal % targetBase))), shorteningId);
            inputVal /= targetBase;
        } while (inputVal > 0);

        return shorteningId;
    }

    /**
     * The inverse algorithm of toBase.
     *
     * @param input shortener id to convert to number
     * @param baseChars the base that is used
     * @return the shortener value that is converted to a number
     */
    private static int fromBase(final String input, final String baseChars) {
        final int srcBase = baseChars.length();
        int urlId = 0;
        final String reverseString = new StringBuilder(input).reverse().toString();

        for (int i = 0; i < reverseString.length(); i++) {
            final int charIndex = baseChars.indexOf(reverseString.charAt(i));
            urlId += (int) (charIndex * Math.pow((double) srcBase, (double) i));
        }
        return urlId;
    }

    /**
     * Stores a config and gets a shortening Id.
     *
     * @param config config to store
     * @return a shortening Id
     */
    public static final String putConfig(final String config) {
        int configId;
        final CacheManager cacheMgt = SingletonCacheShortnerURL.getInstance();
        final Cache cache = cacheMgt.getCache(CACHE_NAME);
        do {
            configId = generateId();
        } while (cache.isKeyInCache(configId));
        cache.put(new Element(configId, new ConfigCache(config)));
        return toBase(configId, ALPHANUMERICALT);
    }

    /**
     * Returns an configCache from its shortening ID.
     *
     * @param shorteningId
     * <p>
     * Raise an IllegalArgumentException when <code>shorteningId</code> is not
     * in the cache
     * </p>
     * @return an URL
     */
    public static final String getConfig(final String shorteningId) {
        String result;
        final int storeId = fromBase(shorteningId, ALPHANUMERICALT);
        final CacheManager cacheMgt = SingletonCacheShortnerURL.getInstance();
        final Cache cache = cacheMgt.getCache(CACHE_NAME);
        if (cache.isKeyInCache(storeId)) {
            LOG.log(Level.INFO, "Cache is used for: {0}", storeId);
            final ConfigCache configCache = (ConfigCache) cache.get(storeId).getObjectValue();
            result = configCache.getConfig();
            configCache.setNbClicks(configCache.getNbClicks() + 1);
            cache.put(new Element(storeId, configCache));
        } else {            
            throw new IllegalArgumentException("Cannot find the record in the cache");
        }
        return result;
    }

    /**
     * Creates cache of the MIZAR config and also about its number of access.
     */
    private static class ConfigCache implements Serializable {

        /**
         * Serial version.
         */
        private static final long serialVersionUID = 1L;
        /**
         * The URL to bookmark.
         */
        private String config;
        /**
         * The number of access on this URL.
         */
        private int nbClicks;

        /**
         * Constructor.
         *
         * @param configVal URL
         */
        public ConfigCache(final String configVal) {
            this(configVal, 0);
        }

        /**
         * Constructor.
         *
         * @param configVal URL
         * @param nbClicksVal number of clicks that has been done on this URL.
         */
        public ConfigCache(final String configVal, final int nbClicksVal) {
            setConfig(configVal);
            setNbClicks(nbClicksVal);
        }

        /**
         * Sets the config.
         *
         * @param configVal config
         */
        private void setConfig(final String configVal) {
            this.config = configVal;
        }

        /**
         * Returns the config.
         *
         * @return the config
         */
        public final String getConfig() {
            return this.config;
        }

        /**
         * Return the number of clicks.
         *
         * @return the nbClicks
         */
        public final int getNbClicks() {
            return nbClicks;
        }

        /**
         * Sets the number of clicks.
         *
         * @param nbClicksVal the nbClicks to set
         */
        public final void setNbClicks(final int nbClicksVal) {
            this.nbClicks = nbClicksVal;
        }
    }
}
