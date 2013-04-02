/*
 * Copyright 2013 - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.SearchGeometryEngine;

import cds.moc.HealpixMoc;
import fr.cnes.sitools.astro.graph.CoordinateDecorator;
import fr.cnes.sitools.astro.graph.GenericProjection;
import fr.cnes.sitools.astro.graph.Graph;
import fr.cnes.sitools.astro.graph.HealpixFootprint;
import fr.cnes.sitools.astro.graph.HealpixGridDecorator;
import fr.cnes.sitools.astro.graph.HealpixMocDecorator;
import fr.cnes.sitools.astro.graph.Utility;
import healpix.essentials.HealpixMapDouble;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.awt.Color;
import java.util.Arrays;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class Test_1 {

  public static void main(String[] args) throws Exception {
    Point p1 = new Point(175, -60, CoordSystem.EQUATORIAL);
    Point p2 = new Point(195, -60, CoordSystem.EQUATORIAL);
    Point p3 = new Point(195, 60, CoordSystem.EQUATORIAL);
    Point p4 = new Point(175, 60, CoordSystem.EQUATORIAL);
    //CCwise is the sens (small polygon)
    //cwise => large polygon
    //Point p1 = new Point(-20, -60, CoordSystem.GEOCENTRIC);
    //Point p2 = new Point(60, -60, CoordSystem.GEOCENTRIC);
    //Point p3 = new Point(60, 60, CoordSystem.GEOCENTRIC);
    //Point p4 = new Point(-20, 60, CoordSystem.GEOCENTRIC);    
    Polygon polygon = new Polygon(Arrays.asList(p1,p2,p3,p4));
    System.out.println("clockwised:"+polygon.isClockwised());
    System.out.println("clockwise:"+polygon.isClockwise());
    //System.out.println("test:"+Polygon.isClockWised(p1, p2, p3));
    //Polygon polygon = new Polygon(new Point(-20, -60, CoordSystem.GEOCENTRIC),
    //        new Point(20, 60, CoordSystem.GEOCENTRIC));    
    System.out.println("clockwised:"+polygon.isClockwised());
    //Polygon polygon = new Polygon(new Point(-180, -89, CoordSystem.GEOCENTRIC),
    //        new Point(180, 0, CoordSystem.GEOCENTRIC));
    Index index = (NestedIndex) GeometryIndex.createIndex(polygon, Scheme.NESTED);
    ((NestedIndex) index).setOrderMax(10);
    HealpixMoc moc = (HealpixMoc) index.getIndex();
//    Shape polygon = new Cone(new Point(180,0,CoordSystem.GEOCENTRIC), Math.PI * 2. / 8.);
//    
    //Index index = GeometryIndex.createIndex(polygon, Scheme.RING);
    //((RingIndex) index).setOrder(10);    
    //RangeSet range = (RangeSet) index.getIndex(); 
    //RangeSet.ValueIterator iter = range.valueIterator();
    //HealpixMapDouble map = new HealpixMapDouble((long) Math.pow(2, 10), Scheme.RING);
    //while (iter.hasNext()) {
    //  map.setPixel(iter.next(), 1.0d);
    //} 
//    
    Graph graph = new GenericProjection(Graph.ProjectionType.ECQ);
    graph = new CoordinateDecorator(graph);
    graph = new HealpixGridDecorator(graph, Scheme.RING, 1);
    graph = new HealpixMocDecorator(graph, Color.yellow, 0.4f);
    ((HealpixMocDecorator) graph).importMoc(moc);
    //graph = new HealpixFootprint(graph, Scheme.RING, 3, 0.8f);
    //((HealpixFootprint) graph).importHealpixMap(map, HealpixGridDecorator.CoordinateTransformation.NATIVE);
    Utility.createJFrame(graph, 900, 500);
  }
}
