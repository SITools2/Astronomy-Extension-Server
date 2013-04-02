/******************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *****************************************************************************/
 
package fr.cnes.sitools.astro.representation;

import fr.cnes.sitools.astro.graph.Graph;
import fr.cnes.sitools.astro.graph.Utility;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

/**
 * PNG representation.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class PngRepresentation extends OutputRepresentation {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(PngRepresentation.class.getName());
  /**
   * Graph.
   */
  private Graph graph;
  /**
   * height of the PNG.
   */
  private int height;
  /**
   * width of the PNG.
   */
  private int width;

  /**
   * Creates a PNG representation.
   *
   * @param graphVal graph
   * @param widthVal width in pixels
   * @param heightVal heights in pixels
   */
  public PngRepresentation(final Graph graphVal, final int widthVal, final int heightVal) {
    super(MediaType.IMAGE_PNG);
    this.graph = graphVal;
    this.width = widthVal;
    this.height = heightVal;
  }

  /**
   * Writes the representation.
   *
   * @param out output
   * @throws IOException Exception
   */
  @Override
  public final void write(final OutputStream out) throws IOException {
    Utility.createPNG(graph, out, width, height);
  }
}
