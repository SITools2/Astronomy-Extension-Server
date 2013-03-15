/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author malapert
 */
public class AstroCoordinateTest {

    /**
     * Test of getType method, of class AstroCoordinate.
     */
    @Test
    public void testGetType() {
        System.out.println("getType");
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setType("IMCCE");
        String expResult = "IMCCE";
        String result = instance.getType();
        assertEquals(expResult, result);
    }

    /**
     * Test of setType method, of class AstroCoordinate.
     */
    @Test
    public void testSetType() {
        System.out.println("setType");
        String type = "IMCCE";
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setType(type);
    }

    /**
     * Test of getRaAsDecimal method, of class AstroCoordinate.
     */
    @Test
    public void testGetRaAsDecimal() {
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
    public void testGetDecAsDecimal() {
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
    public void testGetRaAsSexagesimal() {
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
    public void testGetDecAsSexagesimal() {
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
    public void testSetRaAsDecimal() {
        System.out.println("setRaAsDecimal");
        double ra = 0.0;
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setRaAsDecimal(ra);
    }

    /**
     * Test of setDecAsDecimal method, of class AstroCoordinate.
     */
    @Test
    public void testSetDecAsDecimal() {
        System.out.println("setDecAsDecimal");
        double dec = 0.0;
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setDecAsDecimal(dec);
    }

    /**
     * Test of setRaAsSexagesimal method, of class AstroCoordinate.
     */
    @Test
    public void testSetRaAsSexagesimal() {
        System.out.println("setRaAsSexagesimal");
        String ra = "20:00:00";
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setRaAsSexagesimal(ra);
    }

    /**
     * Test of setDecAsSexagesimal method, of class AstroCoordinate.
     */
    @Test
    public void testSetDecAsSexagesimal() {
        System.out.println("setDecAsSexagesimal");
        String dec = "20:00:00";
        AstroCoordinate instance = new AstroCoordinate(20, 30);
        instance.setDecAsSexagesimal(dec);
    }

}
