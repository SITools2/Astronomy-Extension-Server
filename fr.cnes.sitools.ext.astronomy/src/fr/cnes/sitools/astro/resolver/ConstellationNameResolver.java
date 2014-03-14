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
package fr.cnes.sitools.astro.resolver;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.restlet.data.LocalReference;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import fr.cnes.sitools.astro.resolver.constellations.Wrapper;

/**
 * Queries the SITools2 database and returns the coordinates of one constellation
 * for a given name.<br/>
 * The ConstellationNameResolver lets you get a sky position given an object name.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConstellationNameResolver extends AbstractNameResolver {
    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(ConstellationNameResolver.class.getName());
    /**
     * Constellation name to find.
     */
    private String constellationName;

    /**
     * Credits to return.
     */
    private static final String CREDITS_NAME = "Wikipedia";

    /**
     * Constellation database.
     */
    private static final String CONSTELLATIONS_DB = "constellations.data";
    /**
     * Empty constructor.
     */
    protected ConstellationNameResolver() {
    }
    /**
     * Constructor.
     * @param consNameVal constellation name to find
     */
    public ConstellationNameResolver(final String consNameVal) {
        setConstellationName(consNameVal);
    }
    /**
     * Sets the constellation name.
     * <p>
     * The constellation name is transformed in lower case.
     * </p>
     * @param constellationNameVal constellation name to set
     */
    protected final void setConstellationName(final String constellationNameVal) {
        this.constellationName = constellationNameVal.toLowerCase();
    }
    /**
     * Returns the constellation name.
     * @return the constellation name
     */
    protected final String getConstellationName() {
        return this.constellationName;
    }
    @Override
    public final NameResolverResponse getResponse() {
        NameResolverResponse response = new NameResolverResponse(CREDITS_NAME);
        try {
            final Representation constelDb = new ClientResource(LocalReference.createClapReference(Wrapper.class.getPackage()) + "/"
                    + CONSTELLATIONS_DB).get();
            final JAXBContext jaxb = JAXBContext.newInstance(Wrapper.class);
            final Unmarshaller umMarshaller = jaxb.createUnmarshaller();
            final Wrapper wrapper = (Wrapper) umMarshaller.unmarshal(constelDb.getStream());
            final Map<String, Double[]> database = wrapper.getHashMap();
            if (database.containsKey(getConstellationName())) {
                LOG.log(Level.INFO, "{0} found as constellation", getConstellationName());
                final Double[] coordinates = database.get(getConstellationName());
                response.addAstroCoordinate(coordinates[0], coordinates[1]);
            } else {
                LOG.log(Level.WARNING, "{0} not found as constellation", getConstellationName());
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
        }
        return response;
    }
}
