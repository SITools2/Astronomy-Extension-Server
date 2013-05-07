/*******************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * 
 * This directory is part of SITools2.
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

import fr.cnes.sitools.SearchGeometryEngine.Resampling;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Constructs SQL predicat for Overlaps mode intersection.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OverlapsModeIntersection extends SqlGeometryConstraint {
  
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OverlapsModeIntersection.class.getName());  
  
  /**
   * Default value for resampling along right ascension axis.
   */
  private static final double DEFAULT_SAMPLING_VALUE_RA = 20;
  
  /**
   * Default value for resampling along declination axis.
   */
  private static final double DEFAULT_SAMPLING_VALUE_DEC = 20;
  
  /**
   * geometry attribut.
   */
  private String geomCol;

  /**
   * Empty constructor.
   */
  public OverlapsModeIntersection() {
  }

  @Override
  public final void setGeometry(final Object geometry) {
    if (geometry instanceof String) {
      this.geomCol = String.valueOf(geometry);
    } else {
      throw new IllegalArgumentException("geometry must be a String");
    }
  }

  @Override
  public final String getSqlPredicat() {
    if (this.isPolesCollision()) {
      return null;
    }

    List ranges = (List) computeRange();
    List<Double[]> raRanges = (List<Double[]>) ranges.get(0);
    double[] decRange = (double[]) ranges.get(1);

    String predicatDefinition;
    if (isNorthPoleCollision()) {
      LOG.log(Level.FINEST, "North collision case");
      predicatDefinition = String.format(" AND (spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 0.0, decRange[0], 45.0, 90.0, 90.0, decRange[0]);
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 90.0, decRange[MIN], 135.0, 90.0, 180.0, decRange[MIN]));
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 180.0, decRange[MIN], 225.0, 90.0, 270.0, decRange[MIN]));
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}'))", geomCol, 270.0, decRange[MIN], 325.0, 90.0, 360.0, decRange[MIN]));
      LOG.log(Level.FINEST, predicatDefinition);

    } else if (isSouthPoleCollision()) {
      LOG.log(Level.FINEST, "South collision case");
      predicatDefinition = String.format(" AND (spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 0.0, decRange[MAX], 45.0, -90.0, 90.0, decRange[MAX]);
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 90.0, decRange[MAX], 135.0, -90.0, 180.0, decRange[MAX]));
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 180.0, decRange[MAX], 225.0, -90.0, 270.0, decRange[MAX]));
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}'))", geomCol, 270.0, decRange[MAX], 325.0, -90.0, 360.0, decRange[MAX]));
      LOG.log(Level.FINEST, predicatDefinition);

    } else if (isRing()) {
      LOG.log(Level.FINEST, "Ring case");
      predicatDefinition = String.format(" AND (spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd), (%sd,%sd)}')", geomCol, 0.0, decRange[MIN], 90.0, decRange[MIN], 90.0, decRange[MAX], 0.0, decRange[MAX]);
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd), (%sd,%sd)}')", geomCol, 90.0, decRange[MIN], 180.0, decRange[MIN], 180.0, decRange[MAX], 90.0, decRange[MAX]));
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd), (%sd,%sd)}')", geomCol, 180.0, decRange[MIN], 270.0, decRange[MIN], 270.0, decRange[MAX], 180.0, decRange[MAX]));
      predicatDefinition = predicatDefinition.concat(String.format(" OR spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd), (%sd,%sd)}'))", geomCol, 270.0, decRange[MIN], 0.0, decRange[MIN], 0.0, decRange[MAX], 270.0, decRange[MAX]));
      LOG.log(Level.FINEST, predicatDefinition);
      
    } else if (raRanges.size() == 1 && isLargePolygon()) {
      LOG.log(Level.FINEST, "Large polygon case");
      Double[] raRange1 = raRanges.get(0);
      double mean = (raRange1[MIN] + raRange1[MAX]) / 2.0;
      raRanges.add(new Double[]{mean, raRange1[MAX]});
      raRanges.set(0, new Double[]{raRange1[MIN], mean});
      predicatDefinition = buildMultiPolygon(raRanges, decRange);
      LOG.log(Level.FINEST, predicatDefinition);
    } else {
      LOG.log(Level.FINEST, "other case");
      predicatDefinition = buildMultiPolygon(raRanges, decRange);
      LOG.log(Level.FINEST, predicatDefinition);
    }
    return predicatDefinition;
  }
  
  /**
   * Builds a multi-polygon syntax for PgSphere based on a set right ascension ranges and a declination range.
   * 
   * <p>
   * A range is an array that is composed of two values : min and max value.
   * </p>
   *
   * @param raRanges set of right ascension ranges
   * @param decRange declination range
   * @return the SQL predicat
   */
  private String buildMultiPolygon(final List<Double[]> raRanges, final double[] decRange) {
    StringBuilder sb = new StringBuilder(" AND (");
    buildPolygon(sb, raRanges.get(0), decRange);
    if (raRanges.size() == 2) {
      sb.append(" OR ");
      buildPolygon(sb, raRanges.get(1), decRange);
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * Build a polygon syntax for PgSphere based on the SQL predicat to complete, the right ascension and declination range.
   * <p>
   * A range is an array that is composed of two values : min and max value.
   * </p>
   * @param sb SQL predicat to complete
   * @param raRange right ascension range [min,max]
   * @param decRange declination range [min,max]
   */
  private void buildPolygon(final StringBuilder sb, final Double[] raRange, final double[] decRange) {
    sb.append(String.format("spoly_overlap_polygon(%s,'{", geomCol));
    double[] pointsRa = Resampling.hourCircle(raRange[MIN], raRange[MAX], DEFAULT_SAMPLING_VALUE_RA);
    double[] pointsDec = Resampling.decCircle(decRange[MIN], decRange[MAX], DEFAULT_SAMPLING_VALUE_DEC);
    buildRaLine(sb, pointsRa, decRange[MIN], false);
    buildDecLine(sb, pointsDec, raRange[MAX], false);
    buildRaLine(sb, pointsRa, decRange[MAX], true);
    buildDecLine(sb, pointsDec, raRange[MIN], true);
    sb.deleteCharAt(sb.length() - 1);
    sb.append("}')");
  }

  /**
   * Builds a line of the polygon for the SIAP request along the right ascension axis.
   * @param sb polygon to complete
   * @param raCoordinates values in the line along right ascension axis
   * @param staticDecCoordinate constant declination.
   * @param isReverseOrder indicates if the order where <code>raCoordinates</code> array must be read
   */
  private void buildRaLine(final StringBuilder sb, final double[] raCoordinates, final double staticDecCoordinate, final boolean isReverseOrder) {
    if (isReverseOrder) {
      for (int i = raCoordinates.length - 1; i > 0; i--) {
        sb.append(String.format("(%sd,%sd),", raCoordinates[i], staticDecCoordinate));
      }
    } else {
      for (int i = 0; i < raCoordinates.length - 1; i++) {
        sb.append(String.format("(%sd,%sd),", raCoordinates[i], staticDecCoordinate));
      }
    }
  }

    /**
   * Builds a line of the polygon for the SIAP request along the declination axis.
   * @param sb polygon to complete
   * @param decCoordinates values in the line along declination axis
   * @param staticRaCoordinate constant right ascension.
   * @param isReverseOrder indicates if the order where <code>raCoordinates</code> array must be read
   */
  private void buildDecLine(final StringBuilder sb, final double[] decCoordinates, final double staticRaCoordinate, final boolean isReverseOrder) {
    if (isReverseOrder) {
      for (int i = decCoordinates.length - 1; i > 0; i--) {
        sb.append(String.format("(%sd,%sd),", staticRaCoordinate, decCoordinates[i]));
      }
    } else {
      for (int i = 0; i < decCoordinates.length - 1; i++) {
        sb.append(String.format("(%sd,%sd),", staticRaCoordinate, decCoordinates[i]));
      }
    }
  }
}
