/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.astro.resolver;

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author malapert
 */
public class ReverseNameResolverTest {
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getJsonResponse method, of class ReverseNameResolver.
     */
    @Test
    public void testGetJsonResponse() throws NameResolverException {
        System.out.println("getJsonResponse");
        ReverseNameResolver instance = new ReverseNameResolver("00:42:44.32 +41:16:07.5", 13);
        String expResult = "M  31 ";
        Map result = instance.getJsonResponse();
        List<Map> features = (List<Map>) result.get("features");
        Map feature = features.get(0);
        Map properties = (Map) feature.get("properties");
        assertEquals(expResult, properties.get("identifier"));
    }
}
