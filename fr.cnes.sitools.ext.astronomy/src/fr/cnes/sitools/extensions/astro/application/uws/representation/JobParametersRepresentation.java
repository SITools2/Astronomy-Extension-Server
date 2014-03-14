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

import org.restlet.data.MediaType;
import org.restlet.resource.ResourceException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;

/**
 * Representation for Parameters object.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @see Parameters
 */
public class JobParametersRepresentation extends JobRepresentation {
    /**
     * Creates a new instance of JobParameter representation.
     * @param jobTask job
     * @param isUsedDestructionDate Defines if a destruction has been set
     * @param mediaType media type
     */
    public JobParametersRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate, final MediaType mediaType) {
        super(jobTask, isUsedDestructionDate, mediaType);
    }
    /**
     * Creates a new instance of JobParameter representation.
     * @param jobTask job
     * @param isUsedDestructionDate Defines if a destruction has been set
     */    
    public JobParametersRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate) {
        this(jobTask, isUsedDestructionDate, MediaType.TEXT_XML);
    }
    /**
     * Creates a new instance of JobParameter representation.
     * @param jobTask job
     */
    public JobParametersRepresentation(final AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    @Override
    protected Object checkExistingJobTask(final AbstractJobTask jobTask) throws ResourceException {
        try {
            return JobTaskManager.getInstance().getParameters(jobTask);
        } catch (UniversalWorkerException ex) {
            throw new ResourceException(ex);
        }
    }

    @Override
    protected XStream configureXStream() {
        final QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://www.ivoa.net/xml/UWS/v1.0");
        qnm.setDefaultPrefix("uws");
        final XStream xstream = new XStream(new StaxDriver(qnm));
        xstream.alias("parameters", net.ivoa.xml.uws.v1.Parameters.class);
        xstream.alias("parameter", net.ivoa.xml.uws.v1.Parameter.class);
        xstream.addImplicitCollection(net.ivoa.xml.uws.v1.Parameters.class, "parameter", net.ivoa.xml.uws.v1.Parameter.class);
        xstream.registerConverter(new ContentConverter());
        return xstream;
    }
}
