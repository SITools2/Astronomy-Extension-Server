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
package fr.cnes.sitools.extensions.astro.application.uws.services;

import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.application.uws.representation.JobParametersRepresentation;
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

/**
 * Resource to handle Parameters
 * @author Jean-Chirstophe Malapert
 */
public class ParametersResource extends BaseJobResource {

    @Override
    public void doInit() throws ResourceException {
        super.doInit();
        setName("Parameters Resource");
        setDescription("This resource handles job parameters");
    }


    /**
     * Get parameters
     * @return Returns Parameters representation
     * @exception ResourceException Returns a HTTP Status 404 when jobId is unknown
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     */
    @Get("xml")
    public Representation getParametersToXML() {
        return new JobParametersRepresentation(this.getJobTask(),true);
    }
    
    /**
     * Get parameters
     * @return Returns Parameters representation
     * @exception ResourceException Returns a HTTP Status 404 when jobId is unknown
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     */
    @Get("json")
    public Representation getParametersToJSON() {
        return new JobParametersRepresentation(this.getJobTask(),true, MediaType.APPLICATION_JSON);
    }    

    /**
     * Set Parameters
     * @param form Parameters to set through a form
     * @exception ResourceException Returns a HTTP Status 400 when jobId is unknown
     * @exception ResourceException Returns a HTTP Status 403 when the operation is not allowed
     * @exception ResourceException Returns a HTTP Status 404 when form is not valid
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     */
    @Post("form")
    public void setParameters(Form form) {
        try {
            if (isValidAction()) {
                Parameters parameters = new Parameters();
                Iterator<org.restlet.data.Parameter> iterParam = form.iterator();
                while (iterParam.hasNext()) {
                    org.restlet.data.Parameter parameterData = iterParam.next();
                    Parameter parameter = new Parameter();
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
            throw new ResourceException(ex.getStatus(),ex.getMessage(),ex.getCause());
        }
    }

    protected final boolean isValidAction() throws UniversalWorkerException {
        ExecutionPhase phase = JobTaskManager.getInstance().getStatus(getJobTask());
        return (phase.equals(phase.EXECUTING)) ? false : true;
    }

    @Override
    protected Representation describe() {
        setName("Parameters Resource");
        setDescription("This resource handles job parameters");
        return super.describe();
    }

    @Override
    protected void describeGet(MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("Get Job parameters");

        ResponseInfo responseInfo = new ResponseInfo();
        List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setXmlElement("uws:parameters");
        repInfo.setMediaType(MediaType.TEXT_XML);
        DocumentationInfo docInfo = new DocumentationInfo();
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

        RequestInfo request = new RequestInfo();
        ParameterInfo param = new ParameterInfo();
        param.setStyle(ParameterStyle.TEMPLATE);
        param.setName("job-id");
        param.setDocumentation("job-id value");
        param.setRequired(true);
        param.setType("xs:string");
        request.getParameters().add(param);
        info.setRequest(request);
    }

    @Override
    protected void describePost(MethodInfo info) {
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

        RequestInfo request = new RequestInfo();
        ParameterInfo param = new ParameterInfo();
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
