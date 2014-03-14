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
package fr.cnes.sitools.extensions.common;

import java.util.Map;

/**
 * Specific decorator to validate that the <code>keyword</code> is a range, which
 * is included in [min, max].
 * @see Package Decorator pattern
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class RangeValidation extends NumberValidation {
    /**
     * Minimum value that is allowed in the range.
     */
    private double minValue;
    /**
     * Maximum value that is allowed in the range.
     */
    private double maxValue;

    /**
     * Constructs a new Range validator decorator.
     * @param validation validation
     * @param keyword range to check
     * @param min min value that is allowed
     * @param max max value that is allowed
     */
    public RangeValidation(final Validation validation, final String keyword, final double min, final double max) {
        super(validation, keyword);
        if (max < min) {
            throw new IllegalArgumentException("min cannot be superior to max");
        }
        setMinValue(min);
        setMaxValue(max);
    }

    @Override
    protected Map<String, String> localValidation() {
        final Map<String, String> error = super.localValidation();
        if (!error.containsKey(getKeywordToTest())) {
            final double val = Double.valueOf(getMap().get(getKeywordToTest()));
            if (val < getMinValue() || val > getMaxValue()) {
                error.put(getKeywordToTest(), "value must be included in [" + getMinValue() + "," + getMaxValue() + "]");
            }
        }
        return error;
    }

    /**
     * Returns the min value that is allowed.
     * @return the minValue
     */
    protected final double getMinValue() {
        return minValue;
    }

    /**
     * Sets the min value that is allowed.
     * @param minValueVal the minValue to set
     */
    protected final void setMinValue(final double minValueVal) {
        this.minValue = minValueVal;
    }

    /**
     * Returns the max value that is allowed.
     * @return the maxValue
     */
    protected final double getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the max value that is allowed.
     * @param maxValueVal the maxValue to set
     */
    protected final void setMaxValue(final double maxValueVal) {
        this.maxValue = maxValueVal;
    }
}
