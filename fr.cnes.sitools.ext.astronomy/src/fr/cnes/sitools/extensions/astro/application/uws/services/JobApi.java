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

import java.util.logging.Level;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.representation.CapabilitiesRepresentation;
import fr.cnes.sitools.xml.uws.v1.Job;

/**
 * Provides a semantic description to make interaction with MIZAR.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class JobApi extends BaseJobResource {

    @Override
    public final void doInit() {
        super.doInit();
        this.setAutoDescribing(false);
    }

    /**
     * Returns the job description as XML format.
     * @return the job description
     * @throws ResourceException 
     */
    @Get("xml")
    public final Representation getJobToXML() throws ResourceException {
        try {
            final Job job = AbstractJobTask.getCapabilities(this.app.getJobTaskImplementation());
            Representation rep = null;
            setStatus(Status.SUCCESS_OK);
            return new CapabilitiesRepresentation(job);
        } catch (SecurityException ex) {
            Engine.getLogger(JobApi.class.getName()).log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        } catch (IllegalArgumentException ex) {
            Engine.getLogger(JobApi.class.getName()).log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }
    }
    
    /**
     * Returns the job description as JSON format.
     * @return the job description
     * @throws ResourceException 
     */
    @Get("json")
    public final Representation getJobToJSON() throws ResourceException {
        try {
            final Job job = AbstractJobTask.getCapabilities(this.app.getJobTaskImplementation());
            Representation rep = null;
            setStatus(Status.SUCCESS_OK);
            return new CapabilitiesRepresentation(job, MediaType.APPLICATION_JSON);
        } catch (SecurityException ex) {
            Engine.getLogger(JobApi.class.getName()).log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        } catch (IllegalArgumentException ex) {
            Engine.getLogger(JobApi.class.getName()).log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }
    }
}
