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
 * ****************************************************************************
 */
package fr.cnes.sitools.astro.resolver;

import fr.cnes.sitools.util.ClientResourceProxy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.restlet.Client;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * Queries the CDS name resolver and returns the list of coordinates for a given name.<br/> 
 * The CDSNameResolver lets you get a sky position given an object name.
 *
 * @see <a href="http://cdsweb.u-strasbg.fr/doc/sesame.htx">Sesame</a>
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CDSNameResolver extends AbstractNameResolver {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(CDSNameResolver.class.getName());
  /**
   * Template URL for the CDS name resolver service.
   */
  private static final String TEMPLATE_NAME_RESOLVER = "http://cdsweb.u-strasbg.fr/cgi-bin/nph-sesame/-oxp/<service>?<objectName>";
  /**
   * Credits to return for CDS.
   */
  private static final String CREDITS_NAME = "CDS";
  
  /**
   * Object name to find.
   */
  private transient String objectName;
  
  /**
   * The choice of the name resolver at CDS.
   */
  private transient NameResolverService nameResolverService;

  /**
   * List of name resolver services for stellar objects.
   */
  public enum NameResolverService {

    /**
     * NED resolver.
     */
    ned("N"),
    /**
     * SIMBAD resolver.
     */
    simbad("S"),
    /**
     * Vizier resolver.
     */
    vizier("V"),
    /**
     * All resolver.
     */
    all("A");
    /**
     * Service code representing a name resolver service.
     */
    private String serviceCode;

    /**
     * Constructs a new name resolver service.
     *
     * @param serviceCodeStr the code representing a name resolver service
     */
    NameResolverService(final String serviceCodeStr) {
      this.serviceCode = serviceCodeStr;
    
    }

    /**
     * Returns the name resolver code representing a name resolver service. This code is then used to call the CDS service
     *
     * @return the name resolver code representing a name resolver service
     */
    public String getServiceCode() {
      return this.serviceCode;
    }
  }

  /**
   * Constructs a new CDS name resolver on the object name to resolve, the name resolver service.
   *
   * @param objectNameVal object name to resolve
   * @param service name resolver to use (NED, ...)   
   */
  public CDSNameResolver(final String objectNameVal, final NameResolverService service) {
    checkParameters(objectNameVal, service);    
    this.objectName = objectNameVal;
    this.nameResolverService = service;    
  }
  
  /**
   * Checks the validity of input parameters.
   * 
   * <p>
   * Returns a IllegalArgumentException if one of the input parameters is <code>null</code> or empty.
   * </p>
   *
   * @param objectNameVal object name
   * @param service  name resolver service at CDS
   */
  private void checkParameters(final String objectNameVal, final NameResolverService service) {
    if (objectNameVal == null || objectNameVal.isEmpty()) {
      throw new IllegalArgumentException("Object name must be set.");
    }
    if (service == null) {
      throw new IllegalArgumentException("cannot find the service.");
    }
  }

  @Override
  public final NameResolverResponse getResponse() {
    NameResolverResponse response = new NameResolverResponse(CREDITS_NAME);
    try {
      String url = TEMPLATE_NAME_RESOLVER.replace("<objectName>", this.objectName);
      url = url.replace("<service>", this.nameResolverService.getServiceCode());
      final Sesame sesameResponse = parseResponse(url);
      final String[] coordinates = parseCoordinates(sesameResponse);
      response.addAstroCoordinate(Double.valueOf(coordinates[0]), Double.valueOf(coordinates[1]));
    } catch (NameResolverException ex) {
      if (getSuccessor() == null) {
        response.setError(ex);
      } else {
        response = getSuccessor().getResponse();
      }
    } catch (Exception ex) {
      if (getSuccessor() == null) {
        response.setError(new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex));
      } else {
        response = getSuccessor().getResponse();
      }
    } finally {
      return response;
    }
  }

  /**
   * Parses the name resolver response.
   *
   * @param url url of the CDS name resolver service to call
   * @return the Sesame response
   * @throws NameResolverException if a problem occurs while the response is being parsed
   */
  private Sesame parseResponse(final String url) throws NameResolverException {
    assert url != null;
    LOG.log(Level.INFO, "Call CDS name resolver: {0}", url);
    final ClientResourceProxy clientProxy = new ClientResourceProxy(url, Method.GET);
    final ClientResource client = clientProxy.getClientResource();
    final Client clientHTTP = new Client(Protocol.HTTP);
    clientHTTP.setConnectTimeout(AbstractNameResolver.SERVER_TIMEOUT);
    client.setNext(clientHTTP);
    final Status status = client.getStatus();
    if (status.isSuccess()) {
      try {
        final JAXBContext ctx = JAXBContext.newInstance(new Class[]{fr.cnes.sitools.astro.resolver.CDSFactory.class});
        final Unmarshaller unMarshaller = ctx.createUnmarshaller();
        final Sesame response = (Sesame) unMarshaller.unmarshal(new ByteArrayInputStream(client.get().getText().getBytes()));
        return response;
      } catch (IOException ex) {
        throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
      } catch (JAXBException ex) {
        LOG.log(Level.WARNING, "the response of CDS server may changed");
        throw new NameResolverException(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "Cannot parse CDS response", ex);
      } catch (ResourceException ex) {
        throw new NameResolverException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, ex);
      }
    } else {
      throw new NameResolverException(status, status.getThrowable());
    }
  }

  /**
   * Parses the coordinates from CDS response and return them.
   *
   * @param sesameResponse CDS response
   * @return the following array [ra,dec]
   * @throws NameResolverException - if empty response from CDS
   */
  private String[] parseCoordinates(final Sesame sesameResponse) throws NameResolverException {
    final Target target = sesameResponse.getTarget().get(0);
    final List<Resolver> resolvers = target.getResolver();
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
      throw new NameResolverException(Status.CLIENT_ERROR_NOT_FOUND, "Unknown object");
    }
    return coordinates;
  }

}
