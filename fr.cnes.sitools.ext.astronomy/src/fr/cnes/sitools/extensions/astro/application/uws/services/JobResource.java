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

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.RequestInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.application.uws.representation.JobRepresentation;

/**
 * Resource to handle a submitted job.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class JobResource extends BaseJobResource {

    @Override
    public final void doInit() throws ResourceException {
        super.doInit();
        setName("Job Resource");
        setDescription("This resource handles a job");
    }

    /**
     * Returns a Job as XML format.
     * The server returns a HTTP Status 200 when the operation is completed
     * @return a JobSummary representation
     * @exception ResourceException a HTTP Status 404 when the jobId is not found
     */
    @Get("xml")
    public final Representation getJobToXML() throws ResourceException {
        Representation rep = null;
        setStatus(Status.SUCCESS_OK);
        rep = new JobRepresentation(this.getJobTask(), true);
        return rep;
    }

    /**
     * Returns a Job as JSON format.
     * The server returns a HTTP Status 200 when the operation is completed
     * @return Returns a JobSummary representation
     * @exception ResourceException a HTTP Status 404 when the jobId is not found
     */
    @Get("json")
    public final Representation getJobToJSON() throws ResourceException {
        Representation rep = null;
        setStatus(Status.SUCCESS_OK);        
        return new JobRepresentation(this.getJobTask(), true, MediaType.APPLICATION_JSON);
    }
    
    /**
     * Delete a job:
     *  - when ACTION=DELETE.
     *  - when the job is NOT running ot NOT queued
     * The server returns a HTTP Status 303 (/{job-id}) when the operation is completed
     * @param form Input parameters send by a user
     * @exception ResourceException Returns a HTTP Status 400 when the Form is not valid    
     * @exception ResourceException Returns a HTTP Status 404 when the jobId is not found
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1">HTTP
     * RFC - 10.4.1 400 Bad Request</a>
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.4">HTTP
     * RFC - 10.3.4 303 See Other</a>
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.4">HTTP
     * RFC - 10.4.4 403 Forbidden</a>
     */
    @Post("form")
    public final void deleteJobID(final Form form) throws ResourceException {
        if (isValidAction(form)) {
            this.deleteJobID();
        } else {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Bad request: only ACTION=DELETE is supported");
        }
    }

    /**
     * Delete sa job when the job is NOT running ot NOT queued.
     * The server returns a HTTP Status 303 (/{job-id}) when the operation is completed    
     * @exception ResourceException Returns a HTTP Status 404 when the jobId is not found
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.4">HTTP
     * RFC - 10.3.4 303 See Other</a>
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.4">HTTP
     * RFC - 10.4.4 403 Forbidden</a>
     */
    @Delete()
    public final void deleteJobID() throws ResourceException {
        try {
            JobTaskManager.getInstance().cancel(getJobTask());
        } catch (UniversalWorkerException ex) {
        }
        try {
            JobTaskManager.getInstance().deleteTask(getJobTask());
            this.redirectToJobs();
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex);
        }
    }

    /**
     * Checks whether the DELETE action is allowed.
     * <p>
     * The DELETE action is not allowed when ACTION=DELETE is not set in the Form object
     * </p>
     * @param form Form send by a user
     * @return <code>True</code> when the Form object is valid otherwhise <code>False</code>
     */
    protected boolean isValidAction(final Form form) {
        boolean isValid = false;
        if (form == null || form.size() > 1) {
            isValid = false;
        } else {
            final Parameter param = form.get(0);
            isValid = (param.getName().equals(Constants.ACTION) && param.getValue().equals(Constants.DELETE)) ? true : false;
        }
        return isValid;
    }

    @Override
    protected final Representation describe() {
        setName("Job Resource");
        setDescription("This resource handles a job");
        return super.describe();
    }

    @Override
    protected final void describeGet(final MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("Get a Job - The job is automatically destroyed when the currentDate is superior to destructionDate.");

        ResponseInfo responseInfo = new ResponseInfo();
        final List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setXmlElement("uws:JobSummary");
        repInfo.setMediaType(MediaType.TEXT_XML);
        final DocumentationInfo docInfo = new DocumentationInfo();
        docInfo.setTitle("JobSummary");
        docInfo.setTextContent("The complete representation of the state of a job");
        repInfo.setDocumentation(docInfo);
        repsInfo.add(repInfo);
        responseInfo.getStatuses().add(Status.SUCCESS_OK);
        responseInfo.setRepresentations(repsInfo);
        info.getResponses().add(responseInfo);
        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
        responseInfo.setDocumentation("Job does not exist");
        info.getResponses().add(responseInfo);
        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        info.getResponses().add(responseInfo);

        final RequestInfo request = new RequestInfo();
        final ParameterInfo param = new ParameterInfo();
        param.setStyle(ParameterStyle.TEMPLATE);
        param.setName("job-id");
        param.setDocumentation("job-id value");
        param.setRequired(true);
        param.setType("xs:string");
        request.getParameters().add(param);
        info.setRequest(request);
    }

    @Override
    protected final void describePost(final MethodInfo info) {
        info.setName(Method.POST);
        info.setDocumentation("Delete a job");

        ParameterInfo param = new ParameterInfo();
        param.setName("ACTION");
        param.setStyle(ParameterStyle.QUERY);
        param.setFixed("DELETE");
        param.setDocumentation("Deleting the job");
        param.setRequired(true);
        param.setType("xs:string");

        final RequestInfo request = new RequestInfo();
        request.getParameters().add(param);

        param = new ParameterInfo();
        param.setName("job-id");
        param.setDocumentation("job-id value");
        param.setStyle(ParameterStyle.TEMPLATE);
        param.setRequired(true);
        param.setType("xs:string");
        request.getParameters().add(param);
        info.setRequest(request);

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.REDIRECTION_SEE_OTHER);
        responseInfo.setDocumentation("Redirects to /");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
        responseInfo.setDocumentation("only ACTION=DELETE is supported");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
        responseInfo.setDocumentation("Job does not exist");

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        info.getResponses().add(responseInfo);

        super.describePost(info);
    }

    @Override
    protected final void describeDelete(final MethodInfo info) {
        info.setName(Method.DELETE);
        info.setDocumentation("Deleting a job");

        final RequestInfo request = new RequestInfo();
        final ParameterInfo param = new ParameterInfo();
        param.setName("job-id");
        param.setDocumentation("job-id value");
        param.setStyle(ParameterStyle.TEMPLATE);
        param.setRequired(true);
        param.setType("xs:string");
        request.getParameters().add(param);
        info.setRequest(request);

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.REDIRECTION_SEE_OTHER);
        responseInfo.setDocumentation("Redirects to /");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
        responseInfo.setDocumentation("job does not exist");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        info.getResponses().add(responseInfo);

        super.describeDelete(info);
    }
}
