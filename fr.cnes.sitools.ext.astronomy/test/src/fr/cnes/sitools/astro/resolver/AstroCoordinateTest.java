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

import org.junit.Test;

import fr.cnes.sitools.extensions.common.AstroCoordinate;

/**
 * Test of AstroCoordinate object.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class AstroCoordinateTest {

    /**
     * Test of getRaAsDecimal method, of class AstroCoordinate.
     */
    @Test
    public final void testGetRaAsDecimal() {
        System.out.println("getRaAsDecimal");
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        double expResult = 20.0;
        double result = instance.getRaAsDecimal();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getDecAsDecimal method, of class AstroCoordinate.
     */
    @Test
    public final void testGetDecAsDecimal() {
        System.out.println("getDecAsDecimal");
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        double expResult = 30.0;
        double result = instance.getDecAsDecimal();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getRaAsSexagesimal method, of class AstroCoordinate.
     */
    @Test
    public final void testGetRaAsSexagesimal() {
        System.out.println("getRaAsSexagesimal");
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        String expResult = "01:20:00.000";
        String result = instance.getRaAsSexagesimal();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDecAsSexagesimal method, of class AstroCoordinate.
     */
    @Test
    public final void testGetDecAsSexagesimal() {
        System.out.println("getDecAsSexagesimal");
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        String expResult = "+30:00:00.00";
        String result = instance.getDecAsSexagesimal();
        assertEquals(expResult, result);
    }

    /**
     * Test of setRaAsDecimal method, of class AstroCoordinate.
     */
    @Test
    public final void testSetRaAsDecimal() {
        System.out.println("setRaAsDecimal");
        double ra = 0.0;
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setRaAsDecimal(ra);
    }

    /**
     * Test of setDecAsDecimal method, of class AstroCoordinate.
     */
    @Test
    public final void testSetDecAsDecimal() {
        System.out.println("setDecAsDecimal");
        double dec = 0.0;
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setDecAsDecimal(dec);
    }

    /**
     * Test of setRaAsSexagesimal method, of class AstroCoordinate.
     */
    @Test
    public final void testSetRaAsSexagesimal() {
        System.out.println("setRaAsSexagesimal");
        String ra = "20:00:00";
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setRaAsSexagesimal(ra);
    }

    /**
     * Test of setDecAsSexagesimal method, of class AstroCoordinate.
     */
    @Test
    public final void testSetDecAsSexagesimal() {
        System.out.println("setDecAsSexagesimal");
        String dec = "20:00:00";
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setDecAsSexagesimal(dec);
    }
}
