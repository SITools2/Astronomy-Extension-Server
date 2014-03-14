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


package cds.moc;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.StringTokenizer;

/** HEALPix Multi Order Coverage Map (MOC)
 * This object provides read, write and process methods to manipulate an HEALPix Multi Order Coverage Map (MOC)
 * A MOC is used to define a sky region by using HEALPix sky tesselation
 *
 * @authors Pierre Fernique [CDS], Martin Reinecke [Max Plank]
 * @version 3.4 oct 2012 - operations by RangeSet
 * @version 3.3 July 2012 - PixelIterator() addition (low level pixel iterator)
 * @version 3.2 April 2012 - JSON ASCII support (the previous basic ASCII format is still supported)
 * @version 3.2 March 2012 - union, intersection,... improvements + refactoring isIntersecting(...)
 * @version 3.1 Dec 2011 - check()
 * @version 3.0 Dec 2011 - 1) Use HealpixInterface 2) replace unicityTest by testConsistency 3)code cleaning
 * @version 2.0 Oct 2011 - use of short, int and long, creation of MocIO class...
 * @version 1.3 Sept 2011 - Support for delete
 * @version 1.2 Sept 2011 - COORDSYS support
 * @version 1.1 Sept 2011 - used sorted MOC (speed improvement)
 * @version 1.0 June 2011 - first stable version
 * @version 0.9 May 2011 - creation
 */
public class HealpixMoc implements Iterable<MocCell>,Cloneable {
   
   /** FITS encoding format */
   static public final int FITS  = 1;
   
   /** ASCII encoding format */
   static public final int ASCII = 0;
   
   /** Maximal HEALPix order supported by the library */
   static public final int MAXORDER = 29;
   
   static public final int SHORT = 0;
   static public final int INT   = 1;
   static public final int LONG  = 2;
   
   /** Provide the integer type for a given order */
   static public int getType(int order) { return order<6 ? SHORT : order<14 ? INT : LONG; }

   private String coordSys;             // Coordinate system (HEALPix convention => G=galactic, C=Equatorial, E=Ecliptic)
   private int minLimitOrder=0;         // Min order supported (by default 0)
   private int maxLimitOrder=-1;        // Max order supported (by default depending of Healpix library => typically 29) 
   private Array [] level;              // pixel list for each HEALPix orders
   private int nOrder;                  // The number of orders currently used
   private boolean testConsistency;     // true for checking the consistency during a MOC pixel addition (=> slower)
   private boolean isConsistant;        // true if we are sure that the MOC is consistant
   private int currentOrder=-1;         // last current order for pixel addition
   
   /** HEALPix Multi Order Coverage Map (MOC) creation */
   public HealpixMoc() {
      init("C",0,-1);
   }
   
   /** Moc Creation with a specified max limitOrder (by default 29) */
   public HealpixMoc(int maxLimitOrder) throws Exception {
      init("C",0,maxLimitOrder);
   }
   
   /** Moc Creation with a specified min and max limitOrder (by default 0..29) */
   public HealpixMoc(int minLimitOrder,int maxLimitOrder) throws Exception {
      init("C",minLimitOrder,maxLimitOrder);
   }

   /** Moc Creation with a specified min and max limitOrder (by default 0..29) */
   public HealpixMoc(String coordSys, int minLimitOrder,int maxLimitOrder) throws Exception {
      init(coordSys,minLimitOrder,maxLimitOrder);
   }

    /** HEALPix Multi Order Coverage Map (MOC) creation and initialisation
    * via a string (ex: "order1/npix1-npix2 npix3 ... order2/npix4 ...")
    * @param s list of MOC pixels
    */
   public HealpixMoc(String s) throws Exception {
      this();
      add(s);
   }

   /** HEALPix Multi Order Coverage Map (MOC) creation and initialisation
    * via a stream, either in ASCII encoded format or in FITS encoded format
    * @param in input stream
    * @param mode ASCII - ASCII encoded format, FITS - Fits encoded format
    */
   public HealpixMoc(InputStream in, int mode) throws Exception {
      this();
      read(in,mode);
   }

   /** Clear the MOC */
   public void clear() {
      init(coordSys,minLimitOrder,maxLimitOrder);
   }
   
   /** Deep copy */
   public Object clone() {
      HealpixMoc moc = new HealpixMoc();
      moc.coordSys=coordSys;
      moc.maxLimitOrder=maxLimitOrder;
      moc.minLimitOrder=minLimitOrder;
      moc.nOrder=nOrder;
      moc.testConsistency=testConsistency;
      moc.currentOrder=currentOrder;
      moc.rangeSet= (rangeSet==null) ? null : new RangeSet(rangeSet);
      for( int order=0; order<nOrder; order++ ) {
         moc.level[order] = (Array)level[order].clone();
      }
      return moc;
   }
   
   /** Set the Min limit order supported by the Moc (by default 0)
    *  (and automatically switch on the testConsistency)
    * Any future addition of pixel with order smallest than minLimitOrder will be automatically replaced by the
    * addition of its 4 sons.
    */
   public void setMinLimitOrder(int limitOrder) throws Exception {
      if( limitOrder==minLimitOrder ) return;
      if( limitOrder>MAXORDER ) throw new Exception("Min limit order exceed HEALPix library possibility ("+MAXORDER+")");
      if( limitOrder<0 || maxLimitOrder!=-1 && limitOrder>maxLimitOrder ) throw new Exception("Min limit greater than max limit order");
      isConsistant = false;
      minLimitOrder=limitOrder;
      setCheckConsistencyFlag(true);
   }
   
   /** Set the limit order supported by the Moc (-1 for Healpix library implementation)
    *  (and automatically switch on the testConsistency)
    * Any future addition of pixel with order exceeding limitOrder will be automatically replaced by the
    * addition of the corresponding pixel at the limitOrder. If there is no limitOrder set (-1), an exception
    * will thrown.
    */
   public void setMaxLimitOrder(int limitOrder) throws Exception {
      if( limitOrder==maxLimitOrder ) return;
      if( limitOrder>MAXORDER ) throw new Exception("Max limit order exceed HEALPix library possibility ("+MAXORDER+")");
      if( limitOrder!=-1 && limitOrder<minLimitOrder ) throw new Exception("Max limit order smaller than min limit order");
      isConsistant = false;
      maxLimitOrder=limitOrder;
      setCheckConsistencyFlag(true);
   }

   /** Provide the minimal limit order supported by the Moc (by default 0) */
   public int getMinLimitOrder() { return minLimitOrder; }
   
   /** Provide the limit order supported by the Moc (by default depends of the Healpix library implementation) */
   public int getMaxLimitOrder() {
      if( maxLimitOrder==-1 ) return MAXORDER;
      return maxLimitOrder;
   }

   /** @deprecated see getMaxLimitOrder() */
   public int getLimitOrder() { return getMaxLimitOrder(); }
   
   /** @deprecated see setMaxLimitOrder() */
   public void setLimitOrder(int limitOrder) throws Exception { setMaxLimitOrder(limitOrder); }

   
   /** Return the coordinate system (HEALPix convention: G-galactic, C-Equatorial, E-Ecliptic) */
   public String getCoordSys() {
      return coordSys;
   }

   /** Specify the coordinate system (HEALPix convention: G-galactic, C-Equatorial, E-Ecliptic) */
   public void setCoordSys(String coordSys) {
      this.coordSys=coordSys;
   }
   
   // Use for parsing only
   protected void setCurrentOrder(int order ) { currentOrder=order; }

   /** Provide the number of Healpix pixels (for all MOC orders)
    * @return number of pixels
    */
   public int getSize() {
      int size=0;
      for( int order=0; order<nOrder; order++ ) size+= getSize(order);
      return size;
   }
   
   /** Return approximatively the memory used for this moc (in bytes) */
   public long getMem() {
      long mem=0L;
      for( int order=0; order<nOrder; order++ ) mem += getMem(order);
      return mem;
   }
   
   /** Provide the memory used for a dedicated order */
   public long getMem(int order) {
      return level[order].getMem();
   }

   /** Provide the number of Healpix pixels for a dedicated order */
   public int getSize(int order) {
      return level[order].getSize();
   }
   
   /** Provide the Array of a dedicated order */
   public Array getArray(int order) {
      return level[order];
   }
   
   /** Provide the angular resolution (in degrees) of the MOC (sqrt of the smallest pixel area) */
   public double getAngularRes() {
      return Math.sqrt( getPixelArea( getMaxOrder() ) );
   }

   /** Provide the greatest order used by the MOC
    * @return greatest MOC order, -1 if no order used
    */
   public int getMaxOrder() { return nOrder-1; }

   /**
    * Set the check consistency flag.
    * "true" by default => redundancy check and hierarchy consistency check during addition (=> slower)
    * @param flag
    */
   public void setCheckConsistencyFlag(boolean flag) throws Exception {
      testConsistency=flag;
      if( testConsistency ) checkAndFix();
    }
   
   /** return the order of the first descendant, otherwise -1*/
   public int getDescendantOrder(int order,long npix) {
      long pix=npix/4L;
      for( int o=order-1; o>=0; o--,pix/=4L ) {
         if( level[o].find(pix)>=0 ) return o;
      }
      return -1;
   }
   
   /** Add a list of MOC pixels provided in a string format (JSON format or basic ASCII format)
    * ex JSON:          { "order1":[npix1,npix2,...], "order2":[npix3...] }
    * ex basic ASCII:   order1/npix1-npix2 npix3 ... order2/npix4 ...
    * Note : The string can be submitted in several times. In this case, the insertion will use the last current order
    * Note : in JSON, the syntax is not checked ( in fact {, [ and " are ignored)
    * @see setCheckUnicity(..)
    */
   public void add(String s) throws Exception {
      StringTokenizer st = new StringTokenizer(s," ;,\n\r\t{}");
      while( st.hasMoreTokens() ) {
         String s1 = st.nextToken();
         if( s1.length()==0 ) continue;
         addHpix(s1);
      }
   }
   
   /** Add directly a full Moc */
   public void add(HealpixMoc moc) throws Exception {
      for( int order=moc.nOrder-1; order>=0; order-- ) {
         for( long npix : moc.getArray(order) ) add(order, npix);
      }
   }
   
   /** Add a Moc pixel
    * Recursive addition : since with have the 3 brothers, we remove them and add recursively their father
    * @param cell Moc cell
    * @return true if the cell (or its father) has been effectively inserted
    */
   public boolean add(MocCell cell) throws Exception {
      return add(cell.order,cell.npix); 
   }
   
   /** Add a Moc pixel (at max order) corresponding to the alpha,delta position
    * Recursive addition : since with have the 3 brothers, we remove them and add recursively their father
    * @param alpha, delta position
    * @return true if the cell (or its father) has been effectively inserted
    */
   public boolean add(HealpixImpl healpix,double alpha, double delta) throws Exception {
      int order = getMaxOrder();
      if( order==-1 ) return false;
      long npix = healpix.ang2pix(order, alpha, delta);
      return add(order,npix);
   }


   /** Add a MOC pixel
    * Recursive addition : since with have the 3 brothers, we remove them and add recursively their father
    * @param order HEALPix order
    * @param npix Healpix number
    * @param testHierarchy true if the ascendance and descendance consistance test must be done
    * @return true if something has been really inserted (npix or ascendant)
    */
   public boolean add(int order, long npix) throws Exception { return add(order,npix,true); }
   private boolean add(int order, long npix,boolean testHierarchy) throws Exception {
      rangeSet=null;
      if( order<minLimitOrder ) return add2(order,npix,minLimitOrder);

      // Fast insertion 
      if( !testConsistency ) {
         isConsistant=false;
         return add1(order,npix);
      }
      
//      if( order==minLimitOrder ) return add1(order,npix);
      if( maxLimitOrder!=-1 && order>maxLimitOrder ) return add(order-1, npix>>>2);
      
      if( testHierarchy ) {
         // An ascendant is already inside ?
         if( order>minLimitOrder && isDescendant(order, npix) ) return false;

         // remove potential descendants
         deleteDescendant(order, npix);
      }
      
      if( order>minLimitOrder && deleteBrothers(order,npix) ) {
         return add(order-1,npix>>>2,testHierarchy);
      }
      
      return add1(order,npix);
      
   }
   
   // Delete 3 others brothers if all present
   private boolean deleteBrothers(int order,long me) {
      return level[order].deleteBrothers(me);
   }
   
   /** remove a list of MOC pixels provided in a string format
    * (ex: "order1/npix1-npix2 npix3 ... order2/npix4 ...")
    */
   public void delete(String s) {
      StringTokenizer st = new StringTokenizer(s," ;,\n\r\t");
      while( st.hasMoreTokens() ) deleteHpix(st.nextToken());
   }
   
   /** Remove a MOC pixel
    * @param order HEALPix order
    * @param npix Healpix number
    * @return true if the deletion has been effectively done
    */
   public boolean delete(int order, long npix) {
      if( order>=nOrder ) return false;
      rangeSet=null;
      return level[order].delete(npix);
   }
   
   /** Remove all descendants of a MOC Pixel
    * @param order
    * @param npix
    * @return true if at least one descendant has been removed
    */
   public boolean deleteDescendant(int order,long npix) {
      rangeSet=null;
      long v1 = npix*4;
      long v2 = (npix+1)*4-1;
      boolean rep=false;
      for( int o=order+1 ; o<nOrder; o++, v1*=4, v2 = (v2+1)*4 -1) {
         rep |= getArray(o).delete(v1, v2);
      }
      return rep;
   }

   /** Sort each level of the Moc */
   public void sort() {
      for( int order=0; order<nOrder; order++ ) level[order].sort();
   }
   
   /** Return true if all Moc level is sorted */
   public boolean isSorted() {
      for( int order=0; order<nOrder; order++ ) {
         if( !level[order].isSorted() ) return false;
      }
      return true;
   }
   
   /** @deprecated, see isIntersecting(...) */
   public boolean isInTree(int order,long npix) { return isIntersecting(order,npix); }

   /** @deprecated, see isIntersecting(...) */
   public boolean isInTree(HealpixMoc moc) { return isIntersecting(moc); }
   
   
   /** Check and fix the consistency of the moc
    * => remove cell redundancies
    * => factorize 4 brothers as 1 father (recursively)
    * => check and fix descendance consistancy
    * => Trim the limitOrder if required
    */
   public void checkAndFix() throws Exception {
      if( getMaxOrder()==-1 || isConsistant ) return;
      rangeSet=null;
      sort();
      HealpixMoc res = new HealpixMoc(coordSys,minLimitOrder,maxLimitOrder);
      int p[] = new int[getMaxOrder()+1];
      for( int npix=0; npix<12; npix++) checkAndFix(res,p,0,npix);
      
      boolean flagTrim=true;
      for( int order=p.length-1; order>=0; order-- ) {
         Array a = res.getArray(order);
         level[order]=a;
         if( flagTrim && a.getSize()!=0 ) { nOrder=order+1; flagTrim=false; }
      }
      res=null;
      isConsistant=true;
//      System.out.println("checkAndFix: nOrder="+nOrder+" minLimitOrder="+minLimitOrder+" maxLimitOrder="+maxLimitOrder);
   }
   
   // Recursive MOC tree scanning
   private void checkAndFix(HealpixMoc res, int p[], int order, long pix) throws Exception {
      Array a;
      
//      for( int j=0; j<order; j++ ) System.out.print("  ");
//      System.out.print(order+"/"+pix);
      
      // D�termination de la valeur de la tete du niveaeu
      long t=-1;
      a = getArray(order);
      if( p[order]<a.getSize() ) t = a.get(p[order]);
      
//      System.out.print(" t="+t);
      
      // Ca correspond => on ajoute au resultat, et on supprime la descendance eventuelle
      if( t==pix ) {
         res.add(order,pix,false);
         for( int o=order; o<p.length; o++ ) {
            long mx=(pix+1)<<((o-order)<<1);
            a = getArray(o);
            while( p[o]<a.getSize() && a.get(p[o])<mx ) p[o]++;
         }
//         System.out.println(" => Add + remove possible descendance => return");
         return;
      }
      
      // On n'a pas trouv� la cellule, s'il y a une descendance dans l'un ou l'autre
      // des arbres, on va continuer r�cursivement sur les 4 fils.
      boolean found=false;
      for( int o=order+1; o<p.length; o++ ) {
         long mx = (pix+1)<<((o-order)<<1);
         a = getArray(o);
         if( p[o]<a.getSize() && a.get(p[o])<mx ) { found=true; break; }
      }
      if( found ) {
         if( res.maxLimitOrder!=-1 && order>res.maxLimitOrder ) {
//            System.out.println(" => rien mais descendance au dela de la limite, Add + remove possible descendance => return");
            res.add(order,pix,false);
            for( int o=order; o<p.length; o++ ) {
               long mx=(pix+1)<<((o-order)<<1);
               a = getArray(o);
               while( p[o]<a.getSize() && a.get(p[o])<mx ) p[o]++;
            }
         }

         else for( int i=0; i<4; i++ ) {
//            System.out.println(" => rien mais descendance =>  parcours des fils...");
            checkAndFix(res,p,order+1,(pix<<2)+i);
         }
      }
//    else System.out.println(" => aucun descendance => return");
   }

   
   /** true is the MOC pixel is an ascendant */
   public boolean isAscendant(int order, long npix) {
      long range=4L;
      for( int o=order+1; o<nOrder; o++,range*=4L ) {
         if( level[o].intersectRange(npix*range, (npix+1)*range -1) ) return true;
      }
      return false;
   }
   

   /** true if the MOC pixel is a descendant */
   public boolean isDescendant(int order, long npix) {
      long pix=npix/4L;
      for( int o=order-1; o>=minLimitOrder; o--,pix/=4L ) {
         if( level[o].find(pix)>=0 ) return true;
      }
      return false;
   }

   /** true if the MOC pixel is present at this order */
   public boolean isIn(int order, long npix) {
      return level[order].find(npix)>=0;
   }
   
   /** Return the fraction of the sky covered by the Moc [0..1] */
   public double getCoverage() {
      long area = getArea();
      if( area==0 ) return 0.;
      return (double)getUsedArea() / area;
   }
      
   /** Return the number of low level pixels of the Moc  */
   public long getUsedArea() {
      long n=0;
      long sizeCell = 1L;
      for( int order=nOrder-1; order>=0; order--, sizeCell*=4L ) n += getSize(order)*sizeCell;
      return n;
   }
   
   /** return the area of the Moc computed in pixels at the most low level */
   public long getArea() {
      if( nOrder==0 ) return 0;
      long nside = pow2(nOrder-1);
      return 12L*nside*nside;
   }

   /** Provide an Iterator on the MOC pixel List. Each Item is a couple of longs,
    * the first long is the order, the second long is the pixel number */
   public Iterator<MocCell> iterator() { return new HpixListIterator(); }
   
   /** Provide an Iterator on the low level pixel list covering all the MOC area.
    * => pixel are provided in ascending order */
   public Iterator<Long> pixelIterator() { sort(); return new PixelIterator(); }
   
   /** Remove all the unused space (allocation reservation) */
   public void trim() {
      for( int order=0; order<nOrder; order++ ) level[order].trim();
   }

   // Juste pour du debogage
   public String todebug() {
      StringBuffer s = new StringBuffer();
      double coverage = (int)(getCoverage()*10000)/100.;
      s.append("maxOrder="+getMaxOrder()+" ["+minLimitOrder+".."+(maxLimitOrder==-1?"max":maxLimitOrder+"")+"] mem="+getMem()/1024L+"KB size="+getSize()+" coverage="+coverage+"%"
            +(isSorted()?" sorted":"")+(isConsistant?" consistant":"")+"\n");
      long oOrder=-1;
      Iterator<MocCell> it = iterator();
      for( int i=0; it.hasNext() && i<80; i++ ) {
         MocCell x = it.next();
         if( x.order!=oOrder ) s.append(" "+x.order+"/");
         else s.append(",");
         s.append(x.npix);
         oOrder=x.order;
      }
      if( it.hasNext() ) s.append("...");
      return s.toString();
   }
   
   private static final int MAXWORD=20;
   private static final int MAXSIZE=80;
   
   public String toString() {
      StringBuffer res= new StringBuffer(getSize()*8);
      int order=-1;
      boolean flagNL = getSize()>MAXWORD;
      boolean first=true;
      int sizeLine=0;
      res.append("{");
      for( MocCell c : this ) {
         if( res.length()>0 ) {
            if( c.order!=order ) {
               if( !first ) res.append("],");
               if( flagNL ) { res.append("\n"); sizeLine=0; }
               else res.append(" ");
            } else {
               int n=(c.npix+"").length();
               if( flagNL && n+sizeLine>MAXSIZE ) { res.append(",\n "); sizeLine=3; }
               else { res.append(','); sizeLine++; }
            }
            first=false;
         }
         String s = c.order!=order ?  "\""+c.order+"\":["+c.npix : c.npix+"";
         res.append(s);
         sizeLine+=s.length();
         order=c.order;
      }
      int n = res.length();
      if( res.charAt(n-1)==',' ) res.replace(n-1, n-1, "]"+(flagNL?"\n":" ")+"}");
      else res.append("]"+(flagNL?"\n":" ")+"}");
      return res.toString();
   }
   
//   public static void main(String s[] ) {
//      try {
//         HealpixMoc m = new HealpixMoc("1/2-3 2/16-20 3/85-87");
//         System.out.println(m);
//      } catch( Exception e ) {
//         e.printStackTrace();
//      }
//   }
   
   
   /*************************** Operations on MOCs ************************************************/
  
   private RangeSet rangeSet=null;

   // Store the MOC as a RangeSet if not yet done
   public void toRangeSet() {
      if( rangeSet!=null ) return;   // d�j� fait
      sort();
      rangeSet = new RangeSet( getSize() );
      RangeSet rtmp=new RangeSet();
      for (int order=0; order<nOrder; ++order) {
         rtmp.clear();
         int shift=2*(Healpix.MAXORDER-order);
         for( long npix : getArray(order) ) rtmp.append (npix<<shift,(npix+1)<<shift);
         if( !rtmp.isEmpty() ) rangeSet=rangeSet.union(rtmp);
      }
   }
   
   // Generate the HealpixMoc tree structure from the rangeSet
   private void toHealpixMoc() throws Exception {
      clear();
      RangeSet r2 = new RangeSet(rangeSet);
      RangeSet r3 = new RangeSet();
      for( int o=0; o<=Healpix.MAXORDER; ++o) {
         if( r2.isEmpty() ) return;
         int shift = 2*(Healpix.MAXORDER-o);
         long ofs=(1L<<shift)-1;
         r3.clear();
         for( int iv=0; iv<r2.size(); ++iv ) {
            long a=(r2.ivbegin(iv)+ofs)>>>shift,
            b=r2.ivend(iv)>>>shift;
            r3.append(a<<shift, b<<shift);
            for( long c=a; c<b; ++c ) add1(o,c);
         }
         if( !r3.isEmpty() ) r2 = r2.difference(r3);
      }
   }


   /** Fast test for checking if the cell is intersecting 
    * the current MOC object
    * @return true if the intersection is not null
    */
   public boolean isIntersecting(int order,long npix) {
      return isIn(order,npix) || isAscendant(order,npix) || isDescendant(order,npix);
   }
   
   /** Fast test for checking if the parameter MOC is intersecting
    * the current MOC object
    * @return true if the intersection is not null
    */
   public boolean isIntersecting(HealpixMoc moc) {
      sort();
      moc.sort();
      int n = moc.getMaxOrder();
      for( int o=0; o<=n; o++ ) {
         Array a = moc.getArray(o);
         if( isInTree(o,a) ) return true;
      }
      return false;
   }
   
   public HealpixMoc union(HealpixMoc moc) throws Exception {
      return operation(moc,0);
   }
   public HealpixMoc intersection(HealpixMoc moc) throws Exception {
      return operation(moc,1);
   }
   public HealpixMoc subtraction(HealpixMoc moc) throws Exception {
      return operation(moc,2);
   }
   
   public HealpixMoc complement() throws Exception {
      HealpixMoc allsky = new HealpixMoc();
      allsky.add("0/0-11");
      allsky.toRangeSet();
      toRangeSet();
      HealpixMoc res = new HealpixMoc(coordSys,minLimitOrder,maxLimitOrder);
      res.rangeSet = allsky.rangeSet.difference(rangeSet);
      res.toHealpixMoc();
      return res;
   }      
   
   public HealpixMoc difference(HealpixMoc moc) throws Exception {
      HealpixMoc inter = intersection(moc);
      HealpixMoc union = union(moc);
      return union.subtraction(inter);
   }      
  
   // Generic operation
   private HealpixMoc operation(HealpixMoc moc,int op) throws Exception {
       testCompatibility(moc);
       toRangeSet();
       moc.toRangeSet();
       HealpixMoc res = new HealpixMoc(coordSys,minLimitOrder,maxLimitOrder);
       switch(op) {
          case 0 : res.rangeSet = rangeSet.union(moc.rangeSet); break;
          case 1 : res.rangeSet = rangeSet.intersection(moc.rangeSet); break;
          case 2 : res.rangeSet = rangeSet.difference(moc.rangeSet); break;
       }
       res.toHealpixMoc();
       return res;
   }


   /** Equality test */
   public boolean equals(Object moc){
      if( this==moc ) return true;
      try {
         HealpixMoc m = (HealpixMoc) moc;
         testCompatibility(m);
         if( m.nOrder!=nOrder ) return false;
         for( int o=0; o<nOrder; o++ ) if( getSize(o)!=m.getSize(o) ) return false;
         for( int o=0; o<nOrder; o++ ) {
           if( !getArray(o).equals( m.getArray(o) ) ) return false;
         }
      } catch( Exception e ) {
         return false;
      }
      return true;
   }
   
   
   /*************************** Coordinate query methods ************************************************/
   
   /** Check if the spherical coord is inside the MOC. The coordinate system must be compatible
    * with the MOC coordinate system.
    * @param alpha in degrees
    * @param delta in degrees
    * @return true if the coordinates is in one MOC pixel
    * @throws Exception
    */
   public boolean contains(HealpixImpl healpix,double alpha, double delta) throws Exception {
      int order = getMaxOrder();
      if( order==-1 ) return false;
      long npix = healpix.ang2pix(order, alpha, delta);
      if( level[order].find( npix )>=0 ) return true;
      if( isDescendant(order,npix) ) return true;
      return false;
   }
   
   /**
    * Provide Moc pixels totally or partially inside a circle
    * @param alpha circle center (in degrees)
    * @param delta circle center (in degrees)
    * @param radius circle radius (in degrees)
    * @return an HealpixMox containing the list of pixels
    * @throws Exception
    */
   public HealpixMoc queryDisc(HealpixImpl healpix,double alpha, double delta,double radius) throws Exception {
      int order = getMaxOrder();
      long [] list = healpix.queryDisc(order, alpha, delta, radius);
      HealpixMoc mocA = new HealpixMoc();
      mocA.setCoordSys(getCoordSys());
      for( int i=0; i<list.length; i++ ) mocA.add(order,list[i]);
//      return mocA;
      return intersection(mocA);
   }
   
   public HealpixMoc queryCell(int order,long npix) throws Exception {
       return intersection(new HealpixMoc(order+"/"+npix));

//      HealpixMoc m = new HealpixMoc();
//      m.setCheckConsistencyFlag(false);
//      sort();
//      long range=1L;
//      for( int o=order; o<nOrder; o++,npix*=4L,range*=4L ) {
//         long npixFin =npix+range-1;
//         Array a = getArray(o);
//         if( a.getSize()==0 ) continue;
//         int deb = a.find(npix);
//         int fin = a.find(npixFin);
//         
//      }
   }
   
   
   /**************************** Low level methods for fast manipulations **************************/
   
   /** Set the pixel list at the specified order (order>13 ) 
    * (Dedicated for fast initialisation)  */
   public void setPixLevel(int order,long [] val) throws Exception {
      if( getType(order)!=LONG ) throw new Exception("The order "+order+" requires long[] array");
      level[order]=new LongArray(val);
      if( nOrder<order+1 ) nOrder=order+1;
   }
      
   /** Set the pixel list at the specified order (6<=order<=13 )
   * (Dedicated for fast initialisation)  */
   public void setPixLevel(int order,int [] val) throws Exception {
      if( getType(order)!=INT ) throw new Exception("The order "+order+" requires int[] array");
      level[order]=new IntArray(val);
      if( nOrder<order+1 ) nOrder=order+1;
   }
      
   /** Set the pixel list at the specified order (order<6)
   * (Dedicated for fast initialisation)  */
   public void setPixLevel(int order,short [] val) throws Exception {
      if( getType(order)!=SHORT ) throw new Exception("The order "+order+" requires short[] array");
      level[order]=new ShortArray(val);
      if( nOrder<order+1 ) nOrder=order+1;
   }
   
   /** Provide a copy of the pixel list at the specified order (in longs) */
   public long [] getPixLevel(int order) {
      int size = getSize(order);
      long [] lev = new long[size];
      if( size==0 ) return lev;
      Array a = level[order];
      for( int i=0; i<size; i++ ) lev[i] = a.get(i);
      return lev;
   }
   
   /*************************** read and write *******************************************************/
   
   /** Read HEALPix MOC from a file. Support both FITS and ASCII format
    * @param filename file name
    * @throws Exception
    */
   public void read(String filename) throws Exception {
      (new MocIO(this)).read(filename);
   }

   /** Read HEALPix MOC from a file.
    * @param filename file name
    * @param mode ASCII encoded format or FITS encoded format
    * @throws Exception
    */
   public void read(String filename,int mode) throws Exception {
      (new MocIO(this)).read(filename,mode);
   }

   /** Read HEALPix MOC from a stream.
    * @param in input stream
    * @param mode ASCII encoded format or FITS encoded format
    * @throws Exception
    */
   public void read(InputStream in,int mode) throws Exception {
      (new MocIO(this)).read(in,mode);
   }
   
   public void readASCII(InputStream in) throws Exception {
      (new MocIO(this)).read(in,ASCII);
   }

   /** Read HEALPix MOC from an Binary FITS stream */
   public void readFits(InputStream in) throws Exception {
      (new MocIO(this)).read(in,FITS);
   }
   
   /** Write HEALPix MOC to a file
    * @param filename name of file
    * @param mode encoded format (ASCII or FITS)
    */
   public void write(String filename,int mode) throws Exception {
//      check();
      (new MocIO(this)).write(filename,mode);
   }

   /** Write HEALPix MOC to an output stream
    * @param out output stream
    * @param mode encoded format (ASCII or FITS)
    */
   public void write(OutputStream out,int mode) throws Exception {
      check();
      (new MocIO(this)).write(out,mode);
   }
   
   /** Write HEALPix MOC to an output stream IN ASCII encoded format
    * @param out output stream
    */
   public void writeASCII(OutputStream out) throws Exception {
      check();
      (new MocIO(this)).writeASCII(out);
   }

   /** Write HEALPix MOC to an output stream in FITS encoded format
    * @param out output stream
    */
   public void writeFits(OutputStream out) throws Exception { writeFits(out,false); }
   public void writeFits(OutputStream out,boolean compressed) throws Exception { writeFITS(out,compressed); }
   public void writeFITS(OutputStream out) throws Exception { writeFits(out,false); }
   public void writeFITS(OutputStream out,boolean compressed) throws Exception {
      check();
      (new MocIO(this)).writeFits(out,compressed);
   }
   
   /***************************************  Les classes priv�es **************************************/

   // Cr�ation d'un it�rator sur la liste des pixels
   private class HpixListIterator implements Iterator<MocCell> {
      private int currentOrder=0;
      private int indice=-1;
      private boolean ready=false;

      public boolean hasNext() {
         goNext();
         return currentOrder<nOrder;
       }

      public MocCell next() {
         if( !hasNext() ) return null;
         ready=false;
         Array a = level[currentOrder];
         return new MocCell(currentOrder, a.get(indice));
      }

      public void remove() {  }

      private void goNext() {
         if( ready ) return;
         for( indice++; currentOrder<nOrder && indice>=getSize(currentOrder); currentOrder++, indice=0);
         ready=true;
      }
   }
   
   // Cr�ation d'un it�rator sur la liste des pixels tri�s et ramen�s au max order
   // M�thode : parcours en parall�le tous les niveaux, en conservant pour chacun d'eux l'indice de sa t�te
   //           prends la plus petite "t�te", et pour celle-l�, it�re dans l'intervalle (fonction de la diff�rence par rapport
   //           � l'ordre max => 4 pour order-1, 16 pour order-2 ...).
   private class PixelIterator implements Iterator<Long> {
      private boolean ready=false;      // Le goNext() a �t� effectu� et le pixel courant pas encore lu
      private long current;             // Pixel courant
      private int order=-1;             // L'ordre courant
      private long indice=0L;           // l'indice dans l'intervalle de l'ordre courant
      private long range=0L;            // Nombre d'�l�ments dans l'intervalle courant
      private long currentTete;         // Valeur de la tete courante
      private long tete=-1L;            // Derni�re valeur de la tete
      private boolean hasNext=true;     // false si on a atteind la fin du de tous les ordres
      private int p[] = new int[nOrder];// indice courant pour chaque ordre
      
      public boolean hasNext() {
         goNext();
         return hasNext;
       }

      public Long next() {
         if( !hasNext() ) return null;
         ready=false;
         return current;
      }

      public void remove() {  }
      
      private void goNext() {
         if( ready ) return;
         
         // recherche de la plus petite tete parmi tous les orders
         if( indice==range ) {
            long min = Long.MAX_VALUE;
            long fct=1L;
            long tete=-1L;
            order=-1;
            for( int o=nOrder-1; o>=minLimitOrder; o--, fct*=4 ) {
               Array a = level[o];
               tete = p[o]<a.getSize() ? a.get(p[o])*fct : -1;
               if( tete!=-1 && tete<min ) { min=tete; order=o; range=fct; }
            }
            if( order==-1 ) { hasNext=false; ready=true; return; }
            currentTete=min;
            indice=0L;
         }
         
         // On �num�re tous les pixels du range
         current = new Long(currentTete + indice);
         indice++;
         
         // Si on a termin� le range courant, on avance l'indice de sa tete
         if( indice==range ) p[order]++;
         ready=true;
      }
   }
   

   // Internal initialisations => array of levels allocation
   private void init(String coordSys,int minLimitorder,int maxLimitOrder) {
      this.coordSys=coordSys;
      this.minLimitOrder=minLimitorder;
      this.maxLimitOrder=maxLimitOrder;
      testConsistency=true;
      isConsistant=true;
      level = new Array[MAXORDER+1];
      for( int order=0; order<MAXORDER+1; order++ ) {
         int type = getType(order);
         int bloc = (1+order)*10;
         Array a = type==SHORT ? new ShortArray(bloc)
                   : type==INT ? new IntArray(bloc) : new LongArray(bloc);
         level[order]=a;
      }
   }
   
   // Low level npixel addition.
   private boolean add1(int order, long npix) throws Exception {
      if( order<minLimitOrder ) return add2(order,npix,minLimitOrder);

      if( order>MAXORDER ) throw new Exception("Out of MOC order");
      if( order>=nOrder ) nOrder=order+1;
      
      return level[order].add(npix,testConsistency);
   }
   
   // Low level npixel multi-addition
   private boolean add2(int orderSrc, long npix, int orderTrg) throws Exception {
      if( orderTrg>MAXORDER ) throw new Exception("Out of MOC order");
      if( orderTrg>=nOrder ) nOrder=orderTrg+1;
      long fct = pow2(orderTrg-orderSrc);
      fct *= fct;
      npix *= fct;
      boolean rep=false;
      
      for( int i=0; i<fct; i++ ) rep |=level[orderTrg].add(npix+i,testConsistency);
      return rep;
   }
   
   // Ajout d'un pixel selon le format "order/npix[-npixn]".
   // Si l'order n'est pas mentionn�, utilise le dernier order utilis�
   protected void addHpix(String s) throws Exception {
      int i=s.indexOf('/');
      if( i<0 ) i=s.indexOf(':');
      if( i>0 ) currentOrder = Integer.parseInt( unQuote( s.substring(0,i) ) );
      int j=s.indexOf('-',i+1);
      if( j<0 ) {
         String s1 = unBracket( s.substring(i+1) );
         if( s1.length()>0 ) add(currentOrder, Long.parseLong( s1 ) );
      } else {
         long startIndex = Long.parseLong(s.substring(i+1,j));
         long endIndex = Long.parseLong(s.substring(j+1));
         for( long k=startIndex; k<=endIndex; k++ ) add(currentOrder, k);
      }
   }
   
   private String unQuote(String s) {
      int n=s.length();
      if( n>2 && s.charAt(0)=='"' && s.charAt(n-1)=='"' ) return s.substring(1,n-1);
      return s;
   }
   
   private String unBracket(String s) {
      int n=s.length();
      if( n<1 ) return s;
      int o1 = s.charAt(0)=='[' ? 1:0;
      int o2 = s.charAt(n-1)==']' ? n-1 : n;
      return s.substring(o1,o2);
   }

   // Suppression d'un pixel selon le format "order/npix[-npixn]".
   // Si l'order n'est pas mentionn�, utilise le dernier order utilis�
   private void deleteHpix(String s) {
      int i=s.indexOf('/');
      if( i>0 ) currentOrder = Integer.parseInt(s.substring(0,i));
      int j=s.indexOf('-',i+1);
      if( j<0 ) delete(currentOrder, Integer.parseInt(s.substring(i+1)));
      else {
         int startIndex = Integer.parseInt(s.substring(i+1,j));
         int endIndex = Integer.parseInt(s.substring(j+1));
         for( int k=startIndex; k<=endIndex; k++ ) delete(currentOrder, k);
      }
   }
   
   // Sort and check before writting
   private void check() throws Exception {
      if( !testConsistency ) setCheckConsistencyFlag(true);
      else sort();
   }

   // Throw an exception if the coordsys of the parameter moc differs of the coordsys
   private void testCompatibility(HealpixMoc moc) throws Exception {
      if( getCoordSys().charAt(0)!=moc.getCoordSys().charAt(0) ) throw new Exception("Incompatible MOC coordsys");
   }
   
   // Determine the best "find" strategie
   // @return: 0-no object at all 
   //          1-dichotomy - first arg as external loop
   //          2 dichotomy - second arg as external loop
   //          3-parallel scanning
   private int strategie(int size1, int size2) {
      if( size1==0 || size2==0 ) return 0;
      double m1 = size1 * (1+Math.log(size2)/Math.log(2));
      double m2 = size2 * (1+Math.log(size1)/Math.log(2));
      double m3 = size1+size2;
      if( m1<m2 && m1<m3 ) return 1;
      if( m2<m1 && m2<m3 ) return 2;
      return 3;
   }
   
   // true if the Array intersects the level[order]
   // (determine automatically the best "find" strategy)
   private boolean isIn(int order, Array a) {
      Array a1 = level[order];
      Array a2 = a;
      int size2 = a2.getSize();
      int size1 = a1.getSize();
      if( !a1.intersectRange( a2.get(0), a2.get(size2-1) ) ) return false;
      switch( strategie(size1,size2) ) {
         case 0: return false;
         case 1: 
            for( long x : a1 ) if( a2.find(x)>=0 ) return true;
            return false;
         case 2: 
            for( long x : a2 ) if( a1.find(x)>=0 ) return true;
            return false;
         default :
            boolean incr1=true;
            long x1=a1.get(0),x2=a2.get(0);
            for( int i1=0,i2=0; i1<size1 && i2<size2; ) {
               if( incr1 ) x1 = a1.get(i1);
               else x2 = a2.get(i2);
               if( x1==x2 ) return true;
               incr1 = x1<x2;
               if( incr1 ) i1++;
               else i2++;
            }
      }
      return false;
   }

   // true if the Array intersect the descendance
   // (determine automatically the best "find" strategy)
   private boolean isAscendant(int order, Array a) {
      long range=4L;
      for( int o=order+1; o<nOrder; o++,range*=4L ) {
         Array a1=level[o];
         Array a2=a;
         int size2 = a2.getSize();
         int size1 = a1.getSize();
         if( !a1.intersectRange( a2.get(0)*range, a2.get(size2-1)*range ) ) continue;
         switch( strategie(size1,size2) ) {
            case 0: break;
            case 1:
               long onpix=-1L;
               for( long x : a1 ) {
                  long npix = x/range;
                  if( npix==onpix ) continue;
                  if( a2.find(npix)>=0 ) return true;
                  onpix=npix;
               }
               break;
            case 2:
               for( long pix : a2 ) {
                  if( a1.intersectRange(pix*range, (pix+1)*range -1) ) return true;
               }
               break;
            default:
               boolean incr1=true;
               long x1=a1.get(0),x2=a2.get(0)*range,x3=(a2.get(0)+1)*range-1;
               for( int i1=0,i2=0; i1<size1 && i2<size2; ) {
                  if( incr1 ) x1 = a1.get(i1);
                  else { x2 = a2.get(i2)*range; x3 = (a2.get(i2)+1)*range-1; }
                  if( x2<=x1 && x1<=x3 ) return true;
                  incr1 = x1<x3;
                  if( incr1 ) i1++;
                  else i2++;
               }
               break;
         }

      }
      return false;
   }

   // true if the Array intersect the ascendance
   // (determine automatically the best "find" strategy)
   private boolean isDescendant(int order, Array a) {
      long range=4L;
      for( int o=order-1; o>=0; o--,range*=4L ) {
         Array a1=level[o];
         Array a2=a;
         int size2 = a2.getSize();
         int size1 = a1.getSize();
         if( !a1.intersectRange( a2.get(0)/range, a2.get(size2-1)/range ) ) continue;
         switch( strategie(size1,size2) ) {
            case 0: break;
            case 1: 
               for( long x : a1 ) {
                  if( a2.intersectRange(x*range, (x+1)*range -1) ) return true;
               }
               break;
            case 2: 
               long onpix=-1L;
               for( long x : a2 ) {
                  long npix = x/range;
                  if( npix==onpix ) continue;
                  if( a1.find(npix)>=0 ) return true;
                  onpix=npix;
               }
               break;
            default :
               boolean incr1=true;
               long x1=a1.get(0),x2=a2.get(0)/range;
               for( int i1=0,i2=0; i1<size1 && i2<size2; ) {
                  if( incr1 ) x1 = a1.get(i1);
                  else x2 = a2.get(i2)/range;
                  if( x1==x2 ) return true;
                  incr1 = x1<x2;
                  if( incr1 ) i1++;
                  else i2++;
               }
               break;
         }

      }
      return false;
   }

   // true if the array intersects the Moc
   private boolean isInTree(int order,Array a) {
      if( a==null || a.getSize()==0) return false;
      if( a.getSize()==1 ) return isInTree( order,a.get(0) );
      return isIn(order,a) || isAscendant(order,a) || isDescendant(order,a);
   }
   
   
   /***************************************  Utilities **************************************************************/
   
   /** Code a couple (order,npix) into a unique long integer
    * @param order HEALPix order
    * @param npix HEALPix number
    * @return Uniq long ordering
    */
   static public long hpix2uniq(int order, long npix) {
      long nside = pow2(order);
      return 4*nside*nside + npix;
   }
   
   /** Uncode a long integer into a couple (order,npix)
    * @param uniq Uniq long ordering
    * @return HEALPix order,number
    */
   static public long [] uniq2hpix(long uniq) {
      return uniq2hpix(uniq,null);
   }
   
   /** Uncode a long integer into a couple (order,npix)
    * @param uniq Uniq long ordering
    * @param hpix null for reallocating target couple
    * @return HEALPix order,number
    */
   static public long [] uniq2hpix(long uniq,long [] hpix) {
      if( hpix==null ) hpix = new long[2];
      hpix[0] = log2(uniq/4)/2;
      long nside = pow2(hpix[0]);
      hpix[1] = uniq - 4*nside*nside;
      return hpix;
   }
   
   /** Pixel area (in square degrees) for a given order */
   static private double getPixelArea(int order) {
      if( order<0 ) return SKYAREA;
      long nside = pow2(order);
      long npixels = 12*nside*nside;
      return SKYAREA/npixels;
   }
   
   static public final long pow2(long order){ return 1<<order;}
   static public final long log2(long nside){ int i=0; while((nside>>>(++i))>0); return --i; }
   
   static private final double SKYAREA = 4.*Math.PI*Math.toDegrees(1.0)*Math.toDegrees(1.0);

}
