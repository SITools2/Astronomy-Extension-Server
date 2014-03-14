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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import net.ivoa.xml.uws.v1.ExecutionPhase;

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
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.UwsApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.application.uws.representation.JobDestructionTimeRepresentation;

/**
 * Resource to handle the destruction time.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class DestructionResource extends BaseJobResource {

    @Override
    public final void doInit() throws ResourceException {
        super.doInit();
        final CopyOnWriteArraySet<Method> allowedMethods = new CopyOnWriteArraySet<Method>();
        allowedMethods.add(Method.GET);
        if (((UwsApplicationPlugin) getApplication()).isAllowedExecutionTimePostMethod()) {
            allowedMethods.add(Method.POST);
        }
        setAllowedMethods(allowedMethods);
        setName("Destruction Resource");
        setDescription("This resource handles the destruction time of the job");
    }

    /**
     * Returns the destruction Time.
     * In the case where a call is done after the destruction time, the job is destroyed.
     * @return Return the destruction as ISO8601 format
     * @exception ResourceException Returns a HTTP Status 404 when job-id is unknown
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     */
    @Get("plain")
    public final Representation getDestructionTime() throws ResourceException {
        return new JobDestructionTimeRepresentation(getJobTask(), true);
    }

    /**
     * Sets the destruction time. The destruction must have a ISO8601 format.
     * @param form The form contains must only contain a DESTRUCTION element
     * The server returns a HTTP Status 303 (/{job-id}) when the operation is completed
     * @exception ResourceException Returns a HTTP Status 400 when the Form is not valid
     * @exception ResourceException Returns a HTTP Status 404 when job-id is unknown
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     */
    @Post("form")
    public final void acceptDestructionTime(final Form form) throws ResourceException {
        final Set<Method> methods = getAllowedMethods();
        if (!methods.contains(Method.POST)) {
            throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
        }
        if (isValidAction(form)) {
            final Parameter parameter = form.get(0);
            final String val = parameter.getValue();
            XMLGregorianCalendar calendar = null;
            try {
                calendar = Util.convertIntoXMLGregorian(val);
            } catch (DatatypeConfigurationException ex) {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "The destruction time parameter must be as ISO8601 format");
            } catch (ParseException ex) {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "The destruction time parameter must be as ISO8601 format");
            }
            if (calendar.isValid()) {
                try {
                    JobTaskManager.getInstance().setDestructionTime(getJobTask(), calendar);
                    this.redirectToJobID();
                } catch (UniversalWorkerException ex) {
                    throw new ResourceException(ex.getStatus(), ex.getMessage(), ex.getCause());
                }
            } else {
                throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Bad request: destruction time parameter is wrong");
            }
        } else {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    /**
     * Checks if an action occurs when the server state is PENDING.
     * @return true when the server state is pending otherwise false
     * @exception UniversalWorkerException
     */
    protected final boolean isValidAction() throws UniversalWorkerException {
        final ExecutionPhase phase = JobTaskManager.getInstance().getStatus(getJobTask());
        return phase.equals(ExecutionPhase.PENDING);
    }

    /**
     * Checks if the form is valid.
     * @param form Form
     * @return true when both destruction time is set and the action is during a PENDING phase
     */
    protected final boolean isValidAction(final Form form) {
        try {
            boolean isValid = false;
            if (!isValidAction()) {
                isValid = false;
            } else {
                if (form == null || form.size() > 1) {
                    isValid = false;
                } else {
                    final Parameter param = form.get(0);
                    isValid = param.getName().equals(Constants.DESTRUCTION);
                }
            }
            return isValid;
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    protected final Representation describe() {
        setName("Destruction Resource");
        setDescription("This resource handles the destruction time of the job");
        return super.describe();
    }

    @Override
    protected final void describeGet(final MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("Get the destruction time as ISO8601 format");

        ResponseInfo responseInfo = new ResponseInfo();
        final List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setXmlElement("xs:dateTime");
        repInfo.setMediaType(MediaType.TEXT_PLAIN);
        final DocumentationInfo docInfo = new DocumentationInfo();
        docInfo.setTitle("Destruction");
        docInfo.setTextContent("The time at which the whole job + records + results will be destroyed.");
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
        info.setDocumentation("Changing the Destruction Time");

        ResponseInfo responseInfo = null;
        if (((UwsApplicationPlugin) getApplication()).isAllowedExecutionTimePostMethod()) {
            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.REDIRECTION_SEE_OTHER);
            responseInfo.setDocumentation("Redirects to /{job-id}");
            info.getResponses().add(responseInfo);
            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
            responseInfo.setDocumentation("Job does not exist");
            info.getResponses().add(responseInfo);
            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
            responseInfo.setDocumentation("The format of the destructionTime parameter is not valid");
            info.getResponses().add(responseInfo);
            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
            info.getResponses().add(responseInfo);
        } else {
            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.CLIENT_ERROR_FORBIDDEN);
            responseInfo.setDocumentation("The value cannot be changed by the client");
            info.getResponses().add(responseInfo);
            responseInfo = new ResponseInfo();
            responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
            info.getResponses().add(responseInfo);
        }

        RequestInfo request = new RequestInfo();
        ParameterInfo param = new ParameterInfo();
        param.setName("DESTRUCTION");
        param.setRequired(true);
        param.setType("xs:string");
        request.getParameters().add(param);
        param = new ParameterInfo();
        param.setStyle(ParameterStyle.TEMPLATE);
        param.setName("job-id");
        param.setDocumentation("job-id value");
        param.setRequired(true);
        param.setType("xs:date");
        request.getParameters().add(param);
        info.setRequest(request);

        super.describePost(info);
    }
}
