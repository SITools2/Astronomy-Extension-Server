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

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.representation.WriterRepresentation;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * Abstract Xstream Representation.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @param <T>
 */
public abstract class AbstractXstreamRepresentation<T> extends WriterRepresentation {

    /**
     * The (parsed) object to format.
     */
    private T object;

    /**
     * The modifiable XStream object.
     */
    private XStream xstream = null;

    /**
     * Constructor.
     *
     * @param mediaType The target media type.
     * @param objectVal The object to format.
     */
    public AbstractXstreamRepresentation(final MediaType mediaType, final T objectVal) {
        super(mediaType);
        this.object = objectVal;      
    }

    /**
     * Constructor. 
     * <p>
     * Uses the {@link MediaType#APPLICATION_XML} media type by
     * default.
     * </p>
     *
     * @param objectVal The object to represent.
     */
    public AbstractXstreamRepresentation(final T objectVal) {
        this(MediaType.APPLICATION_XML, objectVal);
    }

    /**
     * Constructor. 
     * <p>
     * Uses the {@link MediaType#APPLICATION_XML} media type by
     * default.
     * </p>
     */    
    public AbstractXstreamRepresentation() {
        this(MediaType.APPLICATION_XML, null);
    }

    /**
     * Creates an XStream object based on a media type. 
     * 
     * <p>
     * By default, it creates a JsonHierarchicalStreamDriver
     * or a StaxDriver.
     * </p>
     * 
     * @param mediaType The serialization media type.
     * @param qnm namespace   
     */
    protected final void createXstream(final MediaType mediaType, final QNameMap qnm) {
        XStream result = null;
        try {
            if (MediaType.APPLICATION_JSON.isCompatible(mediaType)) {
                result = new XStream(new JsonHierarchicalStreamDriver());
                result.setMode(XStream.NO_REFERENCES);
            } else {
                result = new XStream(new StaxDriver(qnm));
            }
            result.autodetectAnnotations(true);
        } catch (Exception e) {
            Context.getCurrentLogger().log(Level.WARNING, "Unable to create the XStream driver.", e);
        }
        setXstream(result);
    }
    
    /**
     * Creates an XStream object based on a media type. 
     *
     * <p>
     * By default, it creates a JsonHierarchicalStreamDriver
     * or a StaxDriver.
     * </p>
     *
     * @return The XStream object.
     */
    private XStream createXstream() {
        return new XStream(new StaxDriver());
    }

    /**
     * Returns the object to represent.
     * @return the object to represent
     */
    public final T getObject() {
        return (this.object != null) ? this.object : null;
    }

    /**
     * Sets the object to represent.
     * @param objectVal the object to set
     */
    public final void setObject(final T objectVal) {
        this.object = objectVal;
    }

    /**
     * Returns the XStream object.
     * @return the XStream object.
     */
    public final XStream getXstream() {
        XStream xstreamResponse;
        if (this.xstream == null) {
            xstreamResponse = createXstream();
        } else {
            xstreamResponse = this.xstream;
        }
        return xstreamResponse;
    }

    /**
     * Sets the XStream object.
     *
     * @param xstreamVal The XStream object.
     */
    public final void setXstream(final XStream xstreamVal) {
        this.xstream = xstreamVal;
    }

    @Override
    public final void write(final Writer writer) throws IOException {
        String response = getXstream().toXML(getObject());
        response = response.replaceFirst("xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\"",
                "xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/UWS/v1.0 http://ivoa.net/xml/UWS/UWS-v1.0.xsd\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
        response = response.replaceAll("class=\"org.apache.xerces.jaxp.datatype.XMLGregorianCalendarImpl\"", "");
        writer.write(response);
        writer.flush();
    }
}
