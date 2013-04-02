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
package fr.cnes.sitools.SearchGeometryEngine;

import cds.moc.HealpixMoc;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class NestedIndexTest {


    /**
     * Test of getShape method, of class NestedIndex.
     */
    @Test
    public void testGetShape() {
        System.out.println("getShape");
        Shape cone = new Cone(new Point(20, 50, CoordSystem.EQUATORIAL), Math.toRadians(30));
        NestedIndex instance;
        try {
            instance = new NestedIndex(cone);
            Shape expResult = new Cone(new Point(20, 50, CoordSystem.EQUATORIAL), Math.toRadians(30));
            Shape result = instance.getShape();
            System.out.println(result.getType()+ ""+result.isSurface());
            assertEquals(expResult, result);
        } catch (Exception ex) {
            Logger.getLogger(NestedIndexTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test of setShape method, of class NestedIndex.
     */
    @Test
    public void testSetShape() {
        System.out.println("setShape");
        Shape val = new Cone(new Point(20, 50, CoordSystem.EQUATORIAL), Math.toRadians(30));
        NestedIndex instance = new NestedIndex();
        instance.setShape(val);
    }

    /**
     * Test of getOrderMax method, of class NestedIndex.
     */
    @Test
    public void testGetOrderMax() {
        System.out.println("getOrderMax");
        NestedIndex instance = new NestedIndex();
        int expResult = HealpixMoc.MAXORDER;
        int result = instance.getOrderMax();
        assertEquals(expResult, result);
    }

    /**
     * Test of getOrderMin method, of class NestedIndex.
     */
    @Test
    public void testGetOrderMin() {
        System.out.println("getOrderMin");
        
        NestedIndex instance = new NestedIndex();
        int expResult = 0;
        int result = instance.getOrderMin();
        assertEquals(expResult, result);
    }

    /**
     * Test of setOrderMax method, of class NestedIndex.
     */
    @Test
    public void testSetOrderMax() {
        System.out.println("setOrderMax");
        int val = 10;
        NestedIndex instance = new NestedIndex();
        instance.setOrderMax(val);
    }

    /**
     * Test of setOrderMin method, of class NestedIndex.
     */
    @Test
    public void testSetOrderMin() {
        System.out.println("setOrderMin");
        int val = 2;
        NestedIndex instance = new NestedIndex();
        instance.setOrderMin(val);
    }
}
