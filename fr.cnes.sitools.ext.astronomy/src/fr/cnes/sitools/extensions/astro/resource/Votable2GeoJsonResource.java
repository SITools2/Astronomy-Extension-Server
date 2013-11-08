/**
 * *****************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * SITools2 is distributed inputStream the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SITools2. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.astro.representation.GeoJsonRepresentation;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeaturesDataModel;
import fr.cnes.sitools.extensions.astro.application.opensearch.processing.JsonDataModelDecorator;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.InputsValidation;
import fr.cnes.sitools.extensions.common.NotNullAndNotEmptyValidation;
import fr.cnes.sitools.extensions.common.StatusValidation;
import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.extensions.common.Validation;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import net.ivoa.xml.votable.v1.Field;
import net.ivoa.xml.votable.v1.Resource;
import net.ivoa.xml.votable.v1.VOTABLE;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.OptionInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

/**
 *
 * @author malapert
 */
public class Votable2GeoJsonResource extends SitoolsParameterizedResource {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(Votable2GeoJsonResource.class.getName());
    /**
     * Coordinate system.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystem;
    /**
     * URL of the VOTable.
     */
    private URL url;

    @Override
    public final void doInit() {
        super.doInit();
    }

    /**
     * Converts VOTable to GeoJSON.
     * @return the GeoJSON representation
     * @throws IOException when a problem happens during the conversion
     */
    @Post("form")
    public final Representation convertVotable2GeoJson(final Representation entity) throws IOException {
        Representation rep;
        try {
            final Form form = new Form(entity);
            String result = form.getFirstValue("votable"); 
            final String coordSystem = form.getFirstValue("coordSystem");
            final JAXBContext ctx = JAXBContext.newInstance(new Class[]{net.ivoa.xml.votable.v1.ObjectFactory.class});
            final Unmarshaller unMarshaller = ctx.createUnmarshaller();                       
            if (result.contains("xmlns")) {
                result = result.replace("http://www.ivoa.net/xml/VOTable/v1.1", "http://www.ivoa.net/xml/VOTable/v1.2");
            } else {
                result = result.replace("<VOTABLE", "<VOTABLE xmlns=\"http://www.ivoa.net/xml/VOTable/v1.2\"");
            }
            final VOTABLE votable = (VOTABLE) unMarshaller.unmarshal(new ByteArrayInputStream(result.getBytes()));
            final List<Resource> resources = votable.getRESOURCE();
            final Resource resource = resources.get(0);
            final List<Map<Field, String>> response = Utility.parseResource(resource);            
            final FeaturesDataModel dataModel = JsonDataModelDecorator.computeJsonDataModel(response, AstroCoordinate.CoordinateSystem.valueOf(coordSystem));
            rep =  new GeoJsonRepresentation(dataModel.getFeatures());
        } catch (JAXBException ex) {
            Logger.getLogger(Votable2GeoJsonResource.class.getName()).log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
        }
        return rep;
    }
    @Override
    public final void sitoolsDescribe() {
        setName("VOTable to GeoJson converter.");
        setDescription("Converts a VOTable to Json");
    }
    
    /**
     * Descibes the GET method in WADL.
     *
     * @param info information
     */
    @Override
    protected final void describePost(final MethodInfo info) {
        this.addInfo(info);
        info.setIdentifier("Votable2GeoJSON");
        info.setDocumentation("Converts a VOTable to GeoJSon.");

        // objecName parameter
        final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
        parametersInfo.add(new ParameterInfo("votable", true, "String", ParameterStyle.QUERY,
                "The VOTable content."));

        // reference frame parameter
        final ParameterInfo paramCoordSys = new ParameterInfo("coordSystem", true, "String", ParameterStyle.QUERY,
                "Coordinate system in which the output is formated");
        final List<OptionInfo> coordsysOption = new ArrayList<OptionInfo>();
        final OptionInfo optionEquatorial = new OptionInfo("Equatorial system in ICRS");
        optionEquatorial.setValue(AstroCoordinate.CoordinateSystem.EQUATORIAL.name());
        coordsysOption.add(optionEquatorial);
        final OptionInfo optionGalactic = new OptionInfo("Galactic system");
        optionGalactic.setValue(AstroCoordinate.CoordinateSystem.GALACTIC.name());
        coordsysOption.add(optionGalactic);
        paramCoordSys.setOptions(coordsysOption);
        parametersInfo.add(paramCoordSys);

        // Set all parameters
        info.getRequest().setParameters(parametersInfo);

        // Response OK
        final ResponseInfo responseOK = new ResponseInfo();
        List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
        RepresentationInfo representationInfo = new RepresentationInfo(MediaType.APPLICATION_JSON);
        final DocumentationInfo doc = new DocumentationInfo();
        doc.setTitle("GeoJSON representation");
        doc.setTextContent("<pre>{\n"
                + "totalResults: 1,\n"
                + "type: \"FeatureCollection\",\n"
                + "features: [\n"
                + "  geometry: {\n"
                + "    coordinates: [10.6847083,41.26875],\n"
                + "    type: \"Point\"\n"
                + "  },\n"
                + "properties: {\n"
                + "  crs: {\n"
                + "    type: \"name\",\n"
                + "    properties: {\n"
                + "      name: \"EQUATORIAL.ICRS\"\n"
                + "    }\n"
                + "  },\n"
                + "  credits: \"CDS\",\n"
                + "  identifier: \"CDS0\"\n"
                + "}\n"
                + "}]}</pre>");
        representationInfo.setDocumentation(doc);
        representationsInfo.add(representationInfo);
        responseOK.getRepresentations().add(representationInfo);
        responseOK.getStatuses().add(Status.SUCCESS_OK);

        // response bad request and internal error
        representationsInfo = new ArrayList<RepresentationInfo>();
        representationInfo = new RepresentationInfo(MediaType.TEXT_HTML);
        representationInfo.setDocumentation("SITools2 status error page");
        representationsInfo.add(representationInfo);

        final ResponseInfo responseError = new ResponseInfo();
        responseError.getRepresentations().add(representationInfo);
        responseError.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        responseError.setDocumentation("Unexpected error");

        final ResponseInfo responseBad = new ResponseInfo();
        responseBad.getRepresentations().add(representationInfo);
        responseBad.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
        responseBad.setDocumentation(Status.CLIENT_ERROR_BAD_REQUEST.getDescription() + "- coordinate system is unknown");

        final ResponseInfo responseUnavailable = new ResponseInfo();
        responseUnavailable.getRepresentations().add(representationInfo);
        responseUnavailable.getStatuses().add(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
        responseUnavailable.setDocumentation(Status.SERVER_ERROR_SERVICE_UNAVAILABLE.getDescription());

        // Set responses
        final List<ResponseInfo> responseInfo = new ArrayList<ResponseInfo>();
        responseInfo.add(responseOK);
        responseInfo.add(responseError);
        responseInfo.add(responseBad);
        responseInfo.add(responseUnavailable);
        info.getResponses().addAll(responseInfo);
    }
}
