/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.astro.vo.sia;

import fr.cnes.sitools.astro.vo.sia.SimpleImageAccessInputParameters;
import java.util.List;

  public class OverlapsModeIntersection extends SqlGeometryConstraint {
    
    /**
     * User input parameters.
     */
    private SimpleImageAccessInputParameters inputParameters;
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
    public void setGeometry(Object geometry) {
      if (geometry instanceof String) {
        this.geomCol = String.valueOf(geometry);
      } else {
        throw new IllegalArgumentException("geometry must be a String");
      }      
    }

    @Override
    public void setInputParameters(SimpleImageAccessInputParameters inputParametersVal) {
      this.inputParameters = inputParametersVal;
    }

    @Override
    public String getSqlPredicat() {
      List ranges = (List) computeRange(inputParameters);
      List<Double[]> raRanges = (List<Double[]>) ranges.get(0);
      double[] decRange = (double[]) ranges.get(1);
      
      String predicatDefinition;
      if (decRange[1] == 90.0) {        
        predicatDefinition = String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 0.0, decRange[0], 45.0, 90.0, 90.0, decRange[0]);
        predicatDefinition = predicatDefinition.concat(String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 90.0, decRange[MIN], 135.0, 90.0, 180.0, decRange[MIN]));
        predicatDefinition = predicatDefinition.concat(String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 180.0, decRange[MIN], 225.0, 90.0, 270.0, decRange[MIN]));
        predicatDefinition = predicatDefinition.concat(String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 270.0, decRange[MIN], 325.0, 90.0, 360.0, decRange[MIN]));

      } else if (decRange[0] == -90.0) {
        predicatDefinition = String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 0.0, decRange[MAX], 45.0, -90.0, 90.0, decRange[MAX]);
        predicatDefinition = predicatDefinition.concat(String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 90.0, decRange[MAX], 135.0, -90.0, 180.0, decRange[MAX]));
        predicatDefinition = predicatDefinition.concat(String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 180.0, decRange[MAX], 225.0, -90.0, 270.0, decRange[MAX]));
        predicatDefinition = predicatDefinition.concat(String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, 270.0, decRange[MAX], 325.0, -90.0, 360.0, decRange[MAX]));        
        
      } else if (ranges.size() == 1) {
        Double[] raRange1 = (Double[]) raRanges.get(0);
        predicatDefinition = String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, raRange1[MIN], decRange[MIN], raRange1[MAX], decRange[MIN], raRange1[MAX], decRange[MAX], raRange1[MIN], decRange[MAX]);
      } else { //ranges.size() ==2)
        Double[] raRange1 = (Double[]) raRanges.get(0);
        predicatDefinition = String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, raRange1[MIN], decRange[MIN], raRange1[MAX], decRange[MIN], raRange1[MAX], decRange[MAX], raRange1[MIN], decRange[MAX]);
        Double[] raRange2 = (Double[]) raRanges.get(1);
        predicatDefinition = predicatDefinition.concat(String.format(" AND spoly_overlap_polygon(%s,'{(%sd,%sd),(%sd,%sd),(%sd,%sd),(%sd,%sd)}')", geomCol, raRange2[MIN], decRange[MIN], raRange2[MAX], decRange[MIN], raRange2[MAX], decRange[MAX], raRange2[MIN], decRange[MAX]));                
      } 
      return predicatDefinition;
    }
    
  }
