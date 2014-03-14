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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;

import fr.cnes.sitools.xml.uws.v1.InputsType;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix;
import fr.cnes.sitools.xml.uws.v1.Job;

/**
 * Representation for the service capabilities.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CapabilitiesRepresentation extends AbstractXstreamRepresentation {
    
    /**
     * Capabilities of the job.
     */
    private Job job;
    
    /**
     * Constructor.
     * @param job job
     * @param mediaType media type
     */
    public CapabilitiesRepresentation(final Job job, final MediaType mediaType) {
        super(mediaType, null);
        setJob(job);
        init();
    }     
    /**
     * Constructor.
     * @param job job
     */
    public CapabilitiesRepresentation(final Job job) {
        this(job, MediaType.TEXT_XML);
    } 
    /**
     * Init.
     */
    private void init() {
        this.setObject(this.getJob());
        final XStream xstream = configureXStream();
        this.setXstream(xstream);
    }

    /**
     * XStream configuration.
     * @return the xstream configuration
     */
    protected XStream configureXStream() {
        final QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://sitools.cnes.fr/xml/UWS/v1.0");
        //qnm.setDefaultPrefix("uws");
        createXstream(getMediaType(), qnm);
        final XStream xstream = getXstream();
        //xstream.addDefaultImplementation(XMLGregorianCalendar.class, XMLGregorianCalendar.class);
        xstream.alias("Job", fr.cnes.sitools.xml.uws.v1.Job.class);
        xstream.aliasField("Name", fr.cnes.sitools.xml.uws.v1.Job.class, "name");
        xstream.aliasField("Title", fr.cnes.sitools.xml.uws.v1.Job.class, "title"); 
        xstream.aliasField("Inputs", fr.cnes.sitools.xml.uws.v1.Job.class, "inputs");     
        xstream.aliasField("Outputs", fr.cnes.sitools.xml.uws.v1.Job.class, "outputs");
        xstream.alias("image", fr.cnes.sitools.xml.uws.v1.InputsType.Image.class);
        xstream.alias("keyword", fr.cnes.sitools.xml.uws.v1.InputsType.Keyword.class);
        xstream.alias("image", fr.cnes.sitools.xml.uws.v1.OutputsType.Image.class);
        xstream.aliasField("geoJson", fr.cnes.sitools.xml.uws.v1.OutputsType.class, "geoJson");
        xstream.aliasField("keyword", fr.cnes.sitools.xml.uws.v1.OutputsType.class, "keyword");
        xstream.addImplicitCollection(fr.cnes.sitools.xml.uws.v1.InputsType.class, "image");
        xstream.addImplicitCollection(fr.cnes.sitools.xml.uws.v1.InputsType.class, "keyword");
        xstream.addImplicitCollection(fr.cnes.sitools.xml.uws.v1.OutputsType.class, "image");
        xstream.registerConverter(new CapabilitiesRepresentation.OutputImage());
        xstream.registerConverter(new CapabilitiesRepresentation.InputImage());
        xstream.registerConverter(new CapabilitiesRepresentation.InputKeyword());
        xstream.registerConverter(new CapabilitiesRepresentation.Geometry());


        return xstream;
    }    

    /**
     * Returns the job.
     * @return the job
     */
    public final Job getJob() {
        return job;
    }

    /**
     * Sets the job.
     * @param job the job to set
     */
    private void setJob(final Job job) {
        this.job = job;
    }

    /**
     * Output image converter.
     */
    protected static class OutputImage implements Converter {

        public OutputImage() {
        }

        @Override
        public final void marshal(final Object o, HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.OutputsType.Image image = (fr.cnes.sitools.xml.uws.v1.OutputsType.Image) o;
            final String format = image.getFormat().value();
            writer.addAttribute("format", format);            
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        /**
         * Returns <code>True</code> when type is compatible with Image otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Image otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.OutputsType.Image.class == type;
        }
    }

    protected static class InputImage implements Converter {

        public InputImage() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Image image = (fr.cnes.sitools.xml.uws.v1.InputsType.Image)o;
            final String name = image.getName();
            final String format = image.getFormat().value();
            final String documentation = image.getDocumentation();
            writer.addAttribute("name", name);
            writer.addAttribute("format", format);
            writer.startNode("documentation");
            writer.setValue(documentation);
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        /**
         * Returns <code>True</code> when type is compatible with Image otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Image otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Image.class == type;
        }
    }

    private static class Geometry implements Converter {

        public Geometry() {
        }

        @Override
        public void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry geom = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry) o;
            final String refFrame = geom.getReferenceFrame().value();
            writer.addAttribute("referenceFrame", refFrame);
            final Circle circle = geom.getCircle();
            if (circle != null) {
                writer.startNode("circle");
                mc.convertAnother(circle.getLongitude(), new Longitude());
                mc.convertAnother(circle.getLatitude(), new Latitude());
                mc.convertAnother(circle.getRadius(), new Radius());
                writer.endNode();                
            }
            final Healpix hpx = geom.getHealpix();
            if (hpx != null) {
                writer.startNode("healpix");
                mc.convertAnother(hpx.getOrder(), new Order());
                mc.convertAnother(hpx.getPixels(), new Pixels());                
                writer.endNode();
            }
            final InputsType.Geometry.Polygon polygon = geom.getPolygon();
            if (polygon != null) {
                writer.startNode("polygon");
                mc.convertAnother(polygon.getLongitude1(), new Longitude1());
                mc.convertAnother(polygon.getLatitude1(), new Latitude1());
                mc.convertAnother(polygon.getLongitude2(), new Longitude2());
                mc.convertAnother(polygon.getLatitude2(), new Latitude2());
                mc.convertAnother(polygon.getLongitude3(), new Longitude3());
                mc.convertAnother(polygon.getLatitude3(), new Latitude3());
                mc.convertAnother(polygon.getLongitude4(), new Longitude4());
                mc.convertAnother(polygon.getLatitude4(), new Latitude4());
                mc.convertAnother(polygon.getRotation(), new Rotation()); 
                writer.endNode(); 
            }            
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        /**
         * Returns <code>True</code> when type is compatible with Geometry otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Geometry otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.class == type;
        }
    }

    protected static class Longitude implements Converter {

        public Longitude() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Longitude longitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Longitude) o;
            final String name = longitude.getName();
            final String unit = longitude.getUnit();
            final String doc = longitude.getDocumentation();
            writer.startNode("longitude");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Longitude.class == type;
        }
    }


    protected static class Latitude implements Converter {

        public Latitude() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Latitude latitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Latitude) o;
            final String name = latitude.getName();
            final String unit = latitude.getUnit();
            final String doc = latitude.getDocumentation();
            writer.startNode("latitude");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Latitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Latitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Latitude.class == type;
        }
    }

    protected static class Radius implements Converter {

        public Radius() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Radius radius = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Radius) o;
            final String name = radius.getName();
            final String unit = radius.getUnit();
            final String doc = radius.getDocumentation();
            writer.startNode("radius");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Radius otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Radius otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Radius.class == type;
        }
    }

    protected static class Documentation implements Converter {

        public Documentation() {
        }

        @Override
        public void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final String doc = (String) o;
            writer.startNode("documentation");
            writer.setValue(doc);
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with String otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with String otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return String.class == type;
        }
    }

    protected static class Order implements Converter {

        public Order() {
        }

        @Override
        public void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Order order = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Order) o;
            final String name = order.getName();
            final String doc = order.getDocumentation();
            writer.startNode("order");
            writer.addAttribute("name", name);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Order otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Order otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Order.class == type;
        }
    }

    protected static class Pixels implements Converter {

        public Pixels() {
        }

        @Override
        public void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Pixels pixels = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Pixels) o;
            final String name = pixels.getName();
            final String doc = pixels.getDocumentation();
            writer.startNode("pixels");
            writer.addAttribute("name", name);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Pixels otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Pixels otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Pixels.class == type;
        }
    }

    protected static class InputKeyword implements Converter {

        public InputKeyword() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Keyword keyword = (fr.cnes.sitools.xml.uws.v1.InputsType.Keyword)o;
            final String name = keyword.getName();
            final String documentation = keyword.getDocumentation();
            writer.addAttribute("name", name);
            writer.startNode("documentation");
            writer.setValue(documentation);
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Keyword otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Keyword otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Keyword.class == type;
        }
    }
    
    protected static class Longitude1 implements Converter {

        public Longitude1() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude1 longitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude1) o;
            final String name = longitude.getName();
            final String unit = longitude.getUnit();
            final String doc = longitude.getDocumentation();
            writer.startNode("longitude1");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude1.class == type;
        }
    }
    
    protected static class Longitude2 implements Converter {

        public Longitude2() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude2 longitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude2) o;
            final String name = longitude.getName();
            final String unit = longitude.getUnit();
            final String doc = longitude.getDocumentation();
            writer.startNode("longitude2");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude2.class == type;
        }
    }      
    
    protected static class Longitude3 implements Converter {

        public Longitude3() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude3 longitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude3) o;
            final String name = longitude.getName();
            final String unit = longitude.getUnit();
            final String doc = longitude.getDocumentation();
            writer.startNode("longitude3");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude3.class == type;
        }
    }      
    
    protected static class Longitude4 implements Converter {

        public Longitude4() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude4 longitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude4) o;
            final String name = longitude.getName();
            final String unit = longitude.getUnit();
            final String doc = longitude.getDocumentation();
            writer.startNode("longitude4");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Longitude4.class == type;
        }
    } 
    
    protected static class Latitude1 implements Converter {

        public Latitude1() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude1 latitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude1) o;
            final String name = latitude.getName();
            final String unit = latitude.getUnit();
            final String doc = latitude.getDocumentation();
            writer.startNode("latitude1");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude1.class == type;
        }
    }
    
    protected static class Latitude2 implements Converter {

        public Latitude2() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude2 latitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude2) o;
            final String name = latitude.getName();
            final String unit = latitude.getUnit();
            final String doc = latitude.getDocumentation();
            writer.startNode("latitude2");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude2.class == type;
        }
    }      
    
    protected static class Latitude3 implements Converter {

        public Latitude3() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude3 latitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude3) o;
            final String name = latitude.getName();
            final String unit = latitude.getUnit();
            final String doc = latitude.getDocumentation();
            writer.startNode("latitude3");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude3.class == type;
        }
    }      
    
    protected static class Latitude4 implements Converter {

        public Latitude4() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude4 latitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude4) o;
            final String name = latitude.getName();
            final String unit = latitude.getUnit();
            final String doc = latitude.getDocumentation();
            writer.startNode("latitude4");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Latitude4.class == type;
        }
    }
    
    protected static class Rotation implements Converter {

        public Rotation() {
        }

        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Rotation rotation = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Rotation) o;
            final String name = rotation.getName();
            final String unit = rotation.getUnit();
            final String doc = rotation.getDocumentation();
            writer.startNode("rotation");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with Longitude otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Longitude otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon.Rotation.class == type;
        }
    }     
}
