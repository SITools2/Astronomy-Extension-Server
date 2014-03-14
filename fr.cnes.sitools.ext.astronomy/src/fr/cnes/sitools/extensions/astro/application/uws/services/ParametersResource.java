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
import java.util.Iterator;
import java.util.List;

import net.ivoa.xml.uws.v1.ExecutionPhase;
import net.ivoa.xml.uws.v1.Parameter;
import net.ivoa.xml.uws.v1.Parameters;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.RequestInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.application.uws.representation.JobParametersRepresentation;

/**
 * Resource to handle Parameters
 * @author Jean-Chirstophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ParametersResource extends BaseJobResource {

    @Override
    public final void doInit() throws ResourceException {
        super.doInit();
        setName("Parameters Resource");
        setDescription("This resource handles job parameters");
    }


    /**
     * Returns parameters as XML format.
     * <p>
     * Returns a HTTP Status 404 when jobId is unknown.
     * Returns a HTTP Status 500 for an Internal Server Error
     * </p>
     * @return Returns Parameters representation
     */
    @Get("xml")
    public final Representation getParametersToXML() {
        return new JobParametersRepresentation(this.getJobTask(), true);
    }
    
    /**
     * Returns parameters as JSON format
     * <p>
     * Returns a HTTP Status 404 when jobId is unknown.
     * Returns a HTTP Status 500 for an Internal Server Error
     * </p>     
     * @return Returns Parameters representation
     */
    @Get("json")
    public final Representation getParametersToJSON() {
        return new JobParametersRepresentation(this.getJobTask(),true, MediaType.APPLICATION_JSON);
    }    

    /**
     * Sets the Parameters.
     * <p>
     * Returns a HTTP Status 400 when jobId is unknown.
     * Returns a HTTP Status 403 when the operation is not allowed.
     * Returns a HTTP Status 404 when form is not valid.
     * Returns a HTTP Status 500 for an Internal Server Error.
     * </p>
     * @param form Parameters to set through a form
     */
    @Post("form")
    public final void setParameters(final Form form) {
        try {
            if (isValidAction()) {
                final Parameters parameters = new Parameters();
                final Iterator<org.restlet.data.Parameter> iterParam = form.iterator();
                while (iterParam.hasNext()) {
                    final org.restlet.data.Parameter parameterData = iterParam.next();
                    final Parameter parameter = new Parameter();
                    parameter.setId(parameterData.getName());
                    if (parameterData.getValue().startsWith("http://")) {
                        parameter.setByReference(Boolean.TRUE);
                    } else {
                        parameter.setByReference(Boolean.FALSE);
                    }
                    parameter.setIsPost(Boolean.TRUE); //to do : check the meaning
                    parameter.setContent(parameterData.getValue());
                    parameters.getParameter().add(parameter);
                }
                JobTaskManager.getInstance().setParameters(getJobTask(), parameters);
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN, "Parameters cannot be set while the job is running");
            }
            this.redirectToJobID();
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex.getStatus(), ex);
        }
    }

    /**
     * Checks if the parameter can be get according to the phase.
     * @return <code>True</code> when the parameter can be get otherwise <code>false</code>
     * @throws UniversalWorkerException 
     */
    protected final boolean isValidAction() throws UniversalWorkerException {
        final ExecutionPhase phase = JobTaskManager.getInstance().getStatus(getJobTask());
        return !(phase.equals(ExecutionPhase.EXECUTING));
    }

    @Override
    protected final Representation describe() {
        setName("Parameters Resource");
        setDescription("This resource handles job parameters");
        return super.describe();
    }

    @Override
    protected final void describeGet(final MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("Get Job parameters");

        ResponseInfo responseInfo = new ResponseInfo();
        final List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setXmlElement("uws:parameters");
        repInfo.setMediaType(MediaType.TEXT_XML);
        final DocumentationInfo docInfo = new DocumentationInfo();
        docInfo.setTitle("Parameters");
        docInfo.setTextContent("the list of input parameters to the job - if the job description language does not naturally have parameters, then this list should contain one element which is the content of the original POST that created the job.");
        repInfo.setDocumentation(docInfo);
        repsInfo.add(repInfo);
        responseInfo.setRepresentations(repsInfo);
        responseInfo.getStatuses().add(Status.SUCCESS_OK);
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
        info.setDocumentation("Add new parameters");

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.REDIRECTION_SEE_OTHER);
        responseInfo.setDocumentation("Redirects to /{job-id}");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
        responseInfo.setDocumentation("Job does not exist");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_FORBIDDEN);
        responseInfo.setDocumentation("Parameters cannot be set while the job is running");
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

        super.describePost(info);
    }
}
