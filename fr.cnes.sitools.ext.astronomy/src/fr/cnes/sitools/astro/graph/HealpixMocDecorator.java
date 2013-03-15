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
 * ****************************************************************************
 */
package fr.cnes.sitools.astro.graph;

import cds.moc.Array;
import cds.moc.HealpixMoc;
import healpix.essentials.Scheme;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.logging.Logger;

/**
 * This objects contains methods to decorate a graph by a MOC.
 * 
 * <p>
 * Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new ImageBackGroundDecorator(graph, new File("/home/malapert/Documents/Equirectangular-projection.jpg"));
 * graph = new HealpixMocDecorator(graph, Color.RED, 0.8);
 * ((HealpixMocDecorator)graph).importMOC(healpixMoc);
 * graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
 * ((CircleDecorator)graph).setColor(Color.yellow); 
 * Utility.createJFrame(graph, 900, 500);
 * </code>
 * </pre>
 * </p>
 * @author Jean-Christophe Malapert
 */
public class HealpixMocDecorator extends HealpixGridDecorator {
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(HealpixMocDecorator.class.getName());

  /**
   * MOC
   */
  private HealpixMoc moc = null;

  /**
   * Constructs a MOC decorator.
   *
   * @param g graph to decorate
   * @param color color of the MOC
   * @param alpha transparency
   * @throws Exception Healpix Exception
   */
  public HealpixMocDecorator(Graph g, final Color color, float alpha) throws Exception {
    super(g, 1, Scheme.NESTED, color, alpha);
  }

  /**
   * Cosntructs a MOC decorator
   * @param g graph to decorate
   * @throws Exception Healpix Exception
   */
  public HealpixMocDecorator(final Graph g) throws Exception {
    this(g, Color.RED, 0.5f);
  }

  /**
   * Imports a MOC.
   * @param mocVal MOC
   */
  public final void importMoc(final HealpixMoc mocVal) {
    this.moc = mocVal;
  }

  @Override
  public void paint(final Graphics g) {
    getGraph().paint(g);
    Graphics2D g2 = (Graphics2D) g;
    Composite originalComposite = g2.getComposite();
    g2.setComposite(makeComposite(this.getAlpha()));
    drawPixels((Graphics2D) g, getColor());
    g2.setComposite(originalComposite);
  }

  @Override
  protected void drawPixels(final Graphics2D g2, final Color color) {
    if (this.moc != null) {
      g2.setPaint(color);
      int minOrder = this.moc.getMinLimitOrder();
      int maxOrder = this.moc.getMaxLimitOrder();
      for (int i = minOrder; i <= maxOrder; i++) {
        long nside = (long) Math.pow(2.0D, (double) i);
        try {
          getHealpixBase().setNside(nside);
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
        Array pixels = this.moc.getArray(i);
        for (int j = 0; j < pixels.getSize(); j++) {
          drawHealpixPolygon(g2, getHealpixBase(), pixels.get(j), getCoordinateTransformation());
        }
      }
    }
  }  
}
