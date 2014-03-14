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
package fr.cnes.sitools.extensions.astro.application.uws.representation;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import net.ivoa.xml.uws.v1.JobSummary;
import net.ivoa.xml.uws.v1.Jobs;
import net.ivoa.xml.uws.v1.ShortJobDescription;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.QNameMap;

import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;

/**
 * Representation for ShortJobDescription object.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @see ShortJobDescription
 */
public class JobsRepresentation extends fr.cnes.sitools.extensions.astro.application.uws.representation.AbstractXstreamRepresentation {

    /**
     * Constructor.
     * @param referenceIdentifier reference identifier
     * @param jobTasks List of jobs 
     * @param isUsedDestructionDate the destruction time is set
     * @throws ResourceException Internal Server Error (500)
     */
    public JobsRepresentation(final String referenceIdentifier, final Map<String, AbstractJobTask> jobTasks, final boolean isUsedDestructionDate) throws ResourceException {
        super();
        init(referenceIdentifier, jobTasks, isUsedDestructionDate);
    }

    /**
     * Constructor.
     * @param referenceIdentifier reference identifier
     * @param jobTasks list of jobs
     * @param isUsedDestructionDate the desctruction time is set
     * @param mediaType media Type
     * @throws ResourceException Internal Server Error (500)
     */
    public JobsRepresentation(final String referenceIdentifier, final Map<String, AbstractJobTask> jobTasks, final boolean isUsedDestructionDate, final MediaType mediaType) throws ResourceException {
        super(mediaType, null);
        init(referenceIdentifier, jobTasks, isUsedDestructionDate);
    }    

    /**
     * Constructor.
     * @param referenceIdentifier reference identifier
     * @param jobTasks list of jobs
     * @throws ResourceException Internal Server Error (500)
     */
    public JobsRepresentation(final String referenceIdentifier, final Map<String, AbstractJobTask> jobTasks) throws ResourceException{
        this(referenceIdentifier, jobTasks, false);
    }
    
    /**
     * Constructor.
     * @param referenceIdentifier reference identifier
     * @param jobTasks list of jobs
     * @param mediaType media type
     * @throws ResourceException Internal Server Error (500)
     */
    public JobsRepresentation(final String referenceIdentifier, final Map<String, AbstractJobTask> jobTasks, final MediaType mediaType) throws ResourceException{
        this(referenceIdentifier, jobTasks, false, mediaType);
    }    

    /**
     * Init
     * @param referenceIdentifier reference identifier
     * @param jobTasks list of jobs
     * @param isUsedDestructionDate descruction time is set
     */
    private void init(final String referenceIdentifier, final Map<String, AbstractJobTask> jobTasks, final boolean isUsedDestructionDate) {
        final Date currentDate = new Date();
        final Jobs jobs = new Jobs();
        final List<ShortJobDescription> shortJobsDescription = jobs.getJobref();
        final Map<String, AbstractJobTask> tmpJobTask = new HashMap<String, AbstractJobTask>(jobTasks);
        for (final Map.Entry<String, AbstractJobTask> entryAbstractJobTask : tmpJobTask.entrySet()) { 
            final String jobKey = entryAbstractJobTask.getKey();
            final AbstractJobTask jobTask = entryAbstractJobTask.getValue();

            if (isUsedDestructionDate) {
                try {
                    XMLGregorianCalendar calendar = null;
                    try {
                        calendar = fr.cnes.sitools.extensions.astro.application.uws.common.Util.convertIntoXMLGregorian(currentDate);
                    } catch (DatatypeConfigurationException ex) {
                        throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, "Cannot convert the current date into XML Gregorian");
                    }
                    final int val = calendar.compare(JobTaskManager.getInstance().getDestructionTime(jobTask));
                    if (val == DatatypeConstants.GREATER) {
                        JobTaskManager.getInstance().deleteTask(jobTask);
                        this.setObject(null);
                    } else {
                        final JobSummary jobSummary = jobTask.getJobSummary();
                        addJobDescription(referenceIdentifier, jobSummary, shortJobsDescription);
                    }
                } catch (UniversalWorkerException ex) {
                    throw new ResourceException(ex);
                }
            } else {
                final JobSummary jobSummary = jobTask.getJobSummary();
                addJobDescription(referenceIdentifier, jobSummary, shortJobsDescription);
            }
        }
        this.setObject(jobs);
        final XStream xstream = configureXStream();
        this.setXstream(xstream);
    }
    /**
     * Configures XStream.
     * @return xstream
     */
    protected final XStream configureXStream() {
        final QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://www.ivoa.net/xml/UWS/v1.0");
        qnm.setDefaultPrefix("uws");        
        createXstream(getMediaType(), qnm);
        final XStream xstream = getXstream();
        xstream.alias("jobs", Jobs.class);
        xstream.alias("jobref", ShortJobDescription.class);
        xstream.useAttributeFor(ShortJobDescription.class, "id");
        xstream.useAttributeFor(ShortJobDescription.class, "href");
        xstream.addImplicitCollection(Jobs.class, "jobref", ShortJobDescription.class);
        return xstream;
    }

    /**
     * Adds job dscription.
     * @param referenceIdentifier reference identifier
     * @param jobSummary job summary
     * @param shortJobsDescription short description
     */
    private void addJobDescription(final String referenceIdentifier, final JobSummary jobSummary, final List<ShortJobDescription> shortJobsDescription) {
        final ShortJobDescription shortJobDescription = new ShortJobDescription();
        shortJobDescription.setId(jobSummary.getJobId());
        shortJobDescription.setPhase(jobSummary.getPhase());
        shortJobDescription.setHref(referenceIdentifier + Constants.SLASH + jobSummary.getJobId());
        shortJobsDescription.add(shortJobDescription);
    }
}
