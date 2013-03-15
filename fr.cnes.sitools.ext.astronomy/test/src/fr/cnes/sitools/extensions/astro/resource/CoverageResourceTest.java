/*
 * Copyright 2013 - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.project.model.Project;
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.test.common.AbstractSitoolsServiceTestCase;
import java.io.IOException;
import java.io.InputStream;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
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
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CoverageResourceTest extends AbstractSitoolsServiceTestCase {
  
    private final String urlAttachment = "/plugin/skycoverage";
    private static final String datasetId = "cc659853-d130-4434-b52b-89ec57db3735";
    private final String projectName = "test";
    private Project pj;
    private ResourceModel rm;  
  
  public CoverageResourceTest(){
  }
  
  @Override
  protected String getBaseUrl() {
    return super.getBaseUrl() + SitoolsSettings.getInstance().getString(Consts.APP_DATASETS_URL) + "/" + datasetId;
  }  
  
  @Before
  public void setUp() throws IOException, Exception {
        setMediaTest(MediaType.APPLICATION_JSON);
        rm = createResourceModel(fr.cnes.sitools.extensions.astro.resource.SkyCoverageResourcePlugin.class.getName(), "skycoverage",this.urlAttachment);
        create(rm, getBaseUrl());
    
  }
  
  @After
  public void tearDown() throws IOException, Exception {
        delete(rm, getBaseUrl());     
  }


  /**
   * Test of getCoverage method, of class CoverageResource.
   */
  @Test
  public void testGetCoverage() throws IOException {
    System.out.println("getCoverage");
    ClientResource clientResource = new ClientResource(getHostUrl() + "/fuse/plugin/skycoverage?moc=http://alasky.u-strasbg.fr/footprints/tables/vizier/B_denis_denis/MOC;http://alasky.u-strasbg.fr/footprints/tables/vizier/II_307_wise/MOC");
    String result = clientResource.get(MediaType.TEXT_PLAIN).getText();
    assertEquals("29.439957936604817%", result);
  }

  /**
   * Test of getFitsResult method, of class CoverageResource.
   */
  @Test
  public void testGetFitsResult() throws IOException, FitsException {
    System.out.println("getFitsResult");
    MediaType.register("image/fits", "Fits image");
    ClientResource clientResource = new ClientResource(getHostUrl() + "/fuse/plugin/skycoverage?moc=http://alasky.u-strasbg.fr/footprints/tables/vizier/B_denis_denis/MOC;http://alasky.u-strasbg.fr/footprints/tables/vizier/II_307_wise/MOC");
    InputStream is = clientResource.get(MediaType.valueOf("image/fits")).getStream();
    Fits fits = new Fits(is);
    assertNotNull(fits);
  }


}
