/*******************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES.
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
 ******************************************************************************/
package fr.cnes.sitools.extensions.astro.converter;

import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.dataset.converter.business.AbstractConverter;
import fr.cnes.sitools.dataset.converter.model.ConverterParameter;
import fr.cnes.sitools.dataset.converter.model.ConverterParameterType;
import fr.cnes.sitools.datasource.jdbc.model.AttributeValue;
import fr.cnes.sitools.datasource.jdbc.model.Record;
import fr.cnes.sitools.util.Util;
import healpix.core.AngularPosition;
import healpix.tools.CoordTransform;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts a coordinates system to another one.
 *
 * <p>CoordinatesTransformationConverter provides a list of converters for converting a reference frame to another one - ECL2EQ : Ecliptic
 * to Equatorial - ECL2GAL : Ecliptic to Galactic - EQ2ECL : Equatorial to Ecliptic - EQ2GAL : Equatorial to Galactic - GAL2ECL : Galactic
 * to Ecliptic - GAL2EQ : Galactic to Equatorial </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CoordinatesTransformationConverter extends AbstractConverter {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(CoordinatesTransformationConverter.class.getName());

  /**
   * Constructs a converter.
   */
  public CoordinatesTransformationConverter() {
    setName("Coordinates Transformation");
    setDescription("Coordinates transformation: ECL2EQ, ECL2GAL, EQ2ECL, EQ2GAL, GAL2ECL, GAL2EQ");
    setClassAuthor("J-C Malapert");
    setClassVersion("1.0");
    setClassOwner("CNES");
    final ConverterParameter longitude = new ConverterParameter("longitude",
            "Longitude (Ra, galactic longitude) in decimal degree", ConverterParameterType.CONVERTER_PARAMETER_IN);
    final ConverterParameter latitude = new ConverterParameter("latitude",
            "Latitude (Dec, galactic latitude) in decimal degree", ConverterParameterType.CONVERTER_PARAMETER_IN);
    final ConverterParameter longitudeConverted = new ConverterParameter("converted longitude",
            "Longitude (Ra, galactic longitude) in decimal degree", ConverterParameterType.CONVERTER_PARAMETER_OUT);
    final ConverterParameter latitudeConverted = new ConverterParameter("converted latitude",
            "Latitude (Dec, galactic latitude) in decimal degree", ConverterParameterType.CONVERTER_PARAMETER_OUT);
    final ConverterParameter precision = new ConverterParameter("precision", "result precision for double (#0.00)",
            ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    precision.setValue("#0.00");
    precision.setValueType("String");
    final ConverterParameter transformation = new ConverterParameter("conversionType",
            "one of the following values: ECL2EQ, ECL2GAL, EQ2ECL, EQ2GAL, GAL2ECL, GAL2EQ",
            ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    transformation.setValue("EQ2GAL");
    transformation.setValueType("xs:enum[ECL2EQ,ECL2GAL,EQ2ECL,EQ2GAL,GAL2ECL,GAL2EQ]");
    addParam(longitude);
    addParam(latitude);
    addParam(longitudeConverted);
    addParam(latitudeConverted);
    addParam(transformation);
    addParam(precision);
  }

  @Override
  public final Record getConversionOf(final Record record) throws Exception {
    Record out = record;

    final Object attrLongitude = getInParam("longitude", record).getValue();
    final Object attrLatitude = getInParam("latitude", record).getValue();
    if (Util.isSet(attrLongitude) && Util.isSet(attrLatitude)) {
      final AttributeValue attrConvertedLongitude = getOutParam("converted longitude", record);
      final AttributeValue attrConvertedLatitude = getOutParam("converted latitude", record);
      final String conversionType = getInternParam("conversionType").getValue();

      final double latitude = Double.valueOf(String.valueOf(attrLatitude));
      final double longitude = Double.valueOf(String.valueOf(attrLongitude));
      final AngularPosition angularPosition = new AngularPosition(latitude, longitude);

      int transformationType = 0;
      if (conversionType.equals("ECL2EQ")) {
        transformationType = CoordTransform.ECL2EQ;
      } else if (conversionType.equals("ECL2GAL")) {
        transformationType = CoordTransform.ECL2GAL;
      } else if (conversionType.equals("EQ2ECL")) {
        transformationType = CoordTransform.EQ2ECL;
      } else if (conversionType.equals("EQ2GAL")) {
        transformationType = CoordTransform.EQ2GAL;
      } else if (conversionType.equals("GAL2ECL")) {
        transformationType = CoordTransform.GAL2ECL;
      } else if (conversionType.equals("GAL2EQ")) {
        transformationType = CoordTransform.GAL2EQ;
      } else {
        throw new IllegalArgumentException("Transformation type is not recognized");
      }
      // convert the coordinates
      final AngularPosition newPosition = CoordTransform.transformInDeg(angularPosition, transformationType);

      final String latitudeStr = roundNumber(newPosition.theta());
      final String longitudeStr = roundNumber(newPosition.phi());

      LOG.log(Level.FINEST, "conversion {0} : ({1},{2}) to ({3},{4})",
              new Object[]{conversionType, longitude, latitude, longitudeStr, latitudeStr});

      // set the converted coordinates
      attrConvertedLongitude.setValue(longitudeStr);
      attrConvertedLatitude.setValue(latitudeStr);

      LOG.log(Level.FINEST, "Conversion of record into {0},{1}", new Object[]{longitudeStr, latitudeStr});
    }
    return out;
  }

  @Override
  public final Validator<?> getValidator() {
    return new Validator<AbstractConverter>() {
      @Override
      public final Set<ConstraintViolation> validate(final AbstractConverter item) {
        final Set<ConstraintViolation> constraints = new HashSet<ConstraintViolation>();
        final Map<String, ConverterParameter> params = item.getParametersMap();

        // Check if the transformation is a part of the supported transformations
        ConverterParameter param = params.get("conversionType");
        final List listReferenceFrameConv = Arrays.asList("ECL2EQ", "ECL2GAL", "EQ2ECL", "EQ2GAL", "GAL2ECL", "GAL2EQ");
        if (!listReferenceFrameConv.contains(param.getValue())) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("A value from " + listReferenceFrameConv.toString() + " must be choosen");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraint.setInvalidValue(param.getValue());
          constraints.add(constraint);
        }

        // Check quickly if the procecision is set and correct
        param = params.get("precision");
        if (!param.getValue().startsWith("#")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("Precision must start by #");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraint.setInvalidValue(param.getValue());
          constraints.add(constraint);
        }
        return constraints;
      }
    };
  }

  /**
   * Round a number.
   *
   * @param d the number
   * @return the rounded number
   */
  public final String roundNumber(final double d) {
    final NumberFormat formatter = new DecimalFormat(this.getInternParam("precision").getValue(),
            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    return formatter.format(d);
  }
}
