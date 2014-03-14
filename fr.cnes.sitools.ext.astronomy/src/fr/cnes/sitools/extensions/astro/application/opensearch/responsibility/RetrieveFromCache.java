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
package fr.cnes.sitools.extensions.astro.application.opensearch.responsibility;

import static fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess.getFromCache;
import static fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess.isKeyInCache;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ivoa.xml.votable.v1.Field;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.restlet.engine.Engine;

import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess.CacheStrategy;
import fr.cnes.sitools.extensions.common.AstroCoordinate;

/**
 * Retrieves an element from the cache based on the cacheID.
 *
 * <p>
 * This implementation is designed by a chain of responsability pattern.<br/>
 * The cacheID is computed according to <code>applicationID</code>, <code>order</code>,
 * <code>healpix</code> and <code>coordinateSystem</code> parameters.
 * </p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class RetrieveFromCache extends AbstractVORequest {
    /**
     * Application ID.
     */
    private String applicationID;
    /**
     * Healpix order.
     */
    private int order;
    /**
     * Healpix pixel.
     */
    private long healpix;
    /**
     * Coordinate system of the Healpix pixel.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystem;
    /**
     * Cache strategy.
     */
    private CacheStrategy cacheControl;
    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(RetrieveFromCache.class.getName());

    /**
     * Empty constructor.
     */
    protected RetrieveFromCache() {        
    }
    /**
     * Constructor.
     * @param applicationIDVal applicationID
     * @param orderVal Healpix order
     * @param healpixVal Healpix pixel
     * @param coordinateSystemVal Healpix pixel coordinate system
     * @param cacheControlVal Cache strategy
     */
    public RetrieveFromCache(final String applicationIDVal, final int orderVal, final long healpixVal, final AstroCoordinate.CoordinateSystem coordinateSystemVal, final CacheStrategy cacheControlVal) {
        setApplicationID(applicationIDVal);
        setOrder(orderVal);
        setHealpix(healpixVal);
        setCoordinateSystem(coordinateSystemVal);
        setCacheControl(cacheControlVal);
        LOG.log(Level.CONFIG, "ApplicationID:", applicationIDVal);
        LOG.log(Level.CONFIG, "Order:", orderVal);
        LOG.log(Level.CONFIG, "Healpix:", healpixVal);
        LOG.log(Level.CONFIG, "Coordinate system:", coordinateSystemVal.name());
        LOG.log(Level.CONFIG, "Cache control", cacheControlVal.getName());
    }

    @Override
    public final Object getResponse() {
        Object responseCache;
        final String cacheID = SingletonCacheHealpixDataAccess.generateId(getApplicationID(),
                                                                          String.valueOf(getOrder()),
                                                                          String.valueOf(getHealpix()),
                                                                          getCoordinateSystem());        
        final CacheManager cacheManager = SingletonCacheHealpixDataAccess.getInstance();
        final Cache cache = SingletonCacheHealpixDataAccess.getCache(cacheManager, getCacheControl());
        if (isKeyInCache(cache, cacheID, getCacheControl())) {
            LOG.log(Level.INFO, "Cache is used in VO request for :", cacheID);
            final List<Map<Field, String>> responseInCache = (List<Map<Field, String>>) getFromCache(cache, cacheID, getCacheControl());
            responseCache = responseInCache;
        } else if (getSuccessor() == null) {
            responseCache = null;
        } else {
            responseCache = getSuccessor().getResponse();
        }
        return responseCache;
    }

    /**
     * Returns the application ID.
     * @return the applicationID
     */
    protected final String getApplicationID() {
        return applicationID;
    }

    /**
     * Sets the application ID.
     * @param applicationIDVal the applicationID to set
     */
    protected final void setApplicationID(final String applicationIDVal) {
        this.applicationID = applicationIDVal;
    }

    /**
     * Returns the Heapix order.
     * @return the order
     */
    protected final int getOrder() {
        return order;
    }

    /**
     * Sets the Healpix order.
     * @param orderVal the order to set
     */
    protected final void setOrder(final int orderVal) {
        this.order = orderVal;
    }

    /**
     * Returns the Healpix pixel.
     * @return the healpix pixel
     */
    protected final long getHealpix() {
        return healpix;
    }

    /**
     * Sets the Healpix pixel.
     * @param healpixVal the healpix to set
     */
    protected final void setHealpix(final long healpixVal) {
        this.healpix = healpixVal;
    }
    /**
     * Returns the cache strategy.
     * @return the cache strategy
     */
    protected final CacheStrategy getCacheControl() {
        return this.cacheControl;
    }
    /**
     * Sets the cache strategy.
     * @param cacheControlVal the cache strategy
     */
    protected final void setCacheControl(final CacheStrategy cacheControlVal) {
        this.cacheControl = cacheControlVal;
    }

    /**
     * Returns the Healpix coordinate system.
     * @return the coordinateSystem
     */
    protected final AstroCoordinate.CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Sets the Healpix coordinate system.
     * @param coordinateSystemVal the coordinateSystem to set
     */
    protected final void setCoordinateSystem(final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        this.coordinateSystem = coordinateSystemVal;
    }
}
