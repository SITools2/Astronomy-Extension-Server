 /*******************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Client;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.util.ClientResourceProxy;

/**
 * Queries the IMCCESsoResolver name resolver and returns the list of coordinates for a given name.<br/> A IMCCESsoResolver lets you a sky
 * position given both a time and a solar body name.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class IMCCESsoResolver extends AbstractNameResolver {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(IMCCESsoResolver.class.getName());
  /**
   * Template URL for the IMCCE resolver service.
   */
  private static final String TEMPLATE_NAME_RESOLVER = "http://vo.imcce.fr/webservices/ssodnet/resolver.php?name=<name>&mime=json&epoch=<epoch>&ephem=1&from=SITools2";
  /**
   * Credits to return for IMCCE.
   */
  private static final String CREDITS_NAME = "IMCCE";
  /**
   * Object name to search.
   */
  private String objectName;
  /**
   * Epoch for which the search is done.
   */
  private String epoch;
  /**
   * Empty constructor.
   */
  protected IMCCESsoResolver() {
  }

  /**
   * Constructs a new IMCCE SsoDNet based on time.
   *
   * @param objectNameVal object name to resolve
   * @param epochVal epoch for which the search is done
   */
  public IMCCESsoResolver(final String objectNameVal, final String epochVal) {
    setObjectName(objectNameVal);
    setEpoch(epochVal);
    checkInputParameters();
  }
  /**
   * Sets the object name.
   * @param objectNameVal object name to set
   */
  protected final void setObjectName(final String objectNameVal) {
      this.objectName = objectNameVal;
  }
  /**
   * Returns the object name.
   * @return the object name
   */
  protected final String getObjectName() {
      return this.objectName;
  }
  /**
   * Sets the epoch.
   * @param epochVal the epoch to set
   */
  protected final void setEpoch(final String epochVal) {
      this.epoch = epochVal;
  }
  /**
   * Returns the epoch.
   * @return the epoch
   */
  protected final String getEpoch() {
      return this.epoch;
  }
  /**
   * Checks if the input parameters are set.
   *
   * <p>
   * Returns a IllegalArgumentException if one of the input parameters is <code>null</code> or empty.
   * </p>
   */
  protected final void checkInputParameters() {
    if (getObjectName() == null || getObjectName().isEmpty()) {
      throw new IllegalArgumentException("Object name must be set.");
    }
    if (getEpoch() == null || getEpoch().isEmpty()) {
      throw new IllegalArgumentException("cannot find the service.");
    }
  }

  @Override
  public final NameResolverResponse getResponse() {
    NameResolverResponse response = new NameResolverResponse(CREDITS_NAME);
    try {
      final Object json = callImcce(objectName, epoch);
      final List<AstroCoordinate> astrocoordinates = processResponse(json);
      if (astrocoordinates.isEmpty()) throw new NameResolverException(Status.SUCCESS_PARTIAL_CONTENT,"The server has send a response but no coordinates have been found.");
      response.addAstoCoordinates(astrocoordinates);
    } catch (NameResolverException ex) {
      if (getSuccessor() == null) {
        response.setError(ex);
      } else {
        response = getSuccessor().getResponse();
      }
    } finally {
      return response;
    }
  }

  /**
   * Queries the IMCCE service and stores the result in
   * <code>json</code>.
   *
   * @param objectNameVal object name to resolve
   * @param epochVal epoch
   * @return the server's response
   * @throws NameResolverException if a problem occurs while the response is being parsed
   */
  private Object callImcce(final String objectNameVal, final String epochVal) throws NameResolverException {
    assert objectNameVal != null;
    assert epochVal != null;
    Object json;
    // building the query
    final String serviceTmp = TEMPLATE_NAME_RESOLVER.replace("<name>", objectNameVal);
    final String service = serviceTmp.replace("<epoch>", epochVal);
    LOG.log(Level.INFO, "Call IMCCE name resolver: {0}", service);

    //requesting
    final ClientResourceProxy clientProxy = new ClientResourceProxy(service, Method.GET);
    final ClientResource client = clientProxy.getClientResource();
    final Client clientHTTP = new Client(Protocol.HTTP);
    clientHTTP.setConnectTimeout(AbstractNameResolver.SERVER_TIMEOUT);
    client.setNext(clientHTTP);
    final Status status = client.getStatus();

    // when the response is fine, we process the response
    if (status.isSuccess()) {
      String result;
      try {
        result = client.get().getText();
      } catch (IOException ex) {
        throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
      } catch (ResourceException ex) {
        throw new NameResolverException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, ex);
      }
      try {
        json = new JSONObject(result);
      } catch (JSONException ex) {
        try {
          json = new JSONArray(result);
        } catch (JSONException ex1) {
          throw new NameResolverException(Status.CLIENT_ERROR_NOT_ACCEPTABLE, ex1);
        }
      }
      return json;
    } else {
      throw new NameResolverException(status, status.getThrowable());
    }
  }

  /**
   * Processes the response of the server.
   * @param json server's response
   * @return the response of the server
   * @throws NameResolverException
   */
  private List<AstroCoordinate> processResponse(final Object json) throws NameResolverException {
    final List<AstroCoordinate> astroCoordinates = new ArrayList<AstroCoordinate>();
    if (json instanceof JSONObject) {
      final JSONObject jsonObj = (JSONObject) json;
      astroCoordinates.add(extractCoordinatesFromRecord(jsonObj));
    } else {
      final JSONArray jsonArray = (JSONArray) json;
      final int length = jsonArray.length();
      for (int i = 0; i < length; i++) {
        try {
          final JSONObject jsonObj = jsonArray.getJSONObject(i);
          astroCoordinates.add(extractCoordinatesFromRecord(jsonObj));
        } catch (JSONException ex) {
          throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
        }
      }
    }
    return astroCoordinates;
  }

  /**
   * Extracts the coordinates from the response.
   *
   * @param jsonObj response
   * @return coordinates on the sky
   * @throws NameResolverException if the transformation Equatorial to galactic failed or a if a problem occurs while the JSON is being
   * parsed
   */
  private AstroCoordinate extractCoordinatesFromRecord(final JSONObject jsonObj) throws NameResolverException {
    assert jsonObj != null;    
    try {
      if (!jsonObj.has("type") || !jsonObj.has("ra") || !jsonObj.has("dec")) {
        throw new NameResolverException(Status.CLIENT_ERROR_NOT_FOUND, "Not found");
      }
      final double rightAscension = Double.valueOf(jsonObj.getString("ra"));
      final double declination = Double.valueOf(jsonObj.getString("dec"));
      final AstroCoordinate astroCoord = new AstroCoordinate(rightAscension, declination);
      astroCoord.addMetadata("type", jsonObj.getString("type"));
      return astroCoord;
    } catch (JSONException ex) {
      throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
    }
  }
}
