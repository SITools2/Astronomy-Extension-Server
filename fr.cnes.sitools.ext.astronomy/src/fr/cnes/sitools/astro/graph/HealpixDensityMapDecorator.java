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
package fr.cnes.sitools.astro.graph;

import healpix.core.HealpixIndex;
import healpix.essentials.HealpixMapDouble;
import healpix.essentials.Scheme;
import healpix.essentials.Vec3;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Povides methods to decorate a graph by a density map.
 *
 * <p>Provides a density map from a Healpix map.</p>
 *
 * <p>Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new ImageBackGroundDecorator(graph, new File("/home/malapert/Documents/Equirectangular-projection.jpg"));
 * HealpixMapDouble map = new HealpixMapDouble((long)Math.pow(2, 6), Scheme.RING);
 * map.fill(0.2);
 * map.setPixel(741, 1.0d);
 * map.setPixel(742, 2.0d);
 * map.setPixel(820, 1.0d);
 * map.setPixel(822, 3.0d);
 * map.setPixel(903, 1.0d);
 * map.setPixel(904, 1.0d);        
 * map.setPixel(905, 1.0d);        
 * map.setPixel(991, 1.0d);        
 * map.setPixel(992, 1.0d); 
 * graph = new HealpixDensityMapDecorator(graph, Scheme.RING, 6, 0.8);
 * ((HealpixDensityMapDecorator)graph).importHealpixMap(map, CoordinateTransformation.NATIVE);
 * graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
 * ((CircleDecorator)graph).setColor(Color.yellow); 
 * Utility.createJFrame(graph, 900, 500);
 * </code>
 * </pre></p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class HealpixDensityMapDecorator extends HealpixGridDecorator {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(HealpixDensityMapDecorator.class.getName());
  /**
   * Map that stores the density map.
   */
  private final transient HealpixMapDouble healpixMapDouble;
  /**
   * Initialize value for the max density.
   */
  private double maxValue = 0;
  /**
   * Initialize value for the mon density.
   */
  private double minValue = 0;

  /**
   * Constructs a Healpix density decorator.
   *
   * @param graph Grap to decorate
   * @param nside Healpix nside
   * @param scheme Healpix scheme
   * @param alpha transparency
   * @throws GraphException if a Healpix Exception happens.
   */
  public HealpixDensityMapDecorator(final Graph graph, final int nside, final Scheme scheme, final float alpha) throws GraphException  {
    super(graph, nside, scheme, null, alpha);
      try {
          this.healpixMapDouble = new HealpixMapDouble(nside, scheme);
      } catch (Exception ex) {
          LOG.log(Level.FINER, null, ex);
          throw new GraphException(ex);
      }
  }

  /**
   * Constructs a Healpix density decorator.
   *
   * @param graph graph to decorate
   * @param scheme Healpix scheme
   * @param order Healpix order
   * @param alpha transparency
   * @throws Exception if a Healpix Exception happens
   */
  public HealpixDensityMapDecorator(final Graph graph, final Scheme scheme, final int order, final float alpha) throws Exception {
    this(graph, (int) Math.pow(2, order), scheme, alpha);
  }

  /**
   * Imports a Healpix density map.
   *
   * @param healpixMapDoubleVal density map
   * @param coordinateTransformation Coordinates transformation to apply
   * @throws Exception Healpix Exception
   */
  public final void importHealpixMap(final HealpixMapDouble healpixMapDoubleVal, final CoordinateTransformation coordinateTransformation) throws Exception {
    this.setCoordinateTransformation(coordinateTransformation);
    this.getHealpixMapDouble().importGeneral(healpixMapDoubleVal, false);
    final double[] data = this.healpixMapDouble.getData();
    setMinValue(data[0]);
    for (int i = 0; i < data.length; i++) {
      setMaxValue(Math.max(getMaxValue(), data[i]));
      setMinValue(Math.min(getMinValue(), data[i]));
    }
  }

  /**
   * Returns the density map.
   *
   * @return the density map
   */
  public final HealpixMapDouble getHealpixMapDouble() {
    return this.healpixMapDouble;
  }

  /**
   * Returns the max density.
   *
   * @return the max density
   */
  private double getMaxValue() {
    return this.maxValue;
  }

  /**
   * Returns the min density.
   *
   * @return the min density
   */
  private double getMinValue() {
    return this.minValue;
  }

  /**
   * Sets the max density.
   *
   * @param max the max density
   */
  private void setMaxValue(final double max) {
    this.maxValue = max;
  }

  /**
   * Sets the min density.
   *
   * @param min the min density
   */
  private void setMinValue(final double min) {
    this.minValue = min;
  }

  /**
   * Draws a Healpix polygon.
   *
   * @param graphic2D graphic to decorate
   * @param healpix Healpix index
   * @param pix Healpix pixel to draw
   * @param coordinateTransformation Coordinate Transformation to apply
   */
  @Override
  protected void drawHealpixPolygon(final Graphics2D graphic2D, final HealpixIndex healpix, final long pix, final CoordinateTransformation coordinateTransformation) {
    try {
      final int numberOfVectors = computeNumberPointsForPixel(getHealpixMapDouble().getNside(), pix);
      final Vec3[] vectors = getHealpixMapDouble().boundaries(pix, numberOfVectors);
      computeReferenceFrameTransformation(vectors, coordinateTransformation);
      final Coordinates[] coordinates = splitHealpixPixelForDetectedBorder(vectors);
      final Polygon2D poly = new Polygon2D();
      for (int i = 0; i < coordinates.length; i++) {
        final Coordinates coordinate = coordinates[i];
        final List<Point2D.Double> pixels = coordinate.getPixelsFromProjection(this.getProjection(), getRange(), getPixelWidth(), getPixelHeight());
        final double numberOfObjectsInCurrentPixel = getHealpixMapDouble().getPixel(pix);
        final Color color = colorGradient(numberOfObjectsInCurrentPixel, getMinValue(), getMaxValue());
        if (color != null) {
          graphic2D.setPaint(color);
          poly.setPoints(pixels);
          graphic2D.draw(poly);
          graphic2D.fill(poly);
        }
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Returns a color gradient based on HSB scale.
   * 
   * <p>The min value is represented by a blue color. The max value is represented by a red color.</p>
   *
   * @param number density number
   * @param min min value
   * @param max max value
   * @return the colot
   */
  protected static Color colorGradient(final double number, final double min, final double max) {
    float saturation;
    if (number <= 0) {
      float h = 0;
      saturation = 1.0f;
      float value = 0;
      return Color.getHSBColor(h, saturation, value);
    } else {
      Color start = Color.BLUE;
      Color end = Color.RED;
      float p = (float) (1.0 - ((number - min) / max));
      float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
      float[] endHSB = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);

      float brightness = (startHSB[2] + endHSB[2]) / 2;
      saturation = (startHSB[1] + endHSB[1]) / 2;

      float hueMax;
      float hueMin;
      if (startHSB[0] > endHSB[0]) {
        hueMax = startHSB[0];
        hueMin = endHSB[0];
      } else {
        hueMin = startHSB[0];
        hueMax = endHSB[0];
      }
      float hue = (hueMax - hueMin) * p + hueMin;
      return Color.getHSBColor(hue, saturation, brightness);
    }
  }
}
