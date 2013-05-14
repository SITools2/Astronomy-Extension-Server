/**
 * **********************************************************************
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is part of SITools2.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * ***********************************************************************
 */
package fr.cnes.sitools.extensions.common;

import java.util.Map;

/**
 *
 * @author malapert
 */
public class RangeValidation extends NumberValidation {
    
    private double minValue;
    
    private double maxValue;
    
    public RangeValidation(final Validation validation, final String keyword, double min, double max) {
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
     * @return the minValue
     */
    protected final double getMinValue() {
        return minValue;
    }

    /**
     * @param minValue the minValue to set
     */
    protected final void setMinValue(final double minValue) {
        this.minValue = minValue;
    }

    /**
     * @return the maxValue
     */
    protected final double getMaxValue() {
        return maxValue;
    }

    /**
     * @param maxValue the maxValue to set
     */
    protected final void setMaxValue(final double maxValue) {
        this.maxValue = maxValue;
    }
    
    
    
}
