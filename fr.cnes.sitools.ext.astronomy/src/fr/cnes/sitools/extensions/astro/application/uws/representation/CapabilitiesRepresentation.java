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
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix;
import fr.cnes.sitools.xml.uws.v1.Job;
import org.restlet.data.MediaType;

/**
 *
 * @author malapert
 */
public class CapabilitiesRepresentation extends AbstractXstreamRepresentation {
    
    private final Job job;
    
    public CapabilitiesRepresentation(final Job job, MediaType mediaType) {
        super(mediaType, null);
        this.job = job;
        init();
    }     
    
    public CapabilitiesRepresentation(final Job job) {
        this(job, MediaType.TEXT_XML);
    } 
    
    private void init() {
        this.setObject(this.job);
        XStream xstream = configureXStream();
        this.setXstream(xstream);
    }

    protected XStream configureXStream() {
        QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://sitools.cnes.fr/xml/UWS/v1.0");
        //qnm.setDefaultPrefix("uws");
        createXstream(getMediaType(), qnm);
        XStream xstream = getXstream();
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
        xstream.registerConverter(new CapabilitiesRepresentation.Longitude());

        return xstream;
    }    

    private static class OutputImage implements Converter {

        public OutputImage() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.OutputsType.Image image = (fr.cnes.sitools.xml.uws.v1.OutputsType.Image) o;
            String format = image.getFormat().value();
            writer.addAttribute("format", format);            
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.OutputsType.Image.class == type;
        }
    }

    private static class InputImage implements Converter {

        public InputImage() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.InputsType.Image image = (fr.cnes.sitools.xml.uws.v1.InputsType.Image)o;
            String name = image.getName();
            String format = image.getFormat().value();
            String documentation = image.getDocumentation();
            writer.addAttribute("name", name);
            writer.addAttribute("format", format);
            writer.startNode("documentation");
            writer.setValue(documentation);
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Image.class == type;
        }
    }

    private static class Geometry implements Converter {

        public Geometry() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.InputsType.Geometry geom = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry) o;
            String refFrame = geom.getReferenceFrame().value();
            writer.addAttribute("referenceFrame", refFrame);
            Circle circle = geom.getCircle();
            if (circle != null) {
                writer.startNode("circle");
                mc.convertAnother(circle.getLongitude(), new Longitude());
                mc.convertAnother(circle.getLatitude(), new Latitude());
                mc.convertAnother(circle.getRadius(), new Radius());
                writer.endNode();                
            }
            Healpix hpx = geom.getHealpix();
            if (hpx != null) {
                writer.startNode("healpix");
                mc.convertAnother(hpx.getOrder(), new Order());
                mc.convertAnother(hpx.getPixels(), new Pixels());                
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.class == type;
        }
    }

    private static class Longitude implements Converter {

        public Longitude() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Longitude longitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Longitude) o;
            String name = longitude.getName();
            String unit = longitude.getUnit();
            String doc = longitude.getDocumentation();
            writer.startNode("longitude");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Longitude.class == type;
        }
    }


    private static class Latitude implements Converter {

        public Latitude() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Latitude latitude = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Latitude) o;
            String name = latitude.getName();
            String unit = latitude.getUnit();
            String doc = latitude.getDocumentation();
            writer.startNode("latitude");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Latitude.class == type;
        }
    }
    
    private static class Radius implements Converter {

        public Radius() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Radius radius = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Radius) o;
            String name = radius.getName();
            String unit = radius.getUnit();
            String doc = radius.getDocumentation();
            writer.startNode("radius");
            writer.addAttribute("name", name);
            writer.addAttribute("unit", unit);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Radius.class == type;
        }
    }   
    
    private static class Documentation implements Converter {

        public Documentation() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            String doc = (String) o;
            writer.startNode("documentation");
            writer.setValue(doc);
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return String.class == type;
        }
    }      

    private static class Order implements Converter {

        public Order() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Order order = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Order) o;
            String name = order.getName();
            String doc = order.getDocumentation();
            writer.startNode("order");
            writer.addAttribute("name", name);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Order.class == type;
        }
    }

    private static class Pixels implements Converter {

        public Pixels() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Pixels pixels = (fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Pixels) o;
            String name = pixels.getName();
            String doc = pixels.getDocumentation();
            writer.startNode("pixels");
            writer.addAttribute("name", name);
            mc.convertAnother(doc, new Documentation());
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Healpix.Pixels.class == type;
        }
    }

    private static class InputKeyword implements Converter {

        public InputKeyword() {
        }

        @Override
        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            fr.cnes.sitools.xml.uws.v1.InputsType.Keyword keyword = (fr.cnes.sitools.xml.uws.v1.InputsType.Keyword)o;
            String name = keyword.getName();
            String documentation = keyword.getDocumentation();
            writer.addAttribute("name", name);
            writer.startNode("documentation");
            writer.setValue(documentation);
            writer.endNode();
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean canConvert(Class type) {
            return fr.cnes.sitools.xml.uws.v1.InputsType.Keyword.class == type;
        }
    }
}
