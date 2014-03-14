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

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsky.coords.WCSTransform;

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
 * Computes the central position of a shape based on WCS inputs.
 * 
 * <p>
 * WCS column must be written in the same way as FITS
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CenterPositionConverter extends PolygonConverter {
  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(CenterPositionConverter.class.getName());

  /**
   * Constucts the converter.
   */
  public CenterPositionConverter() {
    setName("CentralPosConversion");
    setDescription("Compute the central position from WCS inputs. "
            + "The columns name must have the same names than WCS keywords");
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    final ConverterParameter colOutput = new ConverterParameter("ColumnOutput", "Output column representing the footprint polygon",
            ConverterParameterType.CONVERTER_PARAMETER_OUT);
    final ConverterParameter patternOutput = new ConverterParameter("PatternOutput", "Output pattern representing the footprint polygon",
            ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    patternOutput.setValue("(%s,%s)");
    addParam(colOutput);
    addParam(patternOutput);    
  }

  @Override
  public final Record getConversionOf(final Record record) throws Exception {
    this.setRecord(record);
    final WCSTransform wcs = new WCSTransform(this);
    LOG.log(Level.FINEST, "record :{0}", record.toString());
    if (wcs.isWCS()) {
      LOG.log(Level.FINEST, "Wcs is valid");
      final String pattern = getInternParam("PatternOutput").getValue();
      final AttributeValue attrOut = getOutParam("ColumnOutput", getRecord());

      final Point2D.Double ptg = wcs.getWCSCenter();
      final String output = String.format(pattern, ptg.getX(), ptg.getY());
      LOG.log(Level.FINEST, "Conversion of record into {0}", output);
      attrOut.setValue(output);
    } else {
      LOG.log(Level.FINEST, "Wcs is not valid");
    }
    return getRecord();
  }

  @Override
  public final Validator<?> getValidator() {
    return new Validator<AbstractConverter>() {
      @Override
      public final Set<ConstraintViolation> validate(final AbstractConverter item) {
        final Set<ConstraintViolation> constraints = new HashSet<ConstraintViolation>();
        final Map<String, ConverterParameter> params = item.getParametersMap();
        ConverterParameter param = params.get("PatternOutput");
        if (!Util.isNotEmpty(param.getValue())) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("Pattern must be set (e.g (%s,%s)");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraint.setInvalidValue(param.getValue());
          constraints.add(constraint);
        }
        param = params.get("ColumnOutput");
        if (param.getAttachedColumn().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("ColumnOutput must be set");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setValueName(param.getName());
          constraints.add(constraint);
        }
        return constraints;
      }
    };
  }
}
