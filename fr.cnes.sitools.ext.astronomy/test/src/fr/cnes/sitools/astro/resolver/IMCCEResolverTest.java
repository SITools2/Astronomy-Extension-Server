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
package fr.cnes.sitools.astro.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

/**
 * Test of IMCCE name resolver.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class IMCCEResolverTest {

    /**
     * Test of getCoordinates method, of class AbstractNameResolver.
     */
    @Test
    public final void testGetCoordinates() throws Exception {
        System.out.println("getCoordinates");
        AbstractNameResolver imcce = new IMCCESsoResolver("mars","22-03-2013T15:55:00");
        NameResolverResponse response = imcce.getResponse();
        List<fr.cnes.sitools.extensions.common.AstroCoordinate> coords = response.getAstroCoordinates();
        assertNotNull(coords);
        fr.cnes.sitools.extensions.common.AstroCoordinate coordinate = coords.get(0);
        assertEquals("RA is not correct for mars", 7.489974375, coordinate.getRaAsDecimal(),0.0001);
        assertEquals("DEC is not correct for mars", 2.51544325, coordinate.getDecAsDecimal(),0.0001);
    }

    /**
     * Test of getCreditsName method.
     */
    @Test
    public void testGetCreditsName() {
        System.out.println("getCreditsName");
        AbstractNameResolver imcce = new IMCCESsoResolver("mars","22-03-2013T15:55:00");
        NameResolverResponse response = imcce.getResponse();
        assertEquals("IMCCE", response.getCredits());
    }  
}
