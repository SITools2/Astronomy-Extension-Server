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

import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;

/**
 * Representation for Phase Object.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class JobPhaseRepresentation extends JobQuoteRepresentation {

    /**
     * Creates a new instance of Phase object representation.
     * @param jobTask job
     * @param isUsedDestructionDate Defines if a destruction has been set
     */
    public JobPhaseRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate) {
        super(jobTask, isUsedDestructionDate);
    }

    /**
     * Creates a new instance of Phase object representation.
     * @param jobTask job
     */
    public JobPhaseRepresentation(final AbstractJobTask jobTask) {
        this(jobTask, false);
    }
    
    @Override
    protected Object checkExistingJobTask(final AbstractJobTask jobTask) throws ResourceException {
        try {
            return JobTaskManager.getInstance().getPhase(jobTask);
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex);
        }
    }
}
