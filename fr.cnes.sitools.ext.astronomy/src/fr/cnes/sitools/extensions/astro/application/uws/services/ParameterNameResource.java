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
package fr.cnes.sitools.extensions.astro.application.uws.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import net.ivoa.xml.uws.v1.ExecutionPhase;

import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.RequestInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.UwsApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.application.uws.representation.JobParameterNameRepresentation;

/**
 * Resource to handle ParameterName.
 * @author Jean-Chirstophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ParameterNameResource extends BaseJobResource {

    /**
     * Parameter name to retrieve.
     */
    private String parameterNameId = null;

    @Override
    public final void doInit() throws ResourceException {
        super.doInit();
        this.parameterNameId = (String) getRequestAttributes().get("parameter-name");
        final CopyOnWriteArraySet<Method> allowedMethods = new CopyOnWriteArraySet<Method>();
        allowedMethods.add(Method.GET);
        if (((UwsApplicationPlugin) getApplication()).isAllowedParameterNamePutMethod()) {
            allowedMethods.add(Method.PUT);
        }
        setAllowedMethods(allowedMethods);
        setName("ParameterName Resource");
        setDescription("This resource handles parameter value");
    }

    /**
     * Returns the parameter value.
     * <p>
     * a HTTP Status 404 when the jobId or parameter is not found
     * a HTTP Status 500 for an Internal Server Error
     * </p>
     * @return the value.
     */
    @Get("plain")
    public Representation getParameterValue() {
        return new JobParameterNameRepresentation(getJobTask(), getParameterNameId(), true);
    }

    /**
     * Sets a value for a specific parameter.
     * <p>
     * a HTTP Status 400 when the key is wrong
     * a HTTP Status 404 when the jobId or parameter is not found
     * a HTTP Status 500 for an Internal Server Error
     * </p> 
     * @param parameterValue the parameter value to update
     */
    @Put("plain")
    public final void updateParameterValue(final String parameterValue) {
        if (isValidAction()) {
            try {
                JobTaskManager.getInstance().setParameter(getJobTask(), getParameterNameId(), parameterValue);
            } catch (UniversalWorkerException ex) {
                throw new ResourceException(ex.getStatus(), ex);
            }
        } else {
            throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "A parameter value cannot be set while the job is running");
        }
        this.redirectToJobID();
    }

    /**
     * Returns the parameter name.
     * @return the parameter name
     */
    protected final String getParameterNameId() {
        return this.parameterNameId;
    }

    /**
     * Checks if it is possible to update a parameter according to he phase.
     * @return <code>True<code> when it is possible to update a parameter otherwise <code>false</code>
     */
    protected final boolean isValidAction() {
        try {
            final ExecutionPhase phase = JobTaskManager.getInstance().getStatus(getJobTask());
            return !(phase.equals(ExecutionPhase.EXECUTING));
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex.getStatus(), ex);
        }
    }

    @Override
    protected final Representation describe() {
        setName("ParameterName Resource");
        setDescription("This resource handles parameter value");
        return super.describe();
    }

    @Override
    protected final void describeGet(final MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("Get a parameter name");

        ResponseInfo responseInfo = new ResponseInfo();
        final List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setXmlElement("xs:string");
        repInfo.setMediaType(MediaType.TEXT_PLAIN);
        repsInfo.add(repInfo);
        responseInfo.setRepresentations(repsInfo);
        responseInfo.getStatuses().add(Status.SUCCESS_OK);
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
        responseInfo.setDocumentation("job does not exist");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        info.getResponses().add(responseInfo);
    }

    @Override
    protected final void describePut(final MethodInfo info) {
        info.setName(Method.PUT);
        info.setDocumentation("Change a parameter value");

        ResponseInfo responseInfo = null;
        if (((UwsApplicationPlugin) getApplication()).isAllowedParameterNamePutMethod()) {
            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.REDIRECTION_SEE_OTHER);
            responseInfo.setDocumentation("Redirects to /{job-id}");
            info.getResponses().add(responseInfo);

            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
            responseInfo.setDocumentation("The key does not exist");
            info.getResponses().add(responseInfo);

            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
            responseInfo.setDocumentation("Job does not exist");
            info.getResponses().add(responseInfo);

            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.CLIENT_ERROR_FORBIDDEN);
            responseInfo.setDocumentation("A parameter value cannot be set while the job is running");
            info.getResponses().add(responseInfo);

            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
            info.getResponses().add(responseInfo);

            final RequestInfo request = new RequestInfo();
            request.setDocumentation("Value to set");
            final RepresentationInfo repInfo = new RepresentationInfo();
            repInfo.setMediaType(MediaType.TEXT_PLAIN);
            request.getRepresentations().add(repInfo);
            info.setRequest(request);
        } else {
            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.CLIENT_ERROR_FORBIDDEN);
            responseInfo.setDocumentation("The client is not allowed to change the parameter value");
            info.getResponses().add(responseInfo);

            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
            info.getResponses().add(responseInfo);
        }
    }
}
