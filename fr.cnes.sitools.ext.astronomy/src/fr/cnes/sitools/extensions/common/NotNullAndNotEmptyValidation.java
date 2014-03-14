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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Specific decorator to validate if keywords value is not null and not empty.
 * @see Package Decorator pattern
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class NotNullAndNotEmptyValidation extends ValidationDecorator {

    /**
     * Keyword to test.
     */
    private String keywordToTest = null;

    /**
     * Default value.
     */
    private transient String defaultValue = null;

    /**
     * Indicates if the keyword is required.
     */
    private transient boolean isRequiredKeyword;

    /**
     * Constructor to check if all keywords value of the map is not null and not
     * empty.
     *
     * @param validation validation
     */
    public NotNullAndNotEmptyValidation(final Validation validation) {
        super(validation);
        this.isRequiredKeyword = true;
    }

    /**
     * Constructor to check if a specific keyword value is not null and not
     * empty.
     * <p>
     * An
     * <code>IllegalArgumentException</code> is raised when
     * <code>keyword</code> is
     * <code>null</code>
     * All keywords of the map are considered as required.
     * </p>
     *
     * @param validation validation
     * @param keyword keyword to test
     */
    public NotNullAndNotEmptyValidation(final Validation validation, final String keyword) {
        this(validation);
        if (keyword == null || keyword.isEmpty()) {
            throw new IllegalArgumentException("keyword cannot be null or empty");
        }
        this.keywordToTest = keyword;
    }

    /**
     * Constructor to check if a specific keyword value is not null and not
     * empty.
     * <p>
     * An
     * <code>IllegalArgumentException</code> is raised when
     * <code>keyword</code> is
     * <code>null</code>
     * All keywords of the map are considered as required.
     * When a keyword is not find, the default value is applied.
     * </p>
     *
     * @param validation validation
     * @param keyword keyword to test
     * @param defaultKeywordValue default value to apply when <code>keyword</code> is not found.
     */
    public NotNullAndNotEmptyValidation(final Validation validation, final String keyword, final String defaultKeywordValue) {
        this(validation, keyword);
        if (defaultKeywordValue == null || defaultKeywordValue.isEmpty()) {
            throw new IllegalArgumentException("keyword cannot be null or empty");
        }
        this.defaultValue = defaultKeywordValue;
    }

    /**
     * Constructor to check if a specific keyword value is not null and not
     * empty.
     * <p>
     * An
     * <code>IllegalArgumentException</code> is raised when
     * <code>keyword</code> is
     * <code>null</code>
     * </p>
     *
     * @param validation validation
     * @param keyword keyword to test
     * @param isRequired <code>True</code> when the keyword is required otherwise <code>False</code>
     */
    public NotNullAndNotEmptyValidation(final Validation validation, final String keyword, final boolean isRequired) {
        this(validation, keyword);
        this.isRequiredKeyword = isRequired;
    }

    @Override
    protected Map<String, String> localValidation() {
        final Map<String, String> errorKeywords = new HashMap<String, String>();
        if (getKeywordToTest() == null) {
            validateTheWholeMap(errorKeywords);
        } else {
            validateAKeyword(errorKeywords, this.isRequiredKeyword);
        }
        return errorKeywords;
    }

    /**
     * Returns the detected errors after checking the whole map.
     * @param errorKeywords detected errors
     */
    private void validateTheWholeMap(final Map<String, String> errorKeywords) {
        final Set<Entry<String, String>> entries = getMap().entrySet();
        for (Entry<String, String> entry : entries) {
            final String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                errorKeywords.put(entry.getKey(), entry.getKey() + " cannot be null or empty");
            }
        }
    }

    /**
     * Returns the detected errors after checking a specific keyword.
     * <p>
     * An error is added in the <code>errorKeywords</code> when the <code>keywordToTest</code> is not find
     * and required.
     * </p>
     * @param errorKeywords the detected errors
     * @param isRequired keyword is or is not required
     */
    private void validateAKeyword(final Map<String, String> errorKeywords, final boolean isRequired) {
        final String value = getMap().get(getKeywordToTest());
        if (isRequired && (value == null || value.isEmpty())) {
            if (this.defaultValue == null) {
                errorKeywords.put(getKeywordToTest(), "value must be set");
            } else {
                getMap().put(getKeywordToTest(), this.defaultValue);
            }
        }
    }

    /**
     * Returns the keyword to test.
     * @return the keywordToTest
     */
    protected final String getKeywordToTest() {
        return keywordToTest;
    }

    /**
     * Sets the keyword to test.
     * @param keywordToTestVal the keywordToTest to set
     */
    protected final void setKeywordToTest(final String keywordToTestVal) {
        this.keywordToTest = keywordToTestVal;
    }
}
