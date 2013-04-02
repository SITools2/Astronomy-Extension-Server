/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.astro.vo.conesearch.ConeSearchSolarObjectQuery;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.common.CacheBrowser;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.CacheDirective;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Queries the skybot service from IMCCE and returns the result.
 *
 * <p>The query is based on Healpix (healpix number + order) plus a given time. The result is a GeoJSON representation.</p> <p> Here is an
 * example of the response:<br/>
 * <pre>
 * <code>
 * {
 *  totalResults: 1,
 *  type: "FeatureCollection",
 *  features: ["
 *    geometry: {
 *      coordinates: [10.6847083,41.26875],
 *      type: "Point"
 *    },
 *    properties: {
 *      crs: {
 *        type: "name",
 *        properties: {
 *           name: "EQUATORIAL.ICRS"
 *        }
 *      },
 *      credits: "IMCCE",
 *      identifier: "CDS0"
 *   }
 *}]}
 * </code>
 * </pre>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConeSearchSolarObjectResource extends SitoolsParameterizedResource {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ConeSearchSolarObjectResource.class.getName());
  /**
   * Healpix input parameter.
   */
  private String healpix;
  /**
   * Order input parameter.
   */
  private String order;
  /**
   * Time input parameter.
   */
  private String time;
  /**
   * The IMCCE Object.
   */
  private ConeSearchSolarObjectQuery query;

  /**
   * Gets the input parameters and create the IMCCE object.
   */
  @Override
  public final void doInit() {
    this.healpix = getRequest().getResourceRef().getQueryAsForm().getFirstValue("healpix");
    this.order = getRequest().getResourceRef().getQueryAsForm().getFirstValue("order");
    this.time = getRequest().getResourceRef().getQueryAsForm().getFirstValue("EPOCH", "now");
    if (!getRequest().getMethod().equals(Method.OPTIONS)) {
      try {
        query = new ConeSearchSolarObjectQuery(Long.valueOf(healpix), Integer.valueOf(order), time);
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, null, ex);
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    }
  }   

  /**
   * Returns the SkyBot response in GeoJSON.
   * 
   * <p>
   * The cache directive for the browser is set to N0_CACHE.
   * </p>
   *
   * @return the SkyBot response in GeoJSON
   */
  @Get
  public final Representation getSolarObjectsResponse() {
    try {
      Representation rep = this.query.getResponse(); 
      
      CacheBrowser cache = CacheBrowser.createCache(CacheBrowser.CacheDirectiveBrowser.NO_CACHE, rep);
      rep = cache.getRepresentation();
      getResponse().setCacheDirectives(cache.getCacheDirectives()); 
      
      if (fileName != null && !"".equals(fileName)) {
        Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
        disp.setFilename(fileName);
        rep.setDisposition(disp);
      }
      return rep;
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
    }
  }

  /**
   * General WADL description.
   */
  @Override
  public final void sitoolsDescribe() {
    setName("Solar objects search");
    setDescription("Provides a cone search to get solar objects in a given time by the use of SkyBot service from IMCCE.");
  }

  /**
   * Describes the Get method.
   *
   * @param info info
   */
  @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("SolarObjects");
    info.setDocumentation("Get solar objects from a cone");

    // query parameter
    List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo("healpix", true, "Long", ParameterStyle.QUERY,
            "Helapix index"));
    parametersInfo.add(new ParameterInfo("order", true, "Integer", ParameterStyle.QUERY,
            "Helapix order"));
    ParameterInfo epoch = new ParameterInfo("EPOCH", false, "String", ParameterStyle.QUERY,
            "Search at a time");
    epoch.setDefaultValue("now");
    parametersInfo.add(epoch);

    // Set all parameters
    info.getRequest().setParameters(parametersInfo);

    // Response OK
    ResponseInfo responseOK = new ResponseInfo();
    List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    RepresentationInfo representationInfo = new RepresentationInfo(MediaType.APPLICATION_JSON);
    DocumentationInfo doc = new DocumentationInfo();
    doc.setTitle("Name Resolver representation");
    doc.setTextContent("<pre>{\n"
            + "totalResults: 1,\n"
            + "type: \"FeatureCollection\",\n"
            + "features: [\n"
            + "  geometry: {\n"
            + "    coordinates: [10.6847083,41.26875],\n"
            + "    type: \"Point\"\n"
            + "  },\n"
            + "properties: {\n"
            + "  crs: {\n"
            + "    type: \"name\",\n"
            + "    properties: {\n"
            + "      name: \"EQUATORIAL.ICRS\"\n"
            + "    }\n"
            + "  },\n"
            + "  credits: \"IMCCE\",\n"
            + "  identifier: \"CDS0\"\n"
            + "}\n"
            + "}]}</pre>");
    representationInfo.setDocumentation(doc);
    representationsInfo.add(representationInfo);
    responseOK.getRepresentations().add(representationInfo);
    responseOK.getStatuses().add(Status.SUCCESS_OK);

    // response bad request and internal error
    representationsInfo = new ArrayList<RepresentationInfo>();
    representationInfo = new RepresentationInfo(MediaType.TEXT_HTML);
    representationInfo.setDocumentation("SITools2 status error page");
    representationsInfo.add(representationInfo);

    ResponseInfo responseError = new ResponseInfo();
    responseError.getRepresentations().add(representationInfo);
    responseError.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
    responseError.setDocumentation("This error may happen when the response is being writting or when the name resolver is unknown");

    ResponseInfo responseNotFound = new ResponseInfo();
    responseNotFound.getRepresentations().add(representationInfo);
    responseNotFound.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
    responseNotFound.setDocumentation("No data found");

    // Set responses
    List<ResponseInfo> responseInfo = new ArrayList<ResponseInfo>();
    responseInfo.add(responseOK);
    responseInfo.add(responseError);
    responseInfo.add(responseNotFound);
    info.getResponses().addAll(responseInfo);
  }
}
