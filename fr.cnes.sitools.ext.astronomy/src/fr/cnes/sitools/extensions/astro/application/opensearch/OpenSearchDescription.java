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

import fr.cnes.sitools.astro.representation.OpenSearchDescriptionRepresentation;
import fr.cnes.sitools.extensions.astro.application.OpenSearchApplicationPlugin;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Open search description.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchDescription extends OpenSearchBase {
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OpenSearchDescription.class.getName());

  /**
   * Data model.
   */
  private final transient Map dataModel = new HashMap();

  @Override
  public final void doInit() {
    super.doInit();
  }

  /**
   * builds and returns the template URL.
   *
   * @return the template
   * @throws JSONException Exception
   * @throws IOException Exception
   */
  private String buildTemplateURL() throws JSONException, IOException {
    final String description = "%s/search?q={searchTerms}&amp;startPage={startPage?}&amp;startIndex={startIndex?}&amp;count={count?}&amp;%s&amp;format=json%s";
    String fields = "";
    final List<Index> indexedFields = getIndexedFields();
    for (Index indexedField : indexedFields) {
      if (!indexedField.getTopTerms().isEmpty()) {
        fields = fields.concat("&amp;");
        fields = fields.concat(indexedField.getName());
        fields = fields.concat("={sitools:");
        fields = fields.concat(indexedField.getName());
        fields = fields.concat("?}");
      }
    }
    final String coordsystem = "ICRS".equals(getPluginParameters().get("referenceSystem").getValue()) ? "astro" : "geo";
    final String queryShape = getPluginParameters().get("queryShape").getValue();
    final OpenSearchApplicationPlugin.GeometryShape geometryShape = OpenSearchApplicationPlugin.GeometryShape.getGeometryShapeFrom(queryShape);
    return String.format(description,
            getSitoolsSetting("Starter.PUBLIC_HOST_DOMAIN") + getPluginModel().getUrlAttach(),
            geometryShape.getOpenSearchDescription(coordsystem),
            fields);
  }

  /**
   * builds and returns the cluster template URL.
   *
   * @return Return the template
   * @throws JSONException Exception
   * @throws IOException Exception
   */
  private String buildClusterTemplateURL() throws JSONException, IOException {
    final String description = "%s/cluster/search?q={searchTerms}&amp;startPage={startPage?}&amp;startIndex={startIndex?}&amp;count={count?}&amp;%s&amp;format=json%s";
    String fields = "";
    final List<Index> indexedFields = getIndexedFields();
    for (Index indexedField : indexedFields) {
      if (!indexedField.getTopTerms().isEmpty()) {
        fields = fields.concat("&amp;");
        fields = fields.concat(indexedField.getName());
        fields = fields.concat("={sitools:");
        fields = fields.concat(indexedField.getName());
        fields = fields.concat("?}");
      }
    }
    final String coordsystem = "ICRS".equals(getPluginParameters().get("referenceSystem").getValue()) ? "astro" : "geo";
    final String queryShape = getPluginParameters().get("queryShape").getValue();
    final OpenSearchApplicationPlugin.GeometryShape geometryShape = OpenSearchApplicationPlugin.GeometryShape.getGeometryShapeFrom(queryShape);
    return String.format(description,
            getSitoolsSetting("Starter.PUBLIC_HOST_DOMAIN") + getPluginModel().getUrlAttach(),
            geometryShape.getOpenSearchDescription(coordsystem),
            fields);
  }

  /**
   * builds and returns template URL.
   *
   * @return the template
   * @throws JSONException Exception
   * @throws IOException Exception
   */
  private String buildTemplateMOC() throws JSONException, IOException {
    final String description = "%s/moc?q={searchTerms}&amp;%s&amp;format=json%s";
    String fields = "";
    final List<Index> indexedFields = getIndexedFields();
    for (Index indexedField : indexedFields) {
      if (!indexedField.getTopTerms().isEmpty()) {
        fields = fields.concat("&amp;");
        fields = fields.concat(indexedField.getName());
        fields = fields.concat("={sitools:");
        fields = fields.concat(indexedField.getName());
        fields = fields.concat("?}");
      }
    }
    final String coordsystem = "ICRS".equals(getPluginParameters().get("referenceSystem").getValue()) ? "astro" : "geo";
    final String queryShape = getPluginParameters().get("queryShape").getValue();
    final OpenSearchApplicationPlugin.GeometryShape geometryShape = OpenSearchApplicationPlugin.GeometryShape.getGeometryShapeFrom(queryShape);
    return String.format(description,
            getSitoolsSetting("Starter.PUBLIC_HOST_DOMAIN") + getPluginModel().getUrlAttach(),
            geometryShape.getOpenSearchDescription(coordsystem),
            fields);
  }

  /**
   * Fills the data model.
   *
   * @throws JSONException Exception
   * @throws IOException Exception
   */
  private void fillDataModel() throws JSONException, IOException {
    this.dataModel.put("shortName", getPluginParameters().get("shortName").getValue());
    this.dataModel.put("description", getPluginParameters().get("description").getValue());
    this.dataModel.put("templateURL", buildTemplateURL());
    this.dataModel.put("describe", getSitoolsSetting("Starter.PUBLIC_HOST_DOMAIN") + getPluginModel().getUrlAttach() + "/describe");
    if (!getPluginParameters().get("contact").getValue().isEmpty()) {
      this.dataModel.put("contact", getPluginParameters().get("contact").getValue());
    }
    if (!getPluginParameters().get("tags").getValue().isEmpty()) {
      this.dataModel.put("tags", getPluginParameters().get("tags").getValue());
    }
    if (!getPluginParameters().get("longName").getValue().isEmpty()) {
      this.dataModel.put("longName", getPluginParameters().get("longName").getValue());
    }
    if (!getPluginParameters().get("imagePng").getValue().isEmpty()) {
      this.dataModel.put("imagePng", getPluginParameters().get("imagePng").getValue());
    }
    if (!getPluginParameters().get("imageIcon").getValue().isEmpty()) {
      this.dataModel.put("imageIcon", getPluginParameters().get("imageIcon").getValue());
    }
    if (!getPluginParameters().get("syndicationRight").getValue().isEmpty()) {
      this.dataModel.put("syndicationRight", getPluginParameters().get("syndicationRight").getValue());
    }
    this.dataModel.put("clusterTemplateURL", buildClusterTemplateURL());
    this.dataModel.put("mocdescribe", buildTemplateMOC());
    this.dataModel.put("referenceSystem", getPluginParameters().get("referenceSystem").getValue());
  }

  /**
   * Returns the open search description.
   * @return the open search description
   */
  @Get
  public final Representation describeOpenSearch() {
    try {
      fillDataModel();
      return new OpenSearchDescriptionRepresentation(dataModel, "openSearchDescription.ftl");
    } catch (JSONException ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
    }
  }
  
  @Override
  public final void sitoolsDescribe() {
    setName("OpenSearch description for the Cone Search service.");
    setDescription("Returns the description of the openSearch service for a Cone Search Service.");
  }
  
  /**
   * Describes GET method in the WADL.
   *
   * @param info information
   */
  @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("OpenSearchSimpleImageAccessProtocol");
    info.setDocumentation("OpenSearch description for the Simple Image Access Protocol");
    
    final DocumentationInfo documentationXml = new DocumentationInfo();
    documentationXml.setTitle("XML");
    documentationXml.setTextContent("Opensearch description.");

    final DocumentationInfo documentationHTML = new DocumentationInfo();
    documentationHTML.setTitle("Error");
    documentationHTML.setTextContent("Returns the error.");    
    
    final RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
    representationInfoError.setDocumentation(documentationHTML);

    final RepresentationInfo representationInfo = new RepresentationInfo(MediaType.TEXT_XML);
    representationInfo.setDocumentation(documentationXml);

    // represensation when the response is fine
    final ResponseInfo responseOK = new ResponseInfo();
    responseOK.setStatuses(Arrays.asList(Status.SUCCESS_OK));
    responseOK.getRepresentations().add(representationInfo);

    // represensation when the response is not fine
    final ResponseInfo responseNOK = new ResponseInfo();
    responseNOK.setStatuses(Arrays.asList(Status.SERVER_ERROR_INTERNAL, Status.CLIENT_ERROR_BAD_REQUEST));
    responseNOK.getRepresentations().add(representationInfoError);

    info.setResponses(Arrays.asList(responseOK, responseNOK));
  }
}
