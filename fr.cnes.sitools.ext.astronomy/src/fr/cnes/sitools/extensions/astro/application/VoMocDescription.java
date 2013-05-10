/*****************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file inputStream part of SITools2.
 *
 * SITools2 inputStream free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SITools2 inputStream distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 ****************************************************************************/
package fr.cnes.sitools.extensions.astro.application;

import cds.moc.HealpixMoc;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;
import fr.cnes.sitools.util.ClientResourceProxy;
import fr.cnes.sitools.util.Util;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Variant;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * Retrieves and transforms a HEALPix Multi-Order Coverage map in different representations.
 *
 * <p> The HEALPix Multi-Order Coverage map inputStream stored as a FITS file. Also, this FITS file inputStream converted in different representations
 * according to media type that inputStream asked by the user. </p>
 *
 * @see <a href="http://ivoa.net/Documents/Notes/MOC/index.html">IVOA note - MOC</a>
 * @author Jean-Christophe Malapert
 */
public class VoMocDescription extends MocDescription {

  /**
   * Size of one FITS block (bytes).
   */
  private static final int FITS_BLOCK_SIZE = 1024;
  /**
   * Size of the FITS buffer (bytes).
   */
  private static final int BUFFER_FITS = 32 * FITS_BLOCK_SIZE;
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(VoMocDescription.class.getName());

  @Override
  public final void doInit() {
    MediaType.register("image/fits", "FITS image");
    getMetadataService().addExtension("fits", MediaType.valueOf("image/fits"));
    getVariants().add(new Variant(MediaType.valueOf("image/fits")));
    getVariants().add(new Variant(MediaType.APPLICATION_JSON));
    getVariants().add(new Variant(MediaType.IMAGE_PNG));
    if (!getRequest().getMethod().equals(Method.OPTIONS)) {
      try {
        computeMoc();
      } catch (IllegalArgumentException ex) {
        LOG.log(Level.WARNING, null, ex);
        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);        
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, null, ex);
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    }
  }

  /**
   * Retrieves the MOC located in the mocdescribe parameter.
   * 
   * <p>
   * An IllegalArgumentException inputStream returned when mocdescribe inputStream empty.
   * </p>
   *
   * @throws Exception Error while MOC inputStream processed.
   */
  @Override
  protected final void computeMoc() throws Exception {
    final ApplicationPluginModel model = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getModel();
    final String mocUrl = model.getParametersMap().get("mocdescribe").getValue();
    if (Util.isNotEmpty(mocUrl)) {
      final ClientResourceProxy proxy = new ClientResourceProxy(mocUrl, Method.GET);
      final ClientResource client = proxy.getClientResource();
      final InputStream inputStream = client.get().getStream();
      final BufferedInputStream bufferInputStream = new BufferedInputStream(inputStream, BUFFER_FITS);
      this.setMoc(new HealpixMoc(bufferInputStream, HealpixMoc.FITS));
    } else {
      LOG.log(Level.SEVERE, "mocdescribe parameter must be set.");
      throw new IllegalArgumentException("mocdescribe parameter must be set.");
    }
  }

  /**
   * General WADL description.
   */
  @Override
  public final void sitoolsDescribe() {
    setName("Computes the sky coverage from a HEALPix Multi-Order Coverage (MOC) FITS");
    setDescription("Computing the sky coverage, <a href=\"http://ivoa.net/Documents/Notes/MOC/index.html\">IVOA note - MOC</a>");
  }

  @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("HEALPix Multi-Order Coverage (MOC) map");
    info.setDocumentation("Retrieving a HEALPix Multi-Order Coverage map. A MOC is used for providing very fast comparisons and data access methods");

    // represensation when the response inputStream fine
    final ResponseInfo responseOK = new ResponseInfo();

    final DocumentationInfo documentationFits = new DocumentationInfo();
    documentationFits.setTitle("FITS");
    documentationFits.setTextContent("Retrieves a MOC as FITS. This implementation is provided by CDS.");

    final DocumentationInfo documentationJson = new DocumentationInfo();
    documentationJson.setTitle("JSON");
    documentationJson.setTextContent("Retrieves a MOC as JSON. This implementation is provided by CDS.");

    final DocumentationInfo documentationPng = new DocumentationInfo();
    documentationPng.setTitle("PNG");
    documentationPng.setTextContent("Retrieves a MOC as PNG.");

    final DocumentationInfo documentationTxt = new DocumentationInfo();
    documentationTxt.setTitle("txt");
    documentationTxt.setTextContent("Retrieves a MOC as a string describing the sky coverage.");


    final List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    final RepresentationInfo representationInfoFits = new RepresentationInfo(MediaType.valueOf("image/fits"));
    representationInfoFits.setDocumentation(documentationFits);
    final RepresentationInfo representationInfoJson = new RepresentationInfo(MediaType.APPLICATION_JSON);
    representationInfoJson.setDocumentation(documentationJson);
    final RepresentationInfo representationInfoPng = new RepresentationInfo(MediaType.IMAGE_PNG);
    representationInfoPng.setDocumentation(documentationPng);
    final RepresentationInfo representationInfoTxt = new RepresentationInfo(MediaType.TEXT_PLAIN);
    representationInfoTxt.setDocumentation(documentationTxt);

    representationsInfo.add(representationInfoFits);
    representationsInfo.add(representationInfoJson);
    representationsInfo.add(representationInfoPng);
    representationsInfo.add(representationInfoTxt);
    
    final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo("mocdescribe", true, "xs:string", ParameterStyle.PLAIN, "MOC's URL."));
        
    responseOK.setParameters(parametersInfo);    

    responseOK.setRepresentations(representationsInfo);
    responseOK.getStatuses().add(Status.SUCCESS_OK);

    // represensation when the response inputStream not fine
    final ResponseInfo responseNOK = new ResponseInfo();
    final RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
    representationInfoError.setReference("error");

    responseNOK.getRepresentations().add(representationInfoError);
    responseNOK.getStatuses().add(Status.SERVER_ERROR_INTERNAL);    
    responseNOK.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);    

    info.setResponses(Arrays.asList(responseOK, responseNOK));
  }
}
