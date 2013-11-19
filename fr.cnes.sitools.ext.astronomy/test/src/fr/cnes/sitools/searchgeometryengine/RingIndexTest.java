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
public class RingIndexTest {    

    /**
     * Test of setOrder method, of class RingIndex.
     */
    @Test
    public void testSetOrder() throws Exception {
        System.out.println("setOrder");
        int order = 0;
        RingIndex instance = new RingIndex(new Point(0,0,CoordSystem.EQUATORIAL));
        instance.setOrder(order);
    }

    /**
     * Test of getOrder method, of class RingIndex.
     */
    @Test
    public void testGetOrder() throws Exception {
        System.out.println("getOrder");
        RingIndex instance = new RingIndex(new Point(0,0,CoordSystem.EQUATORIAL));
        instance.setOrder(0);
        int expResult = 0;
        int result = instance.getOrder();
        assertEquals(expResult, result);
    }

    /**
     * Test of getShape method, of class RingIndex.
     */
    @Test
    public void testGetShape() throws Exception {
        System.out.println("getShape");
        RingIndex instance = new RingIndex(new Point(0,0,CoordSystem.EQUATORIAL));
        Shape expResult = new Point(0,0,CoordSystem.EQUATORIAL);
        Shape result = instance.getShape();
        assertEquals(expResult, result);
    }
}
