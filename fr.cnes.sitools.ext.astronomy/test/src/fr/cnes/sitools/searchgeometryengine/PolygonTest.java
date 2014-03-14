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
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 *
 * @author malapert
 */
public class PolygonTest {

  @Test
  public void testSearchGeometryGeocentric() throws Exception {
    Polygon polygon = new Polygon(new Point(-118.95597, -36.1787, CoordSystem.GEOCENTRIC),
            new Point(106.04403, 73.68463, CoordSystem.GEOCENTRIC));
    Index index = AbstractGeometryIndex.createIndex(polygon, fr.cnes.sitools.searchgeometryengine.Scheme.valueOf(Scheme.RING.name()));
    ((RingIndex) index).setOrder(3);
    RangeSet range = (RangeSet) index.getIndex();
    RangeSet expectedRange = new RangeSet();
    expectedRange.add(4, 7);
    expectedRange.add(9, 16);
    expectedRange.add(19, 29);
    expectedRange.add(34, 46);
    expectedRange.add(53, 68);
    expectedRange.add(75, 93);
    expectedRange.add(102, 122);
    expectedRange.add(133, 155);
    expectedRange.add(165, 186);
    expectedRange.add(197, 218);
    expectedRange.add(229, 250);
    expectedRange.add(261, 282);
    expectedRange.add(293, 314);
    expectedRange.add(325, 346);
    expectedRange.add(357, 378);
    expectedRange.add(389, 410);
    expectedRange.add(421, 442);
    expectedRange.add(453, 474);
    expectedRange.add(485, 506);
    expectedRange.add(517, 539);
    expectedRange.add(549, 570);
    expectedRange.add(581, 603);
    expectedRange.add(613, 634);
    expectedRange.add(645, 656);
    assertEquals(expectedRange.toString(), range.toString());
  }

  @Test
  public void testSearchGeometryEquatorialFootprint() throws Exception {
    Point p1 = new Point(350, -10, CoordSystem.EQUATORIAL);
    Point p2 = new Point(350, 10, CoordSystem.EQUATORIAL);
    Point p3 = new Point(50, 10, CoordSystem.EQUATORIAL);
    Point p4 = new Point(50, -10, CoordSystem.EQUATORIAL);
    Polygon polygon = new Polygon(Arrays.asList(p1,p4,p3,p2));
    Index index = AbstractGeometryIndex.createIndex(polygon, fr.cnes.sitools.searchgeometryengine.Scheme.valueOf(Scheme.RING.name()));
    ((RingIndex)index).setOrder(3);
    RangeSet range = (RangeSet) index.getIndex();     
    RangeSet expectedRange = new RangeSet();
    expectedRange.add(272, 277);
    expectedRange.add(303, 309);
    expectedRange.add(335, 341);
    expectedRange.add(367, 373);
    expectedRange.add(399, 405);
    expectedRange.add(431, 437);
    expectedRange.add(463, 469);
    expectedRange.add(495, 496);
    assertEquals(expectedRange, range);
  }  
  

  /**
   * Test of getPoints method, of class Polygon.
   */
  @Test
  public void testGetPoints() {
    System.out.println("getPoints");
    Polygon instance = new Polygon(Arrays.asList(new Point(190, -80, CoordSystem.EQUATORIAL),
            new Point(190, 80, CoordSystem.EQUATORIAL),
            new Point(170, 80, CoordSystem.EQUATORIAL),
            new Point(170, -80, CoordSystem.EQUATORIAL)));
    List expResult = Arrays.asList(new Point(190, -80, CoordSystem.EQUATORIAL),
            new Point(190, 80, CoordSystem.EQUATORIAL),
            new Point(170, 80, CoordSystem.EQUATORIAL),
            new Point(170, -80, CoordSystem.EQUATORIAL));
    List result = instance.getPoints();
    assertEquals(expResult, result);
  }

  /**
   * Test of isClockwised method, of class Polygon.
   */
  @Test
  public void testIsClockwise() {
    System.out.println("isClockwise");
    Polygon instance = new Polygon(Arrays.asList(new Point(0, 0, CoordSystem.EQUATORIAL),
            new Point(0, 20, CoordSystem.EQUATORIAL),
            new Point(50, 20, CoordSystem.EQUATORIAL),
            new Point(50, 0, CoordSystem.EQUATORIAL)));
    boolean expResult = true;
    boolean result = instance.isClockwised();
    assertEquals(expResult, result);
  }

  /**
   * Test of isSurface method, of class Polygon.
   */
  @Test
  public void testIsSurface() {
    System.out.println("isSurface");
    Polygon instance = new Polygon();
    boolean expResult = true;
    boolean result = instance.isSurface();
    assertEquals(expResult, result);
  }

}
