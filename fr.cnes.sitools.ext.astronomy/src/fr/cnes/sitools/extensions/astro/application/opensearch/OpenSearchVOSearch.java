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
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SITools2. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.application.opensearch;

import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeaturesDataModel;
import fr.cnes.sitools.astro.representation.GeoJsonRepresentation;
import fr.cnes.sitools.astro.representation.VOTableRepresentation;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.OpenSearchVOApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.opensearch.processing.VORequest;
import fr.cnes.sitools.extensions.astro.application.opensearch.processing.DictionaryDecorator;
import fr.cnes.sitools.extensions.astro.application.opensearch.processing.JsonDataModelDecorator;
import fr.cnes.sitools.extensions.astro.application.opensearch.processing.PutInCacheIfNotDecorator;
import fr.cnes.sitools.extensions.astro.application.opensearch.processing.VORequestInterface;
import fr.cnes.sitools.extensions.astro.application.opensearch.processing.VOTableDataModelDecorator;
import fr.cnes.sitools.extensions.cache.CacheBrowser;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.InputsValidation;
import fr.cnes.sitools.extensions.common.NotNullAndNotEmptyValidation;
import fr.cnes.sitools.extensions.common.StatusValidation;
import fr.cnes.sitools.extensions.common.Validation;
import fr.cnes.sitools.extensions.common.VoDictionary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Provides a search capability on a Simple Cone Search service by the use of
 * (healpix,order) parameters.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOSearch extends SitoolsParameterizedResource {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(OpenSearchVOSearch.class.getName());
    /**
     * URL service.
     */
    private String url;
    /**
     * Healpix order.
     */
    private int order;
    /**
     * Helapix pixel.
     */
    private long healpix;
    /**
     * Coordinate system.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystem;
    /**
     * Dictionary.
     */
    private transient Map<String, VoDictionary> dico;

    @Override
    public final void doInit() {
        try {
            super.doInit();
            setAnnotated(true);
            MediaType.register("application/x-votable+xml", "VOTable");
            getMetadataService().addExtension("votable", MediaType.valueOf("application/x-votable+xml"));
            getVariants().add(new Variant(MediaType.valueOf("application/x-votable+xml")));
            getVariants().add(new Variant(MediaType.APPLICATION_JSON));
            setUrl(((OpenSearchVOApplicationPlugin) getApplication()).getModel().getParametersMap().get("serviceURL").getValue());
            if (!getRequest().getMethod().equals(Method.OPTIONS)) {
                Form form = getRequest().getResourceRef().getQueryAsForm();
                Validation inputsValidation = new InputsValidation(form.getValuesMap());
                inputsValidation = new NotNullAndNotEmptyValidation(inputsValidation, "healpix", true);
                inputsValidation = new NotNullAndNotEmptyValidation(inputsValidation, "order", true);
                inputsValidation = new NotNullAndNotEmptyValidation(inputsValidation, "coordSystem", true);
                StatusValidation validation = inputsValidation.validate();
                if (!validation.isValid()) {
                    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, validation.toString());
                } else {
                    this.setHealpix((long) Long.valueOf(form.getFirstValue("healpix")));
                    this.setOrder((int) Integer.valueOf(form.getFirstValue("order")));
                    this.setCoordinateSystem(AstroCoordinate.CoordinateSystem.valueOf(form.getFirstValue("coordSystem")));
                    this.dico = ((OpenSearchVOApplicationPlugin) getApplication()).getDico();
                }                
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, null, ex);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
        }
    }
    
    @Get("votable")
    public final Representation getVotableResponse() {
        try {
            final boolean cacheableValue = Boolean.parseBoolean(((OpenSearchVOApplicationPlugin)getApplication()).getParameter("cacheable").getValue());
            final String applicationID = ((OpenSearchVOApplicationPlugin)getApplication()).getId();
            final String protocolParam = ((OpenSearchVOApplicationPlugin)getApplication()).getParameter("protocol").getValue();
            final OpenSearchVOApplicationPlugin.Protocol protocol = OpenSearchVOApplicationPlugin.Protocol.valueOf(protocolParam);
            SingletonCacheHealpixDataAccess.CacheStrategy cacheStrategy;
            if (cacheableValue) {
                cacheStrategy = SingletonCacheHealpixDataAccess.CacheStrategy.CACHE_ENABLE_DEEP_OBJECT;
            } else {
                cacheStrategy = SingletonCacheHealpixDataAccess.CacheStrategy.CACHE_ENABLE_SOLAR_OBJECT;
            }            
            VORequestInterface voRequest = new VORequest(applicationID, getUrl(), getOrder(), getHealpix(), getCoordinateSystem(), protocol, cacheStrategy);
            voRequest = new PutInCacheIfNotDecorator(voRequest, applicationID, getOrder(), getHealpix(), getCoordinateSystem(), cacheStrategy);
            voRequest = new DictionaryDecorator(voRequest, dico);
            voRequest = new VOTableDataModelDecorator(voRequest);
            Representation rep = new VOTableRepresentation((Map)voRequest.getOutput());           
            return useCacheBrowser(rep, cacheableValue);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }        
    }
    /**
     * Returns the JSON representation.
     *
     * @return the JSON representation
     */
    @Get("json")
    public final Representation getJsonResponse() {
        try {
            final boolean cacheableValue = Boolean.parseBoolean(((OpenSearchVOApplicationPlugin)getApplication()).getParameter("cacheable").getValue());
            final String applicationID = ((OpenSearchVOApplicationPlugin)getApplication()).getId();
            final String protocolParam = ((OpenSearchVOApplicationPlugin)getApplication()).getParameter("protocol").getValue();
            final OpenSearchVOApplicationPlugin.Protocol protocol = OpenSearchVOApplicationPlugin.Protocol.valueOf(protocolParam);            
            SingletonCacheHealpixDataAccess.CacheStrategy cacheStrategy;
            if (cacheableValue) {
                cacheStrategy = SingletonCacheHealpixDataAccess.CacheStrategy.CACHE_ENABLE_DEEP_OBJECT;
            } else {
                cacheStrategy = SingletonCacheHealpixDataAccess.CacheStrategy.CACHE_ENABLE_SOLAR_OBJECT;
            }            
            VORequestInterface voRequest = new VORequest(applicationID, getUrl(), getOrder(), getHealpix(), getCoordinateSystem(), protocol, cacheStrategy);
            voRequest = new PutInCacheIfNotDecorator(voRequest, applicationID, getOrder(), getHealpix(), getCoordinateSystem(), cacheStrategy);
            voRequest = new DictionaryDecorator(voRequest, dico);       
            voRequest = new JsonDataModelDecorator(voRequest, getCoordinateSystem());
            final FeaturesDataModel dataModel = (FeaturesDataModel) voRequest.getOutput();
            final Representation rep = new GeoJsonRepresentation(dataModel.getFeatures());            
            return useCacheBrowser(rep, cacheableValue);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }
    }
    /**
     * Returns the representation with cache directives cache parameter is set
     * to enable.
     *
     * @param rep representation to cache
     * @param isEnabled True when the cache is enabled
     * @return the representation with the cache directive when the cache is
     * enabled
     */
    private Representation useCacheBrowser(final Representation rep, final boolean isEnabled) {
        Representation cachedRepresentation;
        if (isEnabled) {
            final CacheBrowser cache = CacheBrowser.createCache(CacheBrowser.CacheDirectiveBrowser.DAILY, rep);
            getResponse().setCacheDirectives(cache.getCacheDirectives());
            cachedRepresentation = cache.getRepresentation();
        } else {
            cachedRepresentation = rep;
        }
        return cachedRepresentation;
    }

    @Override
    public final void sitoolsDescribe() {
        setName("Cone Search service.");
        setDescription("Retrieves and transforms a response from a Cone Search service.");
    }
    /**
     * Describes GET method in the WADL.
     *
     * @param info information
     */
    @Override
    protected final void describeGet(final MethodInfo info) {
        this.addInfo(info);
        info.setIdentifier("ConeSearchProtocolJSON");
        info.setDocumentation("Interoperability service to distribute images through a converted format of the Cone Search Protocol");

        final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
        parametersInfo.add(new ParameterInfo("healpix", true, "long", ParameterStyle.QUERY,
                "Healpix number"));
        parametersInfo.add(new ParameterInfo("order", true, "integer", ParameterStyle.QUERY,
                "Healpix order"));
        final ParameterInfo json = new ParameterInfo("format", true, "string", ParameterStyle.QUERY, "JSON format");
        json.setFixed("json");
        parametersInfo.add(json);
        final ParameterInfo coordSystem = new ParameterInfo("coordSystem", true, "string", ParameterStyle.QUERY,
                "Healpix coordinate system");
        parametersInfo.add(coordSystem);

        info.getRequest().setParameters(parametersInfo);

        // represensation when the response is fine
        final ResponseInfo responseOK = new ResponseInfo();

        final DocumentationInfo documentation = new DocumentationInfo();
        documentation.setTitle("GeoJSON");
        documentation.setTextContent("<pre>{\n"
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
                + "  identifier: \"CDS0\"\n"
                + "}\n"
                + "}]}</pre>");

        final List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
        final RepresentationInfo representationInfo = new RepresentationInfo(MediaType.APPLICATION_JSON);
        representationInfo.setDocumentation(documentation);
        representationsInfo.add(representationInfo);
        responseOK.setRepresentations(representationsInfo);
        responseOK.getStatuses().add(Status.SUCCESS_OK);

        // represensation when the response is not fine
        final ResponseInfo responseNOK = new ResponseInfo();
        final RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
        representationInfoError.setReference("error");

        responseNOK.getRepresentations().add(representationInfoError);
        responseNOK.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
        responseNOK.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);

        info.setResponses(Arrays.asList(responseOK, responseNOK));
    }

    /**
     * Returns the URL service.
     * @return the url
     */
    protected final String getUrl() {
        return url;
    }

    /**
     * Sets the URL service.
     * @param urlVal the url to set
     */
    protected final void setUrl(final String urlVal) {
        this.url = urlVal;
    }

    /**
     * Returns the Healpix order.
     * @return the order
     */
    protected final int getOrder() {
        return order;
    }

    /**
     * Sets the Healpix order.
     * @param orderVal the order to set
     */
    protected final void setOrder(final int orderVal) {
        this.order = orderVal;
    }

    /**
     * Returns the Healpix pixel.
     * @return the healpix
     */
    protected final long getHealpix() {
        return healpix;
    }

    /**
     * Sets the Healpix pixel.
     * @param healpixVal the healpix to set
     */
    protected void setHealpix(final long healpixVal) {
        this.healpix = healpixVal;
    }

    /**
     * Returns the coordinate system.
     * @return the coordinateSystem
     */
    protected final AstroCoordinate.CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Sets the coordinate system.
     * @param coordinateSystemVal the coordinateSystem to set
     */
    protected final void setCoordinateSystem(final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        this.coordinateSystem = coordinateSystemVal;
    }
}
