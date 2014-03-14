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
package fr.cnes.sitools.extensions.astro.resource;

import healpix.core.HealpixIndex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.astro.representation.GeoJsonRepresentation;
import fr.cnes.sitools.astro.resolver.NameResolverException;
import fr.cnes.sitools.astro.resolver.ReverseNameResolver;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.cache.CacheBrowser;
import fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem;

/**
 * Finds the object's name based on a cone search.
 *
 * <p>
 * This service uses the CDS web service</p>
 *
 * @see ReverseNameResolverResourcePlugin the plugin
 * @see ReverseNameResolver library for reverse name resolver
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ReverseNameResolverResource extends SitoolsParameterizedResource {

    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(ReverseNameResolverResource.class.getName());
    /**
     * Arsec 2 degree conversion.
     */
    private static final double ARCSEC_2_DEG = 1 / 3600.;
    /**
     * MAX radius of a cone in arcsec.
     */
    private static final double MAX_RADIUS = 1800.;
    /**
     * Allowed difference time between two requests.
     */
    private static final long MAX_TIME_MILLISECONDS_BETWEEN_TWO_REQUESTS = 6000;
    /**
     * Positional region where the request must be done.
     */
    private transient String[] coordinates;

    /**
     * Coordinates system.
     */
    private transient CoordinateSystem coordinatesSystem;
    /**
     * Radius in degree of the cone seach.
     */
    private transient double radius;

    /**
     * Initialize the service and checks if the time between now and the last
     * access is superior to 6s. When the time is inferior to 6s, an exception
     * is raised (service is not available)
     */
    @Override
    public final void doInit() {
        super.doInit();
        if (!getRequest().getMethod().equals(Method.OPTIONS)) {
            if (getContext().getAttributes().containsKey("lastRequestToReverseNameResolver")) {
                final long lastTimeMilliSeconds = Long.valueOf(String.valueOf(getContext().getAttributes().get("lastRequestToReverseNameResolver")));
                final long currentTimeMilliSeconds = System.currentTimeMillis();
                if (currentTimeMilliSeconds - lastTimeMilliSeconds >= MAX_TIME_MILLISECONDS_BETWEEN_TWO_REQUESTS) {
                    getContext().getAttributes().put("lastRequestToReverseNameResolver", currentTimeMilliSeconds);
                } else {
                    throw new ResourceException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, "This service can be called once each 6seconds");
                }
            } else {
                getContext().getAttributes().put("lastRequestToReverseNameResolver", System.currentTimeMillis());
            }

            if (this.getRequestAttributes().containsKey("coordinates-order")) {
                final String[] coordinatesOrder = String.valueOf(this.getRequestAttributes().get("coordinates-order")).split(";");
                try {
                    final int order = Integer.valueOf(coordinatesOrder[1]);
                    final double pixRes = HealpixIndex.getPixRes((long) Math.pow(2, order));
                    this.radius = (pixRes > MAX_RADIUS) ? MAX_RADIUS : pixRes / 2;
                    this.radius *= ARCSEC_2_DEG;
                } catch (NumberFormatException ex) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
                }
                final String coordinatesInput = coordinatesOrder[0];
                if (coordinatesInput.contains("+")) {
                    this.coordinates = coordinatesInput.split("\\+");
                    this.coordinates[0] = this.coordinates[0].replace("%20", "");
                    this.coordinates[1] = "+".concat(coordinates[1]);
                } else if (coordinatesInput.contains("-")) {
                    this.coordinates = coordinatesInput.split("-");
                    this.coordinates[0] = this.coordinates[0].replace("%20", "");
                    this.coordinates[1] = "-".concat(coordinates[1]);
                } else {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Bad input parameter: " + coordinatesInput);
                }

                try {
                    this.coordinatesSystem = CoordinateSystem.valueOf(String.valueOf(this.getRequestAttributes().get("coordSystem")));
                } catch (IllegalArgumentException ex) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
                }

            } else {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "missing input : coordinates-order");
            }
        }
    }

    /**
     * Returns the response by the use of a Freemarker template (GeoJson.ftl).
     *
     * <p>
     * The cache directive is set to FOREVER</p>
     *
     * @return the GeoJSON representation
     */
    @Get
    public final Representation getReverseNameResolverResponse() {
        try {
            LOG.finest(String.format("ReverseNameResolver (ra=%s,dec=%s,radius=%s)", coordinates[0], coordinates[1], radius));
            final ReverseNameResolver reverseNameResolver = new ReverseNameResolver(coordinates[0] + " " + coordinates[1], radius, this.coordinatesSystem);
            final Map response = reverseNameResolver.getJsonResponse();
            LOG.finest(String.format("Result of the reverse name resolver:%s", response.toString()));
            Representation rep = new GeoJsonRepresentation(response);
            final CacheBrowser cache = CacheBrowser.createCache(CacheBrowser.CacheDirectiveBrowser.FOREVER, rep);
            rep = cache.getRepresentation();
            getResponse().setCacheDirectives(cache.getCacheDirectives());
            if (fileName != null && !"".equals(fileName)) {
                final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
                disp.setFilename(fileName);
                rep.setDisposition(disp);
            }
            return rep;
        } catch (NameResolverException ex) {
            throw new ResourceException(ex.getStatus(), ex.getMessage());
        }
    }

    /**
     * General WADL description.
     */
    @Override
    public final void sitoolsDescribe() {
        setName("Reverse Name resolver service");
        setDescription("Provides information on a object based on its coordinates");
    }

    /**
     * Describes the GET method in the WADL.
     *
     * @param info infos
     */
    @Override
    protected final void describeGet(final MethodInfo info) {
        this.addInfo(info);
        info.setIdentifier("ReverseNameResolver");
        info.setDocumentation("Gets the object information based on its coordinates.");

        // coordinates/order parameter and coordSystem
        final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
        parametersInfo.add(new ParameterInfo("coordinates-order", true, "String", ParameterStyle.TEMPLATE,
                "coordinates and the Healpix order separated by ; (e.g.00:42:44.31 +41:16:09.4;12)"));
        parametersInfo.add(new ParameterInfo("coordSystem", true, "String", ParameterStyle.TEMPLATE,
                "Coordinate system in which the output is formated"));
        // Set all parameters
        info.getRequest().setParameters(parametersInfo);

        // Response OK
        final ResponseInfo responseOK = new ResponseInfo();
        List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
        RepresentationInfo representationInfo = new RepresentationInfo(MediaType.APPLICATION_JSON);
        final DocumentationInfo doc = new DocumentationInfo();
        doc.setTitle("Reverse Name Resolver representation");
        doc.setTextContent("<pre>{\n"
                + "totalResults=1,\n"
                + "features=[{\n"
                + "  properties={\n"
                + "    title=IC 1515 ,\n"
                + "    magnitude=14.8,\n"
                + "    credits=CDS, \n"
                + "    seeAlso=http://simbad.u-strasbg.fr/simbad/sim-id?Ident=IC 1515 ,\n"
                + "    type=Seyfert_2, \n"
                + "    identifier=IC 1515\n"
                + "  },\n"
                + "  geometry={\n"
                + "    crs=EQUATORIAL.ICRS,\n"
                + "    type=Point, \n"
                + "    coordinates=[23.934419722222223,-0.9884027777777777]\n"
                + "  }\n"
                + "}]\n"
                + "}</pre>");
        representationInfo.setDocumentation(doc);
        representationsInfo.add(representationInfo);
        responseOK.getRepresentations().add(representationInfo);
        responseOK.getStatuses().add(Status.SUCCESS_OK);

        // response bad request, internal error, service unavailable and not found
        representationsInfo = new ArrayList<RepresentationInfo>();
        representationInfo = new RepresentationInfo(MediaType.TEXT_HTML);
        representationInfo.setDocumentation("SITools2 status error page");
        representationsInfo.add(representationInfo);

        final ResponseInfo responseError = new ResponseInfo();
        responseError.getRepresentations().add(representationInfo);
        responseError.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        responseError.setDocumentation("Not foreseen error");

        final ResponseInfo responseBad = new ResponseInfo();
        responseBad.getRepresentations().add(representationInfo);
        responseBad.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
        responseBad.setDocumentation("This error happens when the user input parameters are wrong");

        final ResponseInfo responseNotFound = new ResponseInfo();
        responseNotFound.getRepresentations().add(representationInfo);
        responseNotFound.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
        responseNotFound.setDocumentation("This error happens when no result is returned from the reverse name resolver");

        final ResponseInfo responseUnavailable = new ResponseInfo();
        responseUnavailable.getRepresentations().add(representationInfo);
        responseUnavailable.getStatuses().add(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
        responseUnavailable.setDocumentation("This error happens when the time between two queries is inferior to 6 seconds");

        // Set responses
        final List<ResponseInfo> responseInfo = new ArrayList<ResponseInfo>();
        responseInfo.add(responseOK);
        responseInfo.add(responseError);
        responseInfo.add(responseBad);
        responseInfo.add(responseNotFound);
        responseInfo.add(responseUnavailable);
        info.getResponses().addAll(responseInfo);
    }
}
