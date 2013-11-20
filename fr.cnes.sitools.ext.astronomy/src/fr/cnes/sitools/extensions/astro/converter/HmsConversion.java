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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jsky.coords.HMS;

/**
 * HmsConversion provides transformation from decimal degree to sexagesimal notation for longitude axis.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @see The conversion from sexagesimal to decimal coordinates is done by the use of <a href="http://jsky.sourceforge.net/">jsky</a>.
 */
public class HmsConversion extends AbstractConverter {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(HmsConversion.class.getName());

  /**
   * Construct a converter.
   */
  public HmsConversion() {
    setName("HmsDegConversion");
    setDescription("Sexadecimal to decimal degree or decimal degree to sexadecimal");
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    final ConverterParameter rightAscension = new ConverterParameter("Ra in", "Right ascension attribute as input",
            ConverterParameterType.CONVERTER_PARAMETER_IN);
    final ConverterParameter valInHms = new ConverterParameter("Ra out", "Right ascension attribute as output",
            ConverterParameterType.CONVERTER_PARAMETER_OUT);
    final ConverterParameter deg2segOrseg2dev = new ConverterParameter("Conversion type",
            "Set conversion type: sexa2deg or deg2sexa", ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    deg2segOrseg2dev.setValue("deg2sexa");
    deg2segOrseg2dev.setValueType("String");
    final ConverterParameter precision = new ConverterParameter("precision", "result precision for double (#0.00)",
            ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    precision.setValue("#0.00");
    precision.setValueType("String");
    addParam(rightAscension);
    addParam(valInHms);
    addParam(deg2segOrseg2dev);
    addParam(precision);
  }

  @Override
  public final Record getConversionOf(final Record record) throws Exception {
    Record out = record;
    Object conversionResult = null;

    final Object attrIn = getInParam("Ra in", record).getValue();
    if (Util.isSet(attrIn)) {
      final AttributeValue attrOut = getOutParam("Ra out", record);
      final String conversionType = getInternParam("Conversion type").getValue();
      final String precision = getInternParam("precision").getValue();

      if (conversionType.equals("sexa2deg")) {
        final String coordinateHms = String.valueOf(attrIn);
        final HMS hms = new HMS(coordinateHms);
        conversionResult = hms.getVal() * 360. / 24.;
        final NumberFormat formatter = new DecimalFormat(precision);
        conversionResult = formatter.format(conversionResult);
      } else if (conversionType.equals("deg2sexa")) {
        double coordinateDegree = new Double(String.valueOf(attrIn)).doubleValue();
        final HMS hms = new HMS(coordinateDegree * 24. / 360.);
        conversionResult = hms.toString(true);
      } else {
        throw new IllegalArgumentException("HMS Converter: conversionType is unknow, please contact the administrator");
      }

      LOG.log(Level.FINEST, "HMS conversion : {0} to {1}", new Object[]{attrIn, conversionResult});

      attrOut.setValue(conversionResult);
    }
    return out;
  }

  @Override
  public final Validator<?> getValidator() {
    return new Validator<AbstractConverter>() {
      @Override
      public final Set<ConstraintViolation> validate(final AbstractConverter item) {
        // Check quickly if the procecision is set and correct
        final Set<ConstraintViolation> constraints = new HashSet<ConstraintViolation>();
        final Map<String, ConverterParameter> params = item.getParametersMap();
        ConverterParameter param = params.get("precision");
        if (!param.getValue().startsWith("#")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("Precision must start by # ");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraint.setInvalidValue(param.getValue());
          constraints.add(constraint);
        }
        param = params.get("Ra in");
        if (param.getAttachedColumn().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("Ra in must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraints.add(constraint);
        }
        param = params.get("Ra out");
        if (param.getAttachedColumn().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("Ra out must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraints.add(constraint);
        }
        return constraints;
      }
    };
  }
}
