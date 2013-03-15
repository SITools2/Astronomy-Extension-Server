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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.ivoa.xml.votable.v1.Data;
import net.ivoa.xml.votable.v1.Field;
import net.ivoa.xml.votable.v1.Resource;
import net.ivoa.xml.votable.v1.Table;
import net.ivoa.xml.votable.v1.TableData;
import net.ivoa.xml.votable.v1.Td;
import net.ivoa.xml.votable.v1.Tr;
import net.ivoa.xml.votable.v1.VOTABLE;
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
public class ConeSearchResourceTest extends AbstractSitoolsServiceTestCase {

  private final String urlAttachment = "/plugin/conesearch";
  private ResourceModel rm;
  private static final String datasetId = "cc659853-d130-4434-b52b-89ec57db3735";
  private static final String request = "/fuse/plugin/conesearch?";

  public ConeSearchResourceTest() {
  }

  @Override
  protected String getBaseUrl() {
    return super.getBaseUrl() + SitoolsSettings.getInstance().getString(Consts.APP_DATASETS_URL) + "/" + datasetId;
  }

  @Before
  public void setUp() {
    try {
      setMediaTest(MediaType.APPLICATION_XML);

      rm = createResourceModel(fr.cnes.sitools.extensions.astro.resource.ConeSearchResourcePlugin.class.getName(),
              "CSP", this.urlAttachment);
      ResourceParameter rp0 = new ResourceParameter("methods", "", ResourceParameterType.PARAMETER_INTERN);
      rp0.setValue("GET");
      rm.addParam(rp0);

      ResourceParameter rp1 = new ResourceParameter("description", "", ResourceParameterType.PARAMETER_INTERN);
      rp1.setValue("Test");
      rm.addParam(rp1);

      ResourceParameter rp2 = new ResourceParameter("PARAM_Dictionary", "", ResourceParameterType.PARAMETER_INTERN);
      rp2.setValue("ConeSearchDico");
      rp2.setValueType("xs:dictionary");
      rm.addParam(rp2);

      ResourceParameter rp3 = new ResourceParameter("COLUMN_X", "", ResourceParameterType.PARAMETER_INTERN);
      rp3.setValue("x_pos");
      rp3.setValueType("xs:dataset.columnAlias");
      rm.addParam(rp3);

      ResourceParameter rp4 = new ResourceParameter("COLUMN_Y", "", ResourceParameterType.PARAMETER_INTERN);
      rp4.setValue("y_pos");
      rp4.setValueType("xs:dataset.columnAlias");
      rm.addParam(rp4);

      ResourceParameter rp5 = new ResourceParameter("COLUMN_Z", "", ResourceParameterType.PARAMETER_INTERN);
      rp5.setValue("y_pos");
      rp5.setValueType("xs:dataset.columnAlias");
      rm.addParam(rp5);

      ResourceParameter rp6 = new ResourceParameter("METADATA_MAX_SR", "", ResourceParameterType.PARAMETER_INTERN);
      rp6.setValue("20");
      rp6.setValueType("xs:string");
      rm.addParam(rp6);

      create(rm, getBaseUrl());

    } catch (ClassNotFoundException ex) {
      Logger.getLogger(ConeSearchResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      Logger.getLogger(ConeSearchResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      Logger.getLogger(ConeSearchResourceTest.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @After
  public void tearDown() {
    delete(rm, getBaseUrl());
  }

  /**
   * Test of getVOResponse method, of class ConeSearchResource.
   */
  @Test
  public void testGetVOResponse20() throws IOException, JAXBException {    
      System.out.println("getConeSearch with radius20");
      ClientResource clientResource = new ClientResource(getHostUrl() + request + "RA=0&DEC=0&SR=20");
      Representation rep = clientResource.get();
      String result = rep.getText();      
      JAXBContext ctx = JAXBContext.newInstance(new Class[]{net.ivoa.xml.votable.v1.VotableFactory.class});
      Unmarshaller um = ctx.createUnmarshaller();      
      VOTABLE votable = (VOTABLE) um.unmarshal(new ByteArrayInputStream(result.getBytes()));
      List<Resource> resources = votable.getRESOURCE();
      Resource resource = resources.get(0);
      List<Map<Field, String>> response = parseResponse(resource);
      int nbResponse = response.size();
      assertEquals(59,nbResponse);
  }
  
  /**
   * Test of getVOResponse method, of class ConeSearchResource.
   */
  @Test
  public void testGetVOResponse80() throws IOException, JAXBException {    
      System.out.println("getConeSearch with radius80");
      ClientResource clientResource = new ClientResource(getHostUrl() + request + "RA=0&DEC=0&SR=80");
      Representation rep = clientResource.get();
      String result = rep.getText();      
      JAXBContext ctx = JAXBContext.newInstance(new Class[]{net.ivoa.xml.votable.v1.VotableFactory.class});
      Unmarshaller um = ctx.createUnmarshaller();      
      VOTABLE votable = (VOTABLE) um.unmarshal(new ByteArrayInputStream(result.getBytes()));
      List<Resource> resources = votable.getRESOURCE();
      Resource resource = resources.get(0);
      List<Map<Field, String>> response = parseResponse(resource);      
      assertNull(response);
  }  
  
  /**
   * Parse Resource from VOTable.
   *
   * @param resourceIter Resource
   * @return records
   */
  private List<Map<Field, String>> parseResponse(final Resource resourceIter) {
    List<Map<Field, String>> responses = null;
    List<Object> objects = resourceIter.getLINKAndTABLEOrRESOURCE();
    for (Object objectIter : objects) {
      if (objectIter instanceof Table) {
        Table table = (Table) objectIter;
        responses = parseTable(table);
      }
    }
    return responses;
  }  
  
  /**
   * Parse table from VO.
   *
   * @param table table
   * @return records
   */
  private List<Map<Field, String>> parseTable(final Table table) {
    int nbFields = 0;
    List<Map<Field, String>> responses = new ArrayList<Map<Field, String>>();
    Map<Integer, Field> responseFields = new HashMap<Integer, Field>();
    List<JAXBElement<?>> currentTable = table.getContent();
    for (JAXBElement<?> currentTableIter : currentTable) {
      // metadata case
      if (currentTableIter.getValue() instanceof Field) {
        JAXBElement<Field> fields = (JAXBElement<Field>) currentTableIter;
        Field field = fields.getValue();
        responseFields.put(nbFields, field);
        nbFields++;
        // data
      } else if (currentTableIter.getValue() instanceof Data) {
        JAXBElement<Data> datas = (JAXBElement<Data>) currentTableIter;
        Data data = datas.getValue();
        TableData tableData = data.getTABLEDATA();
        List<Tr> trs = tableData.getTR();
        for (Tr trsIter : trs) {
          Map<Field, String> response = new HashMap<Field, String>();
          List<Td> tds = trsIter.getTD();
          int nbTd = 0;
          for (Td tdIter : tds) {
            String value = tdIter.getValue();
            response.put(responseFields.get(nbTd), value);
            nbTd++;
          }
          responses.add(response);
        }
      }
    }
    return responses;
  }  
}
