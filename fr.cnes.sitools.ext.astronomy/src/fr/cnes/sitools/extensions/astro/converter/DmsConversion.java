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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsky.coords.DMS;

import org.restlet.engine.Engine;

import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.dataset.converter.business.AbstractConverter;
import fr.cnes.sitools.dataset.converter.model.ConverterParameter;
import fr.cnes.sitools.dataset.converter.model.ConverterParameterType;
import fr.cnes.sitools.datasource.jdbc.model.AttributeValue;
import fr.cnes.sitools.datasource.jdbc.model.Record;
import fr.cnes.sitools.util.Util;

/**
 * DmsConversion provides transformation from decimal degree to sexagesimal notation for latitude axis.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @see The conversion from sexagesimal to decimal coordinates is done by the use of <a href="http://jsky.sourceforge.net/">jsky</a>.
 */
public class DmsConversion extends AbstractConverter {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(DmsConversion.class.getName());

  /**
   * Constructs a converter.
   */
  public DmsConversion() {
    setName("DmsDegConversion");
    setDescription("Sexadecimal to decimal degree or decimal degree to sexadecimal");
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    final ConverterParameter declination = new ConverterParameter("DecIn", "Declination attribute as input",
            ConverterParameterType.CONVERTER_PARAMETER_IN);
    final ConverterParameter valInHms = new ConverterParameter("DecOut", "Declination attribute as output",
            ConverterParameterType.CONVERTER_PARAMETER_OUT);
    final ConverterParameter deg2segOrseg2dev = new ConverterParameter("ConversionType",
            "Set conversion type: dms2deg or deg2dms", ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    deg2segOrseg2dev.setValue("deg2dms");
    deg2segOrseg2dev.setValueType("String");
    final ConverterParameter precision = new ConverterParameter("precision", "result precision for double (#0.00)",
            ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    precision.setValue("#0.00");
    precision.setValueType("String");
    addParam(declination);
    addParam(valInHms);
    addParam(deg2segOrseg2dev);
    addParam(precision);
  }

  @Override
  public final Record getConversionOf(final Record record) throws Exception {
    Record out = record;
    Object conversionResult = null;

    final Object attrIn = getInParam("DecIn", record).getValue();
    if (Util.isSet(attrIn)) {
      final AttributeValue attrOut = getOutParam("DecOut", record);
      final String conversionType = getInternParam("ConversionType").getValue();
      final String precision = getInternParam("precision").getValue();

      if (conversionType.equals("dms2deg")) {
        final String coordinateDms = String.valueOf(attrIn);
        final DMS dms = new DMS(coordinateDms);
        conversionResult = dms.getVal();
        final NumberFormat formatter = new DecimalFormat(precision);
        conversionResult = formatter.format(conversionResult);
      } else if (conversionType.equals("deg2dms")) {
        final double coordinateDegree = Double.valueOf(String.valueOf(attrIn));
        final DMS dms = new DMS(coordinateDegree);
        conversionResult = dms.toString(true);
      } else {
        throw new IllegalArgumentException("DMS Converter: conversionType is unknow, please contact the administrator");
      }

      getContext().getLogger().log(Level.FINEST, "DMS conversion : {0} to {1}", new Object[]{attrIn, conversionResult});

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
        param = params.get("DecIn");
        if (param.getAttachedColumn().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("DecIn must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraints.add(constraint);
        }
        param = params.get("DecOut");
        if (param.getAttachedColumn().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("DecOut must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraints.add(constraint);
        }
        return constraints;
      }
    };
  }
}
