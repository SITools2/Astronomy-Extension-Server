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
import java.util.Date;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;

/**
 * Representation for ParameterName object
 *
 * @author Jean-Christophe Malapert
 */
public class JobParameterNameRepresentation extends StringRepresentation {

    public JobParameterNameRepresentation(AbstractJobTask jobTask, String key, boolean isUsedDestructionDate) {
        super("");
        this.setText(String.valueOf(checkExistingJobTask(jobTask, key)));
        if (isUsedDestructionDate) {
            try {
                XMLGregorianCalendar calendar = null;
                try {
                    calendar = fr.cnes.sitools.extensions.astro.uws.common.Util.convertIntoXMLGregorian(new Date());
                } catch (DatatypeConfigurationException ex) {
                    //TODO Logger.getLogger(JobErrorRepresentation.class.getName()).log(Level.SEVERE, null, ex);
                }
                int val = calendar.compare(JobTaskManager.getInstance().getDestructionTime(jobTask));
                if (val == DatatypeConstants.GREATER) {
                    JobTaskManager.getInstance().deleteTask(jobTask);
                    this.setText(null);
                }
            } catch (UniversalWorkerException ex) {
                throw new ResourceException(ex.getStatus(), ex.getMessage(), ex.getCause());
            }
        }
    }

    public JobParameterNameRepresentation(AbstractJobTask jobTask, String key) {
        this(jobTask, key, false);
    }

    protected Object checkExistingJobTask(AbstractJobTask jobTask, String key) throws ResourceException {
        try {
            String obj = JobTaskManager.getInstance().getValueParameter(jobTask, key);
            return obj;
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex.getStatus(), ex.getMessage(), ex.getCause());
        }
    }
}    //  end __NAME__
