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

import net.ivoa.xml.uws.v1.ExecutionPhase;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.OptionInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.RequestInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.application.uws.representation.JobPhaseRepresentation;

/**
 * Resource to handle Phase resource.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class PhaseResource extends BaseJobResource {

    @Override
    public void doInit() throws ResourceException {
        super.doInit();
        setName("Phase Resource");
        setDescription("This resource handles job phase");
    }


    /**
     * Returns the Job's Phase.
     * <p>
     * Returns a HTTP Status 404 when JobID is unkown.
     * Returns a HTTP Status 500 for an Internal Server Error
     * </p>
     * @return the phase of the Job
     */
    @Get("plain")
    public Representation getPhase() {
        setStatus(Status.SUCCESS_OK);
        return new JobPhaseRepresentation(getJobTask(),true);
    }

    /**
     * Run or abort a job
     * The server returns a HTTP Status 303 (/{job-id}) when the operation is completed
     * @param form Parameters sent by a user
     * @exception Returns a HTTP Status 400 when sent parameters are wrong
     * @exception ResourceException Returns a HTTP Status 404 whenjobId is unkown
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1">HTTP
     * RFC - 10.4.1 400 Bad Request</a>
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.4">HTTP
     * RFC - 10.3.4 303 See Other</a>
     */
    @Post("form")
    public final void acceptPhase(final Form form) throws ResourceException {
        try {
            if (isValidAction(form)) {
                final Parameter parameter = form.get(0);
                if (parameter.getName().equals(Constants.PHASE)) {
                    if (parameter.getValue().equals(Constants.PHASE_RUN)) {
                            JobTaskManager.getInstance().runAsynchrone(getJobTask());
                            this.redirectToJobID();
                    } else if (parameter.getValue().equals(Constants.PHASE_ABORT)) {
                            JobTaskManager.getInstance().cancel(getJobTask());
                            this.redirectToJobID();
                    } else {
                        throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Bad request: PHASE=" + parameter.getValue() + " is not supported");
                    }
                } else {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Bad request: PHASE=" + parameter.getValue() + " is not supported");
                }
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "The parameters sent by the user are wrong");
            }
        } catch (UniversalWorkerException ex) {
           throw new ResourceException(ex.getStatus(), ex);
        }
    }

    /**
     * Checks whether the Phase action is allowed.
     * The Form object is valid when the Form object contains PHASE=RUN ou PHASE=ABORT
     * @param form Form send by a user
     * @return Returns True when the Form object is valid otherwhise False
     */
    protected final boolean isValidAction(final Form form) throws UniversalWorkerException {
        boolean isValid = false;

        if (form == null || form.size() > 1) {
            isValid = false;
        } else {
            final ExecutionPhase currentPhase = JobTaskManager.getInstance().getPhase(getJobTask());
            final Parameter param = form.get(0);
            isValid = (param.getName().equals(Constants.PHASE)
                    && (param.getValue().equals(Constants.PHASE_RUN)
                    ||param.getValue().equals(Constants.PHASE_ABORT))
                    && (currentPhase.equals(ExecutionPhase.PENDING)
                    || currentPhase.equals(ExecutionPhase.QUEUED)
                    || currentPhase.equals(ExecutionPhase.EXECUTING)));
        }

        return isValid;
    }

    @Override
    protected final Representation describe() {
        setName("Phase Resource");
        setDescription("This resource handles job phase");
        return super.describe();
    }

    @Override
    protected final void describeGet(final MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("Get job phase");

        ResponseInfo responseInfo = new ResponseInfo();
        final List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setXmlElement("xs:string");
        repInfo.setMediaType(MediaType.TEXT_PLAIN);
        final DocumentationInfo docInfo = new DocumentationInfo();
        docInfo.setTitle("ExecutionPhase");
        docInfo.setTextContent("Enumeration of possible phases of job execution");
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
        info.setDocumentation("Starting/Aborting the processing");

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.REDIRECTION_SEE_OTHER);
        responseInfo.setDocumentation("Redirects to /{job-id}");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
        responseInfo.setDocumentation("Only PHASE=RUN or PHASE=ABORT can be send by the client at condition that the current PHASE is not RUN or ABORT");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
        responseInfo.setDocumentation("Job does not exist");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        info.getResponses().add(responseInfo);

        final RequestInfo request = new RequestInfo();
        ParameterInfo param = new ParameterInfo();
        param.setName("PHASE");
        OptionInfo opt = new OptionInfo();
        opt.setValue("RUN");
        param.getOptions().add(opt);
        opt = new OptionInfo();
        opt.setValue("ABORT");
        param.getOptions().add(opt);
        param.setRequired(false);
        param.setType("xs:string");
        request.getParameters().add(param);

        param = new ParameterInfo();
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
