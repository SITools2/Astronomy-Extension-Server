/*******************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 * *****************************************************************************/
package fr.cnes.sitools.astro.resolver;

import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Test of ReverseNameResolverTest name resolver.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ReverseNameResolverTest {

    /**
     * Test of getJsonResponse method, of class ReverseNameResolver.
     */
    @Test
    public final void testGetJsonResponse() throws NameResolverException {
        System.out.println("getJsonResponse");
        ReverseNameResolver instance = new ReverseNameResolver("00:42:44.32 +41:16:07.5", 13, fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem.EQUATORIAL);
        String expResult = "M  31 ";
        Map result = instance.getJsonResponse();
        List<Map> features = (List<Map>) result.get("features");
        Map feature = features.get(0);
        Map properties = (Map) feature.get("properties");
        assertEquals(expResult, properties.get("identifier"));
    }
}
