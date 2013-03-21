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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.MocCell;


public class MocTest {

   // Juste pour tester le parsing d'un flux ASCII
   private static boolean testASCII() throws Exception {
      title("testStream: read and ASCII Moc file and display it...");
      HealpixMoc moc = new HealpixMoc();
      String s="/Users/Pierre/Documents/Fits et XML/sdss7_nside128.txt";
      System.out.println("- Reading ASCII file: "+s);
      moc.read(s,HealpixMoc.ASCII);
      System.out.println(moc);
      System.out.println("- Reading ASCII string...");
      HealpixMoc moc1 = new HealpixMoc(moc.toString());
      moc1.setCoordSys(moc.getCoordSys());
      if( !moc1.equals(moc) ) {
         System.out.println("MocTest.testASCII ERROR");
         System.out.println("Moc from ASCII file  : "+moc.todebug());
         System.out.println("Moc from ASCII string: "+moc1.todebug());
         HealpixMoc moc2 = moc1.getSize()>moc.getSize() ? moc1.subtraction(moc) : moc.subtraction(moc1);
         System.out.println("Moc diff: "+moc2.todebug());
         return false;
      }
      System.out.println("MocTest.testASCII OK");
      return true;
   }

   // Tests pour le mode compresse
   private static boolean testCompressed() throws Exception {
      title("testCompressed: Create a simple Moc, write and re-read it in compressed FITS mode...");
      String ref = "{ \"3\":[1,2,4,7,8,9,10] }";
      HealpixMoc moc = new HealpixMoc();
      moc.add(ref);
      String filename = "/Users/Pierre/Desktop/Compressed.fits";
      File f = new File(filename);
      if( f.exists() ) f.delete();
      FileOutputStream fo = new FileOutputStream(f);
      moc.writeFITS(fo,true);
      moc.read(filename,HealpixMoc.FITS);
//      System.out.println("testCompressed: ref="+ref+"\n result => "+moc);
      
      if( !moc.toString().equals(ref) ) {
         System.out.println("MocTest.testCompressed ERROR");
         return false;
      } 
      
      System.out.println("MocTest.testCompressed OK");
      return true;
   }


   // Juste pour tester quelques methodes */
   private static boolean testBasic() throws Exception {
      title("testBasic: Create a Moc manually and check the result...");
      String ref = " 3/1 3/3 3/10 4/16 4/17 4/18 4/22";
      HealpixMoc moc = new HealpixMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.add("4/13-18 5/19 20");
      moc.add("3/1");
      moc.sort();
      Iterator<MocCell> it = moc.iterator();
      StringBuffer s = new StringBuffer();
      while( it.hasNext() ) {
         MocCell p = it.next();
         s.append(" "+p.order+"/"+p.npix);
      }
      boolean rep = s.toString().equals(ref);
      if( !rep ) {
         System.out.println("MocTest.testBasic ERROR: \n.get ["+s+"]\n.ref ["+ref+"]\n");
      } else System.out.println("MocTest.testBasic OK");
      return rep;
   }

   private static boolean testHierarchy() throws Exception {
      title("testHierarchy: Create a Moc manually and check isIn(), isAscendant() and isDescendant() methods...");
      String ref = "3/10-12 5/128";
      HealpixMoc moc = new HealpixMoc(ref);
      boolean b;
      boolean rep=true;
      System.out.println("REF: "+ref);
      System.out.println("MOC:\n"+moc);
      System.out.println("- 3/11 [asserting true] isIn()="+(b=moc.isIn(3,11))); rep &= b;
      System.out.println("- 3/12 [asserting true] isIn()="+(b=moc.isIn(3,11))); rep &= b;
      System.out.println("- 2/0 [asserting false] isAscendant()="+(b=moc.isAscendant(2,0))); rep &= !b;
      System.out.println("- 1/0 [asserting true] isAscendant()="+(b=moc.isAscendant(1,0))); rep &= b;
      System.out.println("- 6/340000 [asserting false] isDescendant()="+(b=moc.isDescendant(6,340000)));  rep &=!b;
      System.out.println("- 6/515 [asserting true] isDescendant()="+(b=moc.isDescendant(6,514))); rep &=b;
      if( !rep ) System.out.println("MocTest.testContains ERROR:");
      else System.out.println("MocTest.testContains OK");
      return rep;
   }

   private static boolean testContains() throws Exception {
      title("testContains: Create a Moc manually and check contains() methods...");
      Healpix hpx = new Healpix();
      HealpixMoc moc = new HealpixMoc("2/0 3/10 4/35");
      System.out.println("MOC: "+moc);
      boolean b;
      boolean rep=true;
      try {
         System.out.println("- contains(028.93342,+18.18931) [asserting IN]    => "+moc.contains(hpx,028.93342,18.18931)); rep &= moc.contains(hpx,028.93342,18.18931);
         System.out.println("- contains(057.23564,+15.34922) [asserting OUT]   => "+moc.contains(hpx,057.23564,15.34922)); rep &= !moc.contains(hpx,057.23564,15.34922);
         System.out.println("- contains(031.89266,+17.07820) [asserting IN]    => "+moc.contains(hpx,031.89266,17.07820)); rep &= moc.contains(hpx,031.89266,17.07820);
      } catch( Exception e ) {
         e.printStackTrace();
         rep=false;
      }
      if( !rep ) System.out.println("MocTest.testContains ERROR:");
      else System.out.println("MocTest.testContains OK");
      return rep;
  }

   private static boolean testFITS() throws Exception {
      title("testFITS: Create a Moc manually, write in FITS and re-read it...");
      HealpixMoc moc = new HealpixMoc();
      moc.setCoordSys("C");
      moc.add("3/10 4/12-15 18 22");
      moc.add("4/13-18 5/19 20");
      moc.add("17/222 28/123456789");
      String mocS="{ \"3\":[3,10], \"4\":[16,17,18,22], \"5\":[19,20], \"17\":[222], \"28\":[123456789] }";
      int mode= HealpixMoc.FITS;
//      int mode= HealpixMoc.ASCII;
      String ext = (mode==HealpixMoc.FITS?"fits":"txt");
      String file = "/Users/Pierre/Desktop/HEALPixMOCM."+ext;
      System.out.println("- MOC created: "+moc);
      moc.write(file,mode);
      System.out.println("- test write ("+ext+") seems OK");
      moc = new HealpixMoc();
      moc.read(file);
      System.out.println("- test read seems OK");
      System.out.println("- MOC re-read: "+moc);
      if( !moc.toString().equals(mocS) ) {
         System.out.println("MocTest.testFITS ERROR: waiting=["+mocS+"]");
         return false;
      }
      
      System.out.println("MocTest.testFITS OK");
      return true;
   }
   
   private static boolean testOperation() throws Exception {
      title("testOperation: Create 2 Mocs manually, test intersection(), union(), equals(), clone()...");
      HealpixMoc moc1 = new HealpixMoc("3/1,3-4,9 4/30-31");
      String moc1S = "{ \"3\":[1,3,4,9], \"4\":[30,31] }";
      moc1.setCoordSys("C");
      System.out.println("- Loading moc1: "+moc1);
      if( !moc1.toString().equals(moc1S) ) {
         System.out.println("MocTest.testOperation load ERROR: waiting=["+moc1S+"]");
         return false;
      }
      
      HealpixMoc moc2 = new HealpixMoc("4/23 3/3 10 4/23-28;4/29 5/65");
      String moc2S = "{ \"3\":[3,6,10], \"4\":[23,28,29], \"5\":[65] }";
      System.out.println("- Loading moc2: "+moc2);
      moc2.setCoordSys("C");
      if( !moc2.toString().equals(moc2S) ) {
         System.out.println("MocTest.testOperation load ERROR: waiting=["+moc2S+"]");
         return false;
      }
      
      HealpixMoc moc3 = (HealpixMoc)moc2.clone();
      System.out.println("- Cloning moc2->moc3: "+moc3);
      if( !moc3.toString().equals(moc2S) ) {
         System.out.println("MocTest.testOperation clone ERROR: waiting=["+moc2S+"]");
         return false;
      }

      HealpixMoc moc4 = moc2.intersection(moc1);
      String moc4S = "{ \"3\":[3], \"5\":[65] }";
      System.out.println("- Intersection moc2 moc1: "+moc4);
      if( !moc4.toString().equals(moc4S) ) {
         System.out.println("MocTest.testOperation intersection ERROR: waiting=["+moc4S+"]");
         return false;
      }
      if( !moc1.intersection(moc2).toString().equals(moc4S) ) {
         System.out.println("MocTest.testOperation intersection ERROR: no commutative");
         return false;
      }
     
      HealpixMoc moc5 = moc3.union(moc1);
      String moc5S = "{ \"3\":[1,3,4,6,7,9,10], \"4\":[23] }";
      System.out.println("- Union moc3 moc1: "+moc5);
      if( !moc1.union(moc2).toString().equals(moc5S) ) {
         System.out.println("MocTest.testOperation union ERROR: no commutative");
         return false;
      }
      if( !moc5.toString().equals(moc5S) ) {
         System.out.println("MocTest.testOperation union ERROR: waiting=["+moc5S+"]");
         return false;
      }

      HealpixMoc moc7 = moc1.subtraction(moc2);
      String moc7S = "{ \"3\":[1,9], \"4\":[17,18,19,30,31], \"5\":[64,66,67] }";
      System.out.println("- Subtraction moc1 - moc2: "+moc7);
      if( !moc7.toString().equals(moc7S) ) {
         System.out.println("MocTest.testOperation subtraction ERROR: waiting=["+moc7S+"]");
         return false;
      }

      String moc6S="{ \"3\":[3,6,10], \"4\":[23,28,29] }";
      HealpixMoc moc6 = new HealpixMoc(moc6S);
      boolean test=moc6.equals(moc2);
      System.out.println("- Not-equals moc2,["+moc6S+"] : "+test);
      if( test ) {
         System.out.println("MocTest.testOperation equals ERROR: waiting=[false]");
         return false;
      }
      moc6.add("5:65");
      test=moc6.equals(moc2);
      System.out.println("- Equals moc2,["+moc2S+"] : "+test);
      if( !test ) {
         System.out.println("MocTest.testOperation equals ERROR: waiting=[true]");
         return false;
      }
      
      HealpixMoc moc8 = moc1.difference(moc2);
      String moc8S = "{ \"3\":[1,6,7,9,10], \"4\":[17,18,19,23], \"5\":[64,66,67] }";
      System.out.println("- difference moc1  moc2: "+moc8);
      if( !moc8.toString().equals(moc8S) ) {
         System.out.println("MocTest.testOperation difference ERROR: waiting=["+moc8S+"]");
         return false;
      }
      if( !moc1.difference(moc2).toString().equals(moc8S) ) {
         System.out.println("MocTest.testOperation difference ERROR: no commutative");
         return false;
      }
      
//      HealpixMoc moc10=new HealpixMoc("0/2-11 1/1-3");
//      HealpixMoc moc9 = moc10.complement();
//      String moc9S = "{ \"0\":[1], \"1\":[0] }";
//      System.out.println("- Moc       : "+moc10);
//      System.out.println("- Complement: "+moc9);
//      if( !moc9.toString().equals(moc9S) ) {
//         System.out.println("MocTest.testOperation complement ERROR: waiting=["+moc9S+"]");
//         return false;
//      }

     
      System.out.println("MocTest.testOperation OK");
      return true;
   }

   // Juste pour convertir d'un format a un autre
   private static boolean testConvert() throws Exception {
      title("testConvert: Load a Moc in ASCII and write it in FITS...");
      HealpixMoc moc = new HealpixMoc();
      moc.read("/Users/Pierre/Documents/Fits et XML/sdss7_nside256.txt",HealpixMoc.ASCII);
//      moc.read("/Users/Pierre/Desktop/coverage-II_294_sdss7-128.txt",HealpixMoc.ASCII);
      String s = "/Users/Pierre/Desktop/SDSS_HpxMOCM.fits";
      int mode = HealpixMoc.FITS;
      moc.write(s,mode);
      System.out.println(s+" generated");
      moc = new HealpixMoc();
      moc.read(s,mode);
      System.out.println("test read seems OK");
      System.out.println("Result:\n"+moc);
      return true;
   }
   
   // Juste pour convertir d'un format a un autre
//   private static boolean testConvert1() throws Exception {
//      HealpixMoc moc = new HealpixMoc();
//      moc.read("C:/Documents and Settings/Standard/Bureau/2MASStestALLSKY/Moc.fits");
//      String s = "/Documents and Settings/Standard/Bureau/2MASStestALLSKY/Moc.txt";
//      int mode = HealpixMoc.ASCII;
//      moc.write(s,mode);
//      System.out.println(s+" generated");
//      return true;
//   }
   
   private static void testIsInTree() throws Exception {
      title("testIsInTree: Create 2 Mocs manually, and test isInTree() method...");
      HealpixMoc moc1 = new HealpixMoc("0/1");
      HealpixMoc moc2 = new HealpixMoc("1/5");
      System.out.println("moc1="+moc1);
      System.out.println("moc2="+moc2);
      System.out.println("moc2 inter moc1 = "+moc2.isInTree(moc1));
      System.out.println("moc1 inter moc2 = "+moc1.isInTree(moc2));
   }
   
   private static void testConsistency() throws Exception {
      title("testConsistency: Create a Mocs manually, and test consistency feature...");
      String s = "0/0 1/2-3 2/60 3/100 4/1000 5/10000";
      System.out.println("- add: "+s);
      HealpixMoc moc2 = new HealpixMoc(1,4);
      moc2.add(s);
      System.out.println("- moc2 limitOrder=["
            +moc2.getMinLimitOrder()+".."+moc2.getMaxLimitOrder()+"]\n"
            +moc2);
      
      HealpixMoc moc1 = new HealpixMoc();
      moc1.setCheckConsistencyFlag(false);
      moc1.add(s);
      System.out.println("moc1="+moc1);
      moc1.setLimitOrder(3);
      System.out.println("moc1="+moc1);
     
      HealpixMoc moc3 = new HealpixMoc();
      moc3.setCheckConsistencyFlag(false);
      try {
         moc3.add("30/56");
         System.out.println("moc3="+moc3);
      } catch( Exception e ) {
         System.out.println("moc3 exception: "+e.getMessage());
      }
      
      HealpixMoc moc4 = new HealpixMoc();
      moc4.setMinLimitOrder(3);
      moc4.add("0/1 3/63-66 4/768");
      System.out.println("moc4="+moc4);
   }
   
   private static boolean testRange() throws Exception {
      title("testRange: Create a Mocs manually, and test setMin and Max limitOrder()...");
      HealpixMoc moc1 = new HealpixMoc("{ \"1\":[0,1], \"2\":[8,9], \"3\":[40,53] }");
      System.out.println("moc1="+moc1);
      moc1.setCheckConsistencyFlag(false);
      moc1.add("3/37 53");
      System.out.println("moc1 + 3/37,46="+moc1);
      moc1.checkAndFix();
      System.out.println("moc1 after check="+moc1);
      String s1 = "{ \"1\":[0,1], \"2\":[8,9], \"3\":[40,53] }";
      if( !moc1.toString().equals(s1) ) {
         System.out.println("MocTest.testRange checkAndFix() ERROR: waiting=["+s1+"]");
         return false;
      }
      
      HealpixMoc moc2 = (HealpixMoc)moc1.clone();
      moc2.setMinLimitOrder(2);
      System.out.println("moc2="+moc2);
      String s2 = "{ \"2\":[0,1,2,3,4,5,6,7,8,9], \"3\":[40,53] }";
      if( !moc2.toString().equals(s2) ) {
         System.out.println("MocTest.testRange setMinLimitOrder(2) ERROR: waiting=["+s2+"]");
         return false;
      }

      HealpixMoc moc3 = (HealpixMoc)moc1.clone();
      moc3.setMaxLimitOrder(2);
      System.out.println("moc3="+moc3);
      String s3 = "{ \"1\":[0,1], \"2\":[8,9,10,13] }";
      if( !moc3.toString().equals(s3) ) {
         System.out.println("MocTest.testRange setMaxLimitOrder(2) ERROR: waiting=["+s3+"]");
         return false;
      }
      
      moc3.setMinLimitOrder(1);
      boolean in1 = moc3.isIntersecting(0, 1);
      if( in1 ) {
         System.out.println("MocTest.testRange isIntersecting(0,1) ERROR: waiting=false]");
         return false;
      }
      boolean in2 = moc3.isIntersecting(0, 0);
      if( !in2 ) {
         System.out.println("MocTest.testRange isIntersecting(0,0) ERROR: waiting=true]");
         return false;
      }
      boolean in3 = moc3.isIntersecting(3, 33);
      if( !in3 ) {
         System.out.println("MocTest.testRange isIntersecting(3,33) ERROR: waiting=true]");
         return false;
      }
      boolean in5 = moc3.isIntersecting(3, 56);
      if( in5 ) {
         System.out.println("MocTest.testRange isIntersecting(3,56) ERROR: waiting=false]");
         return false;
      }
      
      System.out.println("testRange OK");
      return true;
   }
   
   private static boolean testIterators() throws Exception {
      title("testIterators: Test on MOC iterators...");
      String ref = " 0/2 1/4 1/13";
      HealpixMoc moc = new HealpixMoc();
      moc.add(ref);
      
      // Iterator order per order
      Iterator<MocCell> it = moc.iterator();
      StringBuffer s = new StringBuffer();
      while( it.hasNext() ) {
         MocCell p = it.next();
         s.append(" "+p.order+"/"+p.npix);
      }
      boolean rep = s.toString().equals(ref);
      if( !rep ) {
         System.out.println("MocTest.testIterators [iterator()] ERROR:\n.get ["+s+"]\n.ref ["+ref+"]\n");
         return false;
      }
      
      // Iterator on low level pixel
      String ref1 = " 4 8 9 10 11 13";
      Iterator<Long> it1 = moc.pixelIterator();
      s = new StringBuffer();
      while( it1.hasNext() ) {
         Long p = it1.next();
         s.append(" "+p);
      }
      rep = s.toString().equals(ref1);
      if( !rep ) {
         System.out.println("MocTest.testIterators [pixelIterator()] ERROR:\n.get ["+s+"]\n.ref ["+ref1+"]\n");
         return false;
      }

      
      System.out.println("MocTest.testIterators OK");
      return true;
   }
   
   private static boolean testUnionOperation() throws Exception {
      title("testUnionOperation: ...");
      
      HealpixMoc moc1 = new HealpixMoc();
//      moc1.read("C:/Documents and Settings/Standard/Mes documents/Workspace/MocServer/Moc/I_315_out.fits");
      moc1.read("/Users/Pierre/Documents/Workspace/MocServer/Moc/V_95_sky2000.fits");
      HealpixMoc moc2 = new HealpixMoc();
      moc2.read("/Users/Pierre/Documents/Workspace/MocServer/Moc/GALEX_AIS.fits");
      
//      moc1.read("C:/Documents and Settings/Standard/Bureau/MocImg/SDSS.fits");
//      HealpixMoc moc2 = new HealpixMoc();
//      moc2.read("C:/Documents and Settings/Standard/Bureau/MocImg/2xmmi-hpxFP_128.txt");


      System.out.println("Union between "+moc1.getSize()+" and "+moc2.getSize());
      int N=1;
      HealpixMoc moc3=null;
      long t,t1;
      t = System.currentTimeMillis();
      for( int i=0; i<N; i++ ) {
         moc3 = moc1.union(moc2);
      }
      t1=System.currentTimeMillis();
      System.out.println("Union (new method) done in "+((t1-t)/N)+"ms");
      System.out.println("moc3="+moc3);
      
      moc3.write("C:/Documents and Settings/Standard/Bureau/MOC3.txt", HealpixMoc.ASCII);
     
      return true;
   }
   
   private static boolean testIntersectionOperation() throws Exception {
      title("testIntersectionOperation: ...");
      
      HealpixMoc moc1 = new HealpixMoc();
//      moc1.read("C:/Documents and Settings/Standard/Mes documents/Workspace/MocServer/Moc/I_315_out.fits");
//      moc1.read("C:/Documents and Settings/Standard/Mes documents/Workspace/MocServer/Moc/V_95_sky2000.fits");
//      HealpixMoc moc2 = new HealpixMoc();
//      moc2.read("C:/Documents and Settings/Standard/Mes documents/Workspace/MocServer/Moc/GALEX_AIS.fits");
      
      moc1.read("C:/Users/Pierre/desktop/MocImg/SDSS.fits");
      HealpixMoc moc2 = new HealpixMoc();
      moc2.read("C:/Users/Pierre/desktop/MocImg/2xmmi-hpxFP_128.txt");
      moc1.toRangeSet();
      moc2.toRangeSet();

      System.out.println("Intersection between "+moc1.getSize()+" and "+moc2.getSize());
      int N=4;
      HealpixMoc moc3=null;
      long t,t1;
      t = System.currentTimeMillis();
      for( int i=0; i<N; i++ ) {
         moc3 = moc1.intersection(moc2);
      }
      t1=System.currentTimeMillis();
      System.out.println("Intersection (new method) done in "+((t1-t)/N)+"ms");
      
      moc3.write("C:/Users/Pierre/desktop/MOC5.txt", HealpixMoc.ASCII);
      
     
      return true;
   }

   private static boolean testSoustractionOperation() throws Exception {
      title("testsoustractionOperation: ...");
      
      HealpixMoc moc1 = new HealpixMoc();
//      moc1.read("C:/Documents and Settings/Standard/Mes documents/Workspace/MocServer/Moc/I_315_out.fits");
//      moc1.read("C:/Documents and Settings/Standard/Mes documents/Workspace/MocServer/Moc/V_95_sky2000.fits");
//      HealpixMoc moc2 = new HealpixMoc();
//      moc2.read("C:/Documents and Settings/Standard/Mes documents/Workspace/MocServer/Moc/GALEX_AIS.fits");
      
      moc1.read("C:/Users/Pierre/desktop/MocImg/SDSS.fits");
      HealpixMoc moc2 = new HealpixMoc();
      moc2.read("C:/Users/Pierre/desktop/MocImg/2xmmi-hpxFP_128.txt");

      System.out.println("soustraction between "+moc1.getSize()+" and "+moc2.getSize());
      int N=4;
      HealpixMoc moc3=null;
      long t,t1;
      t = System.currentTimeMillis();
      for( int i=0; i<N; i++ ) {
         moc3 = moc1.subtraction(moc2);
      }
      t1=System.currentTimeMillis();
      System.out.println("soustraction (new method) done in "+((t1-t)/N)+"ms");
      System.out.println("moc3="+moc3);
      
      HealpixMoc moc4=null;
      t = System.currentTimeMillis();
      for( int i=0; i<N; i++ ) {
         moc4 = moc1.subtraction(moc2);
      }
      t1=System.currentTimeMillis();
      System.out.println("soustraction done in "+((t1-t)/N)+"ms");
      System.out.println("moc4="+moc4);
      if( !moc3.equals(moc4) ) {
         System.out.println("Y a un bleme => sauvegarde dans MOC4.txt");
         moc4.write("C:/Documents and Settings/Standard/Bureau/MOC4.txt", HealpixMoc.ASCII);
      }
      else System.out.println("Les deux MOC sont identiques => sauvegarde dans MOC3.txt");
      moc3.write("C:/Documents and Settings/Standard/Bureau/MOC3.txt", HealpixMoc.ASCII);
      
     
      return true;
   }
   
   private static void title(String s) {
      StringBuffer s1 = new StringBuffer(100);
      s1.append('\n');
      for( int i=0; i<20; i++ ) s1.append('-');
      s1.append(" "+s+" ");
      for( int i=0; i<20; i++ ) s1.append('-');
      System.out.println(s1);
   }
   

// Juste pour tester
   public static void main(String[] args) {
      try {
         
         testBasic();
         testIterators();
         testContains(); 
         testOperation(); 
         testASCII();
         testFITS();
         testCompressed();
         testHierarchy();
         testConsistency();
         testRange();
          
         
//         testConvert();
//         testIsInTree();
         testIntersectionOperation();
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }



}
