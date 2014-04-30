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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nom.tam.fits.FitsException;

import fr.cnes.sitools.astro.resolver.NameResolverException;

public class Invoker {

  public final static String SITOOLS_MAIN_CLASS = "fr.cnes.sitools.server.Starter";

  public static void main(String[] args) throws NameResolverException, IOException, FitsException, Exception {

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
