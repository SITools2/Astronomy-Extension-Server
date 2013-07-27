/*******************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SITools2. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
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
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * Singleton for server cache based on Shortner URL.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SingletonCacheShortnerURL {

    /**
     * Maximum number of URLS that the service can manage.
     */
    private static final int NUMBER_URLS = 238327;
    /**
     * Cache name for this service.
     */
    private static final String CACHE_NAME = "ShortenerUrl";
    
    private static String ALPHANUMERIC
            = "0123456789"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz";

    //Remove 0oO1iIl - Base52
    private static String ALPHANUMERIC_ALT
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
            Logger.getLogger(SingletonCacheShortnerURL.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Creates the cache.
     */
    public static void create() {
        final SingletonCacheShortnerURL singletonCacheShortnerUrl = new SingletonCacheShortnerURL();
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
     * @param input Identifier to convert
     * @param baseChars the base that is used for the conversion (alphanumeric or alphanumeric_alt)
     * @return the input that is converted to the base
     */
    private static String toBase(long input, final String baseChars) {
        String shorteningId = "";
        final int targetBase = baseChars.length();
        do {
            shorteningId = String.format("%s%s", String.valueOf(baseChars.charAt((int) (input % targetBase))), shorteningId);
            input /= targetBase;
        } while (input > 0);

        return shorteningId;
    }

    /**
     * The inverse algorithm of toBase.
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
     * Stores a URL and gets a shortening Id.
     * @param url URL to store
     * @return a shortening Id
     */
    public static final synchronized String putUrl(final String url) {
        int urlId = 0;
        final CacheManager cacheMgt = SingletonCacheShortnerURL.getInstance();
        final Cache cache = cacheMgt.getCache(CACHE_NAME);
        do {
            urlId = generateId();
        } while (cache.isKeyInCache(urlId));
        cache.put(new Element(urlId, new UrlCache(url)));
        return toBase(urlId, ALPHANUMERIC_ALT);
    }
    
    /**
     * Returns an URL from tits shortening ID.
     * @param shorteningId
     * <p>
     * Raise an IllegalArgumentException when <code>shorteningId</code> is not in the cache
     * </p>
     * @return an URL
     */
    public static final synchronized String getUrl(final String shorteningId) {
        String result;
        final int urlId = fromBase(shorteningId, ALPHANUMERIC_ALT);
        final CacheManager cacheMgt = SingletonCacheShortnerURL.getInstance();
        final Cache cache = cacheMgt.getCache(CACHE_NAME);         
        if (cache.isKeyInCache(urlId)) {
            UrlCache urlCache = (UrlCache) cache.get(urlId).getObjectValue();
            result = urlCache.getUrl();
            urlCache.setNbClick(urlCache.getNbClicks()+1);
            cache.put(new Element(urlId, urlCache));
        } else {
            throw new IllegalArgumentException("Cannot find the record in the cache");
        }
        return result;
    }   
    
    /**
     * Creates cache of the URL and also about its number of access.
     */
    private static class UrlCache implements Serializable {
        private static final long serialVersionUID = 1L;
        /**
         * The URL to bookmark.
         */
        private String url;
        /**
         * The number of access on this URL.
         */
        private int nbClicks;
        
        /**
         * Constructor.
         * @param urlVal URL
         */
        public UrlCache(final String urlVal) {
          this(urlVal, 0);  
        }
        /**
         * Constructor.
         * @param urlVal URL
         * @param nbClicksVal  number of clicks that has been done on this URL.
         */
        public UrlCache(final String urlVal, final int nbClicksVal) {
            this.url = urlVal;
            this.nbClicks = nbClicksVal;
        }
        
        /**
         * Sets the url.
         * @param urlVal url 
         */
        public final void setUrl(final String urlVal) {
            this.url = urlVal;
        }
        /**
         * Returns the url.
         * @return the url
         */
        public final String getUrl() {
            return this.url;
        }
        /**
         * Sets the number of clicks
         * @param nbClickVal the number of clicks
         */
        public final void setNbClick(final int nbClickVal) {
            this.nbClicks = nbClickVal;
        }
        /**
         * Returns the number of  clicks.
         * @return the number of clicks
         */
        public final int getNbClicks() {
            return this.nbClicks;
        }                
    }
}
