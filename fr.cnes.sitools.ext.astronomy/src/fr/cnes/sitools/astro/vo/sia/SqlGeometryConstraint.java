/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.astro.vo.sia;

import fr.cnes.sitools.astro.vo.sia.SimpleImageAccessInputParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class SqlGeometryConstraint {

  protected static final int MIN = 0;
  protected static final int MAX = 1;

  public abstract void setGeometry(final Object geometry);

  public abstract void setInputParameters(final SimpleImageAccessInputParameters inputParameters);

  public abstract String getSqlPredicat();

  protected static Object computeRange(final SimpleImageAccessInputParameters inputParameters) {
    List<Double[]> raRange = new ArrayList<Double[]>();
    double[] decRange = new double[2];
    final double maxValueForDeclination = 90.0;
    final double minValueForDeclination = -90.0;
    final double maxValueForRightAscension = 360.0;
    final double minValueForRightAscension = 0.0;
    double raUser = inputParameters.getRa();
    double decUser = inputParameters.getDec();
    double[] sizeArray = inputParameters.getSize();
    // user input is a rectangle
    if (sizeArray.length == 2) {
      if (decUser - sizeArray[1] / 2.0 < minValueForDeclination) { // Pole south detection case
        decRange[MIN] = minValueForDeclination;
        decRange[MAX] = (decUser + sizeArray[1] / 2.0 > maxValueForDeclination) ? maxValueForDeclination : decUser + sizeArray[1] / 2.0;
        raRange.add(new Double[]{minValueForRightAscension, maxValueForRightAscension});
      } else if (decUser + sizeArray[1] / 2.0 > maxValueForDeclination) { // Pole north detection case
        decRange[MIN] = (decUser - sizeArray[1] / 2.0 < minValueForDeclination) ? minValueForDeclination : decUser - sizeArray[1] / 2.0;
        decRange[MAX] = maxValueForDeclination;
        raRange.add(new Double[]{minValueForRightAscension, maxValueForRightAscension});
      } else { // normal case
        decRange[MIN] = decUser - sizeArray[1] / 2.0;
        decRange[MAX] = decUser + sizeArray[1] / 2.0;
        double ramin = raUser - sizeArray[0] / 2.0;
        double ramax = raUser + sizeArray[0] / 2.0;
        if (ramin < minValueForRightAscension) {
          /**
           * __________________ | | |__ __| | | | | |__| |__| |__________________|
           */
          raRange.add(new Double[]{minValueForRightAscension, raUser - sizeArray[0] / 2.0});
          raRange.add(new Double[]{maxValueForRightAscension - (raUser - sizeArray[0] / 2.0), maxValueForRightAscension});
        } else if (ramax > maxValueForRightAscension) {
          /**
           * __________________ | | |__ __| | | | | |__| |__| |__________________|
           */
          raRange.add(new Double[]{raUser - sizeArray[0] / 2.0, maxValueForRightAscension});
          raRange.add(new Double[]{minValueForRightAscension, (raUser + sizeArray[0] / 2.0) - maxValueForRightAscension});
        } else {
          /**
           * __________________ | | | ____________ | | | | | | |____________| | |__________________|
           */
          raRange.add(new Double[]{raUser - sizeArray[0] / 2.0, raUser + sizeArray[0] / 2.0});
        }
      }
    } else {  // user input is a square
      if (decUser - sizeArray[0] / 2.0 < minValueForDeclination) { // Pole south detection case
        decRange[MIN] = minValueForDeclination;
        decRange[MAX] = (decUser + sizeArray[0] / 2.0 > maxValueForDeclination) ? maxValueForDeclination : decUser + sizeArray[0] / 2.0;
        raRange.add(new Double[]{minValueForRightAscension, maxValueForRightAscension});
      } else if (decUser + sizeArray[0] / 2.0 > maxValueForDeclination) { // Pole north detection case
        decRange[MIN] = (decUser - sizeArray[0] / 2.0 < minValueForDeclination) ? minValueForDeclination : decUser - sizeArray[0] / 2.0;
        decRange[MAX] = maxValueForDeclination;
        raRange.add(new Double[]{minValueForRightAscension, maxValueForRightAscension});
      } else { // normal case
        decRange[MIN] = decUser - sizeArray[0] / 2.0;
        decRange[MAX] = decUser + sizeArray[0] / 2.0;
        double ramin = raUser - sizeArray[0] / 2.0;
        double ramax = raUser + sizeArray[0] / 2.0;
        if (ramin < minValueForRightAscension) {
          /**
           * __________________ | | |__ __| | | | | |__| |__| |__________________|
           */
          raRange.add(new Double[]{minValueForRightAscension, raUser + sizeArray[0] / 2.0});
          raRange.add(new Double[]{maxValueForRightAscension, maxValueForRightAscension - (raUser - sizeArray[0] / 2.0)});
        } else if (ramax > maxValueForRightAscension) {
          /**
           * __________________ | | |__ __| | | | | |__| |__| |__________________|
           */
          raRange.add(new Double[]{raUser - sizeArray[0] / 2.0, maxValueForRightAscension});
          raRange.add(new Double[]{minValueForRightAscension, (raUser + sizeArray[0] / 2.0) - maxValueForRightAscension});
        } else {
          /**
           * __________________ | | | ____________ | | | | | | |____________| | |__________________|
           */
          raRange.add(new Double[]{raUser - sizeArray[0] / 2.0, raUser + sizeArray[0] / 2.0});
        }
      }
    }
    return Arrays.asList(raRange, decRange);
  }
}
