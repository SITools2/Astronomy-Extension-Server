/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.extensions.common;

import java.util.Map;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author malapert
 */
public class NumberArrayValidation extends NotNullAndNotEmptyValidation {

    private String splitChar;
    private int lengthArray = 0;
    private int minArray = 0;
    private int maxArray = 0;

    public NumberArrayValidation(final Validation validation, final String keyword, final String splitChars) {
        super(validation, keyword);
        setSplitChar(splitChars);
    }

    public NumberArrayValidation(final Validation validation, final String keyword, final String splitChars, final int length) {
        this(validation, keyword, splitChars);
        if (length < 0) {
            throw new IllegalArgumentException("length must be a positive number");
        }
        setLengthArray(length);
    }

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
        Map<String, String> error = super.localValidation();
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
    
 

    private boolean isArrayOfNumber(String[] elts) {
        boolean isNumber = true;
        for (int i = 0; i < elts.length; i++) {
            isNumber = isNumber && NumberValidation.isANumber(elts[i]);
        }
        return isNumber;
    }

    /**
     * @return the lengthArray
     */
    protected int getLengthArray() {
        return lengthArray;
    }

    /**
     * @param lengthArray the lengthArray to set
     */
    protected void setLengthArray(int lengthArray) {
        this.lengthArray = lengthArray;
    }

    /**
     * @return the minArray
     */
    protected int getMinArray() {
        return minArray;
    }

    /**
     * @param minArray the minArray to set
     */
    protected void setMinArray(int minArray) {
        this.minArray = minArray;
    }

    /**
     * @return the maxArray
     */
    protected int getMaxArray() {
        return maxArray;
    }

    /**
     * @param maxArray the maxArray to set
     */
    protected void setMaxArray(int maxArray) {
        this.maxArray = maxArray;
    }

    /**
     * @return the splitChar
     */
    protected String getSplitChar() {
        return splitChar;
    }

    /**
     * @param splitChar the splitChar to set
     */
    protected void setSplitChar(String splitChar) {
        this.splitChar = splitChar;
    }
}
