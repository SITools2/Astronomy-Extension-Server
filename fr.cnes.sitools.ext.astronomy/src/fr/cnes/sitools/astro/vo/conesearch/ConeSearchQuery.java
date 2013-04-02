/**
 * *****************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
package fr.cnes.sitools.astro.vo.conesearch;

import fr.cnes.sitools.util.ClientResourceProxy;
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
import org.restlet.data.Method;
import org.restlet.resource.ClientResource;

/**
 * This object provides an implementation to retrieve data through the Cone Search Protocol.
 *
 * @author Jean-Christophe Malapert
 */
public class ConeSearchQuery {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ConeSearchQuery.class.getName());
  /**
   * Service url.
   */
  private String url;

  /**
   * Create a cone search query.
   *
   * @param urlVal service URL
   */
  public ConeSearchQuery(final String urlVal) {
    this.url = urlVal;
  }

  /**
   * Retrieve data from VO.
   *
   * @param ra Ra of the center of the cone
   * @param dec Dec of the center of the cone
   * @param sr radius of the cone
   * @return records from cone search protocol
   * @throws Exception Exception
   */
  public final List<Map<Field, String>> getResponseAt(final double ra, final double dec, final double sr) throws Exception {
    try {
      return process(ra, dec, sr);
    } catch (JAXBException ex) {
      throw new Exception(ex);
    } catch (IOException ex) {
      throw new Exception(ex);
    }
  }

  /**
   * Returns the result of the query.
   *
   * @param ra Ra of the center of the cone
   * @param dec Dec of the center of the cone
   * @param sr radius of the cone
   * @return the result of the query
   * @throws JAXBException Parsing Exception
   * @throws IOException Exception
   */
  private List<Map<Field, String>> process(final double ra, final double dec, final double sr) throws JAXBException, IOException {
    String queryService = String.format("%sRA=%s&DEC=%s&SR=%s", url, ra, dec, sr);
    LOG.log(Level.INFO, queryService);
    ClientResourceProxy proxy = new ClientResourceProxy(queryService, Method.GET);
    ClientResource client = proxy.getClientResource();
    JAXBContext ctx = JAXBContext.newInstance(new Class[]{net.ivoa.xml.votable.v1.VotableFactory.class});
    Unmarshaller um = ctx.createUnmarshaller();
    String result = client.get().getText();
    result = result.replace("http://www.ivoa.net/xml/VOTable/v1.1", "http://www.ivoa.net/xml/VOTable/v1.2");
    VOTABLE votable = (VOTABLE) um.unmarshal(new ByteArrayInputStream(result.getBytes()));
    List<Resource> resources = votable.getRESOURCE();
    Resource resource = resources.get(0);
    List<Map<Field, String>> response = parseResponse(resource);
    return response;
  }

  /**
   * Parse Resource from VOTable.
   *
   * @param resourceIter Resource
   * @return records
   */
  private List<Map<Field, String>> parseResponse(final Resource resourceIter) {
    List<Map<Field, String>> responses = new ArrayList<Map<Field, String>>();
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
