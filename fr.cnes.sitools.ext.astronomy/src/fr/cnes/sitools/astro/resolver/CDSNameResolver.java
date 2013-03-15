/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.sitools.util.ClientResourceProxy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * This object contains methods to handle CDS name resolver.<br/>
 * A CDSNameResolver allows you to get a sky position given an object name.
 * @see <a href="http://cdsweb.u-strasbg.fr/doc/sesame.htx">Sesame</a>
 * @author Jean-Christophe Malapert
 */
public class CDSNameResolver extends AbstractNameResolver {

    private static final String hostResolverName = "http://cdsweb.u-strasbg.fr/cgi-bin/nph-sesame/-oxp/<service>?<objectName>";
    private Sesame sesameResponse = null;
    private final static String creditsName = "CDS";

    @Override
    public String getCreditsName() {
        return creditsName;
    }

    /**
     * List of name resolver service for stellar objects
     */
    public enum NameResolverService {

        /**
         * NED resolver
         */
        ned("N"),
        /**
         * SIMBAD resolver
         */
        simbad("S"),
        /**
         * Vizier resolver
         */
        vizier("V"),
        /**
         * All resolver
         */
        all("A");
        private String serviceCode;

        NameResolverService(final String serviceCode) {
            this.serviceCode = serviceCode;
        }

        /**
         * Get the name resolver code. This code is then used to call the CDS service
         * @return the name resolver code
         */
        public String getServiceCode() {
            return this.serviceCode;
        }
    }

    /**
     * Create a name resolver call based 
     * - on the object name to resolve 
     * - and the name resolver code
     * 
     * @param objectName object name to resolve
     * @param service name resolver to use (NED, ...)
     * @throws NameResolverException  
     */
    public CDSNameResolver(final String objectName, final NameResolverService service) throws NameResolverException {
        setParameters(objectName, service);
    }

    /**
     * Build the URL to call the name resolver
     * 
     * @param objectName object name to resolve
     * @param service one of the supported service
     * @throws NameResolverException  
     */
    public void setParameters(final String objectName, final NameResolverService service) throws NameResolverException {
        String url = hostResolverName.replace("<objectName>", objectName);
        url = url.replace("<service>", service.getServiceCode());
        this.sesameResponse = parseResponse(url);
    }

    /**
     * Parse the name resolver response
     * 
     * @param url url of the service
     * @return the Sesame response
     * @throws NameResolverException
     */
    private Sesame parseResponse(final String url) throws NameResolverException {
        Logger.getLogger(CDSNameResolver.class.getName()).log(Level.INFO, "Call CDS name resolver: {0}", url);
        ClientResourceProxy clientProxy = new ClientResourceProxy(url, Method.GET);
        ClientResource client = clientProxy.getClientResource();
        Status status = client.getStatus();
        if (status.isSuccess()) {
            try {
                JAXBContext ctx = JAXBContext.newInstance(new Class[]{fr.cnes.sitools.astro.resolver.CDSFactory.class});
                Unmarshaller um = ctx.createUnmarshaller();
                Sesame response = (Sesame) um.unmarshal(new ByteArrayInputStream(client.get().getText().getBytes()));
                return response;
            } catch (IOException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            } catch (JAXBException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, "Cannot parse CDS response", ex);
            } catch (ResourceException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            }
        } else {
            throw new NameResolverException(status, status.getThrowable());
        }
    }

    /**
     * Parse coordinates from CDS response
     * @return Returns [ra,dec]
     * @throws NameResolverException - if empty response from CDS
     */
    private String[] parseCoordinates() throws NameResolverException {
        Target target = this.sesameResponse.getTarget().get(0);
        List<Resolver> resolvers = target.getResolver();
        String[] coordinates = new String[2];
        for (Resolver resolver : resolvers) {
            List<JAXBElement<?>> terms = resolver.getINFOOrERROROrOid();
            for (JAXBElement<?> term : terms) {
                if (coordinates[0] != null && coordinates[1] != null) {
                    break;
                } else if ("jradeg".equals(term.getName().getLocalPart())) {
                    coordinates[0] = term.getValue().toString();
                } else if ("jdedeg".equals(term.getName().getLocalPart())) {
                    coordinates[1] = term.getValue().toString();
                }
            }
        }
        if (coordinates[0] == null || coordinates[1] == null) {
            throw new NameResolverException(Status.SUCCESS_NO_CONTENT, "Unknown object");
        }
        return coordinates;
    }

    /**
     * Get coordinates of the name resolver response
     * @param coordinateSystem coordinate system
     * @return the AstroCoordinate
     * @throws NameResolverException
     * @see AstroCoordinate
     */
    @Override
    public List<AstroCoordinate> getCoordinates(final CoordinateSystem coordinateSystem) throws NameResolverException {
        String[] coordinates = parseCoordinates();
        AstroCoordinate astroCoord = new AstroCoordinate(Double.valueOf(coordinates[0]), Double.valueOf(coordinates[1]));
        processTransformation(astroCoord, coordinateSystem);
        return Arrays.asList(astroCoord);
    }

    /**
     * Get the complete response of the name resolver
     * @return the complete response of the name resolver
     */
    @Override
    public Object getCompleteResponse() {
        return this.sesameResponse;
    }
    private static final Logger LOG = Logger.getLogger(CDSNameResolver.class.getName());
}
