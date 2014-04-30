/**
 * *****************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * ****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.application.shortenerurl;

import java.util.ArrayList;
import java.util.List;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
//import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.RequestInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.cache.SingletonCacheShortnerURL;
import fr.cnes.sitools.extensions.common.Utility;
import java.io.IOException;
import org.codehaus.jackson.JsonNode;

/**
 * Resource that handles the shortener URL.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ShorteningResource extends SitoolsParameterizedResource {

    /**
     * ShortenerId that is sent by the client.
     */
    private String shortenerId;

    @Override
    public final void doInit() throws ResourceException {
        this.setShortenerId((String) getRequestAttributes().get("shortenerId"));
    }

    /**
     * Creates a shortener ID.
     * <p>
     * The entity must containt a <code>context</code> parameter.
     * </p>
     *
     * @param entity Entity coming form the user
     * @return the shortener ID
     * @throws ResourceException 400 Bad Request, when context is not provided
     * or when the conxtex value is not a valid Json.
     */
    @Post("form")
    public final Representation acceptJob(final Representation entity) throws ResourceException {
        final Form form = new Form(entity);
        final Parameter contextParameter = form.getFirst("context");
        checkContextParamExists(contextParameter);
        final String config = contextParameter.getValue();
        final String shortenerIdFromCache = SingletonCacheShortnerURL.putConfig(config);
        return new StringRepresentation(shortenerIdFromCache);
        //return new StringRepresentation(getReference().getIdentifier() + '/' + shortenerIdFromCache);
    }

    /**
     * Checks if the context parameter exists and if the JSON object is valid.
     *
     * @param contextParameter Context parameter
     */
    private void checkContextParamExists(final Parameter contextParameter) {
        if (Utility.isSet(contextParameter)) {
            try {
                Utility.mapper.readValue(contextParameter.getValue(), JsonNode.class);
            } catch (IOException ex) {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "the JSON Object is not valid.");
            }
        } else {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "context parameter must be provided");
        }
    }

    /**
     * Returns the configuration according to the shortenerID.
     *
     * @return the configuration according to the shortenerID
     * @throws ResourceException 404 status, when the shortenerID does not exist
     */
    @Get
    public final Representation returnConfig() throws ResourceException {
        try {
            return new StringRepresentation(SingletonCacheShortnerURL.getConfig(getShortenerId()), MediaType.APPLICATION_JSON);
        } catch (IllegalArgumentException ex) {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "This shortenerID does not exist");
        }
    }
//    /**
//     * Redirect to the bookmarked URL.
//     */
//    @Get
//    public final void redirecturl() {
//        final Redirector redirector = new Redirector(getContext(), SingletonCacheShortnerURL.getConfig(shortenerIdFromCache), Redirector.MODE_CLIENT_PERMANENT);
//        redirector.handle(getRequest(), getResponse());
//    }    

    /**
     * Returns the shortenerId.
     *
     * @return the shortenerId
     */
    public final String getShortenerId() {
        return shortenerId;
    }

    /**
     * Sets the shortener Id.
     *
     * @param shortenerIdVal the shortenerIdFromCache to set
     */
    private void setShortenerId(final String shortenerIdVal) {
        this.shortenerId = shortenerIdVal;
    }

    @Override
    protected final Representation describe() {
        setName("Shortenr Resource");
        setDescription("This resource handles shortener ID");
        return super.describe();
    }

    @Override
    protected final void describePost(final MethodInfo info) {
        info.setName(Method.POST);
        info.setDocumentation("Creates a shortener ID");

        final ParameterInfo param = new ParameterInfo();
        param.setName("context");
        param.setStyle(ParameterStyle.QUERY);
        param.setRequired(true);
        param.setType("xs:json");

        final RequestInfo request = new RequestInfo();
        request.getParameters().add(param);

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
        responseInfo.setDocumentation("context parameter does not exist or its value is not a valid JSON");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SUCCESS_OK);
        info.getResponses().add(responseInfo);

        super.describePost(info);
    }

    @Override
    protected final void describeGet(final MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("Get a JSON object from the cache according to the shortenerID Get a Job");

        ResponseInfo responseInfo = new ResponseInfo();
        final List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setMediaType(MediaType.APPLICATION_JSON);
        responseInfo.getStatuses().add(Status.SUCCESS_OK);
        responseInfo.setRepresentations(repsInfo);
        info.getResponses().add(responseInfo);
        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
        responseInfo.setDocumentation("shortener ID does not exist");
        info.getResponses().add(responseInfo);
        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        info.getResponses().add(responseInfo);

        final RequestInfo request = new RequestInfo();
        final ParameterInfo param = new ParameterInfo();
        param.setStyle(ParameterStyle.TEMPLATE);
        param.setName("shortenerId");
        param.setDocumentation("shortener ID value");
        param.setRequired(true);
        param.setType("xs:string");
        request.getParameters().add(param);
        info.setRequest(request);
    }
}
