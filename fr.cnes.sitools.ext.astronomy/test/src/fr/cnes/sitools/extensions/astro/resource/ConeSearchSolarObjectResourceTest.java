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
import fr.cnes.sitools.plugins.resources.model.ResourceParameter;
import fr.cnes.sitools.plugins.resources.model.ResourceParameterType;
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.test.common.AbstractSitoolsServiceTestCase;
import java.io.IOException;
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
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

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
      Logger.getLogger(ConeSearchSolarObjectResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      Logger.getLogger(ConeSearchSolarObjectResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      Logger.getLogger(ConeSearchSolarObjectResourceTest.class.getName()).log(Level.SEVERE, null, ex);
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
  public void testGetSolarObjectsResponse() throws IOException, JSONException {
    System.out.println("getSolarObjectsResponse");
    ClientResource clientResource = new ClientResource(getHostUrl() + request);
    Representation rep = clientResource.get();
    String result = rep.getText();
    JSONObject json = new JSONObject(result);
    String totalResult = String.valueOf(json.get("totalResults"));
    assertEquals("1", totalResult);
  }
}
