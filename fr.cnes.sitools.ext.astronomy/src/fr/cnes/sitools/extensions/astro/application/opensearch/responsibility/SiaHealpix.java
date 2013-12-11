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
package fr.cnes.sitools.extensions.astro.application.opensearch.responsibility;

import fr.cnes.sitools.astro.vo.conesearch.ConeSearchException;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.Utility;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Queries a SIA service based on Healpix parameters and the coordinate system.
 *
 * <p>
 * This implementation is designed by a chain of responsability pattern.<br/>
 * <code>SiaHealpix</code> parses the Healpix parameters and the coordinate system.
 * Healpix parameters are transformed in (longitude, latitude) in Equatorial frame to query the SIAP service.
 * The VO service returns the result in Equatorial frame. This result is then transformed
 * according to the coordinate system.
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SiaHealpix extends AbstractVORequest implements WCSKeywordProvider {

    /**
     * VO service URL.
     */
    private String url;
    /**
     * Healpix order.
     */
    private int order;
    /**
     * Healpix pixel.
     */
    private long healpix;
    /**
     * Healpix coordinate system.
     */
    private AstroCoordinate.CoordinateSystem coordSystem;
    /**
     * Cone search library.
     */
    private transient fr.cnes.sitools.astro.vo.sia.SIASearchQuery siaQuery;
    /**
     * Central position of the pixel along right ascension axis.
     */
    private transient double rightAscension;
    /**
     * Central position of the pixel along declinationInReferenceFrame axis.
     */
    private transient double declination;
    /**
     * Size resolution.
     */
    private transient double size;
    /**
     * Healpix index.
     */
    private transient HealpixIndex index;
    /**
     * data model.
     */
    private transient Map<Field, String> doc;
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
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(SiaHealpix.class.getName());

    /**
     * Empty constructor.
     */
    protected SiaHealpix() {
    }

    /**
     * Constructor.
     *
     * @param urlVal VO service URL
     * @param orderVal Healpix order
     * @param healpixVal Healpix pixel
     * @param coordVal Healpix coordinate system
     */
    public SiaHealpix(final String urlVal, final int orderVal, final long healpixVal, final AstroCoordinate.CoordinateSystem coordVal) {
        setUrl(urlVal);
        setOrder(orderVal);
        setHealpix(healpixVal);
        setCoordSystem(coordVal);
        LOG.log(Level.CONFIG, "URL:", urlVal);
        LOG.log(Level.CONFIG, "Order:", orderVal);
        LOG.log(Level.CONFIG, "Healpix:", healpixVal);
        LOG.log(Level.CONFIG, "Coordinate system:", coordVal.name());
    }

    /**
     * Init the query to the VO service.
     */
    protected final void initQuery() {
        this.siaQuery = new fr.cnes.sitools.astro.vo.sia.SIASearchQuery(url);
    }

    /**
     * Computes the physical parameters based on Healpix.
     * <p>
     * If the coordinate system is in GALACTIC, then the central position of the
     * pixel is transformed in EQUATORIAL because VO services only handle
     * EQUATORIAL frame.
     * </p>
     *
     * @throws Exception when a problem happens during the creation of the
     * Healpix index
     */
    private void computeGeoPhysicalParameters() throws Exception {
        final int nside = (int) Math.pow(2, getOrder());
        final double pixRes = HealpixIndex.getPixRes(nside) * ARCSEC2DEG;
        setSize(pixRes * MULT_FACT);
        index = new HealpixIndex(nside, Scheme.NESTED);
        final Pointing pointing = index.pix2ang(healpix);
        final double longitude = Math.toDegrees(pointing.phi);
        final double latitude = MAX_DEC - Math.toDegrees(pointing.theta);
        switch (getCoordSystem()) {
            case EQUATORIAL:
                setRightAscension(longitude);
                setDeclination(latitude);
                break;
            case GALACTIC:
                final AstroCoordinate astroCoordinates = new AstroCoordinate(longitude, latitude);
                astroCoordinates.setCoordinateSystem(fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem.GALACTIC);
                astroCoordinates.processTo(fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem.EQUATORIAL);
                LOG.log(Level.FINEST, String.format("Coordinates transformation from galactic to equatorial frame (%s,%s) --> (%s,%s)",
                        getRightAscension(), getDeclination(), astroCoordinates.getRaAsDecimal(), astroCoordinates.getDecAsDecimal()));
                setRightAscension(astroCoordinates.getRaAsDecimal());
                setDeclination(astroCoordinates.getDecAsDecimal());
                break;
            default:
                throw new java.util.NoSuchElementException("The coordinate system " + getCoordSystem() + " is not supported");
        }
    }

    @Override
    public final Object getResponse() {
        Object responseSia;
        try {
            initQuery();
            computeGeoPhysicalParameters();
            final List<Map<Field, String>> result = this.siaQuery.getResponseAt(getRightAscension(), getDeclination(), getSize());
            spatialFilter(result);
            responseSia = result;
        } catch (ConeSearchException ex) {
            LOG.log(Level.SEVERE, null, ex);
            responseSia = null;
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            responseSia = null;
        }
        return responseSia;
    }

    /**
     * Updates <code>result</code> by only keeping the records inside the
     *  polygon delimited by the Healpix pixel.
     *
     * @param result the updated pixel
     */
    private void spatialFilter(final List<Map<Field, String>> result) {
        final List<Map<Field, String>> pixelToCheck = new ArrayList<Map<Field, String>>(result);
        for (Map<Field, String> record : pixelToCheck) {
            if (!isInsidePolygon(record)) {
                result.remove(record);
            }
        }
    }
    /**
     * Returns <code>true</code> when the record is inside the polygon
     *  otherwise <code>false</code>.
     * @param record current record
     * @return <code>true</code> when the record is inside the polygon
     *  otherwise <code>false</code>
     */
    private boolean isInsidePolygon(final Map<Field, String> record) {
        this.doc = record;
        boolean result;
        final AstroCoordinate astroCoordinates = new AstroCoordinate();
        final WCSTransform wcs = new WCSTransform(this);
        if (wcs.isWCS()) {
            double[] polygonCelest = computeWcsCorner(wcs);
            switch (getCoordSystem()) {
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
                    throw new java.util.NoSuchElementException("Coordinate system " + getCoordSystem() + " is not supported.");
            }
            final Pointing[] pointings = new Pointing[polygonCelest.length / 2];
            for (int i = 0; i < polygonCelest.length; i += 2) {
                final double rightAscensionInReferenceFrame = Double.valueOf(polygonCelest[i]);
                final double declinationInReferenceFrame = Double.valueOf(polygonCelest[i + 1]);
                pointings[i / 2] = new Pointing(Math.PI / 2 - Math.toRadians(declinationInReferenceFrame), Math.toRadians(rightAscensionInReferenceFrame));
            }
            RangeSet range;
            try {
                range = getHealpixIndex().queryPolygonInclusive(pointings, HEALPIX_RESOLUTION);
                result = range.contains(getHealpix());
            } catch (Exception ex) {
                result = false;
            }
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Computes for each corner of the FOV its position in the sky.
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
     * Returns true when the point (longitude,latitude) is inside the pixel
     * otherwise false.
     *
     * @param longitude longitude in degree
     * @param latitude latitude in degree
     * @param pixel Healpix pixel
     * @return true when the point (longitude,latitude) is inside the pixel
     * otherwise false
     */
    private boolean isPointIsInsidePixel(final double longitude, final double latitude, final long pixel) {
        boolean result;
        try {
            final long healpixFromService = index.ang2pix_nest(Math.PI / 2 - Math.toRadians(latitude), Math.toRadians(longitude));
            result = healpixFromService == pixel;
        } catch (Exception ex) {
            result = false;
            LOG.log(Level.WARNING, null, ex);
        }
        return result;
    }

    /**
     * Returns the VO service URL.
     *
     * @return the url
     */
    protected final String getUrl() {
        return url;
    }

    /**
     * Sets the VO service URL.
     *
     * @param urlVal the url to set
     */
    protected final void setUrl(final String urlVal) {
        this.url = urlVal;
    }

    /**
     * Returns the right ascension in decimal degree.
     *
     * @return the rightAscensionInReferenceFrame
     */
    protected final double getRightAscension() {
        return rightAscension;
    }

    /**
     * Sets the right ascension in decimal degree.
     *
     * @param rightAscensionVal the rightAscensionInReferenceFrame to set
     */
    protected final void setRightAscension(final double rightAscensionVal) {
        this.rightAscension = rightAscensionVal;
    }

    /**
     * Returns the declinationInReferenceFrame in deimal degree.
     *
     * @return the declinationInReferenceFrame
     */
    protected final double getDeclination() {
        return declination;
    }

    /**
     * Sets the declinationInReferenceFrame in decimal degree.
     *
     * @param declinationVal the declinationInReferenceFrame to set
     */
    protected final void setDeclination(final double declinationVal) {
        this.declination = declinationVal;
    }

    /**
     * Returns the size in decimal degree of the ROI.
     *
     * @return the size
     */
    protected final double getSize() {
        return size;
    }

    /**
     * Sets the radius in decimal degree.
     *
     * @param sizeVal the radius to set
     */
    protected final void setSize(final double sizeVal) {
        this.size = sizeVal;
    }

    /**
     * Returns the Healpix order.
     *
     * @return the order
     */
    protected final int getOrder() {
        return order;
    }

    /**
     * Sets the Healpix order.
     *
     * @param orderVal the order to set
     */
    protected final void setOrder(final int orderVal) {
        this.order = orderVal;
    }

    /**
     * Returns the Healpix pixel.
     *
     * @return the healpix
     */
    protected final long getHealpix() {
        return healpix;
    }

    /**
     * Sets the Healpix pixel.
     *
     * @param healpixVal the healpix to set
     */
    protected final void setHealpix(final long healpixVal) {
        this.healpix = healpixVal;
    }

    /**
     * Returns the Healpix coordinate system.
     *
     * @return the coordSystem
     */
    protected final AstroCoordinate.CoordinateSystem getCoordSystem() {
        return coordSystem;
    }

    /**
     * Sets the Healpix coordinate system.
     *
     * @param coordSystemVal the coordSystem to set
     */
    protected final void setCoordSystem(final AstroCoordinate.CoordinateSystem coordSystemVal) {
        this.coordSystem = coordSystemVal;
    }

    /**
     * Returns the Healpix index.
     *
     * @return the Healpix index
     */
    protected final HealpixIndex getHealpixIndex() {
        return this.index;
    }

    /**
     * Sets the Healpix index.
     *
     * @param indexVal Healpix index
     */
    protected final void setHealpixIndex(final HealpixIndex indexVal) {
        this.index = indexVal;
    }
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
        /**
         * Returns <code>true</code> when all required concepts are included
         *  in <code>search</code> otherwise <code>false</code>.
         * @param search list of concepts
         * @return <code>true</code> when all required concepts are included
         *  in <code>search</code> otherwise <code>false</code>
         */
        public static boolean requiredConceptsIsContainedIn(final List<String> search) {
            final List<SiaHealpix.ReservedWords> requiredConcepts = Arrays.asList(
                    SiaHealpix.ReservedWords.IMAGE_TITLE,
                    SiaHealpix.ReservedWords.POS_EQ_RA_MAIN,
                    SiaHealpix.ReservedWords.POS_EQ_DEC_MAIN,
                    SiaHealpix.ReservedWords.IMAGE_NAXIS,
                    SiaHealpix.ReservedWords.IMAGE_SCALE,
                    SiaHealpix.ReservedWords.IMAGE_FORMAT,
                    SiaHealpix.ReservedWords.IMAGE_ACCESS_REFERENCE);
            boolean result = true;
            final Iterator<SiaHealpix.ReservedWords> iter = requiredConcepts.iterator();
            while (iter.hasNext() && result) {
                final SiaHealpix.ReservedWords requiredConcept = iter.next();
                final String word = requiredConcept.getName();
                result = search.contains(word);
            }
            return result;
        }
    }
}
