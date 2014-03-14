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
 * Docorates the validation process by a specific test.
 * @see Package Decorator pattern
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class ValidationDecorator extends Validation {

    /**
     * The validation before applying a specific decorator.
     */
    private final transient Validation orginal;

    /**
     * Constructs a generic decorateur.
     * @param validation The validation before applying a specific decorator
     */
    public ValidationDecorator(final Validation validation) {
        this.orginal = validation;
    }

    @Override
    public final Map<String, String> getMap() {
        return this.orginal.getMap();
    }

    @Override
    public final void processValidation() {
        this.orginal.processValidation();
        final StatusValidation currentVal = this.orginal.getStatusValidation();
        final Map<String, String> localError = localValidation();
        if (hasErrorInValidation(localError)) {
            currentVal.addAll(localValidation());
        }
        setStatusValidation(currentVal);
    }

    /**
     * Returns the validation of a specific decorator.
     * @return the detected error
     */
    protected abstract Map<String, String> localValidation();

    /**
     * Checks if an error happens during the validation of the specific decorator.
     * @param validation the returned String of the localValidation method
     * @return <code>True</code> when an error is detected otherwise <code>False</code>
     */
    private boolean hasErrorInValidation(final Map<String, String> validation) {
        return !validation.isEmpty();
    }
}
