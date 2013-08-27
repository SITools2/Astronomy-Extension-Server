/*******************************************************************************
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
 ******************************************************************************/
package fr.cnes.sitools.extensions.astro.application.opensearch;

import fr.cnes.sitools.astro.representation.GeoJsonRepresentation;
import fr.cnes.sitools.astro.vo.sia.SIASearchQuery;
import fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.OpenSearchVOSiaSearchApplicationPlugin;
import fr.cnes.sitools.extensions.cache.CacheBrowser;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem;
import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.extensions.common.VoDictionary;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.awt.geom.Point2D;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jsky.coords.WCSKeywordProvider;
import jsky.coords.WCSTransform;
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
import org.restlet.ext.wadl.OptionInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Provides a search capability on a Simple Image Access service by the use of
 * (healpix,order) parameters.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOSiaSearch extends SitoolsParameterizedResource implements WCSKeywordProvider {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(OpenSearchVOSiaSearch.class.getName());
    /**
     * Index in polygonCelest array of the X coordinate of the first point of
     * the polygon.
     */
    private static final int P1_X = 0;
    /**
     * Index in polygonCelest array of the Y coordinate of the first point of
     * the polygon.
     */
    private static final int P1_Y = 1;
    /**
     * Index in polygonCelest array of the X coordinate of the second point of
     * the polygon.
     */
    private static final int P2_X = 2;
    /**
     * Index in polygonCelest array of the Y coordinate of the second point of
     * the polygon.
     */
    private static final int P2_Y = 3;
    /**
     * Index in polygonCelest array of the X coordinate of the third point of
     * the polygon.
     */
    private static final int P3_X = 4;
    /**
     * Index in polygonCelest array of the Y coordinate of the third point of
     * the polygon.
     */
    private static final int P3_Y = 5;
    /**
     * Index in polygonCelest array of the X coordinate of the fourth point of
     * the polygon.
     */
    private static final int P4_X = 6;
    /**
     * Index in polygonCelest array of the Y coordinate of the fourth point of
     * the polygon.
     */
    private static final int P4_Y = 7;
    /**
     * Number of coordinates in the polygon. The first point is the last one.
     */
    private static final int NUMBER_COORDINATES_POLYGON = 8;
    /**
     * Origin in FITS along X.
     */
    private static final double ORIGIN_X = 0.5;
    /**
     * Origin in FITS along Y.
     */
    private static final double ORIGIN_Y = 0.5;
    /**
     * Healpix Nside where the processing for the intersection is done.
     */
    private static final int HEALPIX_RESOLUTION = 128;
    /**
     * Query.
     */
    private transient SIASearchQuery query;
    /**
     * User parameters.
     */
    private transient OpenSearchVOSiaSearch.UserParameters userParameters;
    /**
     * data model.
     */
    private transient Map<Field, String> doc;
    /**
     * Mapping keyword<-->UCD.
     */
    private static final Map<String, String[]> MAPPING_KEYWORD_UCD = new HashMap<String, String[]>() {
        {
            put("CD1_1", new String[]{"VOX:WCS_CDMatrix", "0"});
            put("CD1_2", new String[]{"VOX:WCS_CDMatrix", "1"});
            put("CD2_1", new String[]{"VOX:WCS_CDMatrix", "2"});
            put("CD2_2", new String[]{"VOX:WCS_CDMatrix", "3"});
            put("NAXIS1", new String[]{"VOX:Image_Naxis", "0"});
            put("NAXIS2", new String[]{"VOX:Image_Naxis", "1"});
            put("CRVAL1", new String[]{"VOX:WCS_CoordRefValue", "0"});
            put("CRVAL2", new String[]{"VOX:WCS_CoordRefValue", "1"});
            put("CRPIX1", new String[]{"VOX:WCS_CoordRefPixel", "0"});
            put("CRPIX2", new String[]{"VOX:WCS_CoordRefPixel", "1"});
            put("CTYPE1", new String[]{"VOX:WCS_CoordProjection"});
            put("CTYPE2", new String[]{"VOX:WCS_CoordProjection"});
            put("RADESYS", new String[]{"VOX:STC_CoordRefFrame"});
            put("EQUINOX", new String[]{"VOX:STC_CoordEquinox"});
        }
    };

    /**
     * Reserved keywords in SIAP for which a specific processing is needed.
     */
    public enum ReservedWords {

        /**
         * Right ascension.
         */
        POS_EQ_RA_MAIN("POS_EQ_RA_MAIN"),
        /**
         * Declination.
         */
        POS_EQ_DEC_MAIN("POS_EQ_DEC_MAIN"),
        /**
         * Title.
         */
        IMAGE_TITLE("VOX:Image_Title"),
        /**
         * Observation date in MJD.
         */
        IMAGE_MJDATEOBS("VOX:Image_MJDateObs"),
        /**
         * Access to the image.
         */
        IMAGE_ACCESS_REFERENCE("VOX:Image_AccessReference"),
        /**
         * Image format.
         */
        IMAGE_FORMAT("VOX:Image_Format"),
        /**
         * Reference pixel on the CCD.
         */
        WCS_COORD_REF_PIXEL("VOX:WCS_CoordRefPixel"),
        /**
         * Reference value on the sky.
         */
        WCS_COORD_REF_VALUE("VOX:WCS_CoordRefValue"),
        /**
         * Matrix: rotation and scale.
         */
        WCS_CDMATRIX("VOX:WCS_CDMatrix"),
        /**
         * Position.
         */
        POS_EQ("pos.eq"),
        /**
         * CCD dimension.
         */
        IMAGE_NAXIS("VOX:Image_Naxis"),
        /**
         * CCD scale.
         */
        IMAGE_SCALE("VOX:Image_Scale"),
        /**
         * No keyword.
         */
        NONE("");
        /**
         * name.
         */
        private final String name;

        /**
         * Constructor.
         *
         * @param nameVal name
         */
        ReservedWords(final String nameVal) {
            this.name = nameVal;
        }

        /**
         * Returns the name.
         *
         * @return the name
         */
        public final String getName() {
            return this.name;
        }

        /**
         * Finds the enum from its name.
         *
         * <p>
         * If keyword is null, then ReservedWords.NONE is returned</p>
         *
         * @param keyword keyword name
         * @return the enum
         */
        public static ReservedWords find(final String keyword) {
            ReservedWords response = ReservedWords.NONE;
            if (Utility.isSet(keyword)) {
                final ReservedWords[] words = ReservedWords.values();
                for (ReservedWords word : words) {
                    final String reservedName = word.getName();
                    if (keyword.equals(reservedName)) {
                        response = word;
                        break;
                    }
                }
            }
            return response;
        }
    }
    /**
     * VO dictionary.
     */
    private transient Map<String, VoDictionary> dico;

    @Override
    public final void doInit() {
        try {
            super.doInit();
            final String url = ((OpenSearchVOSiaSearchApplicationPlugin) getApplication()).getModel().getParametersMap().get("siaSearchURL").getValue();
            this.query = new SIASearchQuery(url);
            if (!getRequest().getMethod().equals(Method.OPTIONS)) {
                this.userParameters = new OpenSearchVOSiaSearch.UserParameters(getRequest().getResourceRef().getQueryAsForm());
                this.dico = ((OpenSearchVOSiaSearchApplicationPlugin) getApplication()).getDico();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, null, ex);
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
        }
    }

    /**
     * Parses a row and returns the
     * <code>feature</code> data model.
     *
     * @param row row to be parsed
     * @return the data model for one record
     */
    protected final FeatureDataModel parseRow(final Map<Field, String> row) {
        final FeatureDataModel dataModel = new FeatureDataModel();
        final AstroCoordinate astroCoordinates = new AstroCoordinate();
        final WCSTransform wcs = new WCSTransform(this);
        String format = null;
        String download = null;
        double raValue = Double.NaN;
        double decValue = Double.NaN;
        for (Map.Entry<Field, String> entryField : row.entrySet()) {
            final Field field = entryField.getKey();
            final String ucd = field.getUcd();
            final net.ivoa.xml.votable.v1.DataType dataType = field.getDatatype();
            final String value = entryField.getValue();

            if (Utility.isSet(value) && !value.isEmpty()) {
                Object response;
                final ReservedWords ucdWord = ReservedWords.find(ucd);
                switch (ucdWord) {
                    case POS_EQ_RA_MAIN:
                        raValue = Utility.parseRaVO(row, field);
                        break;
                    case POS_EQ_DEC_MAIN:
                        decValue = Utility.parseDecVO(row, field);
                        break;
                    case IMAGE_TITLE:
                        response = Utility.getDataType(dataType, value);
                        dataModel.setIdentifier((String) response);
                        break;
                    case IMAGE_MJDATEOBS:
                        response = Utility.getDataType(dataType, value);
                        dataModel.addDateObs(Utility.modifiedJulianDateToISO(Double.valueOf(String.valueOf(response))));
                        break;
                    case IMAGE_ACCESS_REFERENCE:
                        response = Utility.getDataType(dataType, value);
                        download = String.valueOf(response);
                        break;
                    case IMAGE_FORMAT:
                        response = Utility.getDataType(dataType, value);
                        format = String.valueOf(response);
                        break;
                    case WCS_COORD_REF_PIXEL:
                        break;
                    case WCS_COORD_REF_VALUE:
                        break;
                    case WCS_CDMATRIX:
                        break;
                    case POS_EQ:
                        break;
                    case IMAGE_NAXIS:
                        break;
                    case IMAGE_SCALE:
                        break;
                    default:
                        try {
                        response = Utility.getDataType(dataType, value);
                        dataModel.addProperty(field.getName(), response);
                    } catch (NumberFormatException ex) {
                        dataModel.addProperty(field.getName(), value);
                    }
                        break;
                }
            }
        }

        String coordinates;
        String shape;
        if (wcs.isWCS()) {
            double[] polygonCelest = computeWcsCorner(wcs);
            if (this.userParameters.getCoordSystem() == CoordinateSystem.GALACTIC) {
                for (int i = 0; i < polygonCelest.length; i = i + 2) {
                    astroCoordinates.setRaAsDecimal(polygonCelest[i]);
                    astroCoordinates.setDecAsDecimal(polygonCelest[i + 1]);
                    astroCoordinates.setCoordinateSystem(CoordinateSystem.EQUATORIAL);
                    astroCoordinates.processTo(CoordinateSystem.GALACTIC);
                    polygonCelest[i] = astroCoordinates.getRaAsDecimal();
                    polygonCelest[i + 1] = astroCoordinates.getDecAsDecimal();
                }
            }
            coordinates = String.format("[[[%s,%s],[%s,%s],[%s,%s],[%s,%s],[%s,%s]]]", polygonCelest[P1_X], polygonCelest[P1_Y],
                    polygonCelest[P2_X], polygonCelest[P2_Y],
                    polygonCelest[P3_X], polygonCelest[P3_Y],
                    polygonCelest[P4_X], polygonCelest[P4_Y],
                    polygonCelest[P1_X], polygonCelest[P1_Y]);
            shape = "Polygon";
        } else {
            // No WCS, then we have only the central position of the FOV
            coordinates = String.format("[%s,%s]", raValue, decValue);
            shape = "Point";
        }
        LOG.log(Level.FINEST, "geometry.coordinates: {0}", coordinates);
        dataModel.createGeometry(coordinates, shape);
        dataModel.createCrs(userParameters.getCoordSystem().getCrs());
        if (hasPreview(format, download)) {
            try {
                dataModel.setQuicklook(new URL(download));
            } catch (MalformedURLException ex) {
                Logger.getLogger(OpenSearchVOSiaSearch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (hasFileToDownload(format, download)) {
            try {
                dataModel.createServices(format, new URL(download));
            } catch (MalformedURLException ex) {
                Logger.getLogger(OpenSearchVOSiaSearch.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        spatialFilter(dataModel.getFeature(), dataModel.getGeometry());
        return dataModel;
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
     * @param geometry geometry
     * @see #isValid(java.util.Map)
     */
    private void spatialFilter(final Map feature, final Map geometry) {
        if (isValid(feature)) {
            if (!isGeometryIsInsidePixel(geometry)) {
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
            final Map<Field, String> mapResponse = response.get(0);
            fields = mapResponse.keySet();
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
        while (fieldIter.hasNext()) {
            final Field field = fieldIter.next();
            final String description = (field.getDESCRIPTION() == null) ? null : field.getDESCRIPTION().getContent().get(0).toString();
            this.dico.put(field.getName(), new VoDictionary(description, field.getUnit()));
        }
    }

    /**
     * Returns true when the geometry intersects with the pixel otherwise false.
     *
     * @param geometry image geometry (point or polygon)
     * @return true when the geometry intersects with the pixel otherwise false.
     */
    private boolean isGeometryIsInsidePixel(final Map geometry) {
        boolean result;
        final String type = String.valueOf(geometry.get(FeatureDataModel.GEOMETRY_TYPE));
        String coordinates = String.valueOf(geometry.get(FeatureDataModel.GEOMETRY_COORDINATES));
        if ("Point".equals(type)) {
            coordinates = coordinates.replace("[", "");
            coordinates = coordinates.replace("]", "");
            final String[] coordinateArray = coordinates.split(",");
            final double rightAscension = Double.valueOf(coordinateArray[0]);
            final double declination = Double.valueOf(coordinateArray[1]);
            try {
                final long healpixFromService = this.userParameters.getHealpixIndex().ang2pix_nest(Math.PI / 2 - Math.toRadians(declination),
                        Math.toRadians(rightAscension));
                result = healpixFromService == this.userParameters.getHealpix();
            } catch (Exception ex) {
                result = false;
            }

        } else if ("Polygon".equals(type)) {
            final String[] coordinateArray = coordinates.split("\\],\\[");
            final Pointing[] pointings = new Pointing[coordinateArray.length - 1];
            for (int i = 0; i < coordinateArray.length - 1; i++) {
                coordinateArray[i] = coordinateArray[i].replace("]", "");
                coordinateArray[i] = coordinateArray[i].replace("[", "");
                final String[] skyCoordinate = coordinateArray[i].split(",");
                final double rightAscension = Double.valueOf(skyCoordinate[0]);
                final double declination = Double.valueOf(skyCoordinate[1]);
                final Pointing pointing = new Pointing(Math.PI / 2 - Math.toRadians(declination), Math.toRadians(rightAscension));
                pointings[i] = pointing;
            }
            try {
                final RangeSet range = this.userParameters.getHealpixIndex().queryPolygonInclusive(pointings, HEALPIX_RESOLUTION);
                result = range.contains(this.userParameters.getHealpix());
            } catch (Exception ex) {
                result = false;
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Returns true when the identifier is set.
     *
     * @param feature data model
     * @return true when identifier os set
     */
    private boolean isValid(final Map feature) {
        return ((Map) feature.get(FeatureDataModel.PROPERTIES)).containsKey(FeatureDataModel.PROPERTIES_ID);
    }

    /**
     * Computes for each corner of the camera its position in the sky.
     *
     * @param wcs Word Coordinate System
     * @return the position of the sky of each corner
     */
    private double[] computeWcsCorner(final WCSTransform wcs) {
        final double[] polygonCelest = new double[NUMBER_COORDINATES_POLYGON];
        final double heightPix = wcs.getHeight();
        final double widthPix = wcs.getWidth();
        final double[] polygonPix = {ORIGIN_X, heightPix + ORIGIN_Y,
            widthPix + ORIGIN_X, heightPix + ORIGIN_Y,
            widthPix + ORIGIN_X, ORIGIN_Y,
            ORIGIN_X, ORIGIN_Y};
        for (int i = 0; i < polygonPix.length; i += 2) {
            final Point2D.Double ptg = wcs.pix2wcs(polygonPix[i], polygonPix[i + 1]);
            polygonCelest[i] = ptg.getX();
            polygonCelest[i + 1] = ptg.getY();
        }
        return polygonCelest;
    }

    /**
     * Returns true when the SIA response contains a file to download.
     *
     * @param format file format
     * @param url file to download
     * @return True when the SIA response contains a file to download
     */
    private boolean hasFileToDownload(final String format, final String url) {
        return (format != null && url != null);
    }

    /**
     * Returns true when the SIA response contains a preview.
     *
     * @param format file format
     * @param url url to test
     * @return True when the SIA response contains a preview.
     */
    private boolean hasPreview(final String format, final String url) {
        return (format != null && url != null && SimpleImageAccessProtocolLibrary.GraphicBrowser.contains(format));
    }

    /**
     * Returns the JSON representation.
     *
     * @return the JSON representation
     */
    @Get
    public final Representation getJsonResponse() {
        try {
            double rightAscension = userParameters.getLongitude();
            double declination = userParameters.getLatitude();
            if (this.userParameters.getCoordSystem() == CoordinateSystem.GALACTIC) {
                final AstroCoordinate astroCoordinates = new AstroCoordinate(rightAscension, declination);
                astroCoordinates.setCoordinateSystem(CoordinateSystem.GALACTIC);
                astroCoordinates.processTo(CoordinateSystem.EQUATORIAL);
                rightAscension = astroCoordinates.getRaAsDecimal();
                declination = astroCoordinates.getDecAsDecimal();
            }
            final double size = userParameters.getSize();
            final List<Map<Field, String>> response = useCacheHealpix(query, rightAscension, declination, size, true);
            final Map dataModel = createGeoJsonDataModel(response);
            final Representation rep = new GeoJsonRepresentation(dataModel);
            return useCacheBrowser(rep, true);
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
    private Map createGeoJsonDataModel(final List<Map<Field, String>> response) {
        final FeaturesDataModel dataModel = new FeaturesDataModel();
        final Set<Field> fields = getFields(response);
        fillDictionary(fields);
        for (Map<Field, String> iterDoc : response) {
            this.doc = iterDoc;
            final FeatureDataModel feature = parseRow(doc);
            dataModel.updateFeatureWithSpecialCase(feature);            
            dataModel.addFeature(feature);            
        }
        return dataModel.getFeatures();
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
     * @param siaQuery SIA query
     * @param longitude longitude of the cone's center
     * @param latitude latitude of the cone's center
     * @param radius radius of the cone
     * @param isEnabled cache enable or disable
     * @return the response from the cache or from the VO service
     * @throws Exception - an error occurs when calling the server
     */
    private List<Map<Field, String>> useCacheHealpix(final SIASearchQuery siaQuery, final double longitude, final double latitude, final double radius, final boolean isEnabled) throws Exception {
        final String applicationID = ((OpenSearchVOSiaSearchApplicationPlugin) getApplication()).getId();
        final String cacheID = SingletonCacheHealpixDataAccess.generateId(applicationID, String.valueOf(userParameters.getOrder()), String.valueOf(userParameters.getHealpix()));
        final CacheManager cacheManager = SingletonCacheHealpixDataAccess.getInstance();
        final Cache cache = cacheManager.getCache("VOservices");
        List<Map<Field, String>> response;

        if (cache.isKeyInCache(cacheID) && isEnabled) {
            LOG.log(Level.INFO, "Use of the cache for ID {0}", cacheID);
            response = (List<Map<Field, String>>) cache.get(cacheID).getObjectValue();
        } else if (isEnabled) {
            response = siaQuery.getResponseAt(longitude, latitude, radius);
            LOG.log(Level.INFO, "Caching result for ID {0}", cacheID);
            final Element element = new Element(cacheID, response);
            cache.put(element);
        } else {
            response = siaQuery.getResponseAt(longitude, latitude, radius);
        }
        return response;
    }

    @Override
    public final boolean findKey(final String key) {
        boolean isFound = false;
        final String[] ucdObj = MAPPING_KEYWORD_UCD.get(key);
        final Set<Field> fields = this.doc.keySet();
        final Iterator<Field> fieldIter = fields.iterator();
        while (fieldIter.hasNext()) {
            final Field field = fieldIter.next();
            final String ucd = field.getUcd();
            if (ucd != null && ucdObj != null && ucdObj.length != 0 && ucdObj[0].equals(ucd)) {
                isFound = true;
                break;
            }
        }
        return isFound;
    }

    @Override
    public final String getStringValue(final String key) {
        String value = null;
        final String[] ucdObj = MAPPING_KEYWORD_UCD.get(key);
        final Set<Field> fields = this.doc.keySet();
        final Iterator<Field> fieldIter = fields.iterator();
        while (fieldIter.hasNext()) {
            final Field field = fieldIter.next();
            final String ucd = field.getUcd();
            if (ucd != null && ucdObj != null && ucd.equals(ucdObj[0])) {
                if (ucdObj.length == 2) {
                    final String fieldValue = this.doc.get(field);
                    if (fieldValue != null) {
                        value = fieldValue.split(" ")[Integer.valueOf(ucdObj[1])];
                    }
                } else {
                    value = this.doc.get(field);
                }
            }
        }
        if (key.equalsIgnoreCase("CTYPE1")) {
            value = "RA---TAN";
        } else if (key.equalsIgnoreCase("CTYPE2")) {
            value = "DEC--TAN";
        }
        return value;
    }

    @Override
    public final String getStringValue(final String key, final String defaultValue) {
        final String val = getStringValue(key);
        return (val == null) ? defaultValue : val;
    }

    @Override
    public final double getDoubleValue(final String key) {
        final String val = getStringValue(key);
        return (val == null || val.isEmpty()) ? 0.0 : Double.valueOf(val);
    }

    @Override
    public final double getDoubleValue(final String key, final double defaultValue) {
        final String val = getStringValue(key);
        return (val == null || val.isEmpty()) ? defaultValue : Double.valueOf(val);
    }

    @Override
    public final float getFloatValue(final String key) {
        final String val = getStringValue(key);
        return (val == null || val.isEmpty()) ? 0 : Float.valueOf(val);
    }

    @Override
    public final float getFloatValue(final String key, final float defaultValue) {
        final String val = getStringValue(key);
        return (val == null || val.isEmpty()) ? defaultValue : Float.valueOf(val);
    }

    @Override
    public final int getIntValue(final String key) {
        final String val = getStringValue(key);
        return (val == null || val.isEmpty()) ? 0 : Integer.valueOf(val);
    }

    @Override
    public final int getIntValue(final String key, final int defaultValue) {
        final String val = getStringValue(key);
        return (val == null || val.isEmpty()) ? defaultValue : Integer.valueOf(val);
    }

    /**
     * Parses user parameters from the request.
     */
    private static class UserParameters {

        /**
         * Longitude depending the crs.
         */
        private double longitude;
        /**
         * latitude depending the crs.
         */
        private double latitude;
        /**
         * size of the ROI.
         */
        private double size;
        /**
         * Healpix is not defined.
         */
        public static final int NOT_DEFINED = -1;
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
         * Coordinate system.
         */
        private CoordinateSystem coordSystem;

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
         * Max value in degree of latitude axis.
         */
        private static final double MAX_DEC = 90.;
        /**
         * Multiplation factor to embed the entire Healpix pixel in the ROI.
         */
        private static final double MULT_FACT = 1.5;

        /**
         * Constructor.
         *
         * @param form request parameters
         * @throws Exception if user input parameters are wrong
         */
        public UserParameters(final Form form) throws Exception {
            checkInputs(form);
        }

        /**
         * Checks inputs and computes Ra, Dec and Size.
         *
         * @param form request parameters
         * @throws Exception if user input parameters are wrong
         */
        private void checkInputs(final Form form) throws Exception {
            final String healpixInput = form.getFirstValue("healpix");
            final String orderInput = form.getFirstValue("order");
            setCoordSystem(CoordinateSystem.valueOf(form.getFirstValue("coordSystem")));
            if (healpixInput == null || orderInput == null || getCoordSystem() == null || !Arrays.asList(CoordinateSystem.values()).contains(getCoordSystem())) {
                throw new Exception("wrong input parameters");
            } else {
                setHealpix(Long.valueOf(healpixInput));
                setOrder(Integer.valueOf(orderInput));
                final int nside = (int) Math.pow(2, order);
                final double pixRes = HealpixIndex.getPixRes(nside) * ARCSEC2DEG;
                setHealpixIndex(new HealpixIndex(nside, Scheme.NESTED));
                final Pointing pointing = healpixIndex.pix2ang(healpix);
                setLongitude(Math.toDegrees(pointing.phi));
                setLatitude(MAX_DEC - Math.toDegrees(pointing.theta));
                setSize(pixRes * MULT_FACT);
            }
        }

        /**
         * Returns the coordinate system.
         *
         * @return the coordinate system
         */
        public final CoordinateSystem getCoordSystem() {
            return coordSystem;
        }

        /**
         * Sets the coordinate system.
         *
         * @param coordSystemVal the coordinate system tp set
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
        public final double getSize() {
            return this.size;
        }

        /**
         * Sets the size.
         *
         * @param sizeVal size
         */
        private void setSize(final double sizeVal) {
            this.size = sizeVal;
        }
    }

    @Override
    public final void sitoolsDescribe() {
        setName("Simple Image Access service.");
        setDescription("Retrieves and transforms a response from a Simple Image Access service.");
    }

    /**
     * Describes GET method in the WADL.
     *
     * @param info information
     */
    @Override
    protected final void describeGet(final MethodInfo info) {
        this.addInfo(info);
        info.setIdentifier("SimpleImageAccessProtocolJSON");
        info.setDocumentation("Interoperability service to distribute images through a converted format of the Simple Image Access Protocol");

        final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
        parametersInfo.add(new ParameterInfo("healpix", true, "long", ParameterStyle.QUERY,
                "Healpix number"));
        parametersInfo.add(new ParameterInfo("order", true, "integer", ParameterStyle.QUERY,
                "Healpix order"));
        final ParameterInfo coordSystem = new ParameterInfo("coordSystem", true, "string", ParameterStyle.QUERY,
                "Healpix coordinate system");
        final OptionInfo optionInfoEq = new OptionInfo(CoordinateSystem.EQUATORIAL.name());
        final OptionInfo optionInfoGal = new OptionInfo(CoordinateSystem.GALACTIC.name());
        coordSystem.setOptions(Arrays.asList(optionInfoEq, optionInfoGal));
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
                + "    coordinates: [[[39.0887493969889,8.624773926387784],"
                + "                   [39.072082730322215,8.624773926387784],"
                + "                   [39.072082730322215,8.608107259721104],"
                + "                   [39.0887493969889,8.608107259721104],"
                + "                   [39.0887493969889,8.624773926387784]]],\n"
                + "    type: \"Polygon\"\n"
                + "  },\n"
                + "properties: {\n"
                + "  crs: {\n"
                + "    type: \"name\",\n"
                + "    properties: {\n"
                + "      name: \"EQUATORIAL.ICRS\"\n"
                + "    }\n"
                + "  },\n"
                + "  identifier: \"HST0\"\n"
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
