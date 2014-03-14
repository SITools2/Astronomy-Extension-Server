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

import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;

/**
 * Representation for Quote object.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class JobQuoteRepresentation extends StringRepresentation {

    /**
     * Constructor.
     * @param jobTask job task
     * @param isUsedDestructionDate the destruction time is set
     */
    public JobQuoteRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate) {
        super("");
        this.setText(String.valueOf(checkExistingJobTask(jobTask)));
        if (isUsedDestructionDate) {
            try {
                XMLGregorianCalendar calendar = null;
                try {
                    calendar = fr.cnes.sitools.extensions.astro.application.uws.common.Util.convertIntoXMLGregorian(new Date());
                } catch (DatatypeConfigurationException ex) {
                    throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, ex);
                }
                final int val = calendar.compare(JobTaskManager.getInstance().getDestructionTime(jobTask));
                if (val == DatatypeConstants.GREATER) {
                    JobTaskManager.getInstance().deleteTask(jobTask);
                    this.setText(null);
                }
            } catch (UniversalWorkerException ex) {
                throw new ResourceException(ex);
            }
        }
    }
    /**
     * Constructor.
     * @param jobTask job task
     */
    public JobQuoteRepresentation(final AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    /**
     * Returns a auote if the job task exists.
     * @param jobTask job task
     * @return a job task if it exists.
     * @throws ResourceException 
     */
    protected Object checkExistingJobTask(final AbstractJobTask jobTask) throws ResourceException {
        try {
            return JobTaskManager.getInstance().getQuote(jobTask);         
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex);
        }
    }
}
