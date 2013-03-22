/*******************************************************************************
* Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.extensions.astro.application;

import fr.cnes.sitools.astro.representation.OpenSearchDescriptionRepresentation;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.OpenSearchApplicationPlugin.GeometryShape;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
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
 * Description resource for Cone search description.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOConeDescription extends SitoolsParameterizedResource {
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(OpenSearchVOConeDescription.class.getName());
    /**
     * Data model.
     */
    private Map dataModel = new HashMap();
    /**
     * Plugin configuration.
     */
    private Map<String, ApplicationPluginParameter> parameters;

    @Override
    public final void doInit() {
        super.doInit();            
        parameters = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getModel().getParametersMap();
        // TO remplacer par AbstractApplicationPlugin
    }

    /**
     * Builds and returns athe template URL.
     * @return Return the template
     * @throws JSONException Exception
     * @throws IOException Exception
     */
    private String buildTemplateURL() throws JSONException, IOException {
        final String serviceURL = getSitoolsSetting("Starter.PUBLIC_HOST_DOMAIN") + ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getModel().getUrlAttach();
        String queryShape = parameters.get("queryShape").getValue();
        String shape;        
        if (queryShape.equals(GeometryShape.CONE.getShape())) {
            shape = "cone={sitools:cone}&amp;";
        } else if (queryShape.equals(GeometryShape.HEALPIX.getShape())) {
            shape = "healpix={sitools:healpix}&amp;order={sitools:order}&amp;";
        } else {
            shape = "";
        }
        final String description =  String.format("%s/search?%sformat=json", serviceURL, shape);
        return description;
    }

    /**
     * Fills the data model.
     * @throws JSONException Exception
     * @throws IOException Exception
     */
    private void fillDataModel() throws JSONException, IOException {
        this.dataModel.put("shortName", parameters.get("shortName").getValue());
        this.dataModel.put("description", parameters.get("description").getValue());
        this.dataModel.put("templateURL", buildTemplateURL());
        
        if (!parameters.get("contact").getValue().isEmpty()) {
            this.dataModel.put("contact", parameters.get("contact").getValue());
        }
        if (!parameters.get("tags").getValue().isEmpty()) {
            this.dataModel.put("tags", parameters.get("tags").getValue());
        }
        if (!parameters.get("longName").getValue().isEmpty()) {
            this.dataModel.put("longName", parameters.get("longName").getValue());
        }
        if (!parameters.get("imagePng").getValue().isEmpty()) {
            this.dataModel.put("imagePng", parameters.get("imagePng").getValue());
        }
        if (!parameters.get("imageIcon").getValue().isEmpty()) {
            this.dataModel.put("imageIcon", parameters.get("imageIcon").getValue());
        }
        if (!parameters.get("syndicationRight").getValue().isEmpty()) {
            this.dataModel.put("syndicationRight", parameters.get("syndicationRight").getValue());
        }
        if (!parameters.get("mocdescribe").getValue().isEmpty()) {
            this.dataModel.put("mocdescribe", getSitoolsSetting("Starter.PUBLIC_HOST_DOMAIN") + ((OpenSearchVOConeSearchApplicationPlugin) 
                    getApplication()).getModel().getUrlAttach() + "/moc");
        }        
        this.dataModel.put("referenceSystem", "ICRS");
        this.dataModel.put("dicodescribe", getSitoolsSetting("Starter.PUBLIC_HOST_DOMAIN") + ((OpenSearchVOConeSearchApplicationPlugin) 
                    getApplication()).getModel().getUrlAttach() + "/dico/{name}");
    }

    /**
     * Returns the representation.
     * @return the representation
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
    info.setIdentifier("OpenSearchConeSearchProtocol");
    info.setDocumentation("OpenSearch description for the Cone Search Protocol");

    DocumentationInfo documentationXml = new DocumentationInfo();
    documentationXml.setTitle("XML");
    documentationXml.setTextContent("Opensearch description.");

    DocumentationInfo documentationHTML = new DocumentationInfo();
    documentationHTML.setTitle("Error");
    documentationHTML.setTextContent("Returns the error.");

    RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
    representationInfoError.setIdentifier("error");
    representationInfoError.setDocumentation(documentationHTML);

    RepresentationInfo representationInfo = new RepresentationInfo(MediaType.TEXT_XML);
    representationInfo.setDocumentation(documentationXml);

    // represensation when the response is fine
    ResponseInfo responseOK = new ResponseInfo();
    responseOK.setStatuses(Arrays.asList(Status.SUCCESS_OK));
    responseOK.getRepresentations().add(representationInfo);

    // represensation when the response is fine
    ResponseInfo responseNOK = new ResponseInfo();
    responseNOK.setStatuses(Arrays.asList(Status.SERVER_ERROR_INTERNAL));
    responseNOK.getRepresentations().add(representationInfoError);

    info.setResponses(Arrays.asList(responseOK, responseNOK));
  }
}
