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
package fr.cnes.sitools.extensions.astro.application.uws.jobmanager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

import net.ivoa.xml.uws.v1.ErrorSummary;
import net.ivoa.xml.uws.v1.ExecutionPhase;
import net.ivoa.xml.uws.v1.Parameters;
import net.ivoa.xml.uws.v1.Results;

import org.restlet.Context;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import com.thoughtworks.xstream.XStream;

import fr.cnes.sitools.extensions.astro.application.UwsApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;

/**
 * {Insert class description here}
 *
 * @author Jean-Christophe Malapert
 */
public class JobTaskManager {

    /** singleton instance */
    private static JobTaskManager instance = null;
    /** Map of tasks */
    private Map<String, AbstractJobTask> tasksMap;
    /** The current context */
    private Context context;
    private UwsApplicationPlugin app;

    /**
     * Singleton
     */
    private JobTaskManager() {
        tasksMap = new ConcurrentHashMap<String, AbstractJobTask>();
        this.app = null;
    }

    /**
     * Gets the instance value
     *
     * @return Returns the instance
     */
    public static synchronized JobTaskManager getInstance() {
        if (instance == null) {
            instance = new JobTaskManager();
        }
        return instance;
    }


    /**
     * Get the job tasks
     * @return Returns the tasksMap
     */
    public Map<String, AbstractJobTask> getJobTasks() {
        return tasksMap;
    }

    /**
     * Set the job tasks
     * @param tasksMap Returns the tasksMap to set
     */
    public void setTasks(Map<String, AbstractJobTask> tasksMap) {
        this.tasksMap = tasksMap;
    }

    /**
     * Get a jobTask from an job identifier
     * @param jobTaskId Job identifier
     * @return Returns a JobTask
     * @throws UniversalWorkerException Returns a "client error bad request"
     */
    public AbstractJobTask getJobTaskById(String jobTaskId) throws UniversalWorkerException {
        if (Util.isSet(jobTaskId)) {
            return this.tasksMap.get(jobTaskId);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, jobTaskId + " does not exist.");
        }
    }

    /**
     * Create a specific job task
     * @param entity Entity
     * @return Returns the generated job task identifier
     * @throws UniversalWorkerException Returns an server error internal
     */
    public String createJobTask(UwsApplicationPlugin app, Representation entity) throws UniversalWorkerException {
        this.app = app;
        String jobTaskId = UUID.randomUUID().toString();      
        AbstractJobTask jobTask = AbstractJobTask.create(app, jobTaskId, entity);
        if(jobTask.getPhase().equals(ExecutionPhase.QUEUED)) {
            app.getTaskService().execute(jobTask);
        }
        this.updateJobTask(jobTask);
        return jobTaskId;
    }

    /**
     * Run Asychrone task
     * @param jobTask JobTask
     * @throws UniversalWorkerException Returns a client error bad request
     */
    public void runAsynchrone(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            jobTask.setPhase(ExecutionPhase.QUEUED);
            app.getTaskService().execute(jobTask);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Update a jobTask
     * @param jobTask JobTask to update
     */
    public void updateJobTask(AbstractJobTask jobTask) {
        try {
            this.tasksMap.put(jobTask.getJobTaskId(), jobTask);
            this.save();
        } catch (IOException ex) {
        }
    }

    /**
     * Delete a specific task
     * @param jobTask JobTask
     * @throws UniversalWorkerException Exception
     */
    public void deleteTask(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            try {
                this.tasksMap.remove(jobTask.getJobTaskId());
                jobTask.deleteUserSpace();
                this.save();
            } catch (IOException ex) {
                throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, "Cannot save the result");
            }
        } else {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, "Cannot delete the jobId");
        }
    }

    /**
     * cancel the Thread
     * @throws UniversalWorkerException
     */
    public void cancel(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            this.tasksMap.get(jobTask.getJobTaskId()).cancel();
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Get the phase job
     * @param jobTask Jobtask
     * @return Returns the job phase
     * @throws UniversalWorkerException Client error bad request
     */
    public ExecutionPhase getStatus(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            return this.tasksMap.get(jobTask.getJobTaskId()).getPhase();
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Set the execution time
     * @param jobTask jobtask
     * @param executionTime execution time
     * @throws UniversalWorkerException Client error bad request
     */
    public void setExecutionTime(AbstractJobTask jobTask, int executionTime) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && executionTime >= 0) {
            this.tasksMap.get(jobTask.getJobTaskId()).setExecutionDuration(executionTime);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Get the execution time
     * @param jobTask jobtask
     * @return Returns the execution time
     * @throws UniversalWorkerException Client error not found
     */
    public int getExecutionTime(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            return this.tasksMap.get(jobTask.getJobTaskId()).getExecutionDuration();
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Set the destruction time
     * @param jobTask jobtask
     * @param executionTime set execution time
     * @throws UniversalWorkerException client error bad request
     */
    public void setDestructionTime(AbstractJobTask jobTask, XMLGregorianCalendar executionTime) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(executionTime)) {
            this.tasksMap.get(jobTask.getJobTaskId()).setDestructionTime(executionTime);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Get the destruction time
     * @param jobTask jobtask
     * @return Returns XMLGregorianCalendar
     * @throws UniversalWorkerException
     */
    public XMLGregorianCalendar getDestructionTime(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            return this.tasksMap.get(jobTask.getJobTaskId()).getDestructionTime();
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Get the error
     * @param jobTask jobtask
     * @return Returns the error
     * @throws UniversalWorkerException client error not found
     */
    public ErrorSummary getError(AbstractJobTask jobTask) throws UniversalWorkerException {
        try {
            return this.tasksMap.get(jobTask.getJobTaskId()).getError();
        } catch (NullPointerException ex) {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * set the error
     * @param jobTask jobtask
     * @param error error to set
     * @throws UniversalWorkerException client error bad request
     */
    public void setError(AbstractJobTask jobTask, ErrorSummary error) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(error)) {
            this.tasksMap.get(jobTask.getJobTaskId()).setError(error);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Get quote
     * @param jobTask jobtask
     * @return Returns JAXBElement<XMLGregorianCalendar>
     * @throws UniversalWorkerException client error bad request
     */
    public JAXBElement<XMLGregorianCalendar> getQuote(AbstractJobTask jobTask) throws UniversalWorkerException {
        try {
            return this.tasksMap.get(jobTask.getJobTaskId()).getQuote();
        } catch (NullPointerException ex) {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * set quote
     * @param jobTask jobtask
     * @param quote quote to set
     * @throws UniversalWorkerException client error bad request
     */
    public void setQuote(AbstractJobTask jobTask, JAXBElement<XMLGregorianCalendar> quote) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(quote)) {
            this.tasksMap.get(jobTask.getJobTaskId()).setQuote(quote);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Get results
     * @param jobTask jobtask
     * @return Returns results
     * @throws UniversalWorkerException client error bad request
     */
    public Results getResults(AbstractJobTask jobTask) throws UniversalWorkerException {
        try {
            return this.tasksMap.get(jobTask.getJobTaskId()).getResults();
        } catch (NullPointerException ex) {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * set the results
     * @param jobTask jobrask
     * @param results results to set
     * @throws UniversalWorkerException client error bad request
     */
    public void setResults(AbstractJobTask jobTask, Results results) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(results)) {
            this.tasksMap.get(jobTask.getJobTaskId()).setResults(results);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Get parameters
     * @param jobTask jobtask
     * @return Returns parameters
     * @throws UniversalWorkerException
     */
    public Parameters getParameters(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            return this.tasksMap.get(jobTask.getJobTaskId()).getParameters();
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    public void setParameters(AbstractJobTask jobTask, Parameters parameters) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(parameters)) {
            this.tasksMap.get(jobTask.getJobTaskId()).setParameters(parameters);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    public String getValueParameter(AbstractJobTask jobTask, String parameterName) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(parameterName)) {
            return this.tasksMap.get(jobTask.getJobTaskId()).getParameterValue(parameterName);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    public void setParameter(AbstractJobTask jobTask, String key, String value) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(key) && Util.isSet(value)) {
            this.tasksMap.get(jobTask.getJobTaskId()).setParameterValue(key, value);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    public void setOwnerId(AbstractJobTask jobTask, String ownerId) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(ownerId)) {
            this.tasksMap.get(jobTask.getJobTaskId()).setOwnerId(ownerId);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    public String getOwnerId(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            return this.tasksMap.get(jobTask.getJobTaskId()).getOwnerId();
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    public void setPhase(AbstractJobTask jobTask, ExecutionPhase phase) throws UniversalWorkerException {
        if (Util.isSet(jobTask) && Util.isSet(phase)) {
            this.tasksMap.get(jobTask.getJobTaskId()).setPhase(phase);
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_NOT_FOUND, "The job does not exist.");
        }
    }

    /**
     * Set a default phase to ERROR
     * @param jobTask
     */
    public void setPhase(AbstractJobTask jobTask) {
        jobTask.setPhase(ExecutionPhase.ERROR);
    }

    public ExecutionPhase getPhase(AbstractJobTask jobTask) throws UniversalWorkerException {
        if (Util.isSet(jobTask)) {
            return this.tasksMap.get(jobTask.getJobTaskId()).getPhase();
        } else {
            throw new UniversalWorkerException(Status.CLIENT_ERROR_BAD_REQUEST, "The job does not exist.");
        }
    }

    /**
     * cache the object
     * @throws IOException
     */
    private void save() throws IOException {
        org.restlet.ext.xstream.XstreamRepresentation<Map<String, AbstractJobTask>> xstream = new org.restlet.ext.xstream.XstreamRepresentation<Map<String, AbstractJobTask>>(new HashMap(this.tasksMap));
        XStream xs = xstream.getXstream();
        xs.omitField(fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask.class, "blinker");
        //String uri = "riap://component" + context.getAttributes().get(UwsApplicationPlugin.APP_URL_STORAGE_MANAGEMENT) + "/cache";
        String uri = "riap://application/cache";
        ClientResource client = new ClientResource(uri);
        client.post(xstream.getText());
        client.release();
        
    }
} 
