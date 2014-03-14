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
 * This class represents the component to decore by the use of decorator pattern.
 * <p>
 * The decorator are a set of classes that localValidation the inputs.
 * </p>
 * @see Package Decorator pattern
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class Validation {

    /**
     * Status of the validation.
     */
    private StatusValidation statusValidation = new StatusValidation();

    /**
     * Elements to validate.
     */
    private Map<String, String> map;

    /**
     * Validates map.
     */
    protected abstract void processValidation();

    /**
     * Returns the validation result.
     * @return the validation result
     */
    public final StatusValidation validate() {
        processValidation();
        return this.statusValidation;
    }

    /**
     * Returns the current validation status.
     * @return the current validation status
     */
    protected final StatusValidation getStatusValidation() {
        return this.statusValidation;
    }

    /**
     * Sets the current validation status.
     * @param currentValidation  the current validation status
     */
    protected final void setStatusValidation(final StatusValidation currentValidation) {
        this.statusValidation = currentValidation;
    }

    /**
     * Returns the elements to validate.
     * @return the map the elements to validate
     */
    public Map<String, String> getMap() {
        return map;
    }

    /**
     * Sets the elements to validate.
     * @param mapToValidate  the map to set
     */
    public final void setMap(final Map<String, String> mapToValidate) {
        this.map = mapToValidate;
    }
}
