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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * This object contains methods to handle IMCCESsoResolver name resolver.<br/>
 * A IMCCESsoResolver allows you to get a sky position given both a time and a solar body name. 
 * @author Jean-Christophe Malapert
 */
public class IMCCESsoResolver extends AbstractNameResolver {

    private static final String hostResolverNameAtIMCCE = "http://vo.imcce.fr/webservices/ssodnet/resolver.php?name=<name>&mime=json&epoch=<epoch>&ephem=1&from=SITools2";
    private static final String creditsName = "IMCCE";
    private Object json;

    /**
     * Create a IMCCE SsoDNet based on time.
     * @param objectName object name to resolve
     * @param epoch epoch
     * @throws NameResolverException 
     */
    public IMCCESsoResolver(String objectName, String epoch) throws NameResolverException {
        processResponse(objectName, epoch);
    }

    /**
     * Call the service
     * @param objectName object name
     * @param epoch epoch
     * @throws NameResolverException 
     */
    private void processResponse(String objectName, String epoch) throws NameResolverException {
        // building the query
        String service = hostResolverNameAtIMCCE.replace("<name>", objectName);
        service = service.replace("<epoch>", epoch);
        Logger.getLogger(IMCCESsoResolver.class.getName()).log(Level.INFO, "Call IMCCE name resolver: {0}", service);
        
        //requesting
        ClientResourceProxy client = new ClientResourceProxy(service, Method.GET);
        ClientResource clientResource = client.getClientResource();
        Status status = clientResource.getStatus();
        
        // when the response is fine, we process the response
        if (status.isSuccess()) {
            String result;
            try {
                result = clientResource.get().getText();
            } catch (IOException ex) {                
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            } catch (ResourceException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            }
            try {
                json = new JSONObject(result);
            } catch (JSONException ex) {
                try {
                    json = new JSONArray(result);
                } catch (JSONException ex1) {                   
                    throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex1);
                }
            }
        } else {           
            throw new NameResolverException(status, status.getDescription());
        }
    }

    /**
     * Extract coordinates from the response
     * @param jsonObj response
     * @param coordinateSystem coordinates system
     * @return Point on sky
     * @throws JSONException
     * @throws NameResolverException 
     */
    private AstroCoordinate extractCoordinatesFromRecord(JSONObject jsonObj, CoordinateSystem coordinateSystem) throws JSONException,NameResolverException {        
        if(!jsonObj.has("type")) {
            throw new NameResolverException(Status.SUCCESS_NO_CONTENT, "Not found");
        }
        String type = jsonObj.getString("type");
        double ra = Double.valueOf(jsonObj.getString("ra"));
        double dec = Double.valueOf(jsonObj.getString("dec"));
        AstroCoordinate astroCoord = new AstroCoordinate(ra, dec);
        astroCoord.setType(type);
        processTransformation(astroCoord, coordinateSystem);
        return astroCoord;
    }

    @Override
    public List<AstroCoordinate> getCoordinates(CoordinateSystem coordinateSystem) throws NameResolverException {
        if (json instanceof JSONObject) {
            try {
                JSONObject jsonObj = (JSONObject) json;
                return Arrays.asList(extractCoordinatesFromRecord(jsonObj, coordinateSystem));
            } catch (JSONException ex) {
                throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
            }
        } else {
            JSONArray jsonArray = (JSONArray) json;
            List<AstroCoordinate> astroCoordinatesList = new ArrayList<AstroCoordinate>();
            int length = jsonArray.length();
            for (int i = 0; i < length; i++) {
                try {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);
                    astroCoordinatesList.add(extractCoordinatesFromRecord(jsonObj, coordinateSystem));
                } catch (JSONException ex) {
                    throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
                }
            }
            return astroCoordinatesList;
        }
    }

    @Override
    public Object getCompleteResponse() {
        return this.json;
    }

    @Override
    public String getCreditsName() {
        return IMCCESsoResolver.creditsName;
    }
    private static final Logger LOG = Logger.getLogger(IMCCESsoResolver.class.getName());
}
