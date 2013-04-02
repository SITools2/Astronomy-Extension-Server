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
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.project.model.Project;
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.test.common.AbstractSitoolsServiceTestCase;
import java.io.IOException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 *
 * @author malapert
 */
public class ReverseNameResolverResourceTest extends AbstractSitoolsServiceTestCase {
    
    private final String urlAttachment = "/testreversenameresolver";
    private final String projectName = "test";
    private Project pj;
    private ResourceModel rm;    
    
    @Before
    @Override
    public void setUp() throws IOException, Exception {
        setMediaTest(MediaType.APPLICATION_JSON);
        pj = createProjectObject(projectName, this.urlAttachment);
        createProject(pj);
        activateProject(pj);
        rm = createResourceModel(fr.cnes.sitools.extensions.astro.resource.ReverseNameResolverResourcePlugin.class.getName(), "reverseNameResolver", "/plugin/reverseNameResolver/{coordinates-order}");
        create(rm, getBaseUrl() +SitoolsSettings.getInstance().getString(Consts.APP_PROJECTS_URL) +"/"+projectName);                      
    }
    
    @After
    @Override
    public void tearDown() throws IOException, Exception {
        delete(rm, getBaseUrl() +SitoolsSettings.getInstance().getString(Consts.APP_PROJECTS_URL)+ "/"+projectName);
        deleteProject(pj);          
    }

    /**
     * Test of getReverseNameResolverResponse method, of class ReverseNameResolverResource.
     */
    @Test
    public void testGetReverseNameResolverResponse() throws Exception {
        System.out.println("getReverseNameResolverResponse");
        ClientResource clientResource = new ClientResource(getHostUrl() + this.urlAttachment + "/plugin/reverseNameResolver/00:42:44.32%20+41:16:07.5;13");
        JSONObject result = new JSONObject(clientResource.get(MediaType.APPLICATION_JSON).getText());
        JSONObject expResult = new JSONObject("{\"totalResults\":1,\"features\":[{\"properties\":{\"crs\":{\"properties\":{\"name\":\"EQUATORIAL.ICRS\"},\"type\":\"name\"},\"title\":\"M  31 \",\"magnitude\":4.36,\"credits\":\"CDS\",\"seeAlso\":\"http://simbad.u-strasbg.fr/simbad/sim-id?Ident=M  31 \",\"type\":\"Galaxy\",\"identifier\":\"M  31 \"},\"geometry\":{\"type\":\"Point\",\"coordinates\":[10.684708333333333,41.26875]}}],\"type\":\"FeatureCollection\"}");
        assertEquals(expResult.toString(), result.toString());
    }

}