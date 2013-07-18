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
import javax.xml.datatype.XMLGregorianCalendar;
import org.restlet.resource.ResourceException;

/**
 * Representation for DestructionTime Object
 *
 * @author Jean-Christophe Malapert
 */
public class JobDestructionTimeRepresentation extends JobQuoteRepresentation {


    public JobDestructionTimeRepresentation (AbstractJobTask jobTask, boolean isUsedDestructionDate) {
        super(jobTask, isUsedDestructionDate);
    }

    public JobDestructionTimeRepresentation (AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    @Override
    protected Object checkExistingJobTask(AbstractJobTask jobTask) throws ResourceException {
        try {
            XMLGregorianCalendar obj = JobTaskManager.getInstance().getDestructionTime(jobTask);
            return obj.toXMLFormat();
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex.getStatus(),ex.getMessage(),ex.getCause());
        }
    }
}

