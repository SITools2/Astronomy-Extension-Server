 /*******************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem;
import fr.cnes.sitools.util.ClientResourceProxy;
import fr.cnes.sitools.util.Util;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jsky.coords.DMS;
import jsky.coords.HMS;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

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
    private static final Logger LOG = Logger.getLogger(ReverseNameResolver.class.getName());
   /**
    * Template URL for the CDS reverse name resolver service.
    */
    private static final String TEMPLATE_REVERSE_NAME_RESOLVER = "http://alasky.u-strasbg.fr/cgi/simbad-flat/simbad-quick.py?Ident=<coordinates>&SR=<radius>";
    /**
     * input parameter : sky position.
     */
    private transient String coordinates;
    /**
     * input parameter : radius.
     */
    private final transient double radius;
    
    /**
     * Coordinates system of both inputs and response.
     */    
    private final transient CoordinateSystem coordinatesSystem;
    /**
     * Init the data model.
     */
    private final transient Map dataModel = new HashMap();
    /**
     * Max radius.
     */
    private static final double MAX_RADIUS = 0.5;
    /**
     * Minimum time in seconds to wait to redo another request. 
     */
    private static final int RESPONSE_IN_SECONDS_TO_WAIT = 6;

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
        this.coordinates = coordinatesVal;
        this.radius = (radiusVal > MAX_RADIUS) ? MAX_RADIUS : radiusVal;
        this.coordinatesSystem = coordinateSystemVal;
        process();
    }

    /**
     * Builds the query and creates the response.
     *
     * @throws NameResolverException
     */
    private void process() throws NameResolverException {
        // we convert in galactic because the service only accept equatorial
        if (this.coordinatesSystem == CoordinateSystem.GALACTIC) {
            String[] coordinatesStr = coordinates.split(" ");
            AstroCoordinate astroCoordinate = new AstroCoordinate(coordinatesStr[0], coordinatesStr[1]);            
            astroCoordinate.transformTo(CoordinateSystem.EQUATORIAL);
            this.coordinates = astroCoordinate.getRaAsSexagesimal() + " " + astroCoordinate.getDecAsSexagesimal();
        }
        
        // building the query
        String serviceToQuery = TEMPLATE_REVERSE_NAME_RESOLVER.replace("<coordinates>", coordinates);
        serviceToQuery = serviceToQuery.replace("<radius>", String.valueOf(radius));

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
                    Double magnitude = null;
                    try {
                        magnitude = Double.valueOf(response.substring(posParenthesis + 1, posComma));
                    } catch (NumberFormatException ex) {
                        LOG.log(Level.FINER, null, ex);
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
                    final Map feature = new HashMap();

                    final Map properties = new HashMap();
                    properties.put("identifier", name);
                    properties.put("title", name);
                    properties.put("credits", "CDS");
                    if (Util.isSet(magnitude)) {
                        properties.put("magnitude", magnitude);
                    }
                    properties.put("type", objectType);
                    properties.put("seeAlso", "http://simbad.u-strasbg.fr/simbad/sim-id?Ident=" + name);
                    feature.put("properties", properties);

                    final Map geometry = new HashMap();
                    geometry.put("type", "Point");
                    final AstroCoordinate astroCoordinate = new AstroCoordinate(hms.toString(true), dms.toString(true));
                    geometry.put("coordinates", String.format("[%s,%s]", astroCoordinate.getRaAsDecimal(), astroCoordinate.getDecAsDecimal()));
                    geometry.put("crs", this.coordinatesSystem.getCrs());
                    feature.put("geometry", geometry);

                    dataModel.put("features", Arrays.asList(feature));
                    dataModel.put("totalResults", 1);
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
     *      properties={
     *          title=IC 1515 ,
     *          magnitude=14.8,
     *          credits=CDS,
     *          seeAlso=http://simbad.u-strasbg.fr/simbad/sim-id?Ident=IC 1515 ,
     *          type=Seyfert_2,
     *          identifier=IC 1515
     *      },
     *      geometry={
     *          crs=EQUATORIAL.ICRS,
     *          type=Point,
     *          coordinates=[23.934419722222223,-0.9884027777777777]
     *      }
     * }] } }
     *
     * @return response
     */
    public final Map getJsonResponse() {
        return Collections.unmodifiableMap(this.dataModel);
    }    
}
