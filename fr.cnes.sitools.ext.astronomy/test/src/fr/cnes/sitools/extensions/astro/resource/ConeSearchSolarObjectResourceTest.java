 /*******************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.engine.Engine;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.test.common.AbstractSitoolsServiceTestCase;
import org.codehaus.jackson.JsonNode;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConeSearchSolarObjectResourceTest extends AbstractSitoolsServiceTestCase {

  private final String urlAttachment = "/plugin/solarObjects/{coordSystem}";
  private ResourceModel rm;
  private static final String datasetId = "cc659853-d130-4434-b52b-89ec57db3735";
  private static final String request = "/fuse/plugin/solarObjects/EQUATORIAL?healpix=8&order=8&EPOCH=2013-01-23T18:31:00";

  public ConeSearchSolarObjectResourceTest() {
  }

  @Override
  protected String getBaseUrl() {
    return super.getBaseUrl() + SitoolsSettings.getInstance().getString(Consts.APP_DATASETS_URL) + "/" + datasetId;
  }

  @Before
  public void setUp() {
    try {
      setMediaTest(MediaType.APPLICATION_JSON);

      rm = createResourceModel(fr.cnes.sitools.extensions.astro.resource.ConeSearchSolarObjectResourcePlugin.class.getName(),
              "SolarObject", this.urlAttachment);
      create(rm, getBaseUrl());
    } catch (ClassNotFoundException ex) {
      Engine.getLogger(ConeSearchSolarObjectResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      Engine.getLogger(ConeSearchSolarObjectResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      Engine.getLogger(ConeSearchSolarObjectResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @After
  public void tearDown() {
    delete(rm, getBaseUrl());
  }

  /**
   * Test of getSolarObjectsResponse method, of class ConeSearchSolarObjectResource.
   */
  @Test
  public void testGetSolarObjectsResponse() throws IOException {
    System.out.println("getSolarObjectsResponse");
    ClientResource clientResource = new ClientResource(getHostUrl() + request);
    Representation rep = clientResource.get();
    String result = rep.getText();
    JsonNode json = Utility.mapper.readValue(result, JsonNode.class);
    String totalResult = json.get("totalResults").getValueAsText();
    assertEquals("1", totalResult);
  }
}
