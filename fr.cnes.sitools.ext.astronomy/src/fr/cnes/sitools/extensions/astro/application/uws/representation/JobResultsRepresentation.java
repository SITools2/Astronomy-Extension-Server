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

import java.util.List;

import org.restlet.data.MediaType;
import org.restlet.resource.ResourceException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.QNameMap;

import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;

/**
 * Representation for Results object.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @see Results
 */
public class JobResultsRepresentation extends JobRepresentation {
    
    /**
     * Constructor.
     * @param jobTask jobTask to represent
     * @param isUsedDestructionDate destruction of the resource is set
     * @param mediaType Media Type
     */
    public JobResultsRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate, final MediaType mediaType) {
        super(jobTask, isUsedDestructionDate, mediaType);
    }    

    /**
     * Constructor.
     * @param jobTask jobTask to represent
     * @param isUsedDestructionDate Media Type
     */
    public JobResultsRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate) {
        this(jobTask, isUsedDestructionDate, MediaType.TEXT_XML);
    }

    /**
     * Constructor.
     * @param jobTask jobTask to represent
     */
    public JobResultsRepresentation(final AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    @Override
    protected final Object checkExistingJobTask(final AbstractJobTask jobTask) throws ResourceException {
        try {
            return JobTaskManager.getInstance().getResults(jobTask);            
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    protected final XStream configureXStream() {
        final QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://www.ivoa.net/xml/UWS/v1.0");
        qnm.setDefaultPrefix("uws");
        createXstream(getMediaType(), qnm);
        final XStream xstream = getXstream();
        xstream.alias("results", net.ivoa.xml.uws.v1.Results.class);
        if (MediaType.TEXT_XML == MediaType.valueOf(getMediaType().getName())) {
            xstream.addImplicitCollection(net.ivoa.xml.uws.v1.Results.class, "result", net.ivoa.xml.uws.v1.ResultReference.class);
            xstream.alias("result", net.ivoa.xml.uws.v1.ResultReference.class);
        } else {
        xstream.alias("result", List.class, net.ivoa.xml.uws.v1.ResultReference.class);    
        }                        
        xstream.registerConverter(new JobResultsConverter());
        return xstream;
    }
}
