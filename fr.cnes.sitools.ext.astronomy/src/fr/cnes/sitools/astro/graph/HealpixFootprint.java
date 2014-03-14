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

import org.restlet.engine.Engine;

/**
 * This object contains methods to decorate a graph by footprints.
 * 
 * <p>Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new ImageBackGroundDecorator(graph, new File("/home/malapert/Documents/Equirectangular-projection.jpg"));
 * HealpixMapDouble map = new HealpixMapDouble((long)Math.pow(2, 6), Scheme.RING);
 * map.setPixel(741, 1.0d);
 * map.setPixel(742, 1.0d);
 * map.setPixel(820, 1.0d);
 * map.setPixel(822, 1.0d);
 * map.setPixel(903, 1.0d);
 * map.setPixel(904, 1.0d);        
 * map.setPixel(905, 1.0d);        
 * map.setPixel(991, 1.0d);        
 * map.setPixel(992, 1.0d); 
 * graph = new HealpixFootprint(graph, Scheme.RING, 6, 1.0f);
 * ((HealpixFootprint)graph).importHealpixMap(map, CoordinateTransformation.NATIVE);
 * ((HealpixFootprint)graph).setColor(Color.RED);
 * graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
 * ((CircleDecorator)graph).setColor(Color.yellow); 
 * Utility.createJFrame(graph, 900);
 * </code>
 * </pre></p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class HealpixFootprint extends HealpixDensityMapDecorator {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(HealpixFootprint.class.getName());

  /**
   * Cronstructs a footprint.
   *
   * @param graph graph to decorate
   * @param scheme Healpix scheme
   * @param order Healpix order
   * @param alpha transparency
   * @throws Exception if Healpix Exception happens
   */
  public HealpixFootprint(final Graph graph, final Scheme scheme, final int order, final float alpha) throws Exception {
    super(graph, scheme, order, alpha);
  }

  /**
   * Constructs a footprint.
   *
   * @param graph grap to decorate
   * @param nside Healpix nside
   * @param scheme Healpix scheme
   * @param alpha transparency
   * @throws Exception if Healpix Exception happens
   */
  public HealpixFootprint(final Graph graph, final int nside, final Scheme scheme, final float alpha) throws Exception {
    super(graph, nside, scheme, alpha);
  }

  /**
   * Draws a Healpix pixel.
   *
   * @param graphic2D graph to decorate
   * @param healpix Helapix index
   * @param pix pixel to draw
   * @param coordinateTransformation Coordinate transformation to apply
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
        graphic2D.setPaint(this.getColor());
        poly.setPoints(pixels);
        graphic2D.draw(poly);
        graphic2D.fill(poly);
      }
    } catch (Exception ex) {
      LOG.log(Level.FINER, null, ex);
    }
  }

  /**
   * Draws pixels having its density=1.0d.
   *
   * @param graphic2D graph to decorate
   * @param color color of pixels
   */
  @Override
  protected void drawPixels(final Graphics2D graphic2D, final Color color) {
    graphic2D.setPaint(color);
    final HealpixMapDouble map = this.getHealpixMapDouble();
    final double[] pixels = map.getData();
    for (int i = 0; i < pixels.length; i++) {
      if (pixels[i] == 1.0d) {
        drawHealpixPolygon(graphic2D, getHealpixBase(), i, getCoordinateTransformation());
      }
    }
  }
}
