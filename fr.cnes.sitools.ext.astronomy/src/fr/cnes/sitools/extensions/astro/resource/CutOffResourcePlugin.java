/**
 * *****************************************************************************
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
 *****************************************************************************
 */
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
 * Configures the Images cutOff service.
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
public class CutOffResourcePlugin extends ResourceModel {
  
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(CutOffResourcePlugin.class.getName());

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
  public static final String HDU_NUMBER_INPUT_PARAMETER = "hduNumber";
  /**
   * FITS file input parameter.
   */
  public static final String FITS_FILE_INPUT_PARAMETER = "FitsFile";  
  /**
   * Constructs the administration panel.
   */
  public CutOffResourcePlugin() {
    super();
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    setName("Cut off service");
    setDescription("This service can extract a part of a FITS as a FITS or graphic files (PNG, GIF). For this, the FITS image must contain valid WCS.");
    setResourceClassName(fr.cnes.sitools.extensions.astro.resource.CutOffResource.class.getName());
    this.setApplicationClassName(DataSetApplication.class.getName());
    this.setDataSetSelection(DataSetSelectionType.SINGLE);
    this.getParameterByName("methods").setValue("GET");
    final ResourceParameter rightAscension = new ResourceParameter(RA_INPUT_PARAMETER, "Central point of the cut off", ResourceParameterType.PARAMETER_USER_INPUT);
    final ResourceParameter declination = new ResourceParameter(DEC_INPUT_PARAMETER, "Central point of the cut off", ResourceParameterType.PARAMETER_USER_INPUT);
    final ResourceParameter radius = new ResourceParameter(RADIUS_INPUT_PARAMETER, "Radius in which the image will be cut", ResourceParameterType.PARAMETER_USER_INPUT);
    final ResourceParameter dataStorage = new ResourceParameter(DATA_STORAGE_NAME_PARAMETER, "If data storage is set,"
            + " then FitsFile is an identifier in this data storage", ResourceParameterType.PARAMETER_USER_INPUT);
    final ResourceParameter fitsFile = new ResourceParameter(FITS_FILE_INPUT_PARAMETER, "Fits file to cut", ResourceParameterType.PARAMETER_INTERN);
    fitsFile.setValueType("xs:dataset.columnAlias");
    final ResourceParameter hduNumber = new ResourceParameter(HDU_NUMBER_INPUT_PARAMETER, "HDU number to cut", ResourceParameterType.PARAMETER_INTERN);
    addParam(rightAscension);
    addParam(declination);
    addParam(radius);
    addParam(dataStorage);
    addParam(fitsFile);    
    addParam(hduNumber);
    this.completeAttachUrlWith("/cutoff");
  }

  /**
   * Returns
   * <code>true</code> when the object exists and is not empty(!="").
   *
   * @param object object to check
   * @return <code>true</code> when the object exists and is not empty(!=""); otherwise <code>false</code>
   */
  private static boolean isSet(final Object object) {
    return (object != null && !object.equals("")) ? true : false;
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
        final ResourceParameter fitsFile = params.get("FitsFile");
        final ResourceParameter hduNumber = params.get("hduNumber");

        if (!isSet(fitsFile)) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(fitsFile.getName());
          constraint.setMessage("Fits file must be set");
          constraintList.add(constraint);
        }

        if (!isSet(hduNumber)) {
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
        return constraintList;
      }
    };
  }
}
