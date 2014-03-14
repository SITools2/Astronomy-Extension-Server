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
package fr.cnes.sitools.astro.vo.sia;

/**
 * Chooses the implementation of the SQL request
 * based on the geometry intersection algorithm.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public final class SqlGeometryFactory {
  /**
   * Empty constructor.
   */
  private SqlGeometryFactory() {
  }
  /**
   * Returns the Object responsible of creating the SQL request to send to the server.
   *
   * <p>
   * The supported geometryIntersection is either CENTER or OVERLAPS otherwise
   * an IllegalArgumentException is raised.
   * </p>
   * @param geometryIntersection Geometry Intersection algorithm
   * @return the Object responsible of creating the SQL request to send to the server
   */
  public static AbstractSqlGeometryConstraint create(final String geometryIntersection) {
    AbstractSqlGeometryConstraint result;
    if ("OVERLAPS".equals(geometryIntersection)) {
      result = new OverlapsModeIntersection();
    } else if ("CENTER".equals(geometryIntersection)) {
      result = new CenterModeIntersection();
    } else {
      throw new IllegalArgumentException("geometryMode " + geometryIntersection + " is unknown or not supported");
    }
    return result;
  }
}
