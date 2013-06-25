/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package run;

/**
 * Special Class to be able to run SITools2 from an extension project sitools.properties must be modified : replace ../fr.cnes.sitools.core
 * by the absolute path
 *
 * @author Jean-Christophe Malapert
 */
import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import fr.cnes.sitools.SearchGeometryEngine.CoordSystem;
import fr.cnes.sitools.SearchGeometryEngine.GeometryIndex;
import fr.cnes.sitools.SearchGeometryEngine.Index;
import fr.cnes.sitools.SearchGeometryEngine.Point;
import fr.cnes.sitools.SearchGeometryEngine.Polygon;
import fr.cnes.sitools.SearchGeometryEngine.RingIndex;
import fr.cnes.sitools.SearchGeometryEngine.Shape;
import fr.cnes.sitools.astro.cutoff.FitsHeader;
import fr.cnes.sitools.astro.graph.CircleDecorator;
import fr.cnes.sitools.astro.graph.CoordinateDecorator;
import fr.cnes.sitools.astro.graph.GenericProjection;
import fr.cnes.sitools.astro.graph.Graph;
import fr.cnes.sitools.astro.graph.HealpixDensityMapDecorator;
import fr.cnes.sitools.astro.graph.HealpixFootprint;
import fr.cnes.sitools.astro.graph.HealpixGridDecorator;
import fr.cnes.sitools.astro.graph.HealpixGridDecorator.CoordinateTransformation;
import fr.cnes.sitools.astro.graph.HealpixMocDecorator;
import fr.cnes.sitools.astro.graph.ImageBackGroundDecorator;
import fr.cnes.sitools.astro.graph.Utility;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.astro.resolver.CDSFactory;
import fr.cnes.sitools.astro.resolver.CDSNameResolver;
import fr.cnes.sitools.astro.resolver.NameResolverException;
import fr.cnes.sitools.astro.resolver.AbstractNameResolver;
import fr.cnes.sitools.astro.resolver.ReverseNameResolver;
import fr.cnes.sitools.common.SitoolsSettings;
import healpix.core.Healpix;
import healpix.essentials.HealpixBase;
import healpix.essentials.HealpixMapDouble;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import javax.swing.JFrame;
import jsky.coords.DMS;
import jsky.coords.HMS;
import jsky.coords.WCSKeywordProvider;
import jsky.coords.WCSTransform;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.ClientResource;

public class Invoker {

  public final static String SITOOLS_MAIN_CLASS = "fr.cnes.sitools.server.Starter";

  public static void main(String[] args) throws NameResolverException, JSONException, IOException, FitsException, Exception {

    //02:57:3.9130
//    System.out.println(hms.getVal());
 //   System.exit(0);
//        ClientResource client = new ClientResource("http://localhost:8182/sitools/solr/fuse/select/?q=*:*&rows=0&facet=true&facet.field=order13&facet.limit=-1&facet.mincount=1&wt=json&indent=true");
//        String text = client.get().getText();
//        JSONObject json = new JSONObject(text);
//        json = json.getJSONObject("facet_counts");
//        json = json.getJSONObject("facet_fields");
//        JSONArray array = json.getJSONArray("order13");
//        
//        HealpixMoc moc = new HealpixMoc();
//        for(int i=0;i<array.length();i++) {
//            MocCell mocCell = new MocCell();
//            System.out.println("order13="+array.getLong(i));
//            //mocCell.set(13, array.getLong(i));
//            moc.add(mocCell);
//        }
//        System.out.println(moc.toString());

//        JFrame f = new JFrame("");
///        f.addWindowListener(new WindowAdapter() {
//
//            @Override
//            public void windowClosing(WindowEvent e) {
//                System.exit(0);
//            }
//        });

    //URL url = new URL("http://alasky.u-strasbg.fr/footprints/tables/vizier/II_294_sdss7/MOC");
//        URL url = new URL("http://alasky.u-strasbg.fr/footprints/tables/vizier/I_259_tyc2/MOC");
//        BufferedInputStream bis = new BufferedInputStream(url.openStream(), 32 * 1024);
//        HealpixMoc mocA = new HealpixMoc(bis, HealpixMoc.FITS);      

    //ClientResource client = new ClientResource("http://localhost:8182/ofuse/moc");
    //String mocJson = client.get().getText();
    //HealpixMoc mocA = new HealpixMoc();
    //mocA.add(mocJson);
//        int order = 3;
//    Graph graph = new GenericProjection(Graph.ProjectionType.AITOFF); 
//    graph = new HealpixGridDecorator(graph, Scheme.RING, 4);
//    ((HealpixGridDecorator)graph).setColor(Color.RED);
    //graph = new CoordinateDecorator(graph);
    //graph = new CircleDecorator(graph, 0, 0, 10, Scheme.RING, 4);
    //((CircleDecorator)graph).setColor(Color.BLUE);
    // graph = new CircleDecorator(graph, 0, 80, 20, Scheme.RING, 4);
    //((CircleDecorator)graph).setColor(Color.gray);     
//    Utility.createJFrame(graph, 200);
    //graph = new ImageBackGroundDecorator(graph, new File("/home/malapert/Documents/Equirectangular-projection.jpg"));

    //graph = new HealpixDensityMapDecorator(graph, Scheme.RING, 4, 0.5f);    
    //map.fill(0.0);
   // Polygon polygon = new Polygon(new Point(-118.95597, -36.1787, CoordSystem.GEOCENTRIC), new Point(106.04403, 73.68463, CoordSystem.GEOCENTRIC));
    //Polygon polygon = new Polygon(new Point(80, 30, CoordSystem.GEOCENTRIC), new Point(100, 70, CoordSystem.GEOCENTRIC));
    //Point p1 = new Point(350,-10,CoordSystem.EQUATORIAL);
    //Point p2 = new Point(350,10,CoordSystem.EQUATORIAL);
    //Point p3 = new Point(50,10,CoordSystem.EQUATORIAL);    
    //Point p4 = new Point(50,-10,CoordSystem.EQUATORIAL);    
//    Polygon polygon = new Polygon(Arrays.asList(p1,p2,p3,p4));
    //Polygon polygon = new Polygon(Arrays.asList(p1,p4,p3,p2));
    
//    Index index = GeometryIndex.createIndex(polygon, Scheme.RING);
//    ((RingIndex)index).setOrder(6);
//    RangeSet range = (RangeSet) index.getIndex();    
//    System.out.println(((RingIndex) index).getOrder());
//    RangeSet.ValueIterator iter = range.valueIterator();
//    HealpixMapDouble map = new HealpixMapDouble((long) Math.pow(2, 6), Scheme.RING);
//    while (iter.hasNext()) {
//      map.setPixel(iter.next(), 1.0d);
//    }
//
//    graph = new HealpixFootprint(graph, Scheme.RING, 6, 1.0f);
//    ((HealpixFootprint) graph).importHealpixMap(map, CoordinateTransformation.NATIVE);

    //((HealpixFootprint)graph).setBackground(Color.WHITE);
//    ((HealpixFootprint) graph).setColor(Color.RED);
//    graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
//    ((CircleDecorator) graph).setColor(Color.yellow);
//    graph = new HealpixGridDecorator(graph, Scheme.RING, 1);
//    ((HealpixGridDecorator) graph).setColor(Color.yellow);
//    ((HealpixGridDecorator) graph).setDebug(true);           
//    graph = new CoordinateDecorator(graph, Color.BLUE, 0.5f);

    //       graph = new HealpixMocDecorator(graph, Color.BLACK, 0.5f);
    //       ((HealpixMocDecorator)graph).importMoc(mocA);
    //       ((HealpixMocDecorator)graph).setCoordinateTransformation(CoordinateTransformation.EQ2GAL);
    //((HealpixGridDecorator)graph).setCoordinateTransformation(CoordinateTransformation.EQ2GAL);
    //graph = new CircleDecorator(graph, 0, 0, 10, Scheme.RING, 4);
    //graph = new CircleDecorator(graph, 90, 0, 10, Scheme.RING, 4);     
//        JSONObject json = new JSONObject(mocJson);
//        Iterator iter = json.keys();
//        while(iter.hasNext()) {
//            int order = Integer.valueOf(String.valueOf(iter.next()));           
//            JSONArray array = json.getJSONArray(String.valueOf(order));            
//            HealpixMapDouble mapDouble = new HealpixMapDouble();
//            long nside = (long)Math.pow(2.0, order);
//            mapDouble.setNside(nside);
//            for(int i=0;i<array.length();i++) {
//                mapDouble.setPixel(array.getLong(i), 1);    
//            }
//            graph = new HealpixFootprint(graph, Scheme.NESTED, order, (float)0.8);
//            ((HealpixDensityMapDecorator)graph).importHealpixMap(mapDouble, CoordinateTransformation.EQ2GAL);                       
//        }
//    Utility.createJFrame(graph, 900, 500);
//
//        Shape point = new Point(-86.921 ,74.464 , CoordSystem.GEOCENTRIC);
//        Index index = GeometryIndex.createIndex(point, Scheme.RING);
//        ((RingIndex)index).setOrder(2);
//        Long pix = (Long) index.getIndex();
//        System.out.println(pix);




//        Fits fits = new Fits(new File("/home/malapert/Downloads/DC300_green_4SLD_80HPmap.fits"));
//        WCSKeywordProvider fitsProvider = new FitsHeader(fits.getHDU(1).getHeader());
//        WCSTransform wcs = new WCSTransform(fitsProvider);
//        Point2D.Double p = wcs.pix2wcs(1.0, 1.0);
//        System.out.println(p);
    //USNO_B1Query usno = new USNO_B1Query(0, 0, 0.03);        
    //System.out.println(usno.getResponse().getText());
    //AbstractNameResolver nameResolverInterface = new CDSNameResolver("m31", CDSNameResolver.NameResolverService.all);
    //AstroCoordinate astro = nameResolverInterface.getCoordinates(AbstractNameResolver.CoordinateSystem.EQUATORIAL);
    //System.out.println("ra= "+astro.getRaAsDecimal()+" dec="+astro.getDecAsDecimal());

    //ReverseNameResolver reverse = new ReverseNameResolver("23:55:20.04 -0:52:23.68",10);
    //System.out.println(reverse.getJsonResponse().toString());

//      Shape shape = new Polygon(Arrays.asList(
//              new Point(-89.398,74.493, CoordSystem.GEOCENTRIC),
//              new Point(-80.491,75.859, CoordSystem.GEOCENTRIC),
//              new Point(-87.014,75.829, CoordSystem.GEOCENTRIC),
//              new Point(-86.921,74.464, CoordSystem.GEOCENTRIC)
//              ));
//      Index index = GeometryIndex.createIndex(shape, Scheme.RING);
//      ((RingIndex)index).setOrder(1);
//      RangeSet healpix = (RangeSet) index.getIndex();
//      RangeSet.ValueIterator val= healpix.valueIterator();
//      while(val.hasNext()) {
//        System.out.println(val.next());
//      }


        String[] args1 = new String[1];
        
        args1[0] = SITOOLS_MAIN_CLASS; 
        args = args1;
        if (args.length != 1) {
            System.err.println("Sitools Main class is not set");
            System.exit(1);
        }

        Class[] argTypes = new Class[1];
        argTypes[0] = String[].class;
        try {
            Method mainMethod = Class.forName(args[0]).getDeclaredMethod("main", argTypes);
            Object[] argListForInvokedMain = new Object[1];
            argListForInvokedMain[0] = new String[0];
            mainMethod.invoke(null,argListForInvokedMain);            
        } catch (ClassNotFoundException ex) {
            System.err.println("Class " + args[0] + "not found in classpath.");
        } catch (NoSuchMethodException ex) {
            System.err.println("Class " + args[0] + "does not define public static void main(String[])");
        } catch (InvocationTargetException ex) {
            System.err.println("Exception while executing " + args[0] + ":" + ex.getTargetException());
        } catch (IllegalAccessException ex) {
            System.err.println("main(String[]) in class " + args[0] + " is not public");
        }
  }
}
