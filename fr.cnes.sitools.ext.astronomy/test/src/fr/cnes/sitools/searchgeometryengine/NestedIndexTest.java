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

import java.util.logging.Level;

import org.junit.Test;
import org.restlet.engine.Engine;

import cds.moc.HealpixMoc;

/**
 *
 * @author malapert
 */
public class NestedIndexTest {


    /**
     * Test of getShape method, of class MocIndex.
     */
    @Test
    public void testGetShape() {
        System.out.println("getShape");
        Shape cone = new Cone(new Point(20, 50, CoordSystem.EQUATORIAL), Math.toRadians(30));
        MocIndex instance;
        try {
            instance = new MocIndex(cone);
            Shape expResult = new Cone(new Point(20, 50, CoordSystem.EQUATORIAL), Math.toRadians(30));
            Shape result = instance.getShape();
            System.out.println(result.getType()+ ""+result.isSurface());
            assertEquals(expResult, result);
        } catch (Exception ex) {
            Engine.getLogger(NestedIndexTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Test of setShape method, of class MocIndex.
     */
    @Test
    public void testSetShape() {
        System.out.println("setShape");
        Shape val = new Cone(new Point(20, 50, CoordSystem.EQUATORIAL), Math.toRadians(30));
        MocIndex instance = new MocIndex();
        instance.setShape(val);
    }

    /**
     * Test of getOrderMax method, of class MocIndex.
     */
    @Test
    public void testGetOrderMax() {
        System.out.println("getOrderMax");
        MocIndex instance = new MocIndex();
        int expResult = HealpixMoc.MAXORDER;
        int result = instance.getOrderMax();
        assertEquals(expResult, result);
    }

    /**
     * Test of getOrderMin method, of class MocIndex.
     */
    @Test
    public void testGetOrderMin() {
        System.out.println("getOrderMin");
        
        MocIndex instance = new MocIndex();
        int expResult = 0;
        int result = instance.getOrderMin();
        assertEquals(expResult, result);
    }

    /**
     * Test of setOrderMax method, of class MocIndex.
     */
    @Test
    public void testSetOrderMax() {
        System.out.println("setOrderMax");
        int val = 10;
        MocIndex instance = new MocIndex();
        instance.setOrderMax(val);
    }

    /**
     * Test of setOrderMin method, of class MocIndex.
     */
    @Test
    public void testSetOrderMin() {
        System.out.println("setOrderMin");
        int val = 2;
        MocIndex instance = new MocIndex();
        instance.setOrderMin(val);
    }
}
