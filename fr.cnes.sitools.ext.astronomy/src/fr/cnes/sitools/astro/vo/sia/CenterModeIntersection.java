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

import java.util.List;

  /**
   * Constructs SQL predicat for Center mode intersection.
   */
  public class CenterModeIntersection extends AbstractSqlGeometryConstraint {

    /**
     * Right ascension attribut.
     */
    private transient String raCol;
    /**
     * Declination attribut.
     */
    private transient String decCol;

    @Override
    public final void setGeometry(final Object geometry) {
      if (geometry instanceof String[]) {
        final String[] geometryArray = (String[]) geometry;
        if (geometryArray.length != 2) {
          throw new IllegalArgumentException("geometry must be an array of two elements that contains racolName and decColName");
        } else {
          this.raCol = geometryArray[0];
          this.decCol = geometryArray[1];
        }
      } else {
        throw new IllegalArgumentException("geometry must be an array of two elements that contains racolName and decColName");
      }
    }

    @Override
    public final String getSqlPredicat() {
      if (isPolesCollision()) {
        return null;
      }
      final List ranges = (List) computeRange();
      final List<Double[]> raRanges = (List<Double[]>) ranges.get(0);
      final double[] decRange = (double[]) ranges.get(1);
      String predicatDefinition;
      if (raRanges.size() == 1) {
        final Double[] raRange = raRanges.get(0);
        predicatDefinition = String.format(" AND ( %s BETWEEN %s AND %s ) AND ( %s BETWEEN %s AND %s )", decCol, decRange[0], decRange[1], raCol, raRange[0], raRange[1]);
      } else {
        final Double[] raRange1 = raRanges.get(0);
        final Double[] raRange2 = raRanges.get(1);
        predicatDefinition = String.format(" AND ( %s BETWEEN %s AND %s ) AND (( %s BETWEEN %s AND %s ) OR ( %s BETWEEN %s AND %s ))",
                                             decCol, decRange[0], decRange[1],
                                             raCol, raRange1[0], raRange1[1], raCol, raRange2[0], raRange2[1]);
      }
      return predicatDefinition;
    }
  }
