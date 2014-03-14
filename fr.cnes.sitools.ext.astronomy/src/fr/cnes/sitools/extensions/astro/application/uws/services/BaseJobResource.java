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

import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.UwsApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;

/**
 * Initializes all resources.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class BaseJobResource extends SitoolsParameterizedResource {    
    
    /**
     * Uws application.
     */
    protected UwsApplicationPlugin app = null;
    /**
     * JobId requested by the user.
     */
    protected String requestedJobId = null;        

    @Override
    public void doInit() {
        super.doInit();
        setNegotiated(true);
        getMetadataService().addExtension("json", MediaType.APPLICATION_JSON);
        getMetadataService().addExtension("xml", MediaType.TEXT_XML);
        getMetadataService().setDefaultMediaType(MediaType.TEXT_XML);
        this.app = (UwsApplicationPlugin) getApplication();
        this.requestedJobId = (String) getRequestAttributes().get("job-id");
        this.setAutoDescribing(false);
        if (!this.app.isLoadPersistence()) {
            this.app.loadPersistence();
        }        
    }

    /**
     * Redirection to /{jobs}/{jobId}.
     */
    protected final void redirectToJobID() {
        final Reference refTarget = new Reference(getReference().getPath());
        refTarget.setBaseRef(getSettings().getPublicHostDomain());         
        redirectSeeOther(new Reference(refTarget.getTargetRef().getIdentifier() + Constants.SLASH + getRequestedJobId()));
    }    
    
    /**
     * Redirection to /{jobs}.
     */
    protected final void redirectToJobs() {
        final Reference refTarget = new Reference(getReference().getPath());
        refTarget.setBaseRef(getSettings().getPublicHostDomain());         
        redirectSeeOther(new Reference(refTarget.getTargetRef().getIdentifier()));
    }

    /**
     * Returns JobTask from the job task manager.
     * @return JobTask Submitted task
     */
    protected final AbstractJobTask getJobTask() {
        try {
            return JobTaskManager.getInstance().getJobTaskById(getRequestedJobId());
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex.getStatus(), ex.getMessage(), ex.getCause());
        }
    }

    /**
     * Returns requested Job identifier asked by a user.
     * @return the job identifier
     */
    protected final String getRequestedJobId() {
        return this.requestedJobId;
    }

    /**
     * Sets the JobID.
     * @param jobId JobID
     */
    protected final void setRequestedJobId(final String jobId) {
        this.requestedJobId = jobId;
    }    
}
