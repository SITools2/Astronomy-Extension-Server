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
package fr.cnes.sitools.searchgeometryengine;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author malapert
 */
public class CoordSystemTest {
    

    /**
     * Test of values method, of class CoordSystem.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        CoordSystem[] expResult = {CoordSystem.GEOCENTRIC,CoordSystem.EQUATORIAL,CoordSystem.GALACTIC};
        CoordSystem[] result = CoordSystem.values();
        assertEquals(expResult, result);
    }

    /**
     * Test of valueOf method, of class CoordSystem.
     */
    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String name = "GEOCENTRIC";
        CoordSystem expResult = CoordSystem.GEOCENTRIC;
        CoordSystem result = CoordSystem.valueOf(name);
        assertEquals(expResult, result);
    }

    /**
     * Test of convertLongitudeGeoToPhi method, of class CoordSystem.
     */
    @Test
    public void testConvertLongitudeGeoToPhi() {
        System.out.println("convertLongitudeGeoToPhi");
        double longitude = -10.0;
        double expResult = 350.0;
        double result = CoordSystem.convertLongitudeGeoToPhi(longitude);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of convertLatitudeGeoToTheta method, of class CoordSystem.
     */
    @Test
    public void testConvertLatitudeGeoToTheta() {
        System.out.println("convertLatitudeGeoToTheta");
        double latitude = 20.0;
        double expResult = 70.0;
        double result = CoordSystem.convertLatitudeGeoToTheta(latitude);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of convertRaToPhi method, of class CoordSystem.
     */
    @Test
    public void testConvertRaToPhi() {
        System.out.println("convertRaToPhi");
        double ra = 350.0;
        double expResult = 350.0;
        double result = CoordSystem.convertRaToPhi(ra);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of convertDecToTheta method, of class CoordSystem.
     */
    @Test
    public void testConvertDecToTheta() {
        System.out.println("convertDecToTheta");
        double dec = 20.0;
        double expResult = 70.0;
        double result = CoordSystem.convertDecToTheta(dec);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of convertPhiToRa method, of class CoordSystem.
     */
    @Test
    public void testConvertPhiToRa() {
        System.out.println("convertPhiToRa");
        double phi = 20.0;
        double expResult = 20.0;
        double result = CoordSystem.convertPhiToRa(phi);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of convertThetaToDec method, of class CoordSystem.
     */
    @Test
    public void testConvertThetaToDec() {
        System.out.println("convertThetaToDec");
        double theta = 30.0;
        double expResult = 60.0;
        double result = CoordSystem.convertThetaToDec(theta);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of convertPhiToLongitudeGeo method, of class CoordSystem.
     */
    @Test
    public void testConvertPhiToLongitudeGeo() {
        System.out.println("convertPhiToLongitudeGeo");
        double phi = 330.0;
        double expResult = -30.0;
        double result = CoordSystem.convertPhiToLongitudeGeo(phi);
        assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of convertThetaToLatitudeGeo method, of class CoordSystem.
     */
    @Test
    public void testConvertThetaToLatitudeGeo() {
        System.out.println("convertThetaToLatitudeGeo");
        double theta = 20.0;
        double expResult = 70.0;
        double result = CoordSystem.convertThetaToLatitudeGeo(theta);
        assertEquals(expResult, result, 0.0);
    }
}
