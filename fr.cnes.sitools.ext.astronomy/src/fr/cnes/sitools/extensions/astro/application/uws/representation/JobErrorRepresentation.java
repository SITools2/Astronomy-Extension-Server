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

import net.ivoa.xml.uws.v1.ErrorSummary;

import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;

/**
 * Representation for ErrorSummary object.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @see ErrorSummary
 */
public class JobErrorRepresentation extends JobQuoteRepresentation {

    /**
     * Creates a new instance of JobError representation.
     * @param jobTask job
     * @param isUsedDestructionDate Defines if a destruction has been set
     */
    public JobErrorRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate) {
        super(jobTask, isUsedDestructionDate);
    }
    /**
     * Creates a new instance of ExcecutionDuration representation.
     * @param jobTask job
     */
    public JobErrorRepresentation(final AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    @Override
    protected Object checkExistingJobTask(final AbstractJobTask jobTask) throws ResourceException {
        try {
            return JobTaskManager.getInstance().getError(jobTask).getMessage();
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex);
        }
    }
}
