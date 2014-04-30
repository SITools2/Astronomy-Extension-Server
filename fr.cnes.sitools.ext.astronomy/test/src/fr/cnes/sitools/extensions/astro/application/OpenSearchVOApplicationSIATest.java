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
package fr.cnes.sitools.extensions.astro.application;

import fr.cnes.sitools.extensions.common.Utility;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.resource.ClientResource;

import fr.cnes.sitools.test.common.AbstractSitoolsServiceTestCase;
import org.codehaus.jackson.JsonNode;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOApplicationSIATest extends AbstractSitoolsServiceTestCase {

    private final String request = "/sia";

    public OpenSearchVOApplicationSIATest() {
    }

    @Before
    public void setUp() throws IOException, Exception {
        setMediaTest(MediaType.APPLICATION_JSON);
    }

    /**
     * Test of openSearch description.
     */
    @Test
    public void testOpenSearchDescription() throws IOException {
        System.out.println("getOpenSearch description");
        ClientResource clientResource = new ClientResource(getHostUrl() + request);
        String result = clientResource.get().getText();
        assertEquals(200, clientResource.getStatus().getCode());
    }

    /**
     * Test of VO conesearch result.
     */
    @Test
    public void testSearch() throws IOException {
        System.out.println("getVOSIA results");
        ClientResource clientResource = new ClientResource(getHostUrl() + request + "/search?healpix=10&order=4&coordSystem=EQUATORIAL&format=json");
        String result = clientResource.get().getText();
        JsonNode json = Utility.mapper.readValue(result, JsonNode.class);
        long numberResult = json.get("totalResults").getLongValue();
        assertEquals(2, numberResult);
    }

    /**
     * Test of dico.
     */
    @Test
    public void testDico() throws IOException {
        System.out.println("getDico");
        ClientResource clientResource = new ClientResource(getHostUrl() + request + "/dico/TimeExtent");
        String result = clientResource.get().getText();
        assertEquals(200, clientResource.getStatus().getCode());
        assertTrue(!result.isEmpty());
    }
}
