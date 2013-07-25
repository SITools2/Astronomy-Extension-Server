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
package fr.cnes.sitools.extensions.astro.application.uws.representation;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import net.ivoa.xml.uws.v1.Results;
import org.restlet.data.MediaType;
import org.restlet.resource.ResourceException;

/**
 * Representation for Results object
 * @author Jean-Christophe Malapert
 * @see Results
 */
public class JobResultsRepresentation extends JobRepresentation {
    
    public JobResultsRepresentation(AbstractJobTask jobTask, boolean isUsedDestructionDate, MediaType mediaType) {
        super(jobTask, isUsedDestructionDate, mediaType);
    }    

    public JobResultsRepresentation(AbstractJobTask jobTask, boolean isUsedDestructionDate) {
        this(jobTask, isUsedDestructionDate, MediaType.TEXT_XML);
    }

    public JobResultsRepresentation(AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    @Override
    protected Object checkExistingJobTask(AbstractJobTask jobTask) throws ResourceException {
        try {
            Results obj = JobTaskManager.getInstance().getResults(jobTask);
            return obj;
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex.getStatus(), ex.getMessage(), ex.getCause());
        }
    }

    @Override
    protected XStream configureXStream() {
        QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://www.ivoa.net/xml/UWS/v1.0");
        qnm.setDefaultPrefix("uws");
        createXstream(getMediaType(), qnm);
        XStream xstream = getXstream();
        xstream.alias("results", net.ivoa.xml.uws.v1.Results.class);
        xstream.addImplicitCollection(net.ivoa.xml.uws.v1.Results.class, "result", net.ivoa.xml.uws.v1.ResultReference.class);
        xstream.alias("result", net.ivoa.xml.uws.v1.ResultReference.class);
        xstream.registerConverter(new JobResultsConverter());
        return xstream;
    }

}
