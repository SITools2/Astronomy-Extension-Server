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
package fr.cnes.sitools.astro.representation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.data.MediaType;
import org.restlet.engine.Engine;
import org.restlet.representation.OutputRepresentation;

import fr.cnes.sitools.astro.graph.Graph;
import fr.cnes.sitools.astro.graph.Utility;

/**
 * PNG representation.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class PngRepresentation extends OutputRepresentation {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(PngRepresentation.class.getName());
  /**
   * Graph.
   */
  private Graph graph;
  /**
   * height of the PNG.
   */
  private int height;

  /**
   * Empty constructor.
   */
  protected PngRepresentation() {
    super(MediaType.IMAGE_PNG);
  }
  /**
   * Creates a PNG representation.
   *
   * @param graphVal graph
   * @param heightVal height in pixels
   */
  public PngRepresentation(final Graph graphVal, final int heightVal) {
    super(MediaType.IMAGE_PNG);
    setGraph(graphVal);
    setHeight(heightVal);
  }

  /**
   * Writes the representation.
   *
   * @param out output
   * @throws IOException Exception
   */
  @Override
  public final void write(final OutputStream out) throws IOException {
    LOG.log(Level.FINEST, "PNG size, h : {0}", getHeight());
    Utility.createPNG(getGraph(), out, getHeight());
  }

    /**
     * Returns the graph.
     * @return the graph
     */
    protected final Graph getGraph() {
        return graph;
    }

    /**
     * Sets the graph.
     * @param graphVal the graph to set
     */
    protected final void setGraph(final Graph graphVal) {
        this.graph = graphVal;
    }

    /**
     * Returns the height.
     * @return the height
     */
    protected final int getHeight() {
        return height;
    }

    /**
     * Sets the height in pixels.
     * @param heightVal the height to set
     */
    protected final void setHeight(final int heightVal) {
        this.height = heightVal;
    }
}
