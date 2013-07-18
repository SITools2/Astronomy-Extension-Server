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
package fr.cnes.sitools.extensions.astro.uws.jobmanager;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.common.application.ContextAttributes;
import fr.cnes.sitools.extensions.astro.uws.UwsApplicationPlugin;
import fr.cnes.sitools.extensions.astro.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.uws.common.Util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
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
    //private String storageMgtPath;
    private JobInfo jobInfo;
    private String storagePublic;    
    
    /**
     * Load dynamically a specific jobTask.
     *
     * @param className Class name of the specific jobTask
     * @param context Context
     * @param jobTaskId Job task identifier
     * @param form www-form-urlencoded form
     * @return Returns an instance of a specific jobTask
     * @throws UniversalWorkerException Returns an Internal error
     */
    public static AbstractJobTask create(String className, Context context, String jobTaskId, Representation entity) throws UniversalWorkerException {
        AbstractJobTask jobTask = null;
        try {
            jobTask = (AbstractJobTask) Class.forName(className).newInstance();
            jobTask.doInit(context, jobTaskId, entity);
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
     * Init the specific JobTask
     *
     * @param context Context
     * @param jobTaskId Job task identifier
     * @param form www-form-urlencoded form
     * @throws UniversalWorkerException
     */
    protected void doInit(Context context, String jobTaskId, Representation entity) throws UniversalWorkerException {
        final long delay = Long.parseLong(UwsApplicationPlugin.APP_DESTRUCTION_DELAY);
        //this.storageMgtPath = UwsApplicationPlugin.APP_URL_STORAGE_MANAGEMENT;
        SitoolsSettings settings = (SitoolsSettings) context.getAttributes().get(ContextAttributes.SETTINGS);
        String uwsAttachUrl = (String) context.getAttributes().get(UwsApplicationPlugin.APP_URL_UWS_SERVICE);
        this.storagePublic = settings.getPublicHostDomain() + uwsAttachUrl;
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
        createUserSpace(context);
        Form form = computeForm(entity);
        this.phase = setPhaseAtCreation(form);
        this.parameters = createParametersForJob(form, true);
        JobTaskManager.getInstance().updateJobTask(this);
    }

    private Form computeForm(Representation entity) throws UniversalWorkerException {
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
     */
    private ExecutionPhase setPhaseAtCreation(Form form) throws UniversalWorkerException {
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
    private Parameters createParametersForJob(Form form, boolean isPost) {
        //TODO encoder les paramètres si ce sont des URLS
        Parameters parametersUWS = new Parameters();
        if (form != null && !form.isEmpty()) {
            Iterator<Parameter> iterParam = form.iterator();
            while (iterParam.hasNext()) {
                Parameter param = iterParam.next();
                if (!param.getName().equals(Constants.PHASE)) {
                    net.ivoa.xml.uws.v1.Parameter parameterUWS = new net.ivoa.xml.uws.v1.Parameter();
                    parameterUWS.setId(param.getName());
                    parameterUWS.setContent(param.getValue());
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
     * Asynchronous run
     */
    public abstract void run();

    /**
     * set Thread
     *
     * @param blinker Thread
     */
    public void setBlinker(Thread blinker) {
        this.blinker = blinker;
    }
//    public void run() {
//        this.blinker = Thread.currentThread();
//        setPhase(ExecutionPhase.EXECUTING);
//        JobTaskManager.getInstance().updateJobTask(this);
//    }

    /**
     * Get JobSummary object
     *
     * @return Returns JobSummary object
     */
    public final JobSummary getJobSummary() {
        JobSummary jobSummary = new JobSummary();
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
     * Cancel the thread
     */
    public void cancel() {
        Thread tmpBlinker = blinker;
        blinker = null;
        if (tmpBlinker != null) {
            tmpBlinker.interrupt();
        }
        this.setPhase(ExecutionPhase.ABORTED);
    }

    /**
     * Get execution duration
     *
     * @return Returns the executionDuraction
     */
    protected int getExecutionDuration() {
        return executionDuration;
    }

    /**
     * Set the execution duration and update the JobTaskManager
     *
     * @param executionDuraction the executionDuraction to set
     */
    protected void setExecutionDuration(int executionDuraction) {
        this.executionDuration = executionDuraction;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get Job task identifier
     *
     * @return Returns the job task indentifier
     */
    protected String getJobTaskId() {
        return this.jobTaskId;
    }

    /**
     * Get job phase
     *
     * @return Returns the phase
     */
    protected ExecutionPhase getPhase() {
        return phase;
    }

    /**
     * Set the job phase and update the JobTaskManager when mustBeUpdated = true
     *
     * @param phase Phase to set
     * @param mustBeUpdated true calls the JobTaskManager otherwise false
     * otherwise false
     */
    protected void setPhase(ExecutionPhase phase, boolean mustBeUpdated) {
        this.phase = phase;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set the phase and update the JobTaskManager
     *
     * @param phase
     */
    protected void setPhase(ExecutionPhase phase) {
        this.phase = phase;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get destruction time
     *
     * @return Returns the destructionTime
     */
    protected XMLGregorianCalendar getDestructionTime() {
        return destructionTime;
    }

    /**
     * Set the destruction time and update the JobTaskManager
     *
     * @param destructionTime the destructionTime to set
     * @param mustBeUpdated
     */
    protected void setDestructionTime(XMLGregorianCalendar destructionTime, boolean mustBeUpdated) {
        this.destructionTime = destructionTime;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set the destruction time and update the JobTaskManager
     *
     * @param destructionTime the destructionTime to set
     */
    protected void setDestructionTime(XMLGregorianCalendar destructionTime) {
        this.destructionTime = destructionTime;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get error
     *
     * @return Returns the error
     */
    protected ErrorSummary getError() {
        return error;
    }

    /**
     * Set the error and update the JobTaskManager
     *
     * @param error the error to set
     * @param mustBeUpdated
     */
    protected void setError(ErrorSummary error, boolean mustBeUpdated) {
        this.error = error;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set the error and update the JobTaskManager
     *
     * @param error the error to set
     */
    protected void setError(ErrorSummary error) {
        this.error = error;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    protected JobInfo getJobInfo() {
        return this.jobInfo;
    }

    protected void setJobInfo(JobInfo jobInfo, boolean mustBeUpdated) {
        this.jobInfo = jobInfo;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    protected void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get the quote
     *
     * @return the quote
     */
    protected JAXBElement<XMLGregorianCalendar> getQuote() {
        return quote;
    }

    /**
     * Set the quote and update the JobTaskManager
     *
     * @param quote the quote to set
     * @param mustBeUpdated
     */
    protected void setQuote(JAXBElement<XMLGregorianCalendar> quote, boolean mustBeUpdated) {
        this.quote = quote;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set the quote and update the JobTaskManager
     *
     * @param quote the quote to set
     */
    protected void setQuote(JAXBElement<XMLGregorianCalendar> quote) {
        this.quote = quote;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get results
     *
     * @return the results
     */
    protected Results getResults() {
        return results;
    }

    /**
     * Set results and update the JobTaskManager
     *
     * @param results the results to set
     * @param mustBeUpdated
     */
    protected void setResults(Results results, boolean mustBeUpdated) {
        this.results = results;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set results and update the JobTaskManager
     *
     * @param results the results to set
     */
    protected void setResults(Results results) {
        this.results = results;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get parameters
     *
     * @return the parameters The parameters to set
     */
    protected Parameters getParameters() {
        return parameters;
    }

    /**
     * Set the parameters
     *
     * @param parameters the parameters to set
     * @param mustBeUpdated
     */
    protected void setParameters(Parameters parameters, boolean mustBeUpdated) {
        this.parameters = parameters;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set the parameters
     *
     * @param parameters the parameters to set
     */
    protected void setParameters(Parameters parameters) {
        this.parameters = parameters;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get the value of parameter name
     *
     * @param parameterName Parameter name to find
     * @return Returns the value of the parameter name
     */
    protected String getParameterValue(String parameterName) {
        String valueParam = null;
        Iterator<net.ivoa.xml.uws.v1.Parameter> iterParam = this.parameters.getParameter().iterator();
        while (iterParam.hasNext()) {
            net.ivoa.xml.uws.v1.Parameter parameter = iterParam.next();
            if (parameter.getId().equals(parameterName)) {
                valueParam = parameter.getContent();
            }
        }
        return valueParam;
    }

    /**
     * Set a value to a parameter name
     *
     * @param key Key
     * @param value Value
     * @param mustBeUpdated
     */
    protected void setParameterValue(String key, String value, boolean mustBeUpdated) {
        Iterator<net.ivoa.xml.uws.v1.Parameter> iterParam = this.parameters.getParameter().iterator();
        while (iterParam.hasNext()) {
            net.ivoa.xml.uws.v1.Parameter parameter = iterParam.next();
            if (parameter.getId().equals(key)) {
                parameter.setContent(value);
            }
        }
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set a value to a parameter name
     *
     * @param key Key
     * @param value Value
     */
    protected void setParameterValue(String key, String value) {
        Iterator<net.ivoa.xml.uws.v1.Parameter> iterParam = this.parameters.getParameter().iterator();
        while (iterParam.hasNext()) {
            net.ivoa.xml.uws.v1.Parameter parameter = iterParam.next();
            if (parameter.getId().equals(key)) {
                parameter.setContent(value);
            }
        }
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get start time
     *
     * @return Returns the startTime
     */
    protected final XMLGregorianCalendar getStartTime() {
        return startTime;
    }

    /**
     * Set the start time
     *
     * @param startTime the startTime to set
     */
    protected final void setStartTime(XMLGregorianCalendar startTime) {
        this.startTime = startTime;
    }

    /**
     * Get the end time
     *
     * @return the endTime
     */
    protected final XMLGregorianCalendar getEndTime() {
        return endTime;
    }

    /**
     * Set the end time
     *
     * @param endTime the endTime to set
     */
    protected final void setEndTime(XMLGregorianCalendar endTime) {
        this.endTime = endTime;
    }

    /**
     * Get the ownerID
     *
     * @return Returns the ownerId
     */
    protected String getOwnerId() {
        return ownerId;
    }

    /**
     * Set the owner ID
     *
     * @param ownerId the ownerId to set
     * @param mustBeUpdated
     */
    protected void setOwnerId(String ownerId, boolean mustBeUpdated) {
        this.ownerId = ownerId;
        if (mustBeUpdated) {
            JobTaskManager.getInstance().updateJobTask(this);
        }
    }

    /**
     * Set the owner ID
     *
     * @param ownerId the ownerId to set
     */
    protected void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
        JobTaskManager.getInstance().updateJobTask(this);
    }

    /**
     * Get the Thread
     *
     * @return Returns the thread
     */
    protected Thread getBlinker() {
        return this.blinker;
    }

    private Form uploadFile(Representation rep) throws FileUploadException, Exception {

        // The Apache FileUpload project parses HTTP requests which
        // conform to RFC 1867, "Form-based File Upload in HTML". That
        // is, if an HTTP request is submitted using the POST method,
        // and with a content type of "multipart/form-data", then
        // FileUpload can parse that request, and get all uploaded files
        // as FileItem.
        // 1/ Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(1000240);

        // 2/ Create a new file upload handler based on the Restlet
        // FileUpload extension that will parse Restlet requests and
        // generates FileItems.
        RestletFileUpload upload = new RestletFileUpload(factory);
        List items;

        // 3/ Request is parsed by the handler which generates a
        // list of FileItems
        items = upload.parseRepresentation(rep);
        // Process only the uploaded item  and save it on disk
        Form form = new Form();
        for (final Iterator it = items.iterator(); it.hasNext();) {
            FileItem fi = (FileItem) it.next();
            if (fi.isFormField()) {
                form.add(fi.getFieldName(), fi.getString());
            } else {
                form.add(fi.getFieldName(), fi.getName());
                this.copyFile(fi);
            }
        }
        return form;
    }

    protected void retrieveFile(URI url, File fileDestination) {
        ClientResource client = new ClientResource(url);
        OutputStream os = null;
        if (client.getStatus().isSuccess()) {
            InputStream is = null;
            try {
                Representation rep = client.get();
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

    protected void moveFile(final File fileToCopy) throws UniversalWorkerException {
        String uri = "riap://application/jobCache/" + jobTaskId + "/" + fileToCopy.getName();
        ClientResource client = new ClientResource(uri);
        //client.put(new FileRepresentation(fileToCopy, MediaType.APPLICATION_ALL));
        client.put(new OutputRepresentation(MediaType.ALL) {

            @Override
            public void write(OutputStream outputStream) throws IOException {
                FileInputStream fileOs = new FileInputStream(fileToCopy);
                int c;
                while ((c = fileOs.read()) != -1) {
                    outputStream.write(c);
                }
                fileOs.close();
            }
        });
        if (!client.getStatus().isSuccess()) {
            client.release();
            throw new UniversalWorkerException(client.getStatus(), "Cannot copy " + fileToCopy.getName());
        }
        client.release();
        fileToCopy.delete();
    }

    protected void copyFile(FileItem fi) throws UniversalWorkerException {
        String uri = "riap://application/jobCache/" + jobTaskId + "/" + fi.getName();
        ClientResource client = new ClientResource(uri);
        client.put(fi.getString());
        client.release();
    }

    /**
     * Create a user disk space for the processing
     *
     * @param jobTaskId Job task identifier
     * @throws UniversalWorkerException Returns a client bad request
     */
    protected void createUserSpace(Context context) throws UniversalWorkerException {
        //final SitoolsSettings sitoolsSettings = (SitoolsSettings) context.getAttributes().get(ContextAttributes.SETTINGS);
        //final String dataStorageUrl = sitoolsSettings.getString(Consts.APP_DATASTORAGE_ADMIN_URL) + "/directories";        
        //final StorageDirectory storageDirectory = RIAPUtils.getObjectFromName(dataStorageUrl, "Tmp", context);
        //String directory = storageDirectory.getLocalPath();
        //directory = directory.replaceFirst("file://", "");
        //File fb = new File(directory + File.separator + "jobCache");
        //File fb = new File("/tmp/storage/jobCache" + File.separator + jobTaskId);
        //fb.mkdir();
        //String uri = RIAPUtils.getRiapBase() + "/uws/jobCache";
        //String uri = "riap://component" + this.storageMgtPath + "/jobCache";
        String uri = "riap://application/jobCache";
        ClientResource client = new ClientResource(uri);
        client.post(jobTaskId);
        client.release();
    }

    /**
     * Delete user space disk for a specific jobId
     *
     * @param jobTaskId
     * @throws UniversalWorkerException Returns an CLIENT_ERROR_BAD_REQUEST
     */
    protected void deleteUserSpace(Context context) throws UniversalWorkerException {
        //final SitoolsSettings sitoolsSettings = (SitoolsSettings) context.getAttributes().get(ContextAttributes.SETTINGS);
        //final String dataStorageUrl = sitoolsSettings.getString(Consts.APP_DATASTORAGE_ADMIN_URL) + "/directories";        
        //final StorageDirectory storageDirectory = RIAPUtils.getObjectFromName(dataStorageUrl, "Tmp", context);
        //String directory = storageDirectory.getLocalPath();
        //directory = directory.replaceFirst("file://", "");
        //File file = new File(directory + File.separator + "jobCache" + File.separator + jobTaskId);
        //File file = new File("/tmp/jobCache" + File.separator + jobTaskId);
        //file.delete();
        String uri = "riap://application/jobCache/" + jobTaskId;
        ClientResource client = new ClientResource(uri);
        client.delete();
        client.release();
    }

    protected String getStoragePublicUrl() {
        //final SitoolsSettings sitoolsSettings = (SitoolsSettings) Context.getCurrent().getAttributes().get(ContextAttributes.SETTINGS);
        //return sitoolsSettings.getString(Consts.APP_CLIENT_PUBLIC_URL) + "/storage"; 
        return this.storagePublic + "/storage";
    }
}
