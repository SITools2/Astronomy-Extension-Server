/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.astro.resolver;

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem;
import java.util.Arrays;
import java.util.List;
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
public class CorotIdResolverTest {

    /**
     * Test of getCoordinates method, of class CorotIdResolver.
     */
    @Test
    public void testGetCoordinates() throws Exception {
        System.out.println("getCoordinates");
        CoordinateSystem coordinateSystem = CoordinateSystem.EQUATORIAL;
        CorotIdResolver instance = new CorotIdResolver("105290723");             
        NameResolverResponse response = instance.getResponse();
        List<AstroCoordinate> result = response.getAstroCoordinates();
        assertEquals(279.88184, result.get(0).getRaAsDecimal(),0.001);
        assertEquals(6.4019198, result.get(0).getDecAsDecimal(),0.001);        
    }


}
