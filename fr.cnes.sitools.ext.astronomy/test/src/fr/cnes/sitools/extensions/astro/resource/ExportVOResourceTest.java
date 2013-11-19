/*
 * Copyright 2013 - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.ivoa.xml.votable.v1.VOTABLE;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ExportVOResourceTest extends AbstractSitoolsServiceTestCase {

  private final String urlAttachment = "/plugin/voexport";
  private ResourceModel rm;
  private static final String datasetId = "4ce7cd08-a027-46c7-a652-6a01cb896ff5";
  private static final String request = "/datasets/headers/plugin/voexport?1=1&colModel=%22dataset,%20targname,%20ra_targ,%20dec_targ,%20dateobs,%20exptime,%20aperture,%20mode,%20expos_nbr,%20vmag,%20sp_type,%20ebv,%20objclass,%20src_type,%20datearchiv,%20datepublic,%20ref,%20z,%20starttime,%20endtime,%20elat,%20elong,%20glat,%20glong,%20aper_pa,%20high_proper_motion,%20moving_target,%20pr_inv_l,%20pr_inv_f,%20loadedatiap,%20healpixid,%20x_pos,%20y_pos,%20z_pos%22&p%5B0%5D=LISTBOXMULTIPLE%7Cdataset%7CA0010101%7CA0010201%7CA0010202";

  /**
   * absolute url for dataset management REST API
   *
   * @return url
   */
  @Override
  protected String getBaseUrl() {
    return super.getBaseUrl() + SitoolsSettings.getInstance().getString(Consts.APP_DATASETS_URL) + "/" + datasetId;
  }

  @Before
  @Override
  public void setUp() {
    try {
      setMediaTest(MediaType.APPLICATION_XML);

      rm = createResourceModel(fr.cnes.sitools.extensions.astro.resource.ExportVOResourcePlugin.class.getName(),
              "VOTable_export", this.urlAttachment);
      ResourceParameter rp0 = new ResourceParameter("methods", "", ResourceParameterType.PARAMETER_INTERN);
      rp0.setValue("GET");
      rm.addParam(rp0);

      ResourceParameter rp1 = new ResourceParameter("Description", "", ResourceParameterType.PARAMETER_INTERN);
      rp1.setValue("VOTable export");
      rm.addParam(rp1);

      ResourceParameter rp2 = new ResourceParameter("Dictionary", "", ResourceParameterType.PARAMETER_INTERN);
      rp2.setValue("ConeSearchDico");
      rp2.setValueType("xs:dictionary");
      rm.addParam(rp2);

      create(rm, getBaseUrl());
    } catch (ClassNotFoundException ex) {
      Logger.getLogger(ExportVOResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      Logger.getLogger(ExportVOResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      Logger.getLogger(ExportVOResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @After
  public void tearDown() {
    delete(rm, getBaseUrl());
  }

  /**
   * Test of getVoExport method, of class ExportVOResource.
   */
  @Test
  public void testGetVoExport() {
    try {
      System.out.println("getVoExport");
      ClientResource clientResource = new ClientResource(getHostUrl() + ExportVOResourceTest.request);
      Representation rep = clientResource.get();
      String result = rep.getText();
      JAXBContext ctx = JAXBContext.newInstance(new Class[]{net.ivoa.xml.votable.v1.ObjectFactory.class});
      Unmarshaller um = ctx.createUnmarshaller();
      VOTABLE votable = (VOTABLE) um.unmarshal(new ByteArrayInputStream(result.getBytes()));
      assertNotNull(votable);
    } catch (JAXBException ex) {
      Logger.getLogger(ExportVOResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(ExportVOResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
