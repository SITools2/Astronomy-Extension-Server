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

import cds.moc.Array;
import cds.moc.HealpixMoc;
import healpix.core.HealpixIndex;
import healpix.essentials.Scheme;
import healpix.essentials.Vec3;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This objects contains methods to decorate a graph by a MOC.
 * 
 * <p>Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new ImageBackGroundDecorator(graph, new File("/home/malapert/Documents/Equirectangular-projection.jpg"));
 * graph = new HealpixMocDecorator(graph, Color.RED, 0.8);
 * ((HealpixMocDecorator)graph).importMOC(healpixMoc);
 * graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
 * ((CircleDecorator)graph).setColor(Color.yellow); 
 * Utility.createJFrame(graph, 900);
 * </code>
 * </pre></p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class HealpixMocDecorator extends HealpixGridDecorator {
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(HealpixMocDecorator.class.getName());

  /**
   * MOC.
   */
  private transient HealpixMoc moc = null;
  
  /**
   * Constructs a MOC decorator.
   *
   * @param graph graph to decorate
   * @param color color of the MOC
   * @param alpha transparency
   * @throws Exception Healpix Exception
   */
  public HealpixMocDecorator(final Graph graph, final Color color, final float alpha) throws Exception {
    super(graph, 1, Scheme.NESTED, color, alpha);
  }

  /**
   * Cosntructs a MOC decorator.
   * @param graph graph to decorate
   * @throws Exception Healpix Exception
   */
  public HealpixMocDecorator(final Graph graph) throws Exception {
    this(graph, Color.RED, DEFAULT_TRANSPARENCY);
  }

  /**
   * Imports a MOC.
   * @param mocVal MOC
   */
  public final void importMoc(final HealpixMoc mocVal) {
    this.moc = mocVal;
  }

  @Override
  public void paint(final Graphics graphic) {
    getGraph().paint(graphic);
    final Graphics2D graphic2D = (Graphics2D) graphic;
    final Composite originalComposite = graphic2D.getComposite();
    graphic2D.setComposite(makeComposite(this.getAlpha()));
    drawPixels((Graphics2D) graphic, getColor());
    graphic2D.setComposite(originalComposite);
  }

  @Override
  protected void drawPixels(final Graphics2D graphic2D, final Color color) {
    if (this.moc != null) {
      graphic2D.setPaint(color);      
      final int minOrder = this.moc.getMinLimitOrder();
      final int maxOrder = this.moc.getMaxLimitOrder();
      for (int i = minOrder; i <= maxOrder; i++) {
        final long nside = (long) Math.pow(2.0D, (double) i);
        try {
          getHealpixBase().setNside(nside);
        } catch (Exception ex) {
          throw new GraphRuntimeException(ex);
        }
        final Array pixels = this.moc.getArray(i);
        for (int j = 0; j < pixels.getSize(); j++) {
          drawHealpixPolygon(graphic2D, getHealpixBase(), pixels.get(j), getCoordinateTransformation());
        }
      }
    }
  }
  
  @Override
  protected void drawHealpixPolygon(final Graphics2D graphic2D, final HealpixIndex healpix, final long pix, final CoordinateTransformation coordinateTransformation) {
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
      LOG.log(Level.FINER, null, ex);
    }
  }  
}
