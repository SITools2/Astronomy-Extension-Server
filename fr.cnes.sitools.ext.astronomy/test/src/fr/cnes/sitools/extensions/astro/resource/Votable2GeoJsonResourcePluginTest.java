/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.test.common.AbstractSitoolsServiceTestCase;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 *
 * @author malapert
 */
public class Votable2GeoJsonResourcePluginTest extends AbstractSitoolsServiceTestCase {
    
    private String serviceUrl = "http://vizier.u-strasbg.fr/viz-bin/votable/-A?-source=I/284&RA=0&DEC=0&SR=0.02";
    private String urlToTest = "/ExampleProject/fixed/plugin/votable2geojson";
    
    public Votable2GeoJsonResourcePluginTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testVotable2Json() throws IOException {
        ClientResource client = new ClientResource(serviceUrl);
        Representation rep = client.get();
        String contain = rep.getText();

        client.release();
        Form form = new Form();
        form.add(new Parameter("votable", contain));
        form.add(new Parameter("coordSystem", "EQUATORIAL"));
        client = new ClientResource(getHostUrl() + urlToTest);
        Representation result = client.post(form);
        String json = result.getText();
        assertNotNull(json, "The conversion from VOTable to JSON gives a null result");
    }

}
