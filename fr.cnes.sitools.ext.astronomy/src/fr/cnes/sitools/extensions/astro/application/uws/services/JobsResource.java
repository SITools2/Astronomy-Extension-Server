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

import static fr.cnes.sitools.extensions.astro.application.uws.common.Util.isSet;

import java.util.ArrayList;
import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
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
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.application.uws.representation.JobsRepresentation;
import fr.cnes.sitools.xml.uws.v1.InputsType;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon;
import fr.cnes.sitools.xml.uws.v1.InputsType.Image;
import fr.cnes.sitools.xml.uws.v1.InputsType.Keyword;
import fr.cnes.sitools.xml.uws.v1.Job;

/**
 * Handles jobs.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class JobsResource extends BaseJobResource {

    /**
     * Initializes the resource.
     * @throws ResourceException when a problem occurs
     */
    @Override
    public final void doInit() throws ResourceException {
        super.doInit();         
        this.app = (UwsApplicationPlugin) getApplication();
        setName("Jobs Resource");
        setDescription("This resource contains the whole list of job for which the current date is inferior to destruction date");
    }
    
    /**
     * Returns the list of Jobs as JSON format.
     * @return the list of Jobs as JSON format
     * @throws ResourceException a HTTP Status 500 for an Internal Server Error
     */
    @Get("json")
    public final Representation getJobsToJSON() throws ResourceException {
        setStatus(Status.SUCCESS_OK);
        final Reference refTarget = new Reference(getReference().getPath());
        refTarget.setBaseRef(getSettings().getPublicHostDomain());         
        return new JobsRepresentation(refTarget.getTargetRef().getIdentifier(), JobTaskManager.getInstance().getJobTasks(), true, MediaType.APPLICATION_JSON);
    } 
    
    /**
     * Returns the list of jobs as XML format.
     * @return the JobSummary representation
     * @exception ResourceException a HTTP Status 500 for an Internal Server Error
     */
    @Get("xml")
    public final Representation getJobsToXML() throws ResourceException {
        setStatus(Status.SUCCESS_OK);
        final Reference refTarget = new Reference(getReference().getPath());
        refTarget.setBaseRef(getSettings().getPublicHostDomain());        
        return new JobsRepresentation(refTarget.getTargetRef().getIdentifier(), JobTaskManager.getInstance().getJobTasks(), true);
    }       

    /**
     * Creates a Job.
     * @param entity Parameters sent by a user
     * @exception ResourceException Returns a HTTP Status 400 when form is not valid
     * @exception ResourceException Returns a HTTP Status 500 for an Internal Server Error
     */
    @Post("form")
    public final void acceptJob(final Representation entity) throws ResourceException {
        try {
            final String jobTaskId = JobTaskManager.getInstance().createJobTask(this.app, entity);
            this.setRequestedJobId(jobTaskId);
            this.redirectToJobID();
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    protected final Representation describe() {
        setName("Jobs Resource");
        setDescription("This resource contains the whole list of job for which the current date is inferior to destruction date");
        return super.describe();
    }

    @Override
    protected final void describeGet(final MethodInfo info) {
        info.setName(Method.GET);
        info.setDocumentation("List all created jobs. The list may be empty if the UWS is idle");

        final List<RepresentationInfo> repsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setXmlElement("uws:ShortJobDescription");
        repInfo.setMediaType(MediaType.TEXT_XML);
        final DocumentationInfo docInfo = new DocumentationInfo();
        docInfo.setTitle("ShortJobDescription");
        docInfo.setTextContent("The representation of the Job List is a list of links to extant jobs. The list may be empty if the UWS is idle.");
        repInfo.setDocumentation(docInfo);
        repsInfo.add(repInfo);

        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SUCCESS_OK);
        responseInfo.setRepresentations(repsInfo);
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        responseInfo.setDocumentation("Internal server error - Please contact the administrator");
        info.getResponses().add(responseInfo);        
    }

    @Override
    protected final void describePost(final MethodInfo info) {
        info.setName(Method.POST);
        info.setDocumentation("POSTing a request to the Job List creates a new job (unless the service rejects the request)");

        // response
        ResponseInfo responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.REDIRECTION_SEE_OTHER);
        responseInfo.setDocumentation("Redirects to /{job-id}");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
        responseInfo.setDocumentation("only RUN value is accepted for PHASE keyword. only service parameters are accepted");
        info.getResponses().add(responseInfo);

        responseInfo = new ResponseInfo();
        responseInfo.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        info.getResponses().add(responseInfo);       

        // request
        final RequestInfo request = new RequestInfo();
        final ParameterInfo param = new ParameterInfo();
        param.setName("PHASE");
        param.setStyle(ParameterStyle.QUERY);
        param.setFixed("RUN");
        param.setRequired(false);
        param.setType("xs:string");
        param.setDocumentation("Starting a job after its submission");
        request.getParameters().add(param);
        for (ParameterInfo paramJob : describeInputJobTaskParam()) {
            request.getParameters().add(paramJob);   
        }        
        final RepresentationInfo repInfo = new RepresentationInfo();
        repInfo.setMediaType(MediaType.MULTIPART_FORM_DATA);
        request.getRepresentations().add(repInfo);

        info.setRequest(request);

        super.describePost(info);
    }
    
    private List<ParameterInfo> describeInputJobTaskParam() {
        final List<ParameterInfo> params = new ArrayList<ParameterInfo>();
        final Job job = AbstractJobTask.getCapabilities(this.app.getJobTaskImplementation());
        final InputsType inputs = job.getInputs();
        final Geometry geom = inputs.getGeometry();
        if (isSet(geom)) {
            final Circle circle = geom.getCircle();
            final Polygon polygon = geom.getPolygon();
            if (isSet(circle)) {
                final String longitude = circle.getLongitude().getName();
                ParameterInfo param = new ParameterInfo(longitude, true, "xs:double", ParameterStyle.QUERY, circle.getLongitude().getDocumentation());
                params.add(param);
                final String latitude = circle.getLatitude().getName();
                param = new ParameterInfo(latitude, true, "xs:double", ParameterStyle.QUERY, circle.getLatitude().getDocumentation());
                params.add(param);
                final String radius = circle.getRadius().getName();
                param = new ParameterInfo(radius, true, "xs:double", ParameterStyle.QUERY, circle.getRadius().getDocumentation());
                params.add(param);
            } else if (isSet(polygon)) {
                final String longitude1 = polygon.getLongitude1().getName();
                ParameterInfo param = new ParameterInfo(longitude1, true, "xs:double", ParameterStyle.QUERY, polygon.getLongitude1().getDocumentation());
                params.add(param);
                final String longitude2 = polygon.getLongitude2().getName();
                param = new ParameterInfo(longitude2, true, "xs:double", ParameterStyle.QUERY, polygon.getLongitude2().getDocumentation());
                params.add(param);
                final String longitude3 = polygon.getLongitude3().getName();
                param = new ParameterInfo(longitude3, true, "xs:double", ParameterStyle.QUERY, polygon.getLongitude3().getDocumentation());
                params.add(param);
                final String longitude4 = polygon.getLongitude4().getName();
                param = new ParameterInfo(longitude4, true, "xs:double", ParameterStyle.QUERY, polygon.getLongitude4().getDocumentation());
                params.add(param);                
                final String latitude1 = polygon.getLatitude1().getName();
                param = new ParameterInfo(latitude1, true, "xs:double", ParameterStyle.QUERY, polygon.getLatitude1().getDocumentation());
                params.add(param);
                final String latitude2 = polygon.getLatitude2().getName();
                param = new ParameterInfo(latitude2, true, "xs:double", ParameterStyle.QUERY, polygon.getLatitude2().getDocumentation());
                params.add(param);
                final String latitude3 = polygon.getLatitude3().getName();
                param = new ParameterInfo(latitude3, true, "xs:double", ParameterStyle.QUERY, polygon.getLatitude3().getDocumentation());
                params.add(param);
                final String latitude4 = polygon.getLatitude4().getName();
                param = new ParameterInfo(latitude4, true, "xs:double", ParameterStyle.QUERY, polygon.getLatitude4().getDocumentation());
                params.add(param);                
                final String rotation = polygon.getRotation().getName();
                param = new ParameterInfo(rotation, true, "xs:double", ParameterStyle.QUERY, polygon.getRotation().getDocumentation());
                params.add(param);                
            } else {
                final Healpix hpx = geom.getHealpix();
                final String order = hpx.getOrder().getName();
                ParameterInfo param = new ParameterInfo(order, true, "xs:int", ParameterStyle.QUERY, hpx.getOrder().getDocumentation());
                params.add(param);
                final String pixels = hpx.getPixels().getName();
                param = new ParameterInfo(pixels, true, "xs:long", ParameterStyle.QUERY, hpx.getPixels().getDocumentation());
                params.add(param);
            }
        }
        final List<Image> images = inputs.getImage();
        for (final Image image : images) {
            final String imageParam = image.getName();
            final ParameterInfo param = new ParameterInfo(imageParam, true, "xs:anyURI", ParameterStyle.QUERY, image.getDocumentation());
            params.add(param);
        }
        final List<Keyword> keywords = inputs.getKeyword();
        for (final Keyword keyword : keywords) {
            final String keywordParam = keyword.getName();
            final ParameterInfo param = new ParameterInfo(keywordParam, true, "xs:string", ParameterStyle.QUERY, keyword.getDocumentation());
            params.add(param);
        }
        return params;
    }
}
