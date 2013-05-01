/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.astro.vo.sia;

import fr.cnes.sitools.astro.vo.sia.SimpleImageAccessInputParameters;
import java.util.List;

  /**
   * Constructs SQL predicat for Center mode intersection.
   */
  public class CenterModeIntersection extends SqlGeometryConstraint {

    /**
     * User input parameters.
     */
    private SimpleImageAccessInputParameters inputParameters;
    /**
     * Right ascension attribut.
     */
    private String raCol;
    /**
     * Declination attribut.
     */
    private String decCol;

    /**
     * Empty constructor.
     */
    public CenterModeIntersection() {
    }

    @Override
    public final void setGeometry(final Object geometry) {
      if (geometry instanceof String[]) {
        String[] geometryArray = (String[]) geometry;
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
    public final void setInputParameters(final SimpleImageAccessInputParameters inputParametersVal) {
      this.inputParameters = inputParametersVal;
    }

    @Override
    public final String getSqlPredicat() {      
      List ranges = (List) computeRange(inputParameters);
      List<Double[]> raRanges = (List<Double[]>) ranges.get(0);
      double[] decRange = (double[]) ranges.get(1);
      String predicatDefinition;
      if (raRanges.size() == 1) {
        Double[] raRange = raRanges.get(0);
        predicatDefinition = String.format(" AND ( %s BETWEEN %s AND %s ) AND ( %s BETWEEN %s AND %s )", decCol, decRange[0], decRange[1], raCol, raRange[0], raRange[1]);
      } else {
        Double[] raRange1 = raRanges.get(0);
        Double[] raRange2 = raRanges.get(1);
        predicatDefinition = String.format(" AND ( %s BETWEEN %s AND %s ) AND (( %s BETWEEN %s AND %s ) OR ( %s BETWEEN %s AND %s ))", 
                                             decCol, decRange[0], decRange[1], 
                                             raCol, raRange1[0], raRange1[1], raCol, raRange2[0], raRange2[1]);
      }
      return predicatDefinition;
    }
  }    

