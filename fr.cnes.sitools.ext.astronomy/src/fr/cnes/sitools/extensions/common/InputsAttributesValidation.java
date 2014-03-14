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
import java.util.Set;

/**
 * Concrete component providing the attributes to validate.
 *
 * @see Package Decorator pattern
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class InputsAttributesValidation extends Validation {

    /**
     * Constructs an input validation with a map to validate.
     *
     * @param attributes input to validate
     */
    public InputsAttributesValidation(final Map<String, Object> attributes) {
        final Map<String, String> mapToTest = new HashMap<String, String>();
        final Set<Map.Entry<String, Object>> entries = attributes.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            mapToTest.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        setMap(mapToTest);
    }

    @Override
    protected final void processValidation() {
        if (getMap() == null) {
            throw new IllegalArgumentException("the map cannot be null.");
        }
    }
}
