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
package fr.cnes.sitools.extensions.astro.uws.representation;

import fr.cnes.sitools.extensions.astro.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.uws.jobmanager.JobTaskManager;
import org.restlet.resource.ResourceException;

/**
 * Representation for ExcecutionDuration Object
 *
 * @author Jean-Christophe Malapert
 */
public class JobExecutionDurationRepresentation extends JobQuoteRepresentation {

    /**
     * Creates a new instance of __NAME__
     */
    public JobExecutionDurationRepresentation(AbstractJobTask jobTask, boolean isUsedDestructionDate) {
        super(jobTask, isUsedDestructionDate);
    }

    public JobExecutionDurationRepresentation(AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    @Override
    protected Object checkExistingJobTask(AbstractJobTask jobTask) throws ResourceException {
        try {
            return JobTaskManager.getInstance().getExecutionTime(jobTask);
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex.getStatus(), ex.getMessage(), ex.getCause());
        }
    }
}