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
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.plugins.resources.model.DataSetSelectionType;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.plugins.resources.model.ResourceParameter;
import fr.cnes.sitools.plugins.resources.model.ResourceParameterType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Configures the Images cutOut service.
 *
 * <p> This service returns a part of a FITS file according to a search area on one selected row from the dataset. This means that the row
 * must contain an URI to the FITS file. </p>
 * 
 * <p>This service answers to the following scenario:<br/>
 * As user, I want to cut a interest region from a FITS file in order to show only the interesting part.
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CutOutResourcePlugin extends ResourceModel {
  
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(CutOutResourcePlugin.class.getName());

  /**
   * RA keyword input parameter.
   */
  public static final String RA_INPUT_PARAMETER = "RA";
  /**
   * DEC keyword input parameter.
   */
  public static final String DEC_INPUT_PARAMETER = "DEC";
  /**
   * Radius keyword input parameter.
   */
  public static final String RADIUS_INPUT_PARAMETER = "Radius";
  /**
   * DataStorageName keyword.
   */
  public static final String DATA_STORAGE_NAME_PARAMETER = "DataStorageName";  
  /**
   * Hdu number input parameter.
   */
  public static final String HDU_NUMBER_INPUT_PARAMETER = "hduImageNumber";
  /**
   * FITS file input parameter.
   */
  public static final String FITS_FILE_INPUT_PARAMETER = "FileIdentifier";
  /**
   * Cube index.
   */
  public static final String FITS_CUBE_DEEP_INPUT_PARAMETER = "CubeIndex";
  /**
   * Output format.
   */
  public static final String IMAGE_FORMAT = "OutputFormat";
  /**
   * URI or URL of the FITS file.
   */
  public static final String URI_INPUT_FORMAT = "FitsURI";
  /**
   * Constructs the administration panel.
   */
  public CutOutResourcePlugin() {
    super();
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    setName("cut out service");
    setDescription("This service can extract a part of a FITS as a FITS or graphic files (JPEG). For this, the FITS image must contain valid WCS.");
    setResourceClassName(fr.cnes.sitools.extensions.astro.resource.CutOutResource.class.getName());
    this.setApplicationClassName(DataSetApplication.class.getName());
    this.setDataSetSelection(DataSetSelectionType.SINGLE);
    this.getParameterByName("methods").setValue("GET");
    
    
    final ResourceParameter rightAscension = new ResourceParameter(RA_INPUT_PARAMETER, "Central point of the cut out", ResourceParameterType.PARAMETER_USER_INPUT);
    rightAscension.setUserUpdatable(Boolean.TRUE);
    final ResourceParameter declination = new ResourceParameter(DEC_INPUT_PARAMETER, "Central point of the cut out", ResourceParameterType.PARAMETER_USER_INPUT);
    declination.setUserUpdatable(Boolean.TRUE);    
    final ResourceParameter radius = new ResourceParameter(RADIUS_INPUT_PARAMETER, "Radius in which the image will be cut", ResourceParameterType.PARAMETER_USER_INPUT);
    radius.setUserUpdatable(Boolean.TRUE);
    final ResourceParameter dataStorage = new ResourceParameter(DATA_STORAGE_NAME_PARAMETER, "If data storage is set,"
            + " then FitsFile is an identifier in this data storage", ResourceParameterType.PARAMETER_INTERN);
    final ResourceParameter fitsFile = new ResourceParameter(FITS_FILE_INPUT_PARAMETER, "Fits file to cut. This must be the file identifier if you "
            + "use a datasotrage otherwise, this is the URI or the URL of the FITS file", ResourceParameterType.PARAMETER_INTERN);
    fitsFile.setValueType("xs:dataset.columnAlias");
    final ResourceParameter hduNumber = new ResourceParameter(HDU_NUMBER_INPUT_PARAMETER, "HDU Image number to cut (start=1)", ResourceParameterType.PARAMETER_USER_INPUT);
    hduNumber.setValueType("xs:int");
    hduNumber.setValue("1");
    final ResourceParameter cubeIndex = new ResourceParameter(FITS_CUBE_DEEP_INPUT_PARAMETER, "Cube Index to extract (start=0)", ResourceParameterType.PARAMETER_USER_INPUT);
    cubeIndex.setValueType("xs:int");
    cubeIndex.setValue("0");
    final ResourceParameter outputFormat = new ResourceParameter(IMAGE_FORMAT, "Output format", ResourceParameterType.PARAMETER_USER_INPUT);
    outputFormat.setValue("FITS");
    outputFormat.setValueType("xs:enum-multiple[FITS,JPEG]");
    outputFormat.setUserUpdatable(Boolean.TRUE);
    addParam(rightAscension);
    addParam(declination);
    addParam(radius);
    addParam(dataStorage);
    addParam(fitsFile);
    addParam(hduNumber);
    addParam(cubeIndex); 
    addParam(outputFormat);
    this.completeAttachUrlWith("/cutOut");
  }

  /**
   * Returns
   * <code>true</code> when the object exists and is not empty(!="").
   *
   * @param object object to check
   * @return <code>true</code> when the object exists and is not empty(!=""); otherwise <code>false</code>
   */
  private static boolean isSet(final Object object) {
    return (object != null && !object.equals(""));
  }

  /**
   * Validates.
   *
   * @return error or warning
   */
  @Override
  public final Validator<ResourceModel> getValidator() {
    return new Validator<ResourceModel>() {
      @Override
      public final Set<ConstraintViolation> validate(final ResourceModel item) {
        final Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
        final Map<String, ResourceParameter> params = item.getParametersMap();
        final ResourceParameter fitsFile = params.get(FITS_FILE_INPUT_PARAMETER);
        final ResourceParameter hduNumber = params.get(HDU_NUMBER_INPUT_PARAMETER);

        if (!isSet(fitsFile.getValue())) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(fitsFile.getName());
          constraint.setMessage("Fits file must be set");
          constraintList.add(constraint);
        }
        
        if(!isSet(hduNumber.getValue())) {
            final ConstraintViolation constraint = new ConstraintViolation();
            constraint.setLevel(ConstraintViolationLevel.CRITICAL);
            constraint.setValueName(hduNumber.getName());
            constraint.setMessage("HDU number must be set");
            constraintList.add(constraint);
        } else {
            try {
                final int number = Integer.parseInt(hduNumber.getValue());
                if (number < 0) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setValueName(hduNumber.getName());
                    constraint.setMessage("HDU number must be >= 0");
                    constraint.setInvalidValue(hduNumber.getValue());
                    constraintList.add(constraint);
                }
            } catch (NumberFormatException ex) {
                final ConstraintViolation constraint = new ConstraintViolation();
                constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                constraint.setValueName(hduNumber.getName());
                constraint.setMessage("HDU number must be an integer");
                constraint.setInvalidValue(hduNumber.getValue());
                constraintList.add(constraint);
            }            
        }
        
//        if(!isSet(dataStorage.getValue())) {
//            final ConstraintViolation constraint = new ConstraintViolation();
//            constraint.setLevel(ConstraintViolationLevel.WARNING);
//            constraint.setValueName(dataStorage.getName());
//            constraint.setMessage("You do not use the datastorage, then check that "+FITS_FILE_INPUT_PARAMETER+" is either a URL or a URI");
//            constraintList.add(constraint);
//        }
        return constraintList;
      }
    };
  }
}
