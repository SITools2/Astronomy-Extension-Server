/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is part of SITools2
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program inputStream distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.astro.resolver;

import java.io.IOException;
import java.util.HashMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.restlet.data.LocalReference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * Queries the SITools2 database and returns the coordinates of one constellation
 * for a given name.<br/> 
 * The ConstellationNameResolver lets you get a sky position given an object name.
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConstellationNameResolver extends AbstractNameResolver {

    /**
     * Constellation name to find.
     */
    private final transient String constellationName;

    /**
     * Credits to return.
     */
    private static final String CREDITS_NAME = "SITools2";

    /**
     * Constellation database.
     */
    private static final String CONSTELLATIONS_DB = "constellations.data";

    /**
     * Constructor.
     * @param consNameVal constellation name to find
     */
    public ConstellationNameResolver(final String consNameVal) {
        this.constellationName = consNameVal.toLowerCase();
    }

    @Override
    public final NameResolverResponse getResponse() {
        NameResolverResponse response = new NameResolverResponse(CREDITS_NAME);
        try {
            final Representation constelDb = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/"
                    + CONSTELLATIONS_DB).get();
            final JAXBContext jaxb = JAXBContext.newInstance(Wrapper.class);
            final Unmarshaller umMarshaller = jaxb.createUnmarshaller();
            final Wrapper wrapper = (Wrapper) umMarshaller.unmarshal(constelDb.getStream());
            final HashMap<String, Double[]> database = wrapper.getHashMap();
            if (database.containsKey(this.constellationName)) {
                final Double[] coordinates = database.get(this.constellationName);
                response.addAstroCoordinate(coordinates[0], coordinates[1]);
            } else {
                if (getSuccessor() == null) {
                    response.setError(new NameResolverException(Status.CLIENT_ERROR_NOT_FOUND));
                } else {
                    response = getSuccessor().getResponse();
                }
            }
        } catch (JAXBException ex) {
            if (getSuccessor() == null) {
                response.setError(new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex));
            } else {
                response = getSuccessor().getResponse();
            }
        } catch (IOException ex) {
            if (getSuccessor() == null) {
                response.setError(new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex));
            } else {
                response = getSuccessor().getResponse();
            }
        } finally {
            return response;
        }
    }
}
