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

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;

import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;

/**
 * Representation for JobSummary object.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @see JobSummary
 */
public class JobRepresentation extends AbstractXstreamRepresentation {

    /**
     * Constructor.
     * @param jobTask job task
     * @param isUsedDestructionDate the destruction date is set
     * @param mediaType media type
     */
    public JobRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate, final MediaType mediaType) {
        super(mediaType, null);
        this.setObject(checkExistingJobTask(jobTask));
        if (isUsedDestructionDate) {
            try {
                XMLGregorianCalendar calendar = null;
                try {
                    calendar = Util.convertIntoXMLGregorian(new Date());
                } catch (DatatypeConfigurationException ex) {
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Cannot convert current into XML Gregorian date");
                }
                final int val = calendar.compare(JobTaskManager.getInstance().getDestructionTime(jobTask));
                if (val == DatatypeConstants.GREATER) {
                    JobTaskManager.getInstance().deleteTask(jobTask);
                    this.setObject(null);
                }
            } catch (UniversalWorkerException ex) {
                throw new ResourceException(ex);
            }
        }
        init();
    }
    
    /**
     * Constructor.
     * @param jobTask job task
     * @param isUsedDestructionDate the destruction time is set
     */
    public JobRepresentation(final AbstractJobTask jobTask, final boolean isUsedDestructionDate) {    
        this(jobTask, isUsedDestructionDate, MediaType.TEXT_XML);
    }

    /**
     * Constructor.
     * @param jobTask job task
     */
    public JobRepresentation(final AbstractJobTask jobTask) {
        this(jobTask, false);
    }

    /**
     * Returns the job summary. 
     * @param jobTask job task
     * @return the job summary
     * @throws ResourceException 
     */
    protected Object checkExistingJobTask(final AbstractJobTask jobTask) throws ResourceException {      
        if (Util.isSet(jobTask)) {
            return (Object) jobTask.getJobSummary();
        } else {
            throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Job does not exist");
        }
    }

    /**
     * Init xstream.
     */
    private void init() {
        final XStream xstream = configureXStream();
        this.setXstream(xstream);
    } 

    /**
     * Returns the xstream configuration.
     * @return the xstream configuration
     */
    protected XStream configureXStream() {
        final QNameMap qnm = new QNameMap();
        qnm.setDefaultNamespace("http://www.ivoa.net/xml/UWS/v1.0");
        qnm.setDefaultPrefix("uws");
        createXstream(getMediaType(), qnm);
        final XStream xstream = getXstream();       
        xstream.alias("job", JobSummary.class);
        xstream.aliasField("ownerId", JobSummary.class, "ownerId");
        //xstream.aliasField("owner", String.class, "ownerId");
        xstream.aliasField("destruction", XMLGregorianCalendar.class, "destruction");
        //xstream.addDefaultImplementation(Date.class, org.apache.xerces.jaxp.class);
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

    /**
     * Converter for Parameter.
     */
    protected static class ContentConverter implements Converter {
        /**
         * Transforms the output of parameter.
         * @param o Parameter object
         * @param writer the output
         * @param mc the context
         */
        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final net.ivoa.xml.uws.v1.Parameter parameter = (Parameter) o;
            writer.addAttribute("id", parameter.getId());
            writer.addAttribute("byReference", String.valueOf(parameter.isByReference()));
            if (parameter.isIsPost() != null) {
                writer.addAttribute("isPost", String.valueOf(parameter.isIsPost()));
            }
            writer.setValue(parameter.getContent());
        }

        /**
         * Inverse transformation of marshal.
         * 
         * <p>
         * This method is not supported because we do not need in our case.
         * </p>
         * @param reader the reader
         * @param uc context
         * @return an Exception
         */
        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /**
         * Returns <code>True</code> when type is compatible with Parameter otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with Parameter otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return net.ivoa.xml.uws.v1.Parameter.class == type;
        }
    }

    /**
     * Date converter.
     */
    protected static class DateConverter implements Converter {
        /**
         * Transforms the output of XMLGregorianCalendar.
         * @param o Parameter object
         * @param writer the output
         * @param mc the context
         */
        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final XMLGregorianCalendar calendar =  (XMLGregorianCalendar) o;
            try {               
                writer.setValue(calendar.toXMLFormat());               
            } catch (IllegalStateException e) {
                writer.addAttribute("xsi:nil", "true");
            }
        }
        /**
         * Inverse transformation of marshal.
         * 
         * <p>
         * This method is not supported because we do not need in our case.
         * </p>
         * @param reader the reader
         * @param uc context
         * @return an Exception
         */
        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /**
         * Returns <code>True</code> when type is compatible with XMLGregorianCalendar otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with XMLGregorianCalendar otherwise <code>False</code>
         */        
        @Override
        public final boolean canConvert(final Class type) {
            return XMLGregorianCalendar.class.isAssignableFrom(type);            
        }
    }

    /**
     * Owner converter.
     */
    protected static class OwnerConverter implements Converter {

        /**
         * Transforms the output of String.
         * @param o Parameter object
         * @param writer the output
         * @param mc the context
         */        
        @Override
        public void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final String val = (String) o;
            if (val.equals(Constants.NO_OWNER)) {
                writer.addAttribute("xsi:nil", "true");
            } else {
                writer.setValue(val);
            }
        }

        /**
         * Inverse transformation of marshal.
         * 
         * <p>
         * This method is not supported because we do not need in our case.
         * </p>
         * @param reader the reader
         * @param uc context
         * @return an Exception
         */
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

    /**
     * Quote converter.
     */
    protected static class QuoteConverter implements Converter {
        /**
         * Transforms the output of Quote.
         * @param o Parameter object
         * @param writer the output
         * @param mc the context
         */
        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final JAXBElement<XMLGregorianCalendar> jaxbCalendar = (JAXBElement<XMLGregorianCalendar>) o;
            writer.setValue(jaxbCalendar.getValue().toXMLFormat());
        }
        /**
         * Inverse transformation of marshal.
         *
         * <p>
         * This method is not supported because we do not need in our case.
         * </p>
         * @param reader the reader
         * @param uc context
         * @return an Exception
         */
        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /**
         * Returns <code>True</code> when type is compatible with JAXBElement otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with JAXBElement otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return JAXBElement.class == type;
        }
    }

    /**
     * Error summary converter.
     */
    protected static class ErrorSummaryConverter implements Converter {
        /**
         * Transforms the output of ErrorSummary.
         * @param o Parameter object
         * @param writer the output
         * @param mc the context
         */
        @Override
        public final void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final net.ivoa.xml.uws.v1.ErrorSummary errorSummary = (ErrorSummary) o;
            final ErrorType errorType = errorSummary.getType();
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

        /**
         * Inverse transformation of marshal.
         *
         * <p>
         * This method is not supported because we do not need in our case.
         * </p>
         * @param reader the reader
         * @param uc context
         * @return an Exception
         */
        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        /**
         * Returns <code>True</code> when type is compatible with ErrorSummary otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with ErrorSummary otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return net.ivoa.xml.uws.v1.ErrorSummary.class == type;
        }
    }

    /**
     * Job Results converter.
     */
    protected static class JobResultsConverter implements Converter {
        /**
         * Transforms the output of JobResults.
         * @param o Parameter object
         * @param writer the output
         * @param mc the context
         */
        @Override
        public void marshal(final Object o, final HierarchicalStreamWriter writer, final MarshallingContext mc) {
            final net.ivoa.xml.uws.v1.ResultReference result = (ResultReference) o;
            final String id = result.getId();
            if (Util.isSet(id)) {
                writer.addAttribute("id", id);
            } else {
                writer.addAttribute("id", UUID.randomUUID().toString());
            }
            final String type = result.getType();
            if (Util.isSet(type)) {
                writer.addAttribute("xlink:type", type);
            }
            final String href = result.getHref();
            if (Util.isSet(href)) {
                writer.addAttribute("xlink:href", href);
            }
        }

        /**
         * Inverse transformation of marshal.
         *
         * <p>
         * This method is not supported because we do not need in our case.
         * </p>
         * @param reader the reader
         * @param uc context
         * @return an Exception
         */
        @Override
        public final Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        /**
         * Returns <code>True</code> when type is compatible with ResultReference otherwise <code>False</code>.
         * @param type type to check
         * @return <code>True</code> when type is compatible with ResultReference otherwise <code>False</code>
         */
        @Override
        public final boolean canConvert(final Class type) {
            return net.ivoa.xml.uws.v1.ResultReference.class == type;
        }
    }
}