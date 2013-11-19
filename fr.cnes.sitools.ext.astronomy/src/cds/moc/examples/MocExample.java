// Copyright 2011 - UDS/CNRS
// The MOC API project is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of MOC API java project.
//
//    MOC API java project is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    MOC API java project is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with MOC API java project.
//

package cds.moc.examples;

import java.io.BufferedInputStream;
import java.net.URL;

import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.MocCell;

public class MocExample {
   
    static public void main(String[] args) throws Exception {
       
       try {
         // Creation by Stream
          URL url = new URL("http://alasky.u-strasbg.fr/footprints/tables/vizier/II_311_wise/MOC");
          BufferedInputStream bis = new BufferedInputStream(url.openStream(), 32*1024);
          HealpixMoc mocA = new HealpixMoc(bis, HealpixMoc.FITS);

          System.out.println("Moc sky coverage : "+pourcent(mocA.getCoverage()));
          System.out.println("Moc resolution   : "+(int)(mocA.getAngularRes()*6000)/100.+" arcmin");
          System.out.println("Number of cells  : "+mocA.getSize());
          System.out.println("Max order        : "+mocA.getMaxOrder());
          System.out.println("Sorted           : "+mocA.isSorted());
          System.out.println("In memory        : ~"+mocA.getMem()+" bytes");
          System.out.println("Contents         :"); display("MocA",mocA);
//        for( MocCell item : moc ) System.out.println(" "+item);
          
          // Creation by list of cells (JSON format), write ASCII, and reread it
          HealpixMoc mocB = new HealpixMoc();
          mocB.add("{ \"3\":[2,53,55], \"4\":[20,21,22,25,28,30,50,60], \"5\":[456,567,836], \"9\":[123456] }");
          mocB.write("/Moc.txt", HealpixMoc.ASCII);
          mocB.read("/Moc.txt",HealpixMoc.ASCII);
          display("MocB",mocB);
          
          // Intersection, union, clone
          HealpixMoc clone = (HealpixMoc)mocA.clone(); 
          HealpixMoc union = mocA.union(mocB);
          HealpixMoc inter = mocA.intersection(mocB);
          System.out.println("\nMocA coverage      : "+pourcent(mocA.getCoverage()));
          System.out.println("MocB coverage      : "+pourcent(mocB.getCoverage()));
          System.out.println("Moc union coverage : "+pourcent(union.getCoverage()));
          System.out.println("Moc inter coverage : "+pourcent(inter.getCoverage()));
          
          // Coordinate query
          Healpix hpx = new Healpix();
          double al,del,radius;
          al = 095.73267; del = 69.55885;
          System.out.println("Coordinate ("+al+","+del+") => inside MocA : "+mocA.contains(hpx,al,del));
          al = 095.60671; del = 69.57092;
          System.out.println("\nCoordinate ("+al+","+del+") => inside MocA : "+mocA.contains(hpx,al,del));
          
          // circle query
          al = 282.81215; del =  -70.20608; radius = 30/60.;
          HealpixMoc circle = mocA.queryDisc(hpx, al, del, radius);
          display("Moc intersection with circle("+al+","+del+","+radius+")", circle);
          circle.setMaxLimitOrder(6);
          display("Same result for limit order 6", circle);
          
          
      } catch( Exception e ) {
         e.printStackTrace();
      }

    }
    
    static String pourcent(double d) { return (int)(1000*d)/100.+"%"; }

    // Just for displaying a few cells
    static public void display(String title,HealpixMoc moc) {
       System.out.print("\nDisplay \""+title+"\"");
       int i=0;
       int oOrder=-1;
       for( MocCell item : moc ) {
          if( oOrder!=item.order ) {
             if( oOrder!=-1 ) System.out.print("],");
             System.out.print("\n\""+item.order+"\":[");
             oOrder=item.order;
             i=0;
          }

          if( i!=0 && i<20) System.out.print(",");
          if( i<20 )  System.out.print(item.npix);
          if( i==21 ) System.out.print("...");
          i++;
       }
       System.out.println("]");
    }
}
