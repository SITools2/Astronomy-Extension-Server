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
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.uws.representation.JobErrorRepresentation;

/**
 * Resource to handle errors.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ErrorResource extends BaseJobResource {

    @Override
    public final void doInit() throws ResourceException {
        super.doInit();
        setName("Error Resource");
        setDescription("This resource handles error happening during the job processing");        
    }

    /**
     * Returns the errors as a XML format.
     * <p>
     * a HTTP Status 404 when jobId is unknown or error is undefined.
     * a HTTP Status 500 for an Internal Server Error
     * </p>
     * @return error representation
     */
    @Get("xml")
    public final Representation getErrorToXML() {
        return new JobErrorRepresentation(this.getJobTask(), true);
    }
    
    @Override
    protected final Representation describe() {
        setName("Error Resource");
        setDescription("This resource handles error happening during the job processing");
        return super.describe();
    }

    @Override
    protected final void describeGet(final MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("Get the generated error");

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SUCCESS_OK);
        final List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setXmlElement("uws:ErrorSummary");
        repInfo.setMediaType(MediaType.TEXT_XML);
        final DocumentationInfo docInfo = new DocumentationInfo();
        docInfo.setTitle("ErrorSummary");
        docInfo.setTextContent("This is arbitrary information that can be added to the job description by the UWS implementation.");
        repInfo.setDocumentation(docInfo);
        repsInfo.add(repInfo);
        responseInfo.setRepresentations(repsInfo);
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);
        responseInfo.setDocumentation("Job does not exist or no error was happened");
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
}
