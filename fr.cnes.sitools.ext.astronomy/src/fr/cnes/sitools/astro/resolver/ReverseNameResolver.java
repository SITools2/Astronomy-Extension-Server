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
package fr.cnes.sitools.astro.resolver;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsky.coords.DMS;
import jsky.coords.HMS;

import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.resource.ClientResource;

import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeatureDataModel;
import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeaturesDataModel;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem;
import fr.cnes.sitools.util.ClientResourceProxy;
import fr.cnes.sitools.util.Util;

/**
 * Queries the CDS reverse name resolver and returns the object name for a given sky position and a radius. <br/>
 * N.B: The maximal Radius is fixed to 0.5°
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ReverseNameResolver {
  /**
   * Logger.
   */
    private static final Logger LOG = Engine.getLogger(ReverseNameResolver.class.getName());
   /**
    * Template URL for the CDS reverse name resolver service.
    */
    private static final String TEMPLATE_REVERSE_NAME_RESOLVER = "http://alasky.u-strasbg.fr/cgi/simbad-flat/simbad-quick.py?Ident=<coordinates>&SR=<radius>";
    /**
     * input parameter : sky position.
     */
    private String coordinates;
    /**
     * input parameter : radius.
     */
    private double radius;
    /**
     * Coordinates system of both inputs and response.
     */
    private CoordinateSystem coordinatesSystem;
    /**
     * Init data model.
     */
    private final transient FeaturesDataModel features;
    /**
     * Max radius.
     */
    private static final double MAX_RADIUS = 0.5;
    /**
     * Minimum time in seconds to wait to redo another request.
     */
    private static final int RESPONSE_IN_SECONDS_TO_WAIT = 6;
    /**
     * Empty constructor.
     */
    protected ReverseNameResolver() {
        this.features = new FeaturesDataModel();
    }
    /**
     * Creates a ReverseNameResolver instance based on coordinates (ex: 23:42:30.02 -42:34:12.02) and the Healpix order.<br/>
     *
     * <p>The Healpix order allows to fix the radius of the reverse name resolver</p>
     *
     * @param coordinatesVal coordinates
     * @param radiusVal radius in degree of the cone search
     * @param coordinateSystemVal coordinate reference system of both inputs and response
     * @throws NameResolverException
     */
    public ReverseNameResolver(final String coordinatesVal, final double radiusVal, final CoordinateSystem coordinateSystemVal) throws NameResolverException {
        setCoordinates(coordinatesVal);
        setRadius(radiusVal);
        setCoordinatesSystem(coordinateSystemVal);
        this.features = new FeaturesDataModel();
        checkInputParameters();
        process();
    }
  /**
   * Checks if the input parameters are set.
   *
   * <p>
   * Returns a IllegalArgumentException if one of the input parameters is <code>null</code> or empty.
   * </p>
   */
    protected final void checkInputParameters() {
      if (getCoordinates() == null || getCoordinates().isEmpty()) {
        throw new IllegalArgumentException("Coordinates must be set.");
      }
      if (getRadius() <= 0) {
        throw new IllegalArgumentException("Radius must > 0.");
      }
      if (getCoordinatesSystem() == null) {
        throw new IllegalArgumentException("Coordinates system must be set.");
      }
    }
    /**
     * Sets the coordinates.
     * @param coordinatesVal the coordinates
     */
    protected final void setCoordinates(final String coordinatesVal) {
        this.coordinates = coordinatesVal;
    }
    /**
     * Returs the coordinates.
     * @return the coordinates
     */
    protected final String getCoordinates() {
        return this.coordinates;
    }
    /**
     * Sets the radius.
     * <p>
     * when the radius is > MAX_RADIUS then radius = MAX_RADIUS
     * </p>
     * @param radiusVal the radius
     */
    protected final void setRadius(final double radiusVal) {
        this.radius = (radiusVal > MAX_RADIUS) ? MAX_RADIUS : radiusVal;
    }
    /**
     * Returns the radius.
     * @return the radius
     */
    protected final double getRadius() {
        return this.radius;
    }
    /**
     * Sets the coordinate system.
     * @param coordinateSystemVal the coordinate system
     */
    protected final void setCoordinatesSystem(final CoordinateSystem coordinateSystemVal) {
        this.coordinatesSystem = coordinateSystemVal;
    }
    /**
     * Returns the coordinate system.
     * @return the coordinate system
     */
    protected final CoordinateSystem getCoordinatesSystem() {
        return this.coordinatesSystem;
    }

    /**
     * Builds the query and creates the response.
     *
     * @throws NameResolverException
     */
    private void process() throws NameResolverException {
        // we convert in galactic because the service only accept equatorial
        if (this.coordinatesSystem == CoordinateSystem.GALACTIC) {
            final String[] coordinatesStr = coordinates.split(" ");
            final AstroCoordinate astroCoordinate = new AstroCoordinate(coordinatesStr[0], coordinatesStr[1]);
            astroCoordinate.transformTo(CoordinateSystem.EQUATORIAL);
            this.coordinates = astroCoordinate.getRaAsSexagesimal() + " " + astroCoordinate.getDecAsSexagesimal();
        }
        // building the query
        final String serviceToQueryTmp = TEMPLATE_REVERSE_NAME_RESOLVER.replace("<coordinates>", coordinates);
        final String serviceToQuery = serviceToQueryTmp.replace("<radius>", String.valueOf(radius));
        LOG.log(Level.INFO, "Call reverse name resolver: {0}", serviceToQuery);
        
        // requesting
        final ClientResourceProxy client = new ClientResourceProxy(serviceToQuery, Method.GET);
        final ClientResource clientResource = client.getClientResource();
        final Status status = clientResource.getStatus();

        // when the request is fine then we process the response
        if (status.isSuccess()) {
            try {
                final String response = clientResource.get().getText();
                if (response == null) {
                    // Empty message case
                    throw new NameResolverException(Status.CLIENT_ERROR_NOT_FOUND, "Object not found");
                } else {
                    // we parse the message that is returned by the server
                    final int posParenthesis = response.indexOf('(');
                    final int posComma = response.indexOf(',');
                    final int posSlash = response.indexOf('/');
                    final String position = response.substring(0, posSlash);
                    final String name = response.substring(posSlash + 1, posParenthesis);
                    Double magnitude;
                    try {
                        magnitude = Double.valueOf(response.substring(posParenthesis + 1, posComma));
                    } catch (NumberFormatException ex) {
                        LOG.log(Level.FINER, null, ex);
                        magnitude = Double.NaN;
                    }
                    final String objectType = response.substring(posComma + 1, response.length() - 2);

                    final String[] positionElts = position.split(" ");
                    // The CDS server could return position with seconds or without second term.
                    HMS hms;
                    DMS dms;
                    if (positionElts.length == RESPONSE_IN_SECONDS_TO_WAIT) {
                        hms = new HMS(String.format("%s:%s:%s", positionElts[0], positionElts[1], positionElts[2]));
                        dms = new DMS(String.format("%s:%s:%s", positionElts[3], positionElts[4], positionElts[5]));
                    } else {
                        hms = new HMS(String.format("%s:%s:%s", positionElts[0], positionElts[1],0));
                        dms = new DMS(String.format("%s:%s:%s", positionElts[2], positionElts[3],0));
                    }

                    // we are building the data model for the response
                    final FeatureDataModel feature = new FeatureDataModel(name);
                    feature.addProperty("title", name);
                    feature.addProperty("credits", "CDS");
                    if (Util.isSet(magnitude) && !magnitude.isNaN()) {
                        feature.addProperty("magnitude", magnitude);
                    }
                    feature.addProperty("type", objectType);
                    feature.addProperty("seeAlso", "http://simbad.u-strasbg.fr/simbad/sim-id?Ident=" + name);
                    final AstroCoordinate astroCoordinate = new AstroCoordinate(hms.toString(true), dms.toString(true));
                    feature.createGeometry(String.format("[%s,%s]", astroCoordinate.getRaAsDecimal(), astroCoordinate.getDecAsDecimal()), "Point");
                    feature.createCrs(this.coordinatesSystem.getCrs());
                    features.addFeature(feature);
                }
            } catch (IOException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            }
        } else {
            throw new NameResolverException(status, status.getThrowable());
        }
    }
    /**
     * Get the response from the reverse name resolver. The format of the
     * response is the following :      {@code
     * {
     *   totalResults=1,
     *   features=[{
     *      geometry={
     *          type=Point,
     *          coordinates=[23.934419722222223,-0.9884027777777777]
     *      },
     *      properties={
     *         "crs": {
     *              "type": "name",
     *              "properties": {
     *                  "name": "equatorial.ICRS"
     *              }
     *          },
     *          title=IC 1515 ,
     *          magnitude=14.8,
     *          credits=CDS,
     *          seeAlso=http://simbad.u-strasbg.fr/simbad/sim-id?Ident=IC 1515 ,
     *          type=Seyfert_2,
     *          identifier=IC 1515
     *      }
     * }] } }
     *
     * @return response
     */
    public final Map getJsonResponse() {
        return Collections.unmodifiableMap(this.features.getFeatures());
    }
}
