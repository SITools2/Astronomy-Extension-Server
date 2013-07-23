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

import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.UwsApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import org.restlet.data.Reference;
import org.restlet.resource.ResourceException;

/**
 *
 * @author malapert
 */
public class BaseJobResource extends SitoolsParameterizedResource {    
    
    protected UwsApplicationPlugin app = null;
    /**
     * JobId requested by the user
     */
    protected String requestedJobId = null;        

    @Override
    public void doInit() {
        super.doInit();
        this.app = (UwsApplicationPlugin) getApplication();
        this.requestedJobId = (String) getRequestAttributes().get("job-id");
        this.setAutoDescribing(false);
        if (!this.app.isLoadPersistence()) {
            this.app.loadPersistence();
        }        
    }

    /**
     * Redirection to /{jobs}/{jobId}
     */
    protected void redirectToJobID() {
        redirectSeeOther(new Reference(getReference().getIdentifier() + Constants.SLASH + getRequestedJobId()));
    }    
    
    /**
     * Redirection to /{jobs}
     */
    protected void redirectToJobs() {
        redirectSeeOther(new Reference(getReference().getIdentifier()));
    }

    /**
     * Get JobTask from the job task manager
     * @return Returns JobTask Submitted task
     */
    protected AbstractJobTask getJobTask() {
        try {
            return JobTaskManager.getInstance().getJobTaskById(getRequestedJobId());
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex.getStatus(),ex.getMessage(),ex.getCause());
        }
    }

    /**
     * Get requested Job identifier asked by a user
     * @return Returns the job identifier
     */
    protected String getRequestedJobId() {
        return this.requestedJobId;
    }

    /**
     * Set JobID
     * @param jobId JobID
     */
    protected void setRequestedJobId(String jobId) {
        this.requestedJobId = jobId;
    }
    
}
