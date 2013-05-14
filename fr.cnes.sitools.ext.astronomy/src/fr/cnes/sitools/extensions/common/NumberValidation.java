/************************************************************************
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is part of SITools2.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *************************************************************************/
package fr.cnes.sitools.extensions.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Specific decorator to validate that the <code>keyword</code> is a number.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class NumberValidation extends NotNullAndNotEmptyValidation {
    
    /**
     * The keyword to test.
     */
    private String keywordToTest;
    
    /**
     * Indicates if the keyword is required.
     */
    private boolean requiredKeyword;
    
    /**
     * Constructs a validation decorator to check if the <code>keyword</code> is a number.
     * <p>
     * The keyword is considered as required.
     * </p>
     * @param validation validation
     * @param keyword keyword to test
     */
    public NumberValidation(final Validation validation, final String keyword) {
        super(validation,keyword);;
        setRequiredKeyword(true);
    }
    
    /**
     * Constructs a validation decorator to check if the <code>keyword</code> is a number.
     * @param validation validation
     * @param keyword keyword to test
     * @param isRequired Indicates if the keyword is or is not required
     */
    public NumberValidation(final Validation validation, final String keyword, final boolean isRequired) {
        this(validation, keyword);
        setRequiredKeyword(isRequired);
    }
    
    
    @Override
    protected Map<String, String> localValidation() {
        final String value = getMap().get(this.getKeywordToTest());
        final Map<String, String> error = new HashMap<String, String>();
        if (isRequiredKeyword() && (value == null || value.isEmpty())) {
            error.put(getKeywordToTest(), "value must be set");
        } else if (isRequiredKeyword() && !isANumber(value)) {
            error.put(getKeywordToTest(), "value must be a number");
        }
        return error;
    }
   
    /**
     * Returns <code>True</code> when <code>value</code> is a number otherwise <code>False</code>.
     * @param value value to test
     * @return <code>True</code> when <code>value</code> is a number otherwise <code>False</code>
     */
    protected static boolean isANumber(final String value) {
        if (value == null) return false;
        try {
            new java.math.BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * @return the requiredKeyword
     */
    protected final boolean isRequiredKeyword() {
        return requiredKeyword;
    }

    /**
     * @param requiredKeyword the requiredKeyword to set
     */
    protected final void setRequiredKeyword(final boolean isRequiredKeyword) {
        this.requiredKeyword = isRequiredKeyword;
    }
}
