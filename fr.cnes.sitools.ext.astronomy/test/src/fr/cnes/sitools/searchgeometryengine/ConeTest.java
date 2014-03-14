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
package fr.cnes.sitools.searchgeometryengine;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 * @author malapert
 */
public class ConeTest {

    /**
     * Test of getCenter method, of class Cone.
     */
    @Test
    public void testGetCenter() {
        System.out.println("getCenter");
        Cone instance = new Cone(new Point(20, 30, CoordSystem.EQUATORIAL),Math.toRadians(30));
        Point expResult = new Point(20, 30, CoordSystem.EQUATORIAL);
        Point result = instance.getCenter();
        assertEquals(expResult, result);      
    }

    /**
     * Test of getRadius method, of class Cone.
     */
    @Test
    public void testGetRadius() {
        System.out.println("getRadius");
        Cone instance = new Cone(new Point(20, 30, CoordSystem.EQUATORIAL),Math.toRadians(30));
        double expResult = Math.toRadians(30);
        double result = instance.getRadius();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of isSurface method, of class Cone.
     */
    @Test
    public void testIsSurface() {
        System.out.println("isSurface");
        Cone instance = new Cone();
        boolean expResult = true;
        boolean result = instance.isSurface();
        assertEquals(expResult, result);
    }
}
