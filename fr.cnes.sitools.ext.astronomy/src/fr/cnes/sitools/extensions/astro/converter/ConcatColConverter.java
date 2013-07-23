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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Concats two columns.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConcatColConverter extends AbstractConverter {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ConcatColConverter.class.getName());

  /**
   * Constucts the converter.
   */
  public ConcatColConverter() {
    setName("ConcatColConverter");
    setDescription("Concats two columns.");
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    final ConverterParameter colIn1 = new ConverterParameter("ColIn1", "Column on which the string will be concat",
            ConverterParameterType.CONVERTER_PARAMETER_IN);
    final ConverterParameter colIn2 = new ConverterParameter("ColIn2", "Column on which the string will be concat",
            ConverterParameterType.CONVERTER_PARAMETER_IN);
    final ConverterParameter colOut = new ConverterParameter("ColOut", "Column where the result of this converter is set",
            ConverterParameterType.CONVERTER_PARAMETER_OUT);
    final ConverterParameter pattern = new ConverterParameter("Pattern",
            "pattern of the concatenation.", ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    pattern.setValue("%s %s");
    pattern.setValueType("String");
    addParam(colIn1);
    addParam(colIn2);
    addParam(colOut);
    addParam(pattern);
  }

  @Override
  public final Record getConversionOf(final Record record) throws Exception {
    Record out = record;
    final Object attrIn1 = getInParam("ColIn1", record).getValue();
    final Object attrIn2 = getInParam("ColIn2", record).getValue();
    final AttributeValue attrOut = getOutParam("ColOut", record);
    if (Util.isSet(attrIn1) && Util.isSet(attrIn2)) {
      final String conversionResult = String.format(getInternParam("Pattern").getValue(), attrIn1, attrIn2);
      attrOut.setValue(conversionResult);
    } else if (Util.isSet(attrIn1) && !Util.isSet(attrIn2)) {
      final String conversionResult = String.format(getInternParam("Pattern").getValue(), attrIn1);
      attrOut.setValue(conversionResult);
    } else if (!Util.isSet(attrIn1) && Util.isSet(attrIn2)) {
      final String conversionResult = String.format(getInternParam("Pattern").getValue(), attrIn2);
      attrOut.setValue(conversionResult);
    }
    LOG.log(Level.FINEST, "Conversion of record into {0}", out);
    return out;
  }

  @Override
  public final Validator<?> getValidator() {
    return new Validator<AbstractConverter>() {
      @Override
      public final Set<ConstraintViolation> validate(final AbstractConverter item) {
        final Set<ConstraintViolation> constraints = new HashSet<ConstraintViolation>();
        final Map<String, ConverterParameter> params = item.getParametersMap();
        ConverterParameter param = params.get("Pattern");
        if (Util.isEmpty(param.getValue())) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("A pattern must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraint.setInvalidValue(param.getValue());
          constraints.add(constraint);
        }
        param = params.get("ColIn1");
        if (param.getAttachedColumn().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("ColIn1 must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraints.add(constraint);
        }
        param = params.get("ColIn2");
        if (param.getAttachedColumn().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("ColIn2 must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraints.add(constraint);
        }
        param = params.get("ColOut");
        if (param.getAttachedColumn().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("ColOut must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraints.add(constraint);
        }
        return constraints;
      }
    };
  }
}
