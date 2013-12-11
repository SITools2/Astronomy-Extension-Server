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
package fr.cnes.sitools.extensions.astro.application.uws.jobmanager;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.common.application.ContextAttributes;
import fr.cnes.sitools.extensions.astro.application.UwsApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;
import fr.cnes.sitools.xml.uws.v1.Job;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.ivoa.xml.uws.v1.ErrorSummary;
import net.ivoa.xml.uws.v1.ExecutionPhase;
import net.ivoa.xml.uws.v1.JobSummary;
import net.ivoa.xml.uws.v1.JobSummary.JobInfo;
import net.ivoa.xml.uws.v1.Parameters;
import net.ivoa.xml.uws.v1.Results;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * The AbstractJobTask handles a specific JobTask and contains the needed
 * information to represent a JobSummary element.
 *
 * @author Jean-Christophe Malapert
 */
public abstract class AbstractJobTask implements JobTaskRunnable {

    private volatile Thread blinker;
    private String jobTaskId;
    private ExecutionPhase phase;
    private int executionDuration;
    private XMLGregorianCalendar destructionTime;
    private ErrorSummary error;
    private JAXBElement<XMLGregorianCalendar> quote;
    private Results results;
    private Parameters parameters;
    private XMLGregorianCalendar startTime;
    private XMLGregorianCalendar endTime;
    private String ownerId;
    private String storagePath;
    private JobInfo jobInfo;
    private String storagePublic;

    /**
     * Load dynamically a specific jobTask.
     *
     * @param app UWS application
     * @param jobTaskId Job task identifier
     * @param entity www-form-urlencoded form
     * @return Returns an instance of a specific jobTask
     * @throws UniversalWorkerException Returns an Internal error
     */
    public static AbstractJobTask create(final UwsApplicationPlugin app, final String jobTaskId, final Representation entity) throws UniversalWorkerException {
        AbstractJobTask jobTask = null;
        try {
            jobTask = (AbstractJobTask) Class.forName(app.getJobTaskImplementation()).newInstance();
            jobTask.doInit(app, jobTaskId, entity);
        } catch (InstantiationException ex) {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
        } catch (IllegalAccessException ex) {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
        } catch (ClassNotFoundException ex) {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
        }
        return jobTask;
    }

    /**
     * Init the specific JobTask.
     *
     * @param app Uws application
     * @param jobTaskId Job task identifier
     * @param entity www-form-urlencoded form
     * @throws UniversalWorkerException
     */
    protected final void doInit(final UwsApplicationPlugin app, final String jobTaskId, final Representation entity) throws UniversalWorkerException {
        final long delay = Long.parseLong(UwsApplicationPlugin.APP_DESTRUCTION_DELAY);
        final SitoolsSettings settings = (SitoolsSettings) app.getContext().getAttributes().get(ContextAttributes.SETTINGS);
        final String uwsAttachUrl = app.getAttachementRef();
        this.storagePublic = settings.getPublicHostDomain() + uwsAttachUrl;
        this.storagePath = app.getStorageDirectory();
        this.jobTaskId = jobTaskId;
        this.executionDuration = 0;
        try {
            this.destructionTime = Util.computeDestructionTime(new Date(), delay);
        } catch (DatatypeConfigurationException ex) {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
        }
        this.error = null;
        this.quote = null;
        this.results = new Results();
        try {
            this.startTime = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        } catch (DatatypeConfigurationException ex) {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
        }
        try {
            this.endTime = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        } catch (DatatypeConfigurationException ex) {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
        }
        this.ownerId = Constants.NO_OWNER;
        createUserSpace();
        final Form form = computeForm(entity);
        this.phase = setPhaseAtCreation(form);
        this.parameters = createParametersForJob(form, true);
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the form from a entity.
     * <p>
     * The entity must be either MULTIPART_FORM_DATA or APPLICATION_WWW_FORM.
     * </p>
     *
     * @param entity representation that is sent by the user
     * @return the form
     * @throws UniversalWorkerException This style of representation is not
     * implemented
     */
    private Form computeForm(final Representation entity) throws UniversalWorkerException {
        Form form = null;
        if (!Util.isSet(entity)) {
            return form;
        }
        if (MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
            try {
                form = uploadFile(entity);
            } catch (FileUploadException ex) {
                throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
            } catch (Exception ex) {
                throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
            }
        } else if (MediaType.APPLICATION_WWW_FORM.equals(entity.getMediaType(), true)) {
            form = new Form(entity);
        } else {
            throw new UniversalWorkerException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "This style of representation is not implemented");
        }
        doCheckForm(form);
        return form;
    }

    protected void doCheckForm(Form form) throws UniversalWorkerException {
    }

    /**
     * Set the Job phase at creation. By default, set job phase to PENDING. When
     * a user set PHASE=RUN, the job phase is set to QUEUED
     *
     * @param form www-form-urlencoded form
     * @return Returns the ExecutionPhase
     * @throws UniversalWorkerException only RUN value is accepted for PHASE
     * keyword
     */
    private ExecutionPhase setPhaseAtCreation(final Form form) throws UniversalWorkerException {
        if (Util.isSet(form) && Util.isSet(form.getFirst(Constants.PHASE))) {
            if (form.getFirst(Constants.PHASE).getValue().equals(Constants.PHASE_RUN)) {
                return ExecutionPhase.QUEUED;
            } else {
                throw new UniversalWorkerException(Status.CLIENT_ERROR_BAD_REQUEST, "only RUN value is accepted for PHASE keyword");
            }

        } else {
            return ExecutionPhase.PENDING;
        }
    }

    /**
     * Get job parameters from Job form. All parameters of the form are set
     * excepted for PHASE parameter
     *
     * @param form www-form-urlencoded form
     * @param isPost Set isPost=true for parameters sent by the user
     * @return Returns job parameters
     */
    private Parameters createParametersForJob(final Form form, final boolean isPost) {
        //TODO encoder les paramètres si ce sont des URLS
        final Parameters parametersUWS = new Parameters();
        if (form != null && !form.isEmpty()) {
            final Iterator<Parameter> iterParam = form.iterator();
            while (iterParam.hasNext()) {
                final Parameter param = iterParam.next();
                if (!param.getName().equals(Constants.PHASE)) {
                    final net.ivoa.xml.uws.v1.Parameter parameterUWS = new net.ivoa.xml.uws.v1.Parameter();
                    parameterUWS.setId(param.getName());
                    parameterUWS.setContent(Reference.decode(param.getValue()));
                    parametersUWS.getParameter().add(parameterUWS);
                    parameterUWS.setIsPost(isPost);
                    if (param.getValue().startsWith("http")) {
                        parameterUWS.setByReference(true);
                    }
                }
            }
        }
        return parametersUWS;
    }

    /**
     * Asynchronous run.
     */
    @Override
    public abstract void run();

    /**
     * Returns the get capabilities of the runnable job.
     *
     * @return the get capabilities of the runnable job
     */
    public abstract Job getCapabilities();

    /**
     * Returns the get Capabilities of the Runnable job.
     *
     * @param className runnable class
     * @return the get Capabilities of the Runnable job
     */
    public static final Job getCapabilities(final String className) {
        Job job = null;
        try {
            final Class c = Class.forName(className);
            final Object obj = c.newInstance();
            final Method m = c.getDeclaredMethod("getCapabilities", null);
            job = (Job) m.invoke(obj, null);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        return job;
    }

    /**
     * sets the Thread.
     *
     * @param blinker Thread
     */
    public void setBlinker(final Thread blinker) {
        this.blinker = blinker;
    }

    /**
     * Returns the JobSummary object.
     *
     * @return Returns the JobSummary object
     */
    public final JobSummary getJobSummary() {
        final JobSummary jobSummary = new JobSummary();
        jobSummary.setJobId(getJobTaskId());
        jobSummary.setPhase(getPhase());
        jobSummary.setExecutionDuration(getExecutionDuration());
        jobSummary.setDestruction(getDestructionTime());
        jobSummary.setStartTime(getStartTime());
        jobSummary.setEndTime(getEndTime());
        jobSummary.setParameters(getParameters());
        jobSummary.setOwnerId(getOwnerId());
        jobSummary.setResults(getResults());
        jobSummary.setErrorSummary(getError());
        jobSummary.setJobInfo(getJobInfo());
        return jobSummary;
    }

    /**
     * Cancel the thread.
     */
    public final void cancel() {
        final Thread tmpBlinker = blinker;
        blinker = null;
        if (tmpBlinker != null) {
            tmpBlinker.interrupt();
        }
        this.setPhase(ExecutionPhase.ABORTED);
    }

    /**
     * Returns the execution duration.
     *
     * @return Returns the executionDuraction
     */
    protected final int getExecutionDuration() {
        return executionDuration;
    }

    /**
     * Sets the execution duration and update the JobTaskManager
     *
     * @param executionDuraction the executionDuraction to set
     */
    protected final void setExecutionDuration(final int executionDuraction) {
        this.executionDuration = executionDuraction;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the Job task identifier.
     *
     * @return the job task indentifier
     */
    protected final String getJobTaskId() {
        return this.jobTaskId;
    }

    /**
     * Returns the job phase.
     *
     * @return the phase
     */
    protected final ExecutionPhase getPhase() {
        return phase;
    }

    /**
     * Sets the job phase and update the JobTaskManager when mustBeUpdated =
     * true.
     *
     * @param phaseVal Phase to set
     * @param mustBeUpdated true calls the JobTaskManager otherwise false
     * otherwise false
     */
    protected final void setPhase(final ExecutionPhase phaseVal, final boolean mustBeUpdated) {
        this.phase = phaseVal;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Sets the phase and updates the JobTaskManager.
     *
     * @param phaseVal phase
     */
    protected void setPhase(ExecutionPhase phaseVal) {
        this.phase = phaseVal;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the destruction time.
     *
     * @return Returns the destructionTime
     */
    protected final XMLGregorianCalendar getDestructionTime() {
        return destructionTime;
    }

    /**
     * Sets the destruction time and update the JobTaskManager.
     *
     * @param destructionTime the destructionTime to set
     * @param mustBeUpdated
     */
    protected void setDestructionTime(final XMLGregorianCalendar destructionTime, final boolean mustBeUpdated) {
        this.destructionTime = destructionTime;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Sets the destruction time and update the JobTaskManager
     *
     * @param destructionTime the destructionTime to set
     */
    protected void setDestructionTime(final XMLGregorianCalendar destructionTime) {
        this.destructionTime = destructionTime;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the error.
     *
     * @return the error
     */
    protected final ErrorSummary getError() {
        return error;
    }

    /**
     * Sets the error and update the JobTaskManager.
     *
     * @param error the error to set
     * @param mustBeUpdated
     */
    protected final void setError(final ErrorSummary error, final boolean mustBeUpdated) {
        this.error = error;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set the error and update the JobTaskManager.
     *
     * @param error the error to set
     */
    protected final void setError(final ErrorSummary error) {
        this.error = error;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    protected final JobInfo getJobInfo() {
        return this.jobInfo;
    }

    protected final void setJobInfo(final JobInfo jobInfo, final boolean mustBeUpdated) {
        this.jobInfo = jobInfo;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    protected final void setJobInfo(final JobInfo jobInfo) {
        this.jobInfo = jobInfo;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the quote.
     *
     * @return the quote
     */
    protected final JAXBElement<XMLGregorianCalendar> getQuote() {
        return quote;
    }

    /**
     * Sets the quote and update the JobTaskManager.
     *
     * @param quote the quote to set
     * @param mustBeUpdated
     */
    protected final void setQuote(final JAXBElement<XMLGregorianCalendar> quote, final boolean mustBeUpdated) {
        this.quote = quote;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Sets the quote and updates the JobTaskManager.
     *
     * @param quote the quote to set
     */
    protected final void setQuote(final JAXBElement<XMLGregorianCalendar> quote) {
        this.quote = quote;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the results.
     *
     * @return the results
     */
    protected final Results getResults() {
        return results;
    }

    /**
     * Sets the results and updates the JobTaskManager.
     *
     * @param results the results to set
     * @param mustBeUpdated
     */
    protected final void setResults(final Results results, final boolean mustBeUpdated) {
        this.results = results;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Sets results and update the JobTaskManager.
     *
     * @param results the results to set
     */
    protected final void setResults(final Results results) {
        this.results = results;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the parameters.
     *
     * @return the parameters The parameters to set
     */
    protected final Parameters getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters.
     *
     * @param parameters the parameters to set
     * @param mustBeUpdated
     */
    protected final void setParameters(final Parameters parameters, final boolean mustBeUpdated) {
        this.parameters = parameters;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Sets the parameters.
     *
     * @param parameters the parameters to set
     */
    protected final void setParameters(final Parameters parameters) {
        this.parameters = parameters;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the value of parameter name.
     *
     * @param parameterName Parameter name to find
     * @return Returns the value of the parameter name
     */
    protected final String getParameterValue(final String parameterName) {
        String valueParam = null;
        final Iterator<net.ivoa.xml.uws.v1.Parameter> iterParam = this.parameters.getParameter().iterator();
        while (iterParam.hasNext()) {
            final net.ivoa.xml.uws.v1.Parameter parameter = iterParam.next();
            if (parameter.getId().equals(parameterName)) {
                valueParam = parameter.getContent();
            }
        }
        return valueParam;
    }

    /**
     * Sets a value to a parameter name.
     *
     * @param key Key
     * @param value Value
     * @param mustBeUpdated
     */
    protected final void setParameterValue(final String key, final String value, final boolean mustBeUpdated) {
        final Iterator<net.ivoa.xml.uws.v1.Parameter> iterParam = this.parameters.getParameter().iterator();
        while (iterParam.hasNext()) {
            final net.ivoa.xml.uws.v1.Parameter parameter = iterParam.next();
            if (parameter.getId().equals(key)) {
                parameter.setContent(value);
            }
        }
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Sets a value to a parameter name.
     *
     * @param key Key
     * @param value Value
     */
    protected final void setParameterValue(final String key, final String value) {
        final Iterator<net.ivoa.xml.uws.v1.Parameter> iterParam = this.parameters.getParameter().iterator();
        while (iterParam.hasNext()) {
            final net.ivoa.xml.uws.v1.Parameter parameter = iterParam.next();
            if (parameter.getId().equals(key)) {
                parameter.setContent(value);
            }
        }
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the start time.
     *
     * @return the startTime
     */
    protected final XMLGregorianCalendar getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time.
     *
     * @param startTime the startTime to set
     */
    protected final void setStartTime(final XMLGregorianCalendar startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the end time.
     *
     * @return the endTime
     */
    protected final XMLGregorianCalendar getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time.
     *
     * @param endTime the endTime to set
     */
    protected final void setEndTime(final XMLGregorianCalendar endTime) {
        this.endTime = endTime;
    }

    /**
     * Returns the ownerID.
     *
     * @return the ownerId
     */
    protected final String getOwnerId() {
        return ownerId;
    }

    /**
     * Sets the owner ID.
     *
     * @param ownerId the ownerId to set
     * @param mustBeUpdated
     */
    protected final void setOwnerId(final String ownerId, final boolean mustBeUpdated) {
        this.ownerId = (ownerId == null) ? Constants.NO_OWNER : ownerId;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Sets the owner ID.
     *
     * @param ownerId the ownerId to set
     */
    protected final void setOwnerId(final String ownerId) {
        this.ownerId = (ownerId == null) ? Constants.NO_OWNER : ownerId;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Returns the Thread.
     *
     * @return the thread
     */
    protected final Thread getBlinker() {
        return this.blinker;
    }

    private Form uploadFile(final Representation rep) throws FileUploadException, Exception {

        // The Apache FileUpload project parses HTTP requests which
        // conform to RFC 1867, "Form-based File Upload in HTML". That
        // is, if an HTTP request is submitted using the POST method,
        // and with a content type of "multipart/form-data", then
        // FileUpload can parse that request, and get all uploaded files
        // as FileItem.
        // 1/ Create a factory for disk-based file items
        final DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(1000240);

        // 2/ Create a new file upload handler based on the Restlet
        // FileUpload extension that will parse Restlet requests and
        // generates FileItems.
        final RestletFileUpload upload = new RestletFileUpload(factory);
        List items;

        // 3/ Request is parsed by the handler which generates a
        // list of FileItems
        items = upload.parseRepresentation(rep);
        // Process only the uploaded item  and save it on disk
        final Form form = new Form();
        for (final Iterator it = items.iterator(); it.hasNext();) {
            final FileItem fi = (FileItem) it.next();
            if (fi.isFormField()) {
                form.add(fi.getFieldName(), fi.getString());
            } else {
                form.add(fi.getFieldName(), fi.getName());
                this.copyFile(fi);
            }
        }
        return form;
    }

    protected final void retrieveFile(final URI url, final File fileDestination) {
        final ClientResource client = new ClientResource(url);
        OutputStream os = null;
        if (client.getStatus().isSuccess()) {
            InputStream is = null;
            try {
                final Representation rep = client.get();
                is = rep.getStream();
                os = new FileOutputStream(fileDestination);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
                }
                try {
                    os.close();
                } catch (IOException ex) {
                    Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
                }
                client.release();
            }
        } else {
            client.release();
        }
    }

    protected final void moveFile(final File fileToCopy) throws UniversalWorkerException {
        final String uri = "riap://application/jobCache/" + jobTaskId + "/" + fileToCopy.getName();
        final ClientResource client = new ClientResource(uri);
        client.put(new OutputRepresentation(MediaType.ALL) {

            @Override
            public void write(final OutputStream outputStream) {
                FileInputStream fileOs = null;
                try {
                    fileOs = new FileInputStream(fileToCopy);
                    int c;
                    while ((c = fileOs.read()) != -1) {
                        outputStream.write(c);
                    }
                    fileOs.close();
                    outputStream.close();
                } catch (IOException ex) {
                    Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        if (fileOs != null) {
                            fileOs.close();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(AbstractJobTask.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        if (!client.getStatus().isSuccess()) {
            client.release();
            throw new UniversalWorkerException(client.getStatus(), "Cannot copy " + fileToCopy.getName());
        }
        client.release();
        if (!fileToCopy.delete()) {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, "Cannot delete " + fileToCopy.getName());
        }
    }

    protected final void copyFile(final FileItem fi) throws UniversalWorkerException {
        final String uri = "riap://application/jobCache/" + jobTaskId + "/" + fi.getName();
        final ClientResource client = new ClientResource(uri);
        client.put(fi.getString());
        client.release();
    }

    /**
     * Creates user space.
     *
     * @throws UniversalWorkerException
     */
    private void createUserSpace() throws UniversalWorkerException {
        final String uri = "riap://application/jobCache";
        final ClientResource client = new ClientResource(uri);
        client.post(jobTaskId);
        client.release();
    }

    /**
     * Deletes user space disk for a specific jobId
     *
     * @param jobTaskId
     * @throws UniversalWorkerException Returns an CLIENT_ERROR_BAD_REQUEST
     */
    protected final void deleteUserSpace() throws UniversalWorkerException {
        final String uri = "riap://application/jobCache/" + jobTaskId;
        final ClientResource client = new ClientResource(uri);
        client.delete();
        client.release();
    }

    /**
     * Returns the storage public URL.
     *
     * @return the storage public URL
     */
    protected final String getStoragePublicUrl() {
        return this.storagePublic + "/storage";
    }

    /**
     * Returns the storage path job.
     *
     * @return the storage path job
     */
    protected final String getStoragePathJob() {
        return this.storagePath + File.separator + getJobTaskId();
    }
}
