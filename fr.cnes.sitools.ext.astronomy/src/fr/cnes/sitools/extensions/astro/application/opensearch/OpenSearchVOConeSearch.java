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

import fr.cnes.sitools.astro.representation.GeoJsonRepresentation;
import fr.cnes.sitools.astro.vo.conesearch.ConeSearchQuery;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.OpenSearchVOConeSearchApplicationPlugin;
import fr.cnes.sitools.extensions.cache.CacheBrowser;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem;
import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.extensions.common.VoDictionary;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Field;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
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
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Provides a search capability on a Simple Cone Search service by the use of
 * (healpix,order) parameters.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOConeSearch extends SitoolsParameterizedResource {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(OpenSearchVOConeSearch.class.getName());
    /**
     * Query.
     */
    private transient ConeSearchQuery query;
    /**
     * User parameters.
     */
    private transient UserParameters userParameters;
    /**
     * Dictionary.
     */
    private transient Map<String, VoDictionary> dico;

    @Override
    public final void doInit() {
        try {
            super.doInit();
            final String url = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getModel().getParametersMap().get("coneSearchURL").getValue();
            this.query = new ConeSearchQuery(url);
            if (!getRequest().getMethod().equals(Method.OPTIONS)) {
                this.userParameters = new UserParameters(getRequest().getResourceRef().getQueryAsForm());                
                this.dico = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getDico();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, null, ex);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
        }
    }

    /**
     * Reserved keywords in CSP for which a specific processing is needed.
     */
    public enum ReservedWords {

        /**
         * Right Ascension.
         */
        POS_EQ_RA_MAIN(Arrays.asList("POS_EQ_RA_MAIN", "pos.eq.ra;meta.main")),
        /**
         * Declination.
         */
        POS_EQ_DEC_MAIN(Arrays.asList("POS_EQ_DEC_MAIN", "pos.eq.dec;meta.main")),
        /**
         * ID.
         */
        ID_MAIN(Arrays.asList("ID_MAIN", "meta.id;meta.main")),
        /**
         * None.
         */
        NONE(Arrays.asList(""));
        /**
         * List of keywords for an item.
         */
        private final List<String> names;

        /**
         * Constructor.
         *
         * @param nameVals List of keywords
         */
        ReservedWords(final List<String> nameVals) {
            this.names = nameVals;
        }

        /**
         * Returns a list of keywords related to an item.
         *
         * @return list of keywords
         */
        public List<String> getName() {
            return this.names;
        }

        /**
         * Finds the enum from one of its keywords.
         *
         * @param keyword keyword to match
         * @return the enum
         */
        public static OpenSearchVOConeSearch.ReservedWords find(final String keyword) {
            OpenSearchVOConeSearch.ReservedWords response = OpenSearchVOConeSearch.ReservedWords.NONE;
            final OpenSearchVOConeSearch.ReservedWords[] words = OpenSearchVOConeSearch.ReservedWords.values();
            for (ReservedWords word : words) {
                final List<String> reservedName = word.getName();
                if (reservedName.contains(keyword)) {
                    response = word;
                    break;
                }
            }
            return response;
        }
    }

    /**
     * Returns the JSON representation.
     *
     * @return the JSON representation
     */
    @Get
    public final Representation getJsonResponse() {
        try {
            double longitude = userParameters.getLongitude();
            double latitude = userParameters.getLatitude();
            if (this.userParameters.getCoordSystem() == CoordinateSystem.GALACTIC) {
                final AstroCoordinate astroCoordinates = new AstroCoordinate(longitude, latitude);
                astroCoordinates.setCoordinateSystem(CoordinateSystem.GALACTIC);
                astroCoordinates.processTo(CoordinateSystem.EQUATORIAL);
                longitude = astroCoordinates.getRaAsDecimal();
                latitude = astroCoordinates.getDecAsDecimal();
            }
            final double radius = userParameters.getRadius();
            final List<Map<Field, String>> response = useCacheHealpix(query, longitude, latitude, radius, true);
            final FeaturesDataModel dataModel = createGeoJsonDataModel(response);
            final Representation rep = new GeoJsonRepresentation(dataModel.getFeatures());
            return useCacheBrowser(rep, cacheIsEnabled());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }
    }

    /**
     * Creates and returns the GeoJson data model from the response.
     *
     * @param response response coming from the cache or the VO server
     * @return GeoJson data model
     */
    private FeaturesDataModel createGeoJsonDataModel(final List<Map<Field, String>> response) {
        final FeaturesDataModel dataModel = new FeaturesDataModel();
        final AstroCoordinate astroCoordinates = new AstroCoordinate();
        final Set<Field> fields = getFields(response);
        fillDictionary(fields);
        for (Map<Field, String> iterDoc : response) {
            final FeatureDataModel feature = new FeatureDataModel();
            final Iterator<Field> fieldIter = fields.iterator();
            double raResponse = Double.NaN;
            double decResponse = Double.NaN;

            while (fieldIter.hasNext()) {
                final Field field = fieldIter.next();
                final net.ivoa.xml.votable.v1.DataType dataType = field.getDatatype();
                final String ucd = field.getUcd();
                final String value = iterDoc.get(field);
                if (Utility.isSet(value) && !value.isEmpty()) {
                    final Object responseDataType = Utility.getDataType(dataType, value);
                    final OpenSearchVOConeSearch.ReservedWords ucdWord = OpenSearchVOConeSearch.ReservedWords.find(ucd);
                    switch (ucdWord) {
                        case POS_EQ_RA_MAIN:
                            raResponse = Utility.parseRaVO(iterDoc, field);
                            break;
                        case POS_EQ_DEC_MAIN:
                            decResponse = Utility.parseDecVO(iterDoc, field);
                            break;
                        case ID_MAIN:
                            feature.setIdentifier(iterDoc.get(field));
                            break;
                        default:
                            feature.addProperty(field.getName(), responseDataType);
                            break;
                    }
                }
            }
            if (this.userParameters.getCoordSystem() == CoordinateSystem.GALACTIC) {
                astroCoordinates.setRaAsDecimal(raResponse);
                astroCoordinates.setDecAsDecimal(decResponse);
                astroCoordinates.setCoordinateSystem(CoordinateSystem.EQUATORIAL);
                astroCoordinates.processTo(CoordinateSystem.GALACTIC);
                raResponse = astroCoordinates.getRaAsDecimal();
                decResponse = astroCoordinates.getDecAsDecimal();
            }
            feature.createGeometry(String.format("[%s,%s]", raResponse, decResponse), "Point");
            feature.createCrs(this.userParameters.getCoordSystem().getCrs());
            spatialFilter(feature, raResponse, decResponse);
            dataModel.addFeature(feature);
        }
        return dataModel;
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

    /**
     * Returns the response from the cache or from the VO service.
     *
     * <p>
     * A cache is always done with the option cacheable=false.<br/>
     * When cacheable is true then the cache VOservices is used. In this cache
     * the results are loaded to the VO servers once a day.<br/>
     * When cacheable is false then the cache VOservices#solarBodies is used. In
     * this cache the results are loaded to the VO servers each two minutes.
     * </p>
     *
     * @param csQuery cone search query
     * @param longitude right ascension of the cone's center
     * @param latitude latitude of the cone's center
     * @param radius radius of the cone
     * @param isEnabled cache enable or disable
     * @return the response from the cache or from the VO service
     * @throws Exception - an error occurs dwhen calling the server
     */
    private List<Map<Field, String>> useCacheHealpix(final ConeSearchQuery csQuery, final double longitude, final double latitude, final double radius, final boolean isEnabled) throws Exception {
        final String applicationID = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getId();
        final String cacheID = SingletonCacheHealpixDataAccess.generateId(applicationID, String.valueOf(userParameters.getOrder()), String.valueOf(userParameters.getHealpix()));
        final CacheManager cacheManager = SingletonCacheHealpixDataAccess.getInstance();
        final Cache cache = (isEnabled) ? cacheManager.getCache("VOservices") : cacheManager.getCache("VOservices#solarBodies");
        List<Map<Field, String>> response;
        if (cache.isKeyInCache(cacheID)) {
            LOG.log(Level.INFO, "Use of the cache for ID {0}", cacheID);
            response = (List<Map<Field, String>>) cache.get(cacheID).getObjectValue();
        } else {
            response = csQuery.getResponseAt(longitude, latitude, radius);
            LOG.log(Level.INFO, "Caching result for ID {0}", cacheID);
            final Element element = new Element(cacheID, response);
            cache.put(element);
        }
        return response;
    }

    /**
     * Returns True when the cache is enabled otherwise False.
     *
     * @return True when the cache is enabled otherwise False
     */
    private boolean cacheIsEnabled() {
        return Boolean.parseBoolean(((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getParameter("cacheable").getValue());
    }

    /**
     * Filters the response.
     *
     * <p>
     * the
     * <code>feature</code> is cleared : <ul> <li>when the query mode is Healpix
     * and the record is not in the queried Healpix pixel</li>
     * <li>when is not valid</li> </ul> </p>
     *
     * @param feature record
     * @param longitude right asciension
     * @param latitude latitude
     * @see #isValid(java.util.Map)
     */
    private void spatialFilter(final FeatureDataModel feature, final double longitude, final double latitude) {
        if (isValid(feature)) {
            if (!isPointIsInsidePixel(longitude, latitude)) {
                LOG.log(Level.FINE, "This record {0} is ignored.", feature.toString());
                feature.clear();
            }
        } else {
            LOG.log(Level.WARNING, "The record record {0} is not valid : No identifier found in the response.", feature.toString());
            feature.clear();
        }
    }

    /**
     * Returns the list of distinct fields from the response.
     * <p>
     * if response is empty, then returns an empty list. </p>
     *
     * @param response respone
     * @return the list of distinct fields from the response
     */
    private Set<Field> getFields(final List<Map<Field, String>> response) {
        Set<Field> fields;
        if (response.isEmpty()) {
            fields = new HashSet<Field>();
        } else {
            final Map<Field, String> map = response.get(0);
            fields = map.keySet();
        }
        return fields;
    }

    /**
     * Parses the description TAG of each field and sets it to
     * <code>dico</code>.
     *
     * @param fields keywords of the response
     */
    private void fillDictionary(final Set<Field> fields) {
        final Iterator<Field> fieldIter = fields.iterator();
        final VoDictionary vodico = new VoDictionary();
        while (fieldIter.hasNext()) {
            final Field field = fieldIter.next();
            if (field.getDESCRIPTION() == null) {
                vodico.setDescription(null);
                vodico.setUnit(field.getUnit());
            } else {
                vodico.setDescription(field.getDESCRIPTION().getContent().get(0).toString());
                vodico.setUnit(field.getUnit());
            }
            this.dico.put(field.getName(), vodico);
        }        
    }

    /**
     * Returns true when identifier is set.
     *
     * @param feature data model
     * @return true when identifier os set
     */
    private boolean isValid(final FeatureDataModel feature) {
        return !feature.getIdentifier().isEmpty();
    }

    /**
     * Returns true when the point (longitude,latitude) is inside the
     * pixel otherwise false.
     *
     * @param longitude longitude in degree
     * @param latitude latitude in degree
     * @return true when the point (longitude,latitude) is inside the
     * pixel otherwise false
     */
    private boolean isPointIsInsidePixel(final double longitude, final double latitude) {
        boolean result;
        try {
            final long healpixFromService = this.userParameters.getHealpixIndex().ang2pix_nest(Math.PI / 2 - Math.toRadians(latitude), Math.toRadians(longitude));
            result = healpixFromService == this.userParameters.getHealpix();
        } catch (Exception ex) {
            result = false;
            LOG.log(Level.WARNING, null, ex);
        }
        return result;
    }

    /**
     * User parameters.
     */
    public class UserParameters {

        /**
         * Value when Healpix is not defined.
         */
        public static final int NOT_DEFINED = -1;
        /**
         * Multiplation factor to embed the entire Healpix pixel in the ROI.
         */
        private static final double MULT_FACT = 1.5;
        /**
         * Max value in degree of latitude axis.
         */
        private static final double MAX_DEC = 90.;
        /**
         * One degree.
         */
        private static final double ONE_DEG = 1.0;
        /**
         * One degree in arsec.
         */
        private static final double ONE_DEG_IN_ARSEC = 3600.;
        /**
         * Arcsec to degree conversion.
         */
        private static final double ARCSEC2DEG = ONE_DEG / ONE_DEG_IN_ARSEC;
        /**
         * longitude.
         */
        private transient double longitude;
        /**
         * latitude.
         */
        private transient double latitude;
        /**
         * radius.
         */
        private transient double radius;
        /**
         * Healpix order.
         */
        private int order;
        /**
         * Healpix pixel.
         */
        private long healpix;
        /**
         * Healpix index.
         */
        private HealpixIndex healpixIndex;

        /**
         * Coordinate system of query.
         */
        private CoordinateSystem coordSystem;

        /**
         * Returns the coordinate system.
         * @return the coordinate system
         */
        public final CoordinateSystem getCoordSystem() {
            return coordSystem;
        }

        /**
         * Sets the coordinate system.
         * @param coordSystemVal the coordinate system to set
         */
        private void setCoordSystem(final CoordinateSystem coordSystemVal) {
            this.coordSystem = coordSystemVal;
        }
        
        /**
         * Returns the Healpix index.
         *
         * @return the Healpix index
         */
        public final HealpixIndex getHealpixIndex() {
            return this.healpixIndex;
        }

        /**
         * Sets the Healpix Index.
         *
         * @param healpixIndexVal the Healpix index
         */
        private void setHealpixIndex(final HealpixIndex healpixIndexVal) {
            this.healpixIndex = healpixIndexVal;
        }

        /**
         * Returns the Healpix order.
         *
         * @return Healpix order
         */
        public final int getOrder() {
            return this.order;
        }

        /**
         * Sets the order.
         *
         * @param orderVal order
         */
        private void setOrder(final int orderVal) {
            this.order = orderVal;
        }

        /**
         * Returns the Healpix pixel.
         *
         * @return the Healpix pixel
         */
        public final long getHealpix() {
            return this.healpix;
        }

        /**
         * Sets the Healpix value.
         *
         * @param healpixVal Healpix value
         */
        private void setHealpix(final long healpixVal) {
            this.healpix = healpixVal;
        }
        
        /**
         * Returns the longitude in degree.
         *
         * @return the longitude in degree
         */
        public final double getLongitude() {
            return this.longitude;
        }

        /**
         * Sets the longitude.
         *
         * @param longitudeVal longitude
         */
        private void setLongitude(final double longitudeVal) {
            this.longitude = longitudeVal;
        }

        /**
         * Returns the latitude in degree.
         *
         * @return the latitude
         */
        public final double getLatitude() {
            return this.latitude;
        }

        /**
         * Sets the latitude.
         *
         * @param latitudeVal latitude
         */
        private void setLatitude(final double latitudeVal) {
            this.latitude = latitudeVal;
        }

        /**
         * Returns the size in degree.
         *
         * @return the size
         */
        public final double getRadius() {
            return this.radius;
        }

        /**
         * Sets the radius.
         *
         * @param radiusVal radius
         */
        private void setRadius(final double radiusVal) {
            this.radius = radiusVal;
        }        

        /**
         * Constructor.
         *
         * @param form form
         * @throws Exception Exception
         */
        public UserParameters(final Form form) throws Exception {
            computeInputChoice(form);
        }

        /**
         * Computes input parameters.
         *
         * @param form form
         * @throws Exception Exception
         */
        private void computeInputChoice(final Form form) throws Exception {
            final String healpixInput = form.getFirstValue("healpix");
            final String orderInput = form.getFirstValue("order");
            setCoordSystem(CoordinateSystem.valueOf(form.getFirstValue("coordSystem")));
            if (Utility.isSet(healpixInput) && Utility.isSet(orderInput) && Utility.isSet(getCoordSystem())) {           
                setHealpix(Long.valueOf(healpixInput));
                setOrder(Integer.valueOf(orderInput));
                final int nside = (int) Math.pow(2, order);
                final double pixRes = HealpixIndex.getPixRes(nside) * ARCSEC2DEG;
                setHealpixIndex(new HealpixIndex(nside, Scheme.NESTED));
                final Pointing pointing = healpixIndex.pix2ang(healpix);
                setLongitude(Math.toDegrees(pointing.phi));
                setLatitude(MAX_DEC - Math.toDegrees(pointing.theta));
                setRadius(pixRes * MULT_FACT);
            } else {
                throw new Exception("wrong input parameters ");                
            }
        }
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
}
