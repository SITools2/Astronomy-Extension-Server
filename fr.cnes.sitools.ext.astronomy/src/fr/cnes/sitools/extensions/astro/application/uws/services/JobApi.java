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
package fr.cnes.sitools.extensions.astro.application.uws.services;

import fr.cnes.sitools.extensions.astro.application.UwsApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.representation.JobRepresentation;
import fr.cnes.sitools.xml.uws.v1.Job;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 *
 * @author malapert
 */
public class JobApi extends BaseJobResource {

    @Override
    public void doInit() {
        super.doInit();
        this.setAutoDescribing(false);
    }

    @Get("xml")
    public final Representation getJob() throws ResourceException {
        try {

            final Job job = AbstractJobTask.getCapabilities(this.app.getJobTaskImplementation());
            Representation rep = null;
            setStatus(Status.SUCCESS_OK);
            rep = new OutputRepresentation(MediaType.TEXT_XML) {

                @Override
                public void write(OutputStream out) throws IOException {
                    try {
                        JAXBContext jaxbContext = JAXBContext.newInstance("fr.cnes.sitools.xml.uws.v1");
                        Marshaller marshaller = jaxbContext.createMarshaller();
                        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
                        marshaller.marshal(job, out);
                    } catch (JAXBException ex) {
                        Logger.getLogger(JobApi.class.getName()).log(Level.SEVERE, null, ex);
                        throw new IOException(ex);
                    }
                }
            };

            return rep;
        } catch (SecurityException ex) {
            Logger.getLogger(JobApi.class.getName()).log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(JobApi.class.getName()).log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }
    }

}
