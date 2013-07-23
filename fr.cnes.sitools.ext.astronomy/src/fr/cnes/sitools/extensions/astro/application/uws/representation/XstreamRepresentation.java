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
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import org.restlet.Context;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.WriterRepresentation;

/**
 * Abstract Xstream Representation
 * @author Jean-Christophe Malapert
 */
public abstract class XstreamRepresentation<T> extends WriterRepresentation {

    /** The XStream JSON driver class. */
    private Class<? extends HierarchicalStreamDriver> jsonDriverClass;

    /** The (parsed) object to format. */
    private T object;

    /** The representation to parse. */
    private Representation representation;

    /** The XStream XML driver class. */
    private Class<? extends HierarchicalStreamDriver> xmlDriverClass;

    /** The modifiable XStream object. */
    private XStream xstream;

    /**
     * Constructor.
     *
     * @param mediaType
     *            The target media type.
     * @param object
     *            The object to format.
     */
    public XstreamRepresentation(MediaType mediaType, T object) {
        super(mediaType);
        this.object = object;
        this.representation = null;
        this.jsonDriverClass = JettisonMappedXmlDriver.class;
        this.xmlDriverClass = DomDriver.class;
        this.xstream = null;
    }

    /**
     * Constructor.
     *
     * @param representation
     *            The representation to parse.
     */
    public XstreamRepresentation(Representation representation) {
        super(representation.getMediaType());
        this.object = null;
        this.representation = representation;
        this.jsonDriverClass = JettisonMappedXmlDriver.class;
        this.xmlDriverClass = DomDriver.class;
        this.xstream = null;
    }

    /**
     * Constructor. Uses the {@link MediaType#APPLICATION_XML} media type by
     * default.
     *
     * @param object
     *            The object to format.
     */
    public XstreamRepresentation(T object) {
        this(MediaType.APPLICATION_XML, object);
    }

    public XstreamRepresentation() {
        super(MediaType.APPLICATION_XML);
        this.representation = null;
        this.jsonDriverClass = JettisonMappedXmlDriver.class;
        this.xmlDriverClass = DomDriver.class;
        this.xstream = null;
    }

    /**
     * Creates an XStream object based on a media type. By default, it creates a
     * {@link HierarchicalStreamDriver} or a {@link DomDriver}.
     *
     * @param mediaType
     *            The serialization media type.
     * @return The XStream object.
     */
    protected XStream createXstream(MediaType mediaType) {
        XStream result = null;

        try {
            if (MediaType.APPLICATION_JSON.isCompatible(mediaType)) {
                result = new XStream(getJsonDriverClass().newInstance());
                result.setMode(XStream.NO_REFERENCES);
            } else {
                result = new XStream(getXmlDriverClass().newInstance());
            }

            result.autodetectAnnotations(true);
        } catch (Exception e) {
            Context.getCurrentLogger().log(Level.WARNING,
                    "Unable to create the XStream driver.", e);
        }

        return result;
    }

    /**
     * Returns the XStream JSON driver class.
     *
     * @return TXStream JSON driver class.
     */
    public Class<? extends HierarchicalStreamDriver> getJsonDriverClass() {
        return jsonDriverClass;
    }

    @SuppressWarnings("unchecked")
    public T getObject() {
        T result = null;

        if (this.object != null) {
            result = this.object;
        } else if (this.getRepresentation() != null) {
            try {
                result = (T) getXstream().fromXML(
                        this.getRepresentation().getStream());
            } catch (IOException e) {
                Context.getCurrentLogger().log(Level.WARNING,
                        "Unable to parse the object with XStream.", e);
            }
        }

        return result;
    }

    /**
     * Returns the XStream XML driver class.
     *
     * @return The XStream XML driver class.
     */
    public Class<? extends HierarchicalStreamDriver> getXmlDriverClass() {
        return xmlDriverClass;
    }

    /**
     * Returns the modifiable XStream object. Useful to customize mappings.
     *
     * @return The modifiable XStream object.
     */
    public XStream getXstream() {
        if (this.xstream == null) {
            this.xstream = createXstream(getMediaType());
        }

        return this.xstream;
    }

    /**
     * Sets the XStream JSON driver class.
     *
     * @param jsonDriverClass
     *            The XStream JSON driver class.
     */
    public void setJsonDriverClass(
            Class<? extends HierarchicalStreamDriver> jsonDriverClass) {
        this.jsonDriverClass = jsonDriverClass;
    }

    /**
     * Sets the XStream XML driver class.
     *
     * @param xmlDriverClass
     *            The XStream XML driver class.
     */
    public void setXmlDriverClass(
            Class<? extends HierarchicalStreamDriver> xmlDriverClass) {
        this.xmlDriverClass = xmlDriverClass;
    }

    /**
     * Sets the XStream object.
     *
     * @param xstream
     *            The XStream object.
     */
    public void setXstream(XStream xstream) {
        this.xstream = xstream;
    }

    @Override
    public void write(Writer writer) throws IOException {
        if (getRepresentation() != null) {
            getRepresentation().write(writer);
        } else if (getObject() != null) {
            CharacterSet charSet = (getCharacterSet() == null) ? CharacterSet.ISO_8859_1
                    : getCharacterSet();

//            if (!MediaType.APPLICATION_JSON.isCompatible(getMediaType())) {
//                writer.append("<?xml version=\"1.0\" encoding=\""
//                        + charSet.getName() + "\" ?>\n");
//            }
            // Fix pb due to StaxDriver
            String response = getXstream().toXML(getObject());
            //response = response.replaceFirst("<\\?xml version=\"1.0\" \\?>", "");
            response = fixXStreamBug(response);
            writer.write(response);
            writer.flush();
            //getXstream().toXML(object, writer);
        }
    }

    protected abstract String fixXStreamBug(String representation);

    /**
     * @param object the object to set
     */
    public void setObject(T object) {
        this.object = object;
    }

    /**
     * @return the representation
     */
    public Representation getRepresentation() {
        return representation;
    }

    /**
     * @param representation the representation to set
     */
    public void setRepresentation(Representation representation) {
        this.representation = representation;
    }
}
