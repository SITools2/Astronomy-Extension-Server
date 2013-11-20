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

import fr.cnes.sitools.util.ClientResourceProxy;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Client;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * Queries the Corot name resolver and returns the list of coordinates for a given identifier.<br/> A CorotIdResolver lets you get a sky
 * position given a Corot identifier
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CorotIdResolver extends AbstractNameResolver {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(CorotIdResolver.class.getName());
  /**
   * Credits to return for CDS.
   */
  private static final String CREDITS_NAME = "IAS/CNES";
  /**
   * Template URL for the Corot identifier resolver service.
   */
  private static final String TEMPLATE_NAME_RESOLVER = "http://idoc-corotn2-public-v2.ias.u-psud.fr/exo_dset/records?en2_windescriptor%22&nocount=false&start=0&limit=1"
          + "&filter[0][columnAlias]=corotid&filter[0][data][type]=numeric"
          + "&filter[0][data][comparison]=eq&filter[0][data][value]=<corotid>"
          + "&filter[1][columnAlias]=en2_windescriptor&filter[1][data][type]=boolean"
          + "&filter[1][data][comparison]=eq&filter[1][data][value]=false&media=json";
  /**
   * Corot service response.
   */
  private String corotId;

  /**
   * Empty constructor.
   */
  protected CorotIdResolver() {
  }

  /**
   * Constructs a new CorotId resolver.
   *
   * @param corotIdVal Corot ID
   */
  public CorotIdResolver(final String corotIdVal) {
    setCorotId(corotIdVal);
    checkInputParameters();      
  }
  /**
   * Sets the Corot Id.
   * @param corotIdVal the Corot ID to set
   */
  protected final void setCorotId(final String corotIdVal) {
      this.corotId = corotIdVal;
  }
  /**
   * Returns the Corot ID.
   * @return the Corot ID
   */
  protected final String getCorotId() {
      return this.corotId;
  }
  /**
   * Tests if the coroID is set.
   *
   * <p>
   * Returns IllegalArgumentException if <code>corotId</code> is <code>null</code> or empty.
   * </p>
   */
  protected final void checkInputParameters() {
    if (getCorotId() == null || getCorotId().isEmpty()) {
      throw new IllegalArgumentException("corotId must be set.");
    }
  }

  @Override
  public final NameResolverResponse getResponse() {
    NameResolverResponse response = new NameResolverResponse(CREDITS_NAME);
    try {
      final String query = TEMPLATE_NAME_RESOLVER.replace("<corotid>", corotId);
      LOG.log(Level.INFO, "{0} found from Corot service", getCorotId());
      final JSONObject json = parseResponse(query);
      final String[] coordinates = parseCoordinates(json);
      final double rightAscension = Double.valueOf(coordinates[0]);
      final double declination = Double.valueOf(coordinates[1]);
      response.addAstroCoordinate(rightAscension, declination);
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
   * Queries the SITools2 service at IAS and stores the result in <code>json</code>.
   *
   * @param query query the Corot service
   * @return the response from the server
   * @throws NameResolverException if a problem occurs while the response is being parsed
   */
  private JSONObject parseResponse(final String query) throws NameResolverException {
    assert query != null;
    LOG.log(Level.INFO, "Call IAS name resolver: {0}", query);
    final ClientResourceProxy proxy = new ClientResourceProxy(query, Method.GET);
    final ClientResource client = proxy.getClientResource();
    client.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "guest", "sitools2public"));
    final Client clientHTTP = new Client(Protocol.HTTP);
    clientHTTP.setConnectTimeout(AbstractNameResolver.SERVER_TIMEOUT);
    client.setNext(clientHTTP);
    final Status status = client.getStatus();
    if (status.isSuccess()) {
      JSONObject json;
      try {
        json = new JSONObject(client.get().getText());
      } catch (IOException ex) {
        throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
      } catch (JSONException ex) {
        LOG.log(Level.WARNING, "the response of Corot server may changed");
        throw new NameResolverException(Status.CLIENT_ERROR_NOT_ACCEPTABLE, ex);
      } catch (ResourceException ex) {
        throw new NameResolverException(Status.SERVER_ERROR_SERVICE_UNAVAILABLE, ex);
      }
      return json;
    } else {
      throw new NameResolverException(status, status.getThrowable());
    }
  }

  /**
   * Parses the coordinates from CDS response and return them.
   * @param json the server's response
   * @return the following array [rightAscension,declination]
   * @throws NameResolverException - if empty response from Corot
   */
  private String[] parseCoordinates(final JSONObject json) throws NameResolverException {
    try {
      final JSONArray jsonArray = json.getJSONArray("data");
      if (jsonArray.length() != 1) {
        throw new NameResolverException(Status.CLIENT_ERROR_NOT_FOUND, "Not found");
      }
      final JSONObject record = jsonArray.getJSONObject(0);
      if (!record.has("alpha_from") || !record.has("delta_from")) {
        throw new NameResolverException(Status.CLIENT_ERROR_NOT_FOUND, "Not found");
      }
      final String rightAscension = record.getString("alpha_from");
      final String declination = record.getString("delta_from");
      return new String[]{rightAscension, declination};
    } catch (JSONException ex) {
      throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, "cannot parse the coordinates");
    }
  }
}
