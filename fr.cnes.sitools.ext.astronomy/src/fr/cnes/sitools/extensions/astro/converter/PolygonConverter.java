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

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsky.coords.WCSKeywordProvider;
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
 * Computes the footprint from WCS inputs.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class PolygonConverter extends AbstractConverter implements WCSKeywordProvider {
  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(PolygonConverter.class.getName());
  
  /**
   * P1 X.
   */
  private static final int P1_X = 0;
  /**
   * P1 Y.
   */  
  private static final int P1_Y = 1;
  /**
   * P2 X.
   */  
  private static final int P2_X = 2;
  /**
   * P2 Y.
   */  
  private static final int P2_Y = 3;
  /**
   * P3 X.
   */  
  private static final int P3_X = 4;
  /**
   * P3 Y.
   */  
  private static final int P3_Y = 5;
  /**
   * P4 X.
   */  
  private static final int P4_X = 6;
  /**
   * P4 Y.
   */  
  private static final int P4_Y = 7;  
  /**
   * record to convert.
   */
  private Record record;
  /**
   * Number of points in the polygon. The first point is the last one.
   */
  private static final int NUMBER_POINTS_POLYGON = 8;
  /**
   * Origin in FITS along X.
   */
  private static final double ORIGIN_X = 0.5;
  /**
   * Origin in FITS along Y.
   */
  private static final double ORIGIN_Y = 0.5;

  /**
   * Constructs a converter.
   */
  public PolygonConverter() {
    setName("PolygonConversion");
    setDescription("Compute a polygon from WCS inputs");
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("0.1");
    final ConverterParameter colOutput = new ConverterParameter("ColumnOutput", "Output column representing the footprint polygon",
            ConverterParameterType.CONVERTER_PARAMETER_OUT);
    final ConverterParameter patternOutput = new ConverterParameter("PatternOutput", "Output pattern representing the footprint polygon",
            ConverterParameterType.CONVERTER_PARAMETER_INTERN);
    patternOutput.setValue("((%s,%s),(%s,%s),(%s,%s),(%s,%s),(%s,%s))");
    addParam(colOutput);
    addParam(patternOutput);
  }

  @Override
  public Record getConversionOf(final Record recordVal) throws Exception {
    this.setRecord(recordVal);
    final WCSTransform wcs = new WCSTransform(this);
    if (wcs.isWCS()) {
      final double[] polygonCelest = new double[NUMBER_POINTS_POLYGON];
      final double heightPix = wcs.getHeight();
      final double widthPix = wcs.getWidth();
      final double[] polygonPix = {ORIGIN_X, heightPix + ORIGIN_Y,
        widthPix + ORIGIN_X, heightPix + ORIGIN_Y,
        widthPix + ORIGIN_X, ORIGIN_Y,
        ORIGIN_X, ORIGIN_Y};

      for (int i = 0; i < polygonPix.length; i += 2) {
        final Point2D.Double ptg = wcs.pix2wcs(polygonPix[i], polygonPix[i + 1]);
        polygonCelest[i] = ptg.getX();
        polygonCelest[i + 1] = ptg.getY();
      }

      final String pattern = getInternParam("PatternOutput").getValue();
      final AttributeValue attrOut = getOutParam("ColumnOutput", this.getRecord());

      final String output = String.format(pattern, polygonCelest[P1_X], polygonCelest[P1_Y],
              polygonCelest[P2_X], polygonCelest[P2_Y],
              polygonCelest[P3_X], polygonCelest[P3_Y],
              polygonCelest[P4_X], polygonCelest[P4_Y],
              polygonCelest[P1_X], polygonCelest[P1_Y]);
      attrOut.setValue(output);
      LOG.log(Level.FINEST, "Conversion of record into {0}", output);
    }
    return this.getRecord();
  }

  @Override
  public Validator<?> getValidator() {
    return new Validator<AbstractConverter>() {
      @Override
      public Set<ConstraintViolation> validate(final AbstractConverter item) {
        final Set<ConstraintViolation> constraints = new HashSet<ConstraintViolation>();
        final Map<String, ConverterParameter> params = item.getParametersMap();
        ConverterParameter param = params.get("PatternOutput");
        if (!Util.isNotEmpty(param.getValue())) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setMessage("Pattern must be set (e.g ((%s,%s),(%s,%s),(%s,%s),(%s,%s),(%s,%s)))");
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

  @Override
  public final boolean findKey(final String key) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public final String getStringValue(final String key) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return String.valueOf(iterAtt.getValue());
      }
    }
    return null;
  }

  @Override
  public final String getStringValue(final String key, final String defaultValue) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return String.valueOf(iterAtt.getValue());
      }
    }
    return defaultValue;
  }

  @Override
  public final double getDoubleValue(final String key) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return Double.valueOf(String.valueOf(iterAtt.getValue()));
      }
    }
    throw new RuntimeException(key + " was not found");
  }

  @Override
  public final double getDoubleValue(final String key, final double defaultValue) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return Double.valueOf(String.valueOf(iterAtt.getValue()));
      }
    }
    return defaultValue;
  }

  @Override
  public final float getFloatValue(final String key) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return Float.valueOf(String.valueOf(iterAtt.getValue()));
      }
    }
    throw new RuntimeException(key + " was not found");
  }

  @Override
  public final float getFloatValue(final String key, final float defaultValue) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return Float.valueOf(String.valueOf(iterAtt.getValue()));
      }
    }
    return defaultValue;
  }

  @Override
  public final int getIntValue(final String key) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return Integer.valueOf(String.valueOf(iterAtt.getValue()));
      }
    }
    throw new RuntimeException(key + " was not found");
  }

  @Override
  public final int getIntValue(final String key, final int defaultValue) {
    final List<AttributeValue> att = this.getRecord().getAttributeValues();
    for (AttributeValue iterAtt : att) {
      if (iterAtt.getName().toUpperCase().equals(key)) {
        return Integer.valueOf(String.valueOf(iterAtt.getValue()));
      }
    }
    return defaultValue;
  }

  /**
   * Returns the record.
   * @return the record
   */
  protected final Record getRecord() {
    return record;
  }

  /**
   * Sets the record.
   * @param recordVal the record to set
   */
  protected final void setRecord(final Record recordVal) {
    this.record = recordVal;
  }
}
