/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is a part of SITools2
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program inputStream distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.extensions.astro.resource;

import cds.moc.HealpixMoc;
import fr.cnes.sitools.astro.graph.CoordinateDecorator;
import fr.cnes.sitools.astro.graph.GenericProjection;
import fr.cnes.sitools.astro.graph.Graph;
import fr.cnes.sitools.astro.graph.HealpixGridDecorator;
import fr.cnes.sitools.astro.graph.HealpixMocDecorator;
import fr.cnes.sitools.astro.representation.FitsMocRepresentation;
import fr.cnes.sitools.astro.representation.PngRepresentation;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.util.ClientResourceProxy;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Computes a sky coverage based on Healpix MOCs as input parameters.
 * 
 * @see SkyCoverageResourcePlugin the sky coverage plugin.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SkyCoverageResource extends SitoolsParameterizedResource {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(SkyCoverageResource.class.getName());
  /**
   * Size of a FITS block (1024 bytes).
   */
  private static final int FITS_BLOCK = 1024;
  /**
   * Size of the FITS buffer (32 * FITS_BLOCK).
   */
  private static final int FITS_BUFFER = 32 * FITS_BLOCK;
  /**
   * Transforms the value of the sky coverage in percent. This constant inputStream used when the TXT representation inputStream called
   */
  private static final int NORMALIZE_TO_PERCENT = 100;
  /**
   * Default opacity (0.1) for coodinates. This constant inputStream used when the PNG representation inputStream called
   */
  private static final float DEFAULT_OPACITY_COORDINATES = 0.1f;
  /**
   * Default opacity (1.0) for MOC. This constant inputStream used when the PNG representation inputStream called
   */
  private static final float DEFAULT_OPACITY_MOC = 1.0f;
  /**
   * Default PNG width. This constant inputStream used when the PNG representation inputStream called
   */
  private static final int DEFAULT_GRAPH_WIDTH = 800;
  /**
   * Default PNG height. This constant inputStream used when the PNG representation inputStream called
   */
  private static final int DEFAULT_GRAPH_HEIGHT = 400;
  /**
   * Result of processing.
   */
  private HealpixMoc moc;
  /**
   * Sets the PNG width the DEFAULT_GRAPH_WIDTH.
   */
  private int pngWidth = DEFAULT_GRAPH_WIDTH;
  /**
   * Sets the PNG height the DEFAULT_GRAPH_HEIGHT.
   */
  private int pngHeight = DEFAULT_GRAPH_HEIGHT;
  /**
   * Sets the coordinates opacity to the DEFAULT_GRAPH_WIDTH.
   */
  private float coordinatesOpacity = DEFAULT_OPACITY_COORDINATES;
  /**
   * Sets the MOC opacity to the DEFAULT_OPACITY_MOC.
   */
  private float mocOpacity = DEFAULT_OPACITY_MOC;

  @Override
  public final void doInit() {
    super.doInit();
    MediaType.register("image/fits", "Fits image");
    getMetadataService().addExtension("fits", MediaType.valueOf("image/fits"));
    getVariants().add(new Variant(MediaType.valueOf("fits")));
    getVariants().add(new Variant(MediaType.APPLICATION_JSON));
    getVariants().add(new Variant(MediaType.IMAGE_PNG));
    final String mocs = getRequest().getResourceRef().getQueryAsForm().getFirstValue(SkyCoverageResourcePlugin.INPUT_PARAMETER);
    final String[] mocArray = mocs.split(";");
    if (!getRequest().getMethod().equals(Method.OPTIONS)) {
      try {
        procesSkyCoverage(mocArray);
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, null, ex);
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    }
  }

  /**
   * Computes the sky coverage with a list of MOC's URLs and stores the result in moc variable.
   *
   * @param mocArray List of MOC's URL.
   * @throws Exception if an error occurs during the Healpix processing, if the URL inputStream malformed or if an error occurs when getting the
   * result as a stream
   */
  protected final void procesSkyCoverage(final String[] mocArray) throws Exception {
    final String firstMoc = mocArray[0];
    HealpixMoc mocA = readMoc(firstMoc);
    for (int i = 1; i < mocArray.length; i++) {
      final HealpixMoc mocB = readMoc(mocArray[i]);
      mocA = mocA.intersection(mocB);
    }
    this.setMoc(mocA);
  }

  /**
   * Retrieves a MOC and loads it in memory.
   *
   * @param mocUrl MOC's URL
   * @return the MOC
   * @throws Exception if an error occurs during the Healpix processing or when getting the stream
   */
  protected final HealpixMoc readMoc(final String mocUrl) throws Exception {
    final ClientResourceProxy client = new ClientResourceProxy(mocUrl, Method.GET);
    final Representation rep = client.getClientResource().get(MediaType.valueOf("image/fits"));
    final InputStream inputStream = rep.getStream();
    final BufferedInputStream bis = new BufferedInputStream(inputStream, FITS_BUFFER);
    return new HealpixMoc(bis, HealpixMoc.FITS);
  }

  /**
   * Expresses the sky coverage in percent.
   *
   * @param skyCoverageArea sky coverage from 0 to 1
   * @return the sky coverage in percent
   */
  private String percent(final double skyCoverageArea) {
    return NORMALIZE_TO_PERCENT * skyCoverageArea + "%";
  }

  /**
   * Returns the sky coverage in percent.
   *
   * @return the sky coverage in percent
   */
  @Get("txt")
  public final Representation getCoverage() {
    final Representation rep = new StringRepresentation(percent(this.getMoc().getCoverage()), MediaType.TEXT_PLAIN);
    if (fileName != null && !"".equals(fileName)) {
      final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
      disp.setFilename(fileName);
      rep.setDisposition(disp);
    }
    return rep;
  }

  /**
   * Returns the sky coverage as a MOC in JSON.
   *
   * @return the sky coverage as a MOC in JSON
   */
  @Get("json")
  public final Representation getJsonResult() {
    Representation rep;
    if (getMoc() == null) {
      rep = new EmptyRepresentation();
    } else {
      rep = new JsonRepresentation(getMoc().toString());
    }
    if (fileName != null && !"".equals(fileName)) {
      final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
      disp.setFilename(fileName);
      rep.setDisposition(disp);
    }
    return rep;
  }

  /**
   * Returns the sky coverage as a MOC in FITS.
   *
   * @return the sky coverage as a MOC in FITS
   */
  @Get("fits")
  public final Representation getFitsResult() {
    final Representation rep = new FitsMocRepresentation("skyCoverage.fits", getMoc());
    if (fileName != null && !"".equals(fileName)) {
      final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
      disp.setFilename(fileName);
      rep.setDisposition(disp);
    }
    return rep;
  }

  /**
   * Returns the sky coverage as a PNG.
   *
   * @return the sky coverage as a PNG
   */
  @Get("png")
  public final Representation getPngResponse() {
    Graph graph = null;
    if (getMoc() == null) {
      return new EmptyRepresentation();
    } else {
      try {
        graph = new GenericProjection(Graph.ProjectionType.AITOFF);
        graph = new CoordinateDecorator(graph, Color.BLUE, getCoordinatesOpacity());
        graph = new HealpixMocDecorator(graph, Color.RED, getMocOpacity());
        ((HealpixMocDecorator) graph).importMoc(getMoc());
        ((HealpixMocDecorator) graph).setCoordinateTransformation(HealpixGridDecorator.CoordinateTransformation.EQ2GAL);
      } catch (Exception ex) {
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    }

    final Representation rep = new PngRepresentation(graph, getPngHeight());
    if (fileName != null && !"".equals(fileName)) {
      final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
      disp.setFilename(fileName);
      rep.setDisposition(disp);
    }
    return rep;
  }

  /**
   * Returns the MOC that must be represented.
   *
   * @return the moc
   */
  protected final HealpixMoc getMoc() {
    return moc;
  }

  /**
   * Sets the MOC.
   *
   * @param mocVal the moc to set
   */
  protected final void setMoc(final HealpixMoc mocVal) {
    this.moc = mocVal;
  }

  /**
   * Returns the PNG width in pixels.
   *
   * @return the pngWidth
   */
  protected final int getPngWidth() {
    return pngWidth;
  }

  /**
   * Sets the PNG width in pixels.
   *
   * @param pngWidthVal the pngWidth to set
   */
  protected final void setPngWidth(final int pngWidthVal) {
    this.pngWidth = pngWidthVal;
  }

  /**
   * Returns the PNG height in pixels.
   *
   * @return the pngHeight
   */
  protected final int getPngHeight() {
    return pngHeight;
  }

  /**
   * Sets the PNG height in pixels.
   *
   * @param pngHeightVal the pngHeight to set
   */
  protected final void setPngHeight(final int pngHeightVal) {
    this.pngHeight = pngHeightVal;
  }

  /**
   * Returns the coordinates opacity [0 1].
   *
   * @return the coordinatesOpacity
   */
  protected final float getCoordinatesOpacity() {
    return coordinatesOpacity;
  }

  /**
   * Sets the coordinate opacity [0 1].
   *
   * @param coordinatesOpacityVal the coordinatesOpacity to set
   */
  protected final void setCoordinatesOpacity(final float coordinatesOpacityVal) {
    this.coordinatesOpacity = coordinatesOpacityVal;
  }

  /**
   * Returns the MOC opacity [0 1].
   *
   * @return the mocOpacity
   */
  protected final float getMocOpacity() {
    return mocOpacity;
  }

  /**
   * Sets the MOC opacity [0 1].
   *
   * @param mocOpacityVal the mocOpacity to set
   */
  protected final void setMocOpacity(final float mocOpacityVal) {
    this.mocOpacity = mocOpacityVal;
  }

  @Override
  public final void sitoolsDescribe() {
    setName("Sky coverage service");
    setDescription("Returns the sky coverage.");
  }

  @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("skyCoverage");
    info.setDocumentation("Retrieves the sky coverage.");
    
    final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo(SkyCoverageResourcePlugin.INPUT_PARAMETER, true, "string", ParameterStyle.QUERY,
            "list of MOC resources separated by a ;"));
    info.getRequest().setParameters(parametersInfo);

    info.getResponse().getStatuses().add(Status.SUCCESS_OK);

    final DocumentationInfo documentationJson = new DocumentationInfo();
    documentationJson.setTitle("Sky coverage in  JSON");
    documentationJson.setTextContent("Returns the sky coverage as a MOC in JSON");

    final DocumentationInfo documentationPng = new DocumentationInfo();
    documentationPng.setTitle("Sky coverage in PNG");
    documentationPng.setTextContent("Returns the sky coverage as a PNG file");

    final DocumentationInfo documentationTxt = new DocumentationInfo();
    documentationTxt.setTitle("Sky coverage in TXT");
    documentationTxt.setTextContent("Returns the sky coverage as a percent of the full sky");

    final DocumentationInfo documentationFits = new DocumentationInfo();
    documentationFits.setTitle("Sky coverage in FITS");
    documentationFits.setTextContent("Returns the sky coverage as a FITS");

    final List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
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

    info.getResponse().setRepresentations(representationsInfo);
  }
}
