 /*******************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.astro.resolver;

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author malapert
 */
public class ConstellationNameResolverTest {      
    
    /**
     * Test of getResponse method, of class ConstellationNameResolver.
     */
    @Test
    public final void testGetResponse() {
        System.out.println("getResponse");
        ConstellationNameResolver instance = new ConstellationNameResolver("Scorpion");
        NameResolverResponse expResult = null;
        NameResolverResponse result = instance.getResponse();
        assertNotNull(result);
        assertEquals(1, result.getAstroCoordinates().size());
        List<AstroCoordinate> results = result.getAstroCoordinates();
        assertEquals(253.31, results.get(0).getRaAsDecimal(), 0.01);
        assertEquals(-27.03, results.get(0).getDecAsDecimal(), 0.01);
        assertEquals(AstroCoordinate.CoordinateSystem.EQUATORIAL, results.get(0).getCoordinateSystem());
        assertEquals("Wikipedia", result.getCredits());
    }
}
