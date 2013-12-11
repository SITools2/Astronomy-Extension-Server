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
package fr.cnes.sitools.extensions.astro.application.opensearch;

import fr.cnes.sitools.searchgeometryengine.CoordSystem;
import fr.cnes.sitools.solr.query.AbstractSolrQueryRequestFactory;
import healpix.essentials.Scheme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Provides a search capability on observations by the use of (healpix,order) parameters.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchClusterSearch extends OpenSearchSearch {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OpenSearchClusterSearch.class.getName());

  @Get
  @Override
  public final Representation getJsonResponse() {
    try {
      final String referenceSystem = getPluginParameters().get("referenceSystem").getValue();
      final CoordSystem coordSystem = referenceSystem.equals("geocentric") ? CoordSystem.GEOCENTRIC : CoordSystem.EQUATORIAL;
      final String healpixSchemeParam = getPluginParameters().get("healpixScheme").getValue();
      final Scheme healpixScheme = Scheme.valueOf(healpixSchemeParam);
      final AbstractSolrQueryRequestFactory querySolr = AbstractSolrQueryRequestFactory.createInstance(this.getQueryParameters(), coordSystem, getSolrBaseUrl(), healpixScheme);
      querySolr.createQueryBuilder();
      String query = querySolr.getSolrQueryRequest();
      query = query.concat("&rows=0&facet=true&facet.limit=-1&facet.mincount=1&wt=json"
              + "&indent=true&facet.field=order3&facet.field=order4"
              + "&facet.field=order5&facet.field=order6&facet.field=order7"
              + "&facet.field=order8&facet.field=order9&facet.field=order10"
              + "&facet.field=order11&facet.field=order12&facet.field=order13");
      LOG.log(Level.INFO, query);
      final ClientResource client = new ClientResource(query);
      final Representation rep = client.get();
      final JsonRepresentation json = new JsonRepresentation(rep.getText());
      json.setIndenting(true);
      return json;
    } catch (Exception ex) {
      Logger.getLogger(OpenSearchSearch.class.getName()).log(Level.SEVERE, null, ex);
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
    }
  }
  
  @Override
  public final void sitoolsDescribe() {
    setName("Observations service.");
    setDescription("Retrieves and transforms observations from SOLR.");
  }
  
  

  /**
   * Describes GET method in the WADL.
   *
   * @param info information
   */
  @Override
  protected final void describeGet(final MethodInfo info) {  
    this.addInfo(info);
    info.setIdentifier("ObservationsJSON");
    info.setDocumentation("Service to distribute observations from SOLR");

    final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo("healpix", true, "long", ParameterStyle.QUERY,
            "Healpix number"));
    parametersInfo.add(new ParameterInfo("order", true, "integer", ParameterStyle.QUERY,
            "Healpix order"));
    final ParameterInfo json = new ParameterInfo("format", true, "string", ParameterStyle.QUERY, "JSON format");
    json.setFixed("json");
    parametersInfo.add(json);

    info.getRequest().setParameters(parametersInfo);
    
    // represensation when the response is fine
    final ResponseInfo responseOK = new ResponseInfo();    

    final DocumentationInfo documentation = new DocumentationInfo();
    documentation.setTitle("Observations service");
    documentation.setTextContent("Services on observations as JSON");

    final List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    final RepresentationInfo representationInfo = new RepresentationInfo(MediaType.APPLICATION_JSON);
    representationInfo.setDocumentation(documentation);    
    representationsInfo.add(representationInfo);
    responseOK.setRepresentations(representationsInfo);
    responseOK.getStatuses().add(Status.SUCCESS_OK);

    // represensation when the response is not fine
    final ResponseInfo responseNOK = new ResponseInfo();
    final RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
    final DocumentationInfo documentationHTML = new DocumentationInfo();
    documentationHTML.setTitle("Error");
    documentationHTML.setTextContent("Returns the error.");
    representationInfoError.setDocumentation(documentationHTML);

    responseNOK.getRepresentations().add(representationInfoError);
    responseNOK.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
    responseNOK.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);

    info.setResponses(Arrays.asList(responseOK, responseNOK));
  }  
}
