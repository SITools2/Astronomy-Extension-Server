/**
 * *****************************************************************************
 * Copyright 2012, 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.application;

import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import fr.cnes.sitools.SearchGeometryEngine.CoordSystem;
import fr.cnes.sitools.astro.graph.CoordinateDecorator;
import fr.cnes.sitools.astro.graph.GenericProjection;
import fr.cnes.sitools.astro.graph.Graph;
import fr.cnes.sitools.astro.graph.HealpixGridDecorator.CoordinateTransformation;
import fr.cnes.sitools.astro.graph.HealpixMocDecorator;
import fr.cnes.sitools.astro.representation.FitsRepresentation;
import fr.cnes.sitools.astro.representation.PngRepresentation;
import fr.cnes.sitools.solr.query.AbstractSolrQueryRequestFactory;
import healpix.essentials.Scheme;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Computes a HEALPix Multi-Order Coverage map in different formats from a SOLR server and represents it.
 * 
 * <p>
 * The HEALPix Multi-Order Coverage map is stored as NESTED pixel in SOLR.
 * Also, the MOC is computed and returned in in different representations according to media type that is asked by the user.
 * </p>
 * 
 * @see <a href="http://ivoa.net/Documents/Notes/MOC/index.html">IVOA note - MOC</a>
 * @author Jean-Christophe Malapert
 */
public class MocDescription extends OpenSearchBase {
  
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(MocDescription.class.getName());
  /**
   * Converts normalized number to percent.
   */
  private static final double CONVERT_TO_PERCENT = 100;
  /**
   * Width in pixels of the PNG file.
   */
  private static final int DEFAULT_PNG_WIDTH = 800;
  /**
   * Height in pixels of the PNG file.
   */
  private static final int DEFAULT_PNG_HEIGHT = 400;
  /**
   * Default transparency that is applied for drawing coordinates.
   */
  private static final float DEFAULT_TRANSPARENCY_COORDINATE = 0.1f;
  /**
   * Default transparency that is applied for drawing MOC.
   */
  private static final float DEFAULT_TRANSPARENCY_MOC = 1.0f;
  /**
   * Maximum order of the MOC.
   */
  private static final int ORDER_MAX = 13;
  /**
   * Default count on a SOLR page.
   */
  private static final String DEFAULT_COUNT = "1000";
  /**
   * Default SOLR start index.
   */
  public static final String DEFAULT_START_INDEX = "1";
  /**
   * Default SOLR start page.
   */
  public static final String DEFAULT_START_PAGE = "1";
  /**
   * Stores Healpix.
   */
  private HealpixMoc moc = null;
  /**
   * Stores user query parameters.
   */
  private Map<String, Object> queryParameters;

  @Override
  public void doInit() {
    super.doInit();   
    MediaType.register("image/fits", "FITS image");
    getMetadataService().addExtension("fits", MediaType.valueOf("image/fits"));
    getVariants().add(new Variant(MediaType.valueOf("image/fits")));
    getVariants().add(new Variant(MediaType.APPLICATION_JSON));
    getVariants().add(new Variant(MediaType.IMAGE_PNG));
    setQueryParameters();
    if (!getRequest().getMethod().equals(Method.OPTIONS)) {
      try {
        computeMoc();
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, null, ex);
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    }
  }

  /**
   * Set query parameters for SOLR.
   */
  private void setQueryParameters() {
    final Form form = this.getRequest().getResourceRef().getQueryAsForm();
    Set<String> parameters = form.getNames();

    this.queryParameters = new HashMap<String, Object>(parameters.size());

    for (String parameterIter : parameters) {
      Object value = form.getFirstValue(parameterIter);
      this.queryParameters.put(parameterIter, value);
    }
    if (!this.queryParameters.containsKey("count")) {
      this.queryParameters.put("count", DEFAULT_COUNT);
    }
    if (!this.queryParameters.containsKey("startIndex")) {
      this.queryParameters.put("startIndex", DEFAULT_START_INDEX);
    }
    if (!this.queryParameters.containsKey("startPage")) {
      this.queryParameters.put("startPage", DEFAULT_START_PAGE);
    }
    this.queryParameters.put("format", "json");
  }

  /**
   * Computes MOC at order 13 from the SOLR server.
   * @throws Exception Error while processing MOC
   */
  protected void computeMoc() throws Exception {
    AbstractSolrQueryRequestFactory querySolr = AbstractSolrQueryRequestFactory.createInstance(queryParameters, CoordSystem.EQUATORIAL, getSolrBaseUrl(), Scheme.NESTED);
    querySolr.createQueryBuilder();
    String query = querySolr.getSolrQueryRequest();
    query = query.concat("&rows=0&facet=true&facet.field=order13&facet.limit=-1&facet.mincount=1");
    //ClientResource client = new ClientResource(getSolrBaseUrl() + "/select/?q=*:*&rows=0&facet=true&facet.field=order13&facet.limit=-1&facet.mincount=1&wt=json");
    ClientResource client = new ClientResource(query);
    String text = client.get().getText();
    JSONObject json = new JSONObject(text);
    json = json.getJSONObject("facet_counts");
    json = json.getJSONObject("facet_fields");
    JSONArray array = json.getJSONArray("order13");

    setMoc(new HealpixMoc());
    for (int i = 0; i < array.length(); i++) {
      MocCell mocCell = new MocCell();
      mocCell.set(ORDER_MAX, array.getLong(i));
      getMoc().add(mocCell);
    }
  }

  /**
   * Returns the percent of the sky that is covered.
   * @param d relative value of the sky that is covered
   * @return the percent of the sky that is covered
   */
  private String percent(final double d) {
    return CONVERT_TO_PERCENT * d + "%";
  }

  /**
   * Returns the sky coverage as a string.
   * @return the sky coverage as a string
   */
  @Get("txt")
  public final Representation getCoverage() {
    return new StringRepresentation(percent(this.getMoc().getCoverage()), MediaType.TEXT_PLAIN);
  }

  /**
   * Returns the sky coverage as JSON.
   * @return the sky coverage as JSON
   */
  @Get("json")
  public final Representation getJsonResponse() {
    return new JsonRepresentation(getMoc().toString());
  }

  /**
   * Returns the sky coverage as FITS.
   * @return the sky coverage as FITS
   */
  @Get("fits")
  public final Representation getFitsResult() {
    return new FitsRepresentation("moc.fits", getMoc());
  }

  /**
   * Returns the sky coverage as PNG.
   * @return the sky coverage as PNG
   */
  @Get("png")
  public final Representation getPngResponse() {
    Graph graph = null;
    if (getMoc() == null) {
      return new EmptyRepresentation();
    } else {
      try {
        graph = new GenericProjection(Graph.ProjectionType.AITOFF);
        graph = new CoordinateDecorator(graph, Color.BLUE, DEFAULT_TRANSPARENCY_COORDINATE);
        graph = new HealpixMocDecorator(graph, Color.RED, DEFAULT_TRANSPARENCY_MOC);
        ((HealpixMocDecorator) graph).importMoc(getMoc());
        ((HealpixMocDecorator) graph).setCoordinateTransformation(CoordinateTransformation.EQ2GAL);
      } catch (Exception ex) {
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    }

    return new PngRepresentation(graph, DEFAULT_PNG_WIDTH, DEFAULT_PNG_HEIGHT);
  }

  /**
   * General WADL description.
   */
  @Override
  public void sitoolsDescribe() {
    setName("Computes the sky coverage from SOLR server");
    setDescription("Returns the sky coverage.");
  }

  @Override
  protected void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("Sky coverage service");
    info.setDocumentation("Sky coverage service is based on the MOC library from CDS.");

    DocumentationInfo documentationFits = new DocumentationInfo();
    documentationFits.setTitle("Sky coverage");
    documentationFits.setTextContent("Returns the sky coverage as a FITS");    
    
    DocumentationInfo documentationJson = new DocumentationInfo();
    documentationJson.setTitle("Sky coverage");
    documentationJson.setTextContent("Returns the sky coverage as a MOC");

    DocumentationInfo documentationPng = new DocumentationInfo();
    documentationPng.setTitle("Sky coverage");
    documentationPng.setTextContent("Returns the sky coverage as a PNG file");

    DocumentationInfo documentationTxt = new DocumentationInfo();
    documentationTxt.setTitle("Sky coverage");
    documentationTxt.setTextContent("Returns the sky coverage as a percent of the full sky");
    
    DocumentationInfo documentationHTML = new DocumentationInfo();
    documentationHTML.setTitle("Error");
    documentationHTML.setTextContent("Returns the error");    

    List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    RepresentationInfo representationInfo = new RepresentationInfo(MediaType.APPLICATION_JSON);
    representationInfo.setDocumentation(documentationJson);
    representationsInfo.add(representationInfo);
    representationInfo = new RepresentationInfo(MediaType.IMAGE_PNG);
    representationInfo.setDocumentation(documentationPng);
    representationsInfo.add(representationInfo);
    representationInfo = new RepresentationInfo(MediaType.TEXT_PLAIN);
    representationInfo.setDocumentation(documentationTxt);
    representationsInfo.add(representationInfo);
    representationInfo = new RepresentationInfo(MediaType.valueOf("image/fits"));
    representationInfo.setDocumentation(documentationFits);
    representationsInfo.add(representationInfo);    
    
    RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
    representationInfoError.setDocumentation(documentationHTML);

    ResponseInfo responseOK = new ResponseInfo();
    responseOK.setRepresentations(representationsInfo);
    responseOK.getStatuses().add(Status.SUCCESS_OK);

    ResponseInfo responseNOK = new ResponseInfo();
    responseNOK.getRepresentations().add(representationInfoError);
    responseNOK.getStatuses().add(Status.SERVER_ERROR_INTERNAL);

    info.setResponses(Arrays.asList(responseOK, responseNOK));
  }

  /**
   * Returns the MOC.
   * @return the moc
   */
  protected final HealpixMoc getMoc() {
    return moc;
  }

  /**
   * Sets the MOC.
   * @param mocVal the moc to set
   */
  protected final void setMoc(final HealpixMoc mocVal) {
    this.moc = mocVal;
  }
}
