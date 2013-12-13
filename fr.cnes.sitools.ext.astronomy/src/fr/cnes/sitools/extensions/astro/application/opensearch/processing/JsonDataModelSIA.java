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
package fr.cnes.sitools.extensions.astro.application.opensearch.processing;

import fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary;
import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeatureDataModel;
import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeaturesDataModel;
import fr.cnes.sitools.extensions.astro.application.opensearch.responsibility.SiaHealpix;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.Utility;
import java.awt.geom.Point2D;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jsky.coords.WCSKeywordProvider;
import jsky.coords.WCSTransform;
import net.ivoa.xml.votable.v1.Field;

/**
 * Tansforms the server response in a data model that allowsto use the
 * GeoJsonRepresentation.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
class JsonDataModelSIA extends AbstractJsonDataModel implements WCSKeywordProvider {

    /**
     * Coordinate system.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystem;
    /**
     * Server response.
     */
    private List<Map<Field, String>> response;
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(JsonDataModelSIA.class.getName());
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
     * Constructor.
     *
     * @param responseVal server response
     * @param coordinateSystemVal coordinate system
     */
    public JsonDataModelSIA(final List<Map<Field, String>> responseVal, final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        setResponse(responseVal);
        setCoordinateSystem(coordinateSystemVal);
    }

    @Override
    public FeaturesDataModel getDataModel() {
        final FeaturesDataModel dataModel = new FeaturesDataModel();
        for (Map<Field, String> iterDoc : response) {
            this.doc = iterDoc;
            final FeatureDataModel feature = parseRow(doc);
            dataModel.updateFeatureWithSpecialCase(feature);
            dataModel.addFeature(feature);
        }
        return dataModel;
    }

    /**
     * Parses a row and returns the <code>feature</code> data model.
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
                final SiaHealpix.ReservedWords ucdWord = SiaHealpix.ReservedWords.find(ucd);
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
                            LOG.log(Level.SEVERE, "No number has been provided for " + field.getName() +" - skip the attribute", ex);
                        }
                        break;
                }
            }
        }

        String coordinates;
        String shape;
        if (wcs.isWCS()) {
            double[] polygonCelest = computeWcsCorner(wcs);
            switch (getCoordinateSystem()) {
                case GALACTIC:
                    for (int i = 0; i < polygonCelest.length; i = i + 2) {
                        astroCoordinates.setRaAsDecimal(polygonCelest[i]);
                        astroCoordinates.setDecAsDecimal(polygonCelest[i + 1]);
                        astroCoordinates.setCoordinateSystem(AstroCoordinate.CoordinateSystem.EQUATORIAL);
                        astroCoordinates.processTo(AstroCoordinate.CoordinateSystem.GALACTIC);
                        polygonCelest[i] = astroCoordinates.getRaAsDecimal();
                        polygonCelest[i + 1] = astroCoordinates.getDecAsDecimal();
                    }
                    break;
                case EQUATORIAL:
                    break;
                default:
                    break;
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
        dataModel.createCrs(getCoordinateSystem().getCrs());
        if (hasPreview(format, download)) {
            try {
                dataModel.setQuicklook(new URL(download));
            } catch (MalformedURLException ex) {
                Logger.getLogger(SiaHealpix.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (hasFileToDownload(format, download)) {
            try {
                dataModel.createServices(format, new URL(download));
            } catch (MalformedURLException ex) {
                Logger.getLogger(SiaHealpix.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return dataModel;
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
     * Returns the coordinate system.
     *
     * @return the coordinateSystem
     */
    protected final AstroCoordinate.CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Sets the coordinate system.
     *
     * @param coordinateSystemVal the coordinateSystem to set
     */
    protected final void setCoordinateSystem(final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        this.coordinateSystem = coordinateSystemVal;
    }

    /**
     * Returns the response.
     *
     * @return the response
     */
    protected final List<Map<Field, String>> getResponse() {
        return response;
    }

    /**
     * Sets the response.
     *
     * @param responseVal the response to set
     */
    protected final void setResponse(final List<Map<Field, String>> responseVal) {
        this.response = responseVal;
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
}
