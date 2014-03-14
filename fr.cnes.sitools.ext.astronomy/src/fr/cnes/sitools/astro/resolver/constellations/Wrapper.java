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

package fr.cnes.sitools.astro.resolver.constellations;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Wrapper.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
@XmlRootElement
public class Wrapper {

    /**
     * Data to wrap.
     */
    private Map<String, Double[]> hashMap;

    /**
     * Returns data.
     * @return data
     */
    public final Map<String, Double[]> getHashMap() {
        return hashMap;
    }

    /**
     * Sets data.
     * @param hashMapVal data
     */
    public final void setHashMap(final Map<String, Double[]> hashMapVal) {
        this.hashMap = hashMapVal;
    }
}
