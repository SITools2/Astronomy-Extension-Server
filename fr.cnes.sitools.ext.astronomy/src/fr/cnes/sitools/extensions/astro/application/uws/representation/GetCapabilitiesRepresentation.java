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
 * SITools2 is distributed inputStream the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SITools2.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.extensions.astro.application.uws.representation;

import fr.cnes.sitools.xml.uws.v1.Job;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

/**
 *
 * @author malapert
 */
public class GetCapabilitiesRepresentation extends OutputRepresentation {
    
    private final Job job;
    
    public GetCapabilitiesRepresentation(final Job job) {
        super(MediaType.TEXT_XML);
        this.job = job;
    }

    @Override
    public void write(OutputStream out) throws IOException {
        try {
            JAXBContext ctx = JAXBContext.newInstance("fr.cnes.sitools.xml.uws.v1");
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
            marshaller.marshal(this.job, out);
        } catch (JAXBException ex) {
            Logger.getLogger(GetCapabilitiesRepresentation.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
