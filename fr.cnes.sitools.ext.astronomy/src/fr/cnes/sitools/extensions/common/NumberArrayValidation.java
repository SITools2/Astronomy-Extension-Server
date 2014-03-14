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
import java.util.regex.PatternSyntaxException;

/**
 * Specific decorator to validate that the length of an array represented by a
 * <code>keyword</code> .
 *
 * @see Package Decorator pattern
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class NumberArrayValidation extends NotNullAndNotEmptyValidation {

    /**
     * Characters to use to create the array.
     */
    private String splitChar;
    /**
     * Init the length of th array to 0.
     */
    private int lengthArray = 0;
    /**
     * Init the minimum length of the array to 0.
     */
    private int minArray = 0;
    /**
     * Init the maximum length of the array to 0.
     */
    private int maxArray = 0;

    /**
     * Constructs an array validation.
     *
     * @param validation validation
     * @param keyword keyword to validate
     * @param splitChars chars to use to split the array
     */
    public NumberArrayValidation(final Validation validation, final String keyword, final String splitChars) {
        super(validation, keyword);
        setSplitChar(splitChars);
    }

    /**
     * Constructs an array validation based on the specify length of element in
     * the array.
     *
     * @param validation validation
     * @param keyword keyword to validate
     * @param splitChars chars to use to split the array
     * @param length length to check in the array
     */
    public NumberArrayValidation(final Validation validation, final String keyword, final String splitChars, final int length) {
        this(validation, keyword, splitChars);
        if (length < 0) {
            throw new IllegalArgumentException("length must be a positive number");
        }
        setLengthArray(length);
    }

    /**
     * Constructs an array validation based on the specify length that must be
     * included in [minLength, maxLength].
     *
     * @param validation validation
     * @param keyword keyword to validate
     * @param splitChars chars to use to split the array
     * @param minLength minimum length that must have the array
     * @param maxLength maximum length that must have the array
     */
    public NumberArrayValidation(final Validation validation, final String keyword, final String splitChars, final int minLength, final int maxLength) {
        this(validation, keyword, splitChars);
        if (maxLength <= minLength) {
            throw new IllegalArgumentException("maxLength must be > minLength");
        }
        setMinArray(minLength);
        setMaxArray(maxLength);
    }

    @Override
    protected Map<String, String> localValidation() {
        final Map<String, String> error = super.localValidation();
        if (error.isEmpty()) {
            final String value = getMap().get(this.getKeywordToTest());
            try {
                if (getMinArray() != 0 || getMaxArray() != 0) {
                    final String[] array = value.split(getSplitChar());
                    final int arrayLength = array.length;
                    if (isArrayOfNumber(array) && (arrayLength < getMinArray() || arrayLength > getMaxArray())) {
                        error.put(getKeywordToTest(), "the length of the array must be included in [" + getMinArray() + "," + getMaxArray() + "]");
                    } else if (!isArrayOfNumber(array)) {
                        error.put(getKeywordToTest(), "the inputs are not an array of numbers");
                    }
                } else if (getLengthArray() != 0) {
                    final String[] array = value.split(getSplitChar());
                    final int arrayLength = array.length;
                    if (arrayLength != getLengthArray() && isArrayOfNumber(array)) {
                        error.put(getKeywordToTest(), "the length of the array must be " + getLengthArray());
                    } else if (!isArrayOfNumber(array)) {
                        error.put(getKeywordToTest(), "the input is not an array of numbers");
                    }
                } else {
                    final String[] array = value.split(getSplitChar());
                    if (!isArrayOfNumber(array)) {
                        error.put(getKeywordToTest(), "the input is not an array of numbers");
                    }
                }
            } catch (PatternSyntaxException ex) {
                error.put(getKeywordToTest(), ex.getMessage());
            }
        }
        return error;
    }

    /**
     * Returns True when the array is only composed of numbers.
     *
     * @param elts array to test
     * @return True when the array is only composed of numbers otherwise False
     */
    private boolean isArrayOfNumber(final String[] elts) {
        boolean isNumber = true;
        for (String elt : elts) {
            isNumber = isNumber && NumberValidation.isANumber(elt);
        }
        return isNumber;
    }

    /**
     * Returns the length of the array.
     *
     * @return the lengthArray
     */
    protected final int getLengthArray() {
        return lengthArray;
    }

    /**
     * Sets the length of the array.
     *
     * @param lengthArrayVal the lengthArray to set
     */
    protected final void setLengthArray(final int lengthArrayVal) {
        this.lengthArray = lengthArrayVal;
    }

    /**
     * Returns the minimum length that has been specified.
     *
     * @return the minimum length that has been specified
     */
    protected final int getMinArray() {
        return minArray;
    }

    /**
     * Sets the minimum length that has been specified.
     *
     * @param minArrayVal the minArray to set
     */
    protected final void setMinArray(final int minArrayVal) {
        this.minArray = minArrayVal;
    }

    /**
     * Returns the maximum length that has been specified.
     *
     * @return the maximum length that has been specified
     */
    protected final int getMaxArray() {
        return maxArray;
    }

    /**
     * Sets the maximum length that has been specified.
     *
     * @param maxArrayVal the maxArray to set
     */
    protected final void setMaxArray(final int maxArrayVal) {
        this.maxArray = maxArrayVal;
    }

    /**
     * Returns the chars that have been specified to split the array.
     *
     * @return the chars that have been specified to split the array
     */
    protected final String getSplitChar() {
        return splitChar;
    }

    /**
     * Sets the chars that have been specified to split the array.
     *
     * @param splitCharVal the splitChar to set
     */
    protected final void setSplitChar(final String splitCharVal) {
        this.splitChar = splitCharVal;
    }
}
