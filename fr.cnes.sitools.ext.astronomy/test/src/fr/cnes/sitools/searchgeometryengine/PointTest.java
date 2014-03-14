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
public class PointTest {

    /**
     * Test of getLatitude method, of class Point.
     */
    @Test
    public void testGetLatitude() {
        System.out.println("getLatitude");
        Point instance = new Point(20,30,CoordSystem.EQUATORIAL);
        double expResult = 30.0;
        double result = instance.getLatitude();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getLongitude method, of class Point.
     */
    @Test
    public void testGetLongitude() {
        System.out.println("getLongitude");
        Point instance = new Point(20,30,CoordSystem.EQUATORIAL);
        double expResult = 20.0;
        double result = instance.getLongitude();
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getCoordSystem method, of class Point.
     */
    @Test
    public void testGetCoordSystem() {
        System.out.println("getCoordSystem");
        Point instance = new Point(20,30,CoordSystem.EQUATORIAL);
        CoordSystem expResult = CoordSystem.EQUATORIAL;
        CoordSystem result = instance.getCoordSystem();
        assertEquals(expResult, result);
    }

    /**
     * Test of isSurface method, of class Point.
     */
    @Test
    public void testIsSurface() {
        System.out.println("isSurface");
        Point instance = new Point(20,30,CoordSystem.EQUATORIAL);
        boolean expResult = false;
        boolean result = instance.isSurface();
        assertEquals(expResult, result);
    }
}
