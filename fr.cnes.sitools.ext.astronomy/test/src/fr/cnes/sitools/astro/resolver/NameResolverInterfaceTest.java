/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.astro.resolver;

import fr.cnes.sitools.astro.resolver.AbstractNameResolver.CoordinateSystem;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONException;
import org.json.JSONObject;
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
public class NameResolverInterfaceTest {
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getCoordinates method, of class AbstractNameResolver.
     */
    @Test
    public void testGetCoordinates() throws Exception {
        System.out.println("getCoordinates");
        CoordinateSystem coordinateSystem = CoordinateSystem.EQUATORIAL;
        NameResolverInterfaceImpl instance = new NameResolverInterfaceImpl();
        List<AstroCoordinate> expResult = Arrays.asList(new AstroCoordinate(10.6847083, 41.26875));
        List<AstroCoordinate> result = instance.getCoordinates(coordinateSystem);
        assertEquals(expResult.get(0).getRaAsDecimal(), result.get(0).getRaAsDecimal(),0.0);
        assertEquals(expResult.get(0).getDecAsDecimal(), result.get(0).getDecAsDecimal(),0.0);
    }

    /**
     * Test of getCompleteResponse method, of class AbstractNameResolver.
     */
    @Test
    public void testGetCompleteResponse() throws JSONException {
        System.out.println("getCompleteResponse");
        NameResolverInterfaceImpl instance = new NameResolverInterfaceImpl();        
        JSONObject result = (JSONObject) instance.getCompleteResponse();
        assertEquals("241.991821375", result.getString("ra"));
        assertEquals("-21.037578777778", result.getString("dec"));
    }

    /**
     * Test of getCreditsName method, of class AbstractNameResolver.
     */
    @Test
    public void testGetCreditsName() {
        System.out.println("getCreditsName");
        NameResolverInterfaceImpl instance = new NameResolverInterfaceImpl();
        String expResult = "IMCCE";
        String result = instance.getCreditsName();
        assertEquals(expResult, result);
    }

    public class NameResolverInterfaceImpl extends AbstractNameResolver {

        @Override
        public List<AstroCoordinate> getCoordinates(CoordinateSystem coordinateSystem) throws NameResolverException {
            AbstractNameResolver resolver = new CDSNameResolver("m31", CDSNameResolver.NameResolverService.all);
            return resolver.getCoordinates(coordinateSystem);
        }

        @Override
        public Object getCompleteResponse() {
            AbstractNameResolver resolver = null;
            try {
                resolver = new IMCCESsoResolver("mars", "2006-12-12");
            } catch (NameResolverException ex) {
                Logger.getLogger(NameResolverInterfaceTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            return resolver.getCompleteResponse();
        }

        @Override
        public String getCreditsName() {
            return "IMCCE";
        }
    }
}
