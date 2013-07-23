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
import com.thoughtworks.xstream.io.xml.StaxDriver;
import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import java.util.Date;
import java.util.UUID;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import net.ivoa.xml.uws.v1.ErrorSummary;
import net.ivoa.xml.uws.v1.ErrorType;
import net.ivoa.xml.uws.v1.JobSummary;
import net.ivoa.xml.uws.v1.Parameter;
import net.ivoa.xml.uws.v1.ResultReference;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * Representation for JobSummary object
 * @author Jean-Christophe Malapert
 * @see JobSummary
 */
public class JobRepresentation extends XstreamRepresentation {

    public JobRepresentation(AbstractJobTask jobTask, boolean isUsedDestructionDate) {
        super();
        this.setObject(checkExistingJobTask(jobTask));
        if (isUsedDestructionDate) {
            try {
                XMLGregorianCalendar calendar = null;
                try {
                    calendar = Util.convertIntoXMLGregorian(new Date());
                } catch (DatatypeConfigurationException ex) {
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Cannot convert current into XML Gregorian date");
                }
                int val = calendar.compare(JobTaskManager.getInstance().getDestructionTime(jobTask));
                if (val == DatatypeConstants.GREATER) {
                    JobTaskManager.getInstance().deleteTask(jobTask);
                    this.setObject(null);
                }
            } catch (UniversalWorkerException ex) {
                throw new ResourceException(ex.getStatus(), ex.getMessage(), ex.getCause());
            }
        }
        init();
    }

    public JobRepresentation(AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    protected Object checkExistingJobTask(AbstractJobTask jobTask) throws ResourceException {
        Object obj = null;
        if (Util.isSet(jobTask)) {
            obj = (Object) jobTask.getJobSummary();
        } else {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Job does not exist");
        }
        return obj;
    }

    private void init() {
        XStream xstream = configureXStream();
        this.setXstream(xstream);
    }

    protected XStream configureXStream() {
        QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://www.ivoa.net/xml/UWS/v1.0");
        qnm.setDefaultPrefix("uws");
        XStream xstream = new XStream(new StaxDriver(qnm));
        xstream.addDefaultImplementation(XMLGregorianCalendar.class, XMLGregorianCalendar.class);
        xstream.alias("job", JobSummary.class);
        xstream.aliasField("destruction", XMLGregorianCalendar.class, "destruction");
        xstream.aliasField("startTime", XMLGregorianCalendar.class, "startTime");
        xstream.aliasField("endTime", XMLGregorianCalendar.class, "endTime");
        xstream.alias("parameter", net.ivoa.xml.uws.v1.Parameter.class);
        xstream.alias("result", net.ivoa.xml.uws.v1.ResultReference.class);
        xstream.alias("jobinfo", net.ivoa.xml.uws.v1.JobSummary.JobInfo.class);
        xstream.alias("errorSummary", net.ivoa.xml.uws.v1.ErrorSummary.class);
        xstream.addImplicitCollection(net.ivoa.xml.uws.v1.Results.class, "result", net.ivoa.xml.uws.v1.ResultReference.class);
        xstream.addImplicitCollection(net.ivoa.xml.uws.v1.Parameters.class, "parameter", net.ivoa.xml.uws.v1.Parameter.class);
        xstream.registerConverter(new DateConverter());
        xstream.registerConverter(new ContentConverter());
        xstream.registerConverter(new QuoteConverter());
        xstream.registerConverter(new OwnerConverter());
        xstream.registerConverter(new ErrorSummaryConverter());
        xstream.registerConverter(new JobResultsConverter());
        return xstream;
    }

    @Override
    protected String fixXStreamBug(String representation) {
        return representation.replaceFirst("uws:job xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\"", "uws:job xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ivoa.net/xml/UWS/v1.0 http://ivoa.net/xml/UWS/UWS-v1.0.xsd\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
    }

    public class ContentConverter implements Converter {

        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            net.ivoa.xml.uws.v1.Parameter parameter = (Parameter) o;
            writer.addAttribute("id", parameter.getId());
            writer.addAttribute("byReference", String.valueOf(parameter.isByReference()));
            if (parameter.isIsPost() != null) {
                writer.addAttribute("isPost", String.valueOf(parameter.isIsPost()));
            }
            writer.setValue(parameter.getContent());
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean canConvert(Class type) {
            return net.ivoa.xml.uws.v1.Parameter.class == type;
        }
    }

    public class DateConverter implements Converter {

        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            XMLGregorianCalendar calendar =  (XMLGregorianCalendar) o;
            try {
                writer.setValue(calendar.toXMLFormat());
            } catch (IllegalStateException e) {
                writer.addAttribute("xsi:nil", "true");
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean canConvert(Class type) {
            return XMLGregorianCalendar.class.isAssignableFrom(type);            
        }
    }

    public class OwnerConverter implements Converter {

        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            String val = (String) o;
            if (val.equals(Constants.NO_OWNER)) {
                writer.addAttribute("xsi:nil", "true");
            } else {
                writer.setValue(val);
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean canConvert(Class type) {
            return String.class == type;
        }
    }

    public class QuoteConverter implements Converter {

        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            JAXBElement<XMLGregorianCalendar> jaxbCalendar = (JAXBElement<XMLGregorianCalendar>) o;
            writer.setValue(jaxbCalendar.getValue().toXMLFormat());
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean canConvert(Class type) {
            return JAXBElement.class == type;
        }
    }

    public class ErrorSummaryConverter implements Converter {

        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            net.ivoa.xml.uws.v1.ErrorSummary errorSummary = (ErrorSummary) o;
            ErrorType errorType = errorSummary.getType();
            if (Util.isSet(errorType)) {
                writer.addAttribute("type", errorType.value());
            } else {
                writer.addAttribute("type", errorType.FATAL.value());
            }
            writer.addAttribute("hasDetail", String.valueOf(errorSummary.isHasDetail()));
            writer.startNode("message");
            writer.setValue(errorSummary.getMessage());
            writer.endNode();
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean canConvert(Class type) {
            return net.ivoa.xml.uws.v1.ErrorSummary.class == type;
        }
    }

    public class JobResultsConverter implements Converter {

        public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
            net.ivoa.xml.uws.v1.ResultReference result = (ResultReference) o;
            String id = result.getId();
            if (Util.isSet(id)) {
                writer.addAttribute("id", id);
            } else {
                writer.addAttribute("id", UUID.randomUUID().toString());
            }
            String href = result.getHref();
            if(Util.isSet(href)) {
                writer.addAttribute("xlink:href", href);
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean canConvert(Class type) {
            return net.ivoa.xml.uws.v1.ResultReference.class == type;
        }
    }
}