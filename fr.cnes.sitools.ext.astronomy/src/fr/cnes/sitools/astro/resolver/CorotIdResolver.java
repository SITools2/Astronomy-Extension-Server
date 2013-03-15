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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * This object contains methods to handle Corot name resolver.<br/>
 * A CorotIdResolver allows you to get a sky position given a Corot identifier
 * @author Jean-Christophe Malapert
 */
public class CorotIdResolver extends AbstractNameResolver {

    private static final String credits = "IAS/CNES";
    private static final String serviceURL = "http://idoc-corotn2-public-v2.ias.u-psud.fr/exo_dset/records?en2_windescriptor%22&nocount=false&start=0&limit=1&filter[0][columnAlias]=corotid&filter[0][data][type]=numeric&filter[0][data][comparison]=eq&filter[0][data][value]=<corotid>&filter[1][columnAlias]=en2_windescriptor&filter[1][data][type]=boolean&filter[1][data][comparison]=eq&filter[1][data][value]=false&media=json";
    private JSONObject json;

    /**
     * Create a CorotId resolver
     * @param corotId Corot ID
     * @throws NameResolverException  
     */
    public CorotIdResolver(final String corotId) throws NameResolverException {
        String query = serviceURL.replace("<corotid>", corotId);
        process(query);
    }

    /**
     * Call the SITools2 service at IAS
     * @param query query
     * @throws IOException
     * @throws JSONException 
     */
    private void process(final String query) throws NameResolverException {
        Logger.getLogger(CorotIdResolver.class.getName()).log(Level.INFO, "Call IAS name resolver: {0}", query);
        ClientResourceProxy proxy = new ClientResourceProxy(query, Method.GET);
        ClientResource client = proxy.getClientResource();
        client.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "guest", "sitools2public"));
        Status status = client.getStatus();
        if (status.isSuccess()) {
            try {
                json = new JSONObject(client.get().getText());
            } catch (IOException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            } catch (JSONException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            } catch (ResourceException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            }
        } else {
            throw new NameResolverException(status, status.getThrowable());
        }
    }

    @Override
    public List<AstroCoordinate> getCoordinates(CoordinateSystem coordinateSystem) throws NameResolverException {
        try {
            List<AstroCoordinate> astroList = new ArrayList<AstroCoordinate>();
            JSONArray jsonArray = json.getJSONArray("data");

            if (jsonArray.length() != 1) {
                throw new NameResolverException(Status.SUCCESS_NO_CONTENT, "Not found");
            }

            JSONObject record = jsonArray.getJSONObject(0);
            double ra = Double.valueOf(record.getString("alpha_from"));
            double dec = Double.valueOf(record.getString("delta_from"));
            AstroCoordinate astro = new AstroCoordinate(ra, dec);
            processTransformation(astro, coordinateSystem);
            astroList.add(astro);
            return astroList;
        } catch (JSONException ex) {
            throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, "cannot parse IMMCE response", ex);
        }
    }

    @Override
    public Object getCompleteResponse() {
        return this.json;
    }

    @Override
    public String getCreditsName() {
        return credits;
    }
    private static final Logger LOG = Logger.getLogger(CorotIdResolver.class.getName());
}
