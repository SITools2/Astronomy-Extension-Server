 /*******************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * Concats a string with a column.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConcatConverter extends AbstractConverter {
/**
 * Logger.
 */
      private static final Logger LOG = Engine.getLogger(ConcatConverter.class.getName());

  /**
   * Constructs a converter.
   */
    public ConcatConverter() {
        setName("ConcatConverter");
        setDescription("Concat a string with a cell of a record");
        setClassAuthor("J-C Malapert");
        setClassOwner("CNES");
        setClassVersion("1.0");
        final ConverterParameter colIn = new ConverterParameter("ColIn", "Column on which the string will be concat",
                ConverterParameterType.CONVERTER_PARAMETER_IN);
        final ConverterParameter colOut = new ConverterParameter("ColOut", "Column where the result of this converter is set",
                ConverterParameterType.CONVERTER_PARAMETER_OUT);
        final ConverterParameter stringToConcat = new ConverterParameter("StringToConcat",
                "pattern of the string. %s is the column. When a cell from colIn is null then the function is not applied", ConverterParameterType.CONVERTER_PARAMETER_INTERN);
        stringToConcat.setValue("http://foo.com/%s/test/");
        stringToConcat.setValueType("String");
        addParam(colIn);
        addParam(colOut);
        addParam(stringToConcat);
    }

    @Override
    public final Record getConversionOf(final Record record) throws Exception {
        Record out = record;
        final Object attrIn = getInParam("ColIn", record).getValue();
        final AttributeValue attrOut = getOutParam("ColOut", record);
        if (Util.isSet(attrIn)) {
           final String conversionResult = String.format(getInternParam("StringToConcat").getValue(), attrIn);
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
                ConverterParameter param = params.get("StringToConcat");
                if (Util.isEmpty(param.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("A pattern must be set ");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setValueName(param.getName());
                    constraint.setInvalidValue(param.getValue());
                    constraints.add(constraint);
                }
                param = params.get("ColIn");
                if (param.getAttachedColumn().isEmpty()) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("ColIn must be set");
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
