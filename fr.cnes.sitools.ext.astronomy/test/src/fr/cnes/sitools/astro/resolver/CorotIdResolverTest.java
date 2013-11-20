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
import org.junit.Test;

/**
 * Test of CorotIdResolver object.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CorotIdResolverTest {

    /**
     * Test of getCoordinates method, of class CorotIdResolver.
     */
    @Test
    public final void testGetCoordinates() throws Exception {
        System.out.println("getCoordinates");
        CorotIdResolver instance = new CorotIdResolver("105290723");             
        NameResolverResponse response = instance.getResponse();
        List<AstroCoordinate> result = response.getAstroCoordinates();
        assertEquals(279.88184, result.get(0).getRaAsDecimal(),0.001);
        assertEquals(6.4019198, result.get(0).getDecAsDecimal(),0.001);        
    }
}
