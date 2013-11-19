/**
 * *****************************************************************************
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
 * ***************************************************************************
 */
package fr.cnes.sitools.extensions.cache;

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.Utility;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.restlet.data.LocalReference;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * Singleton for server cache based on Healpix.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SingletonCacheHealpixDataAccess {

    /**
     * Cache configuration.
     */
    private final transient Representation cacheConf = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/ehcache.xml").get();
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(SingletonCacheHealpixDataAccess.class.getName());

    /**
     * Constructs a cache.
     */
    protected SingletonCacheHealpixDataAccess() {
        try {
            CacheManager.create(cacheConf.getStream());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Creates the cache.
     */
    public static void create() {
        new SingletonCacheHealpixDataAccess();
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
     * Creates an unique identifier for Healpix application.
     *
     * @param applicationId application ID
     * @param order order
     * @param healpixNumber healpix number
     * @param coordinateSystem Coordinate system
     * @return the unique identifier
     */
    public static String generateId(final String applicationId, final String order, final String healpixNumber, final AstroCoordinate.CoordinateSystem coordinateSystem) {
        return String.format("%s#%s#%s#%s", applicationId, order, healpixNumber, coordinateSystem.name());
    }

    /**
     * Returns the cache according to the
     * <code>cacheStrategy</code>.
     *
     * @param cacheManager cache manager
     * @param cacheStrategy the cache strategy
     * @return the cache
     */
    public static Cache getCache(final CacheManager cacheManager, final CacheStrategy cacheStrategy) {
        Cache cache;
        switch (cacheStrategy) {
            case CACHE_ENABLE_SOLAR_OBJECT:
                cache = cacheManager.getCache("VOservices#solarBodies");
                break;
            case CACHE_ENABLE_DEEP_OBJECT:
                cache = cacheManager.getCache("VOservices");
                break;
            case CACHE_ENABLE_IMAGE_BACKGROUND:
                cache = cacheManager.getCache("healpixImage");
                break;
            default:
                throw new java.util.NoSuchElementException(
                        "This cache strategy " + cacheStrategy
                        + " is not yet implemented");
        }
        return cache;
    }

    /**
     * Returns
     * <code>true</code> when the key
     * <code>cacheID</code> is in the cache otherwise
     * <code>false</code>.
     *
     * @param cacheID key to check
     * @param cacheStrategy cache strategy
     * @return Returns <code>true</code> when the key <code>cacheID</code> is in
     * the cache otherwise <code>false</code>
     */
    public static boolean isKeyInCache(final Cache cache, final String cacheID, final CacheStrategy cacheStrategy) {
        boolean result;
        if (Utility.isSet(cacheStrategy)) {
            result = cache.isKeyInCache(cacheID);
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Returns the stored value from the cache.
     *
     * @param cache cache
     * @param cacheID ID to extract from the cache
     * @param cacheStrategy cache strategy
     * @return the stored value from the cache
     */
    public static Object getFromCache(final Cache cache, final String cacheID, final CacheStrategy cacheStrategy) {
        LOG.log(Level.FINER, "cacheID: {0} - strategy:{1}", new Object[]{cacheID, cacheStrategy.getName()});
        return cache.get(cacheID).getObjectValue();
    }

    /**
     * Inserts the
     * <code>valueToStore</code> in cache.
     *
     * @param cache cache
     * @param cacheID ID of the key in the cache.
     * @param cacheStrategy cache strategy
     * @param valueToStore value to store in the the cache
     */
    public static void putInCache(final Cache cache, final String cacheID, final CacheStrategy cacheStrategy, final Object valueToStore) {
        final Element element = new Element(cacheID, valueToStore);
        cache.put(element);
    }

    /**
     * Cache strategy.
     */
    public enum CacheStrategy {

        /**
         * Cache for static objects.
         */
        CACHE_ENABLE_DEEP_OBJECT("VOservices"),
        /**
         * Cache for solar objects.
         */
        CACHE_ENABLE_SOLAR_OBJECT("VOservices#solarBodies"),
        /**
         * Cache for background images.
         */
        CACHE_ENABLE_IMAGE_BACKGROUND("healpixImage");
        /**
         * Cache name.
         */
        private final String name;

        /**
         * Constructs a cache strategy.
         *
         * @param nameVal cache name.
         */
        CacheStrategy(final String nameVal) {
            this.name = nameVal;
        }

        /**
         * Returns the cache name to use.
         *
         * @return the cache name to use
         */
        public final String getName() {
            return this.name;
        }
    }
}
