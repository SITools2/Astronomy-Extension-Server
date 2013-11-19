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
 *****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.application.opensearch.processing;

import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess.CacheStrategy;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.Utility;
import java.util.logging.Logger;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 * Puts the VO result from the server in a cache.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class PutInCacheIfNotDecorator extends VORequestDecorator {

    /**
     * Application ID.
     */
    private String applicationID;
    /**
     * Helapix order.
     */
    private int order;
    /**
     * Healpix pixel.
     */
    private long healpix;
    /**
     * Cache control.
     */
    private CacheStrategy cacheControl;
    /**
     * Healpix Coordinates system.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystem;
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(PutInCacheIfNotDecorator.class.getName());

    /**
     * Constructor.
     *
     * @param decorateVORequestVal VO response to cache.
     * @param applicationIDVal applicationID
     * @param orderVal Helapix order
     * @param healpixVal Healpix pixel
     * @param coordinateSystemVal Healpix coordinate system
     * @param cacheControlVal Cache strategy
     */
    public PutInCacheIfNotDecorator(final VORequestInterface decorateVORequestVal, final String applicationIDVal, final int orderVal, final long healpixVal, final AstroCoordinate.CoordinateSystem coordinateSystemVal, final CacheStrategy cacheControlVal) {
        super(decorateVORequestVal);
        setApplicationID(applicationIDVal);
        setOrder(orderVal);
        setHealpix(healpixVal);
        setCacheControl(cacheControlVal);
        setCoordinateSystem(coordinateSystemVal);
    }

    /**
     * Returns the applicationID.
     *
     * @return the applicationID
     */
    protected final String getApplicationID() {
        return applicationID;
    }

    /**
     * Sets the applicationID.
     *
     * @param applicationIDVal the applicationID to set
     */
    protected final void setApplicationID(final String applicationIDVal) {
        this.applicationID = applicationIDVal;
    }

    /**
     * Returns the Healpix order.
     *
     * @return the order
     */
    protected final int getOrder() {
        return order;
    }

    /**
     * Sets the Healpix order.
     *
     * @param orderVal the order to set
     */
    protected final void setOrder(final int orderVal) {
        this.order = orderVal;
    }

    /**
     * Returns the Healpix pixel.
     *
     * @return the healpix
     */
    protected final long getHealpix() {
        return healpix;
    }

    /**
     * Sets the Healpix pixel.
     *
     * @param healpixVal the healpix to set
     */
    protected final void setHealpix(final long healpixVal) {
        this.healpix = healpixVal;
    }

    /**
     * Returns the cache strategy.
     *
     * @return the cache strategy
     */
    protected final CacheStrategy getCacheControl() {
        return this.cacheControl;
    }

    /**
     * Sets the cache control.
     *
     * @param cacheControlVal the cache control
     */
    protected final void setCacheControl(final CacheStrategy cacheControlVal) {
        this.cacheControl = cacheControlVal;
    }

    /**
     * Returns the Helapix coordinate system.
     *
     * @return the coordinateSystem
     */
    protected final AstroCoordinate.CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Sets the Healpix coordinate system.
     *
     * @param coordinateSystemVal the coordinateSystem to set
     */
    protected final void setCoordinateSystem(final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        this.coordinateSystem = coordinateSystemVal;
    }

    @Override
    public final Object getOutput() {
        final Object result = super.getOutput();
        final CacheManager cacheManager = SingletonCacheHealpixDataAccess.getInstance();
        final String cacheID = SingletonCacheHealpixDataAccess.generateId(getApplicationID(), String.valueOf(getOrder()), String.valueOf(getHealpix()), getCoordinateSystem());
        final Cache cache = SingletonCacheHealpixDataAccess.getCache(cacheManager, getCacheControl());
        synchronized (cache) {
            if (Utility.isSet(result) && Utility.isSet(getCacheControl()) && !SingletonCacheHealpixDataAccess.isKeyInCache(cache, cacheID, getCacheControl())) {
                SingletonCacheHealpixDataAccess.putInCache(cache, cacheID, getCacheControl(), result);
            } else if (!Utility.isSet(result)) {
                LOG.severe("Try to put a null value in the cache");
            }
        }
        return result;
    }

}
