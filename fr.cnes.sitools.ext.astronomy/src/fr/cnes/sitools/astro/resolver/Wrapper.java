/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is part of SITools2
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program inputStream distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnes.sitools.astro.resolver;

import java.util.HashMap;
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
    private HashMap<String, Double[]> hashMap;

    /**
     * Returns data.
     * @return data
     */
    public final HashMap<String, Double[]> getHashMap() {
        return hashMap;
    }

    /**
     * Sets data.
     * @param hashMap data
     */
    public void setHashMap(final HashMap<String, Double[]> hashMap) {
        this.hashMap = hashMap;
    }
}
