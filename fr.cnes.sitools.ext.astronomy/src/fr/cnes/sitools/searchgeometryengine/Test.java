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
package fr.cnes.sitools.searchgeometryengine;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class Test {
  
  public static void main(String[] args) throws Exception {
    //List<Point> points = new ArrayList<Point>();
    //points.add(new Point(-50,10,CoordSystem.GEOCENTRIC));
    //points.add(new Point(-50,30,CoordSystem.GEOCENTRIC));
    //points.add(new Point(50,30,CoordSystem.GEOCENTRIC));
    //points.add(new Point(50,10,CoordSystem.GEOCENTRIC));
    
    double[] val = Resampling.decCircle(-40, 80, 10);
    for (int i=0 ; i<val.length; i++) 
      System.out.print(val[i]+" ");
    //Polygon polygon = new Polygon(points);
//    points.add(new Point(40,20,CoordSystem.EQUATORIAL));
//    points.add(new Point(60,70,CoordSystem.EQUATORIAL));
//    points.add(new Point(0,70,CoordSystem.EQUATORIAL));    
    //Shape polygon = new Polygon(new Point(-170,-80,CoordSystem.GEOCENTRIC), new Point(170,80,CoordSystem.GEOCENTRIC));    
    //Polygon polygon = new Polygon(new Point(-118.95597, -36.1787, CoordSystem.GEOCENTRIC),
    //        new Point(106.04403, 73.68463, CoordSystem.GEOCENTRIC));    
    //Shape polygon = new Polygon(points);
    //Index index = (NestedIndex) GeometryIndex.createIndex(polygon, Scheme.NESTED);
    //((NestedIndex)index).setOrderMax(10);
    //Index index = (RingIndex) GeometryIndex.createIndex(polygon, Scheme.RING);
    //((RingIndex)index).setOrder(6);    
    //HealpixMoc moc = (HealpixMoc) index.getIndex();
//    RangeSet range = (RangeSet) index.getIndex();
//    RangeSet.ValueIterator iter = range.valueIterator();
//    HealpixMapDouble map = new HealpixMapDouble((long) Math.pow(2, 6), Scheme.RING);
//    while (iter.hasNext()) {
//      map.setPixel(iter.next(), 1.0d);
//    }    

    //Graph graph = new GenericProjection(Graph.ProjectionType.ECQ);
    //graph = new HealpixGridDecorator(graph, Scheme.NESTED, 1);
    //graph = new HealpixFootprint(graph, Scheme.RING, 6, 1.0f);
    //((HealpixFootprint)graph).importHealpixMap(map, CoordinateTransformation.NATIVE);
    //graph = new HealpixMocDecorator(graph, Color.yellow, 0.4f);
    //((HealpixMocDecorator)graph).importMoc(moc);
    //Utility.createJFrame(graph, 900, 500);
  }
  
}
