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
package fr.cnes.sitools.extensions.astro.application.uws.representation;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import net.ivoa.xml.uws.v1.JobSummary;
import net.ivoa.xml.uws.v1.Jobs;
import net.ivoa.xml.uws.v1.ShortJobDescription;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * Representation for ShortJobDescription object
 * @author Jean-Christophe Malapert
 * @see ShortJobDescription
 */
public class JobsRepresentation extends fr.cnes.sitools.extensions.astro.application.uws.representation.XstreamRepresentation {

    /**
     * Jobs representation
     * @param referenceIdentifier
     * @param jobTasks JobTask
     * @param isUsedDestructionDate
     * @throws ResourceException Internal Server Error (500)
     */
    public JobsRepresentation(String referenceIdentifier, Map<String, AbstractJobTask> jobTasks, boolean isUsedDestructionDate) throws ResourceException {
        super();
        init(referenceIdentifier, jobTasks, isUsedDestructionDate);
    }

    /**
     *
     * @param referenceIdentifier
     * @param jobTasks
     * @throws ResourceException Internal Server Error (500)
     */
    public JobsRepresentation(String referenceIdentifier, Map<String, AbstractJobTask> jobTasks) throws ResourceException{
        this(referenceIdentifier, jobTasks, false);
    }

    private void init(String referenceIdentifier, Map<String, AbstractJobTask> jobTasks, boolean isUsedDestructionDate) {
        Date currentDate = new Date();
        Jobs jobs = new Jobs();
        List<ShortJobDescription> shortJobsDescription = jobs.getJobref();
        Iterator<String> iterJobTasks = jobTasks.keySet().iterator();
        while (iterJobTasks.hasNext()) {
            String jobKey = iterJobTasks.next();
            AbstractJobTask jobTask = jobTasks.get(jobKey);

            if (isUsedDestructionDate) {
                try {
                    XMLGregorianCalendar calendar = null;
                    try {
                        calendar = fr.cnes.sitools.extensions.astro.application.uws.common.Util.convertIntoXMLGregorian(currentDate);
                    } catch (DatatypeConfigurationException ex) {
                        throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, "Cannot convert the current date into XML Gregorian");
                    }
                    int val = calendar.compare(JobTaskManager.getInstance().getDestructionTime(jobTask));
                    if (val == DatatypeConstants.GREATER) {
                        JobTaskManager.getInstance().deleteTask(jobTask);
                        this.setObject(null);
                    } else {
                        JobSummary jobSummary = jobTask.getJobSummary();
                        addJobDescription(referenceIdentifier, jobSummary, shortJobsDescription);
                    }
                } catch (UniversalWorkerException ex) {
                    throw new ResourceException(ex.getStatus(),ex.getMessage(),ex.getCause());
                }
            } else {
                JobSummary jobSummary = jobTask.getJobSummary();
                addJobDescription(referenceIdentifier, jobSummary, shortJobsDescription);
            }
        }
        this.setObject(jobs);
        XStream xstream = configureXStream();
        this.setXstream(xstream);
    }

    protected XStream configureXStream() {
        QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://www.ivoa.net/xml/UWS/v1.0");
        qnm.setDefaultPrefix("uws");
        XStream xstream = new XStream(new StaxDriver(qnm));
        xstream.alias("jobs", Jobs.class);
        xstream.alias("jobref", ShortJobDescription.class);
        xstream.useAttributeFor(ShortJobDescription.class, "id");
        xstream.useAttributeFor(ShortJobDescription.class, "href");
        xstream.addImplicitCollection(Jobs.class, "jobref", ShortJobDescription.class);
        return xstream;
    }

    protected String fixXStreamBug(String representation) {
        representation = representation.replaceFirst("uws:jobs xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\"", "uws:jobs xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/UWS/v1.0 http://ivoa.net/xml/UWS/UWS-v1.0.xsd\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
        return representation.replaceAll("href=", "xlink:href=");
    }

    private void addJobDescription(String referenceIdentifier, JobSummary jobSummary, List<ShortJobDescription> shortJobsDescription) {
        ShortJobDescription shortJobDescription = new ShortJobDescription();
        shortJobDescription.setId(jobSummary.getJobId());
        shortJobDescription.setPhase(jobSummary.getPhase());
        shortJobDescription.setHref(referenceIdentifier + Constants.SLASH + jobSummary.getJobId());
        shortJobsDescription.add(shortJobDescription);
    }
}
