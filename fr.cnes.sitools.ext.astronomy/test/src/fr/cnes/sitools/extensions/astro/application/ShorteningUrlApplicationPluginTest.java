/**
 * *****************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SITools2. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.application;

import static fr.cnes.sitools.AbstractSitoolsServerTestCase.setMediaTest;
import fr.cnes.sitools.test.common.AbstractSitoolsServiceTestCase;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 *
 * @author malapert
 */
public class ShorteningUrlApplicationPluginTest extends AbstractSitoolsServiceTestCase {

    private final String request = "/shortener";

    public ShorteningUrlApplicationPluginTest() {
    }

    @Before
    public void setUp() throws IOException, Exception {
        setMediaTest(MediaType.APPLICATION_JSON);
    }

    /**
     * Test of posting and retrieving a contain of, class
     * ShorteningUrlApplicationPlugin.
     */
    @Test
    public void postAndRetrieveContain() throws IOException {
        System.out.println("postContain");
        ClientResource clientResource = new ClientResource(getHostUrl() + request);
        Form form = new Form();
        Parameter parameter = new Parameter("context", "{toto:\"fdfd\"}");
        form.add(parameter);
        Representation rep = clientResource.post(form);
        String result = rep.getText();
        assertEquals(200, clientResource.getStatus().getCode());
        assertNotNull(result, "Cannot post the contain");
        clientResource.release();
        System.out.println("retrieveContain");
        String query = getHostUrl() + request + '/' + result;
        clientResource = new ClientResource(query);
        rep = clientResource.get();
        assertNotNull(rep.getText(), "Cannot retrieve the contain from " + query);
    }
}
