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
