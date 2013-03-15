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
package fr.cnes.sitools.astro.graph;

import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import healpix.essentials.Vec3;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This object provides methods to decorate the graph by a circle.
 *
 * <p>
 * Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new HealpixGridDecorator(graph, Scheme.RING, 0);
 * ((HealpixGridDecorator)graph).setCoordinateTransformation(CoordinateTransformation.NATIVE);
 * ((HealpixGridDecorator)graph).setDebug(true);
 * graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
 * ((CircleDecorator)graph).setColor(Color.yellow); 
 * Utility.createJFrame(graph, 900, 500);
 * </code>
 * </pre>
 * </p>
 * @author Jean-Christophe Malapert
 */
public class CircleDecorator extends HealpixGridDecorator {
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(CircleDecorator.class.getName());
  /**
   * Index representing by a set of pixels range.
   */
  private RangeSet range;
  
  /**
   * Default composite sets to 0.2.
   */
  private static final float DEFAULT_ALPHA_COMOSITE = 0.2f;

  /**
   * Constructs a circle.
   *
   * @param graph graph
   * @param ra Right Ascension of the center in decimal degree
   * @param dec Declination of the center in decimal degree
   * @param radius radius in decimal degree
   * @param nside nside
   * @param scheme scheme
   * @param color color
   * @param alpha alpha composite [0..1]
   * @throws Exception Exception
   */
  public CircleDecorator(final Graph graph, final double ra, final double dec, final double radius, final int nside, final Scheme scheme,
                         final Color color, final float alpha) throws Exception {
    super(graph, nside, scheme, color, alpha);
    this.range = computeIntersect(ra, dec, radius);
  }

  /**
   * Constructor a circle.
   *
   * @param graph graph
   * @param ra Right Ascension of the center in decimal degree
   * @param dec Declination of the center in decimal degree
   * @param radius radius in decimal degree
   * @param scheme scheme
   * @param order order
   * @param color color
   * @param alpha alpha composite [0..1]
   * @throws Exception Exception
   */
  public CircleDecorator(final Graph graph, final double ra, final double dec, final double radius, final Scheme scheme, int order,
                         final Color color, final float alpha) throws Exception {
    this(graph, ra, dec, radius, (int) Math.pow(2, order), scheme, color, alpha);
  }

  /**
   * Constructs a circle.
   *
   * @param graph graph
   * @param ra Right Ascension of the center in decimal degree
   * @param dec Declination of the center in decimal degree
   * @param radius radius in decimal degree
   * @param scheme scheme
   * @param order order
   * @throws Exception Exception
   */
  public CircleDecorator(final Graph graph, final double ra, final double dec, final double radius,
                         final Scheme scheme, final int order) throws Exception {
    this(graph, ra, dec, radius, (int) Math.pow(2, order), scheme, Color.CYAN, DEFAULT_ALPHA_COMOSITE);
  }

  /**
   * Computes circle intersection.
   *
   * @param ra Right Ascension of the center in decimal degree
   * @param dec Declination of the center in decimal degree
   * @param radius radius in decimal degree
   * @return Returns the ranges of intersected pixels
   * @throws Exception Healpix Exception
   */
  private RangeSet computeIntersect(final double ra, final double dec, final double radius) throws Exception {
    Pointing point = new Pointing(Math.toRadians(Graph.DEC_MAX - dec), Math.toRadians(ra));
    return this.getHealpixBase().queryDisc(point, Math.toRadians(radius));
  }

  @Override
  protected final void drawPixels(final Graphics2D g2, final Color color) {
    g2.setPaint(color);
    try {
      RangeSet.ValueIterator iter = this.range.valueIterator();
      while (iter.hasNext()) {
        long pixel = iter.next();
        drawHealpixPolygon(g2, getHealpixBase(), pixel, getCoordinateTransformation());
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Draws a pixel based on its coordinates.
   *
   * @param g2 graphic
   * @param healpix Healpix index
   * @param pix Pixel to draw
   * @param coordinateTransformation Coordinate transformation
   */
  @Override
  protected final void drawHealpixPolygon(final Graphics2D g2, final HealpixIndex healpix, final long pix, final CoordinateTransformation coordinateTransformation) {
    try {
      int numberOfVectors = computeNumberPointsForPixel(getHealpixBase().getNside(), pix);
      Vec3[] vectors = healpix.boundaries(pix, numberOfVectors);
      computeReferenceFrameTransformation(vectors, coordinateTransformation);
      Coordinates[] shapes = splitHealpixPixelForDetectedBorder(vectors);
      for (int i = 0; i < shapes.length; i++) {
        Coordinates shape = shapes[i];
        List<Point2D.Double> pixels = shape.getPixelsFromProjection(this.getProjection(), getRange(), getPixelWidth(), getPixelHeight());
        g2.draw(new Polygon2D(pixels));
        g2.fill(new Polygon2D(pixels));
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }  
}
