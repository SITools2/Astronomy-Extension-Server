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

import org.restlet.engine.Engine;

/**
 * Provides methods to decorate the graph by a circle.
 *
 * <p>Here is a code to illustrate how to use it:<br/>
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
 * </pre></p>
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CircleDecorator extends HealpixGridDecorator {
  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(CircleDecorator.class.getName());
  
  /**
   * Index representing by a set of pixels range.
   */
  private transient RangeSet range;
  
  /**
   * Default alpha composite sets to 0.2.
   */
  private static final float DEFAULT_ALPHA_COMOSITE = 0.2f;

  /**
   * Constructs a new circle on the <code>graph</code> layer with a <code>color</code>
   * and an <code>alpha</code> composite. 
   * 
   * <p>The circle is defined by its center (<code>rightAscension</code>, <code>declination</code>) and its
   * <code>radius</code> in decimal degree. The circle is computed as Healpix pixels at
   * <code>nside</code> with the <code>scheme</code>.</p>
   *
   * @param graph graph component to decore
   * @param rightAscension Right Ascension of the center in decimal degree
   * @param declination Declination of the center in decimal degree
   * @param radius radius in decimal degree
   * @param nside nside
   * @param scheme scheme
   * @param color color
   * @param alpha alpha composite [0..1]
   * @throws GraphException Exception
   */
  public CircleDecorator(final Graph graph, final double rightAscension, final double declination, final double radius, final int nside, final Scheme scheme,
                         final Color color, final float alpha) throws GraphException {
    super(graph, nside, scheme, color, alpha);
    this.range = computeIntersect(rightAscension, declination, radius);
  }

  /**
   * Constructs a new circle on the <code>graph</code> layer with a <code>color</code>
   * and an <code>alpha</code> composite. 
   * 
   * <p>The circle is defined by its center (<code>rigthAscension</code>, <code>declination</code>) and its
   * <code>radius</code> in decimal degree. The circle is computed as Healpix pixels at
   * <code>order</code> with the <code>scheme</code>.</p>
   *
   * @param graph graph component to decore
   * @param rigthAscension Right Ascension of the center in decimal degree
   * @param declination Declination of the center in decimal degree
   * @param radius radius in decimal degree
   * @param scheme scheme
   * @param order order
   * @param color color
   * @param alpha alpha composite [0..1]
   * @throws GraphException if the transformation into Healpix pixels failed.
   */
  public CircleDecorator(final Graph graph, final double rigthAscension, final double declination, final double radius, final Scheme scheme, final int order,
                         final Color color, final float alpha) throws GraphException {
    this(graph, rigthAscension, declination, radius, (int) Math.pow(2, order), scheme, color, alpha);
  }

  /**
   * Constructs a new circle on the <code>graph</code> layer. 
   *
   * <p>The circle is defined by its center (<code>rightAscension</code>, <code>declination</code>) and its
   * <code>radius</code> in decimal degree. The circle is computed as Healpix pixels at
   * <code>order</code> with the <code>scheme</code>.</p>
   *
   * <p>A default <i>CYAN</i> color is used to represent this circle.
   * Moreover <code>DEFAULT_ALPHA_COMOSITE</code> is used by default.</p>
   *
   * @param graph graph component to decore
   * @param rightAscension Right Ascension of the center in decimal degree
   * @param declination Declination of the center in decimal degree
   * @param radius radius in decimal degree
   * @param scheme scheme
   * @param order order
   * @throws GraphException Exception if the transformation into Healpix pixels failed.
   */
  public CircleDecorator(final Graph graph, final double rightAscension, final double declination, final double radius,
                         final Scheme scheme, final int order) throws GraphException {
    this(graph, rightAscension, declination, radius, (int) Math.pow(2, order), scheme, Color.CYAN, DEFAULT_ALPHA_COMOSITE);
  }

  /**
   * Computes and returns the Healpix pixels that intersect with the circle.
   *
   * @param rightAscension Right Ascension of the cirlce's center in decimal degree
   * @param declination Declination of the circle's center in decimal degree
   * @param radius radius in decimal degree
   * @return Returns the ranges of intersected pixels
   * @throws GraphException if Healpix transformation failed.
   */
  private RangeSet computeIntersect(final double rightAscension, final double declination, final double radius) throws GraphException {
    final Pointing point = new Pointing(Math.toRadians(Graph.DEC_MAX - declination), Math.toRadians(rightAscension));
      try {
          return this.getHealpixBase().queryDisc(point, Math.toRadians(radius));
      } catch (Exception ex) {
          LOG.log(Level.FINER, null, ex);
          throw new GraphException(ex);
      }
  }

  @Override
  protected final void drawPixels(final Graphics2D graphic2D, final Color color) {
    graphic2D.setPaint(color);
    try {
      final RangeSet.ValueIterator iter = this.range.valueIterator();
      while (iter.hasNext()) {
        final long pixel = iter.next();
        drawHealpixPolygon(graphic2D, getHealpixBase(), pixel, getCoordinateTransformation());
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Draws a Healpix pixel according to a coordinate system.
   *
   * <p>When the pixel area is "small", only the four points of the pixel are used to plot it.
   * When the pixel area is "large", intermediate points on boundaries pixel are automatically computed
   * to smooth the boundary of the pixel on the plot.</p>
   *
   * @param graphic2D graph component to decore
   * @param healpix Healpix index
   * @param pix Pixel to draw
   * @param coordinateTransformation Coordinate transformation
   */
  @Override
  protected final void drawHealpixPolygon(final Graphics2D graphic2D, final HealpixIndex healpix, final long pix, final CoordinateTransformation coordinateTransformation) {
    try {
      final int numberOfVectors = computeNumberPointsForPixel(getHealpixBase().getNside(), pix);
      final Vec3[] vectors = healpix.boundaries(pix, numberOfVectors);
      computeReferenceFrameTransformation(vectors, coordinateTransformation);
      final Coordinates[] shapes = splitHealpixPixelForDetectedBorder(vectors);
      final Polygon2D poly = new Polygon2D();
      for (int i = 0; i < shapes.length; i++) {
        final Coordinates shape = shapes[i];
        final List<Point2D.Double> pixels = shape.getPixelsFromProjection(this.getProjection(), getRange(), getPixelWidth(), getPixelHeight());
        poly.setPoints(pixels);
        graphic2D.draw(poly);
        graphic2D.fill(poly);
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }
}
