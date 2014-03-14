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
package fr.cnes.sitools.extensions.astro.application.opensearch.processing;

import fr.cnes.sitools.extensions.astro.application.OpenSearchVOApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.opensearch.responsibility.AbstractVORequest;
import fr.cnes.sitools.extensions.astro.application.opensearch.responsibility.ConeSearchHealpix;
import fr.cnes.sitools.extensions.astro.application.opensearch.responsibility.RetrieveFromCache;
import fr.cnes.sitools.extensions.astro.application.opensearch.responsibility.SiaHealpix;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess.CacheStrategy;
import fr.cnes.sitools.extensions.common.AstroCoordinate;

/**
 * Queries the VO service.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class VORequest implements VORequestInterface {
    /**
     * URL of the VO service.
     */
    private String url;
    /**
     * Healpix order.
     */
    private int order;
    /**
     * Healpix pixel.
     */
    private long healpix;
    /**
     * Healpix coordinate system.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystem;
    /**
     * Application ID.
     */
    private String applicationID;
    /**
     * Protocol.
     */
    private OpenSearchVOApplicationPlugin.Protocol protocol;
    /**
     * Cache strategy.
     */
    private CacheStrategy cacheControl;

    /**
     * Constructor.
     * @param applicationIDVal applicationID
     * @param urlVal URL of the VO service
     * @param orderVal Healpix order
     * @param healpixVal Healpix pixel
     * @param coordinateSystemVal Healpix coordinate system
     * @param protocolVal protocol
     * @param cacheControlVal Cache strategy
     */
    public VORequest(final String applicationIDVal, final String urlVal, final int orderVal, final long healpixVal, final AstroCoordinate.CoordinateSystem coordinateSystemVal, final OpenSearchVOApplicationPlugin.Protocol protocolVal, final CacheStrategy cacheControlVal) {
        setUrl(urlVal);
        setOrder(orderVal);
        setHealpix(healpixVal);
        setCoordinateSystem(coordinateSystemVal);
        setApplicationID(applicationIDVal);
        setProtocol(protocolVal);
        setCacheControl(cacheControlVal);
    }

    @Override
    public final Object getOutput() {
        final AbstractVORequest cache = new RetrieveFromCache(getApplicationID(), getOrder(), getHealpix(), getCoordinateSystem(), getCacheControl());
        final AbstractVORequest cs = new ConeSearchHealpix(getUrl(), getOrder(), getHealpix(), getCoordinateSystem());
        final AbstractVORequest sia = new SiaHealpix(getUrl(), getOrder(), getHealpix(), getCoordinateSystem());
        switch(getProtocol()) {
            case DETECT_AUTOMATICALLY:
                cache.setNext(cs);
                cs.setNext(sia);
                break;
            case CONE_SEARCH_PROTOCOL:
                cache.setNext(cs);
                break;
            case SIMPLE_IMAGE_ACCESS_PROTOCOL:
                cache.setNext(sia);
                break;
            default:
                throw new IllegalAccessError(getProtocol() + " is not supported");
        }
        return cache.getResponse();
    }

    /**
     * Returns the VO service URL.
     * @return the url
     */
    protected final String getUrl() {
        return url;
    }

    /**
     * Sets the VO service URL.
     * @param urlVal the url to set
     */
    protected final void setUrl(final String urlVal) {
        this.url = urlVal;
    }

    /**
     * Returns the Helapix order.
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
     * @return the healpix
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
    /**
     * Returns the applicationID.
     * @return the applicationID
     */
    protected final String getApplicationID() {
        return this.applicationID;
    }
    /**
     * Sets the application ID.
     * @param applicationIDVal the application ID
     */
    protected final void setApplicationID(final String applicationIDVal) {
        this.applicationID = applicationIDVal;
    }
    /**
     * Returns the cache strategy.
     * @return the cache strategy
     */
    protected final CacheStrategy getCacheControl() {
        return cacheControl;
    }

    /**
     * Sets the cache strategy.
     * @param cacheControlVal the cache strategy to set
     */
    protected final void setCacheControl(final CacheStrategy cacheControlVal) {
        this.cacheControl = cacheControlVal;
    }

    /**
     * Returns the protocol.
     * @return the protocol
     */
    protected final OpenSearchVOApplicationPlugin.Protocol getProtocol() {
        return protocol;
    }

    /**
     * Sets the protocol.
     * @param protocolVal the protocol to set
     */
    protected final void setProtocol(final OpenSearchVOApplicationPlugin.Protocol protocolVal) {
        this.protocol = protocolVal;
    }
}
