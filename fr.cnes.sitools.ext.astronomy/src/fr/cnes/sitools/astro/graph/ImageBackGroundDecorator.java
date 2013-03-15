/**
 * *****************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
*****************************************************************************
 */
package fr.cnes.sitools.astro.graph;

import com.jhlabs.map.MapMath;
import com.jhlabs.map.proj.ProjectionException;
import healpix.core.AngularPosition;
import healpix.tools.CoordTransform;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Another concrete decorator for adding an image as a background.<br/> This concrete decorator allows to display a map on the graph
 * component. The map must be in plate carree projection
 * 
 * <p>
 * Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new ImageBackGroundDecorator(graph, new File("/home/malapert/Documents/Equirectangular-projection.jpg")); 
 * Utility.createJFrame(graph, 900, 500);
 * </code>
 * </pre>
 * </p>
 *
 * @author Jean-Christophe Malapert
 */
public class ImageBackGroundDecorator extends AbstractGraphDecorator {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ImageBackGroundDecorator.class.getName());
  /**
   * Image filename to project.
   */
  private final File file;
  /**
   * Coordinate transformation to apply.
   */
  private CoordinateTransformationMap coordinateTransformation = CoordinateTransformationMap.NATIVE;

  /**
   * List of supported projections.
   */
  public enum CoordinateTransformationMap {

    /**
     * Native coordinate.
     */
    NATIVE(-1),
    /**
     * Galactic to Equatorial.
     */
    GAL2EQ(1),
    /**
     * Galactic to Ecliptic.
     */
    GAL2ECL(5);
    /**
     * Transformation code.
     */
    private int transformationCode;

    /**
     * Construtor.
     *
     * @param transformationCodeVal transformation code
     */
    CoordinateTransformationMap(final int transformationCodeVal) {
      this.transformationCode = transformationCodeVal;
    }

    /**
     * Returns a transformation code.
     *
     * @return a transformation code
     */
    public int getTransformationCode() {
      return this.transformationCode;
    }
  }

  /**
   * Constructs an Image background decorator.
   *
   * @param g graph to decorate
   * @param fileVal map file
   * @exception FileNotFoundException image not found
   */
  public ImageBackGroundDecorator(final Graph g, final File fileVal) throws FileNotFoundException {
    setGraph(g);
    this.file = fileVal;
  }

  @Override
  public void paint(final Graphics g) {
    getGraph().paint(g);
    try {
      // setup an output image
      BufferedImage imageOutput = new BufferedImage(getGraph().getPixelWidth() + 1, getGraph().getPixelHeight() + 1, BufferedImage.TYPE_INT_RGB);
      WritableRaster rasterWrite = imageOutput.getRaster();
      // read the image to project
      BufferedImage image = ImageIO.read(file);
      Raster raster = image.getData();
      // for RGB
      double[] color = new double[3];
      // browse the image to project
      for (int i = 0; i < raster.getHeight(); i++) {
        for (int j = 0; j < raster.getWidth(); j++) {
          double[] rgb = raster.getPixel(j, i, color);
          // convert to Earth observation convention for the projection library
          double longEarth = Graph.LONG_MIN + j * getGraph().getWidthLongitude() / raster.getWidth();
          double latEarth = Graph.LAT_MAX - i * getGraph().getWidthLatitude() / raster.getHeight();
          // convert coordinates if needed
          if (!getCoordinateTransformation().equals(CoordinateTransformationMap.NATIVE)) {
            double ra = this.convertLongitudeFromEarthObsToAstro(longEarth);
            double dec = latEarth;
            AngularPosition angularPosition = new AngularPosition(dec, ra);
            angularPosition = CoordTransform.transformInDeg(angularPosition, getCoordinateTransformation().getTransformationCode());
            dec = angularPosition.theta();
            ra = angularPosition.phi();
            latEarth = dec;
            longEarth = convertLongitudeFromAstroToEarth(ra);
          }
          // project the image on the projection
          Point2D.Double point2D = new Point2D.Double();
          try {
            if (getGraph().getProjection().inside(MapMath.degToRad(longEarth), MapMath.degToRad(latEarth))) {
              getGraph().getProjection().project(MapMath.degToRad(longEarth), MapMath.degToRad(latEarth), point2D);
              double xprojected = getGraph().scaleX(point2D.getX());
              double yprojected = getGraph().scaleY(point2D.getY());
              rasterWrite.setPixel((int) xprojected, (int) yprojected, rgb);
            }
          } catch (ProjectionException ex) {
          }
        }
      }
      // draw the projected image
      ((Graphics2D) g).drawImage(imageOutput, null, this);
    } catch (Exception ex) {
      Logger.getLogger(Graph.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Returns the coordinate transformation.
   *
   * @return the coordinateTransformation
   */
  public final CoordinateTransformationMap getCoordinateTransformation() {
    return coordinateTransformation;
  }

  /**
   * Sets the coordinate transformation.
   *
   * @param coordinateTransformationVal the coordinateTransformation to set
   */
  public final void setCoordinateTransformation(final CoordinateTransformationMap coordinateTransformationVal) {
    this.coordinateTransformation = coordinateTransformationVal;
  }
}
