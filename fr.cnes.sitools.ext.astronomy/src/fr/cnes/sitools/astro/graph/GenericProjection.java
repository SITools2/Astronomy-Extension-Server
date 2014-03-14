 /*******************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.engine.Engine;

import com.jhlabs.map.MapMath;
import com.jhlabs.map.proj.ProjectionException;
import com.jhlabs.map.proj.ProjectionFactory;

/**
 * Concrete component of the decorator pattern.
 *
 * <p> The concrete component works for whatever projection </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class GenericProjection extends Graph {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(GenericProjection.class.getName());
  /**
   * Default value for initialization.
   */
  private static final double DEFAULT_VALUE = 100;

  /**
   * Constructs a GenericProject.
   *
   * @param projectionType projection type
   */
  public GenericProjection(final ProjectionType projectionType) {
    this.setProjection(ProjectionFactory.fromPROJ4Specification(new String[]{"+proj=" + projectionType.getProjectionCode()}));
    this.setRange(analyzeRange());
  }

  /**
   * Computes xmin, xmax, ymin, ymax values of the projection and returns them.
   *
   * @return [xmin, xmax, ymin, ymax]
   */
  private double[] analyzeRange() {
    double ymin = DEFAULT_VALUE;
    double ymax = -1 * DEFAULT_VALUE;
    double xmin = DEFAULT_VALUE;
    double xmax = -1 * DEFAULT_VALUE;
    final double[] rangePixels = new double[NUMBER_VALUES_RANGE];
    for (int i = Graph.LAT_MIN; i <= Graph.LAT_MAX; i++) {
      for (int j = Graph.LONG_MIN; j <= Graph.LONG_MAX; j++) {
        final Point2D.Double point2D = new Point2D.Double();
        try {
          this.getProjection().project(MapMath.degToRad(j), MapMath.degToRad(i), point2D);
        } catch (ProjectionException ex) {
          LOG.log(Level.SEVERE, ex.getMessage());
        }

        if (point2D.getY() < ymin) {
          ymin = point2D.getY();
        } else if (point2D.getY() > ymax) {
          ymax = point2D.getY();
        }
        if (point2D.getX() < xmin) {
          xmin = point2D.getX();
        } else if (point2D.getX() > xmax) {
          xmax = point2D.getX();
        }
      }
    }
    rangePixels[X_MIN] = xmin;
    rangePixels[X_MAX] = xmax;
    rangePixels[Y_MIN] = ymin;
    rangePixels[Y_MAX] = ymax;
    return rangePixels;
  }
}
