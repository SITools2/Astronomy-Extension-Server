/**
 * *****************************************************************************
 * Copyright 2012, 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * 
 * This file is part of SITools2.
 * 
 * SITools2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * SITools2. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package fr.cnes.sitools.astro.graph;

import java.awt.Graphics;

/**
 * This class is the decorator of the Graph component.
 *
 * <p> This pattern is designed so that multiple decorators can be stacked on
 * top of each other, each time adding a new functionality to the overridden
 * method(s).A decorator in this context is a plot on a graph such as
 * coordinates.</p>
 *
 * @author Jean-Christophe Malapert
 */
public abstract class AbstractGraphDecorator extends Graph {

  /**
   * Graph component.
   */
  private Graph graph;

  @Override
  public final com.jhlabs.map.proj.Projection getProjection() {
    return getGraph().getProjection();
  }

  @Override
  public final int getPixelWidth() {
    return getGraph().getPixelWidth();
  }

  @Override
  public final int getPixelHeight() {
    return getGraph().getPixelHeight();
  }

  @Override
  public final double[] getRange() {
    return getGraph().getRange();
  }

  @Override
  public abstract void paint(Graphics g);

  /**
   * Returns the graph.
   *
   * @return the graph
   */
  protected final Graph getGraph() {
    return graph;
  }

  /**
   * Sets the graph.
   *
   * @param graphObj the graph to set
   */
  protected final void setGraph(final Graph graphObj) {
    this.graph = graphObj;
  }
}
