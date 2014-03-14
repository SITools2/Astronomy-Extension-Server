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

import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ivoa.xml.votable.v1.Field;

import org.restlet.engine.Engine;

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.Utility;

/**
 * Queries a CSP service based on Healpix parameters and the coordinate system.
 *
 * <p>
 * This implementation is designed by a chain of responsability pattern.<br/>
 * <code>ConeSearchHealpix</code> parses the Healpix parameters and the coordinate system.
 * Healpix parameters are transformed in (longitude, latitude) in Equatorial frame to query the CSP service.
 * The VO service returns the result in Equatorial frame. This result is then transformed
 * according to the coordinate system.
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConeSearchHealpix extends AbstractVORequest {

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
    private transient fr.cnes.sitools.astro.vo.conesearch.ConeSearchQuery csQuery;
    /**
     * Central position of the pixel along right ascension axis.
     */
    private transient double rightAscension;
    /**
     * Central position of the pixel along declination axis.
     */
    private transient double declination;
    /**
     * Radius based on the pixel resolution.
     */
    private transient double radius;
    /**
     * Healpix index.
     */
    private transient HealpixIndex index;
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
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(ConeSearchHealpix.class.getName());
    /**
     * Empty constructor.
     */
    protected ConeSearchHealpix() {
    }
    /**
     * Constructor.
     * @param urlVal VO service URL
     * @param orderVal Healpix order
     * @param healpixVal Healpix pixel
     * @param coordVal Healpix coordinate system
     */
    public ConeSearchHealpix(final String urlVal, final int orderVal, final long healpixVal, final AstroCoordinate.CoordinateSystem coordVal) {
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
        this.csQuery = new fr.cnes.sitools.astro.vo.conesearch.ConeSearchQuery(url);
    }
    /**
     * Computes the physical parameters based on Healpix.
     * <p>
     * If the coordinate system is in GALACTIC, then the central position
     * of the pixel is transformed in EQUATORIAL because VO services only
     * handle EQUATORIAL frame.
     * </p>
     * @throws Exception when a problem happens during the creation of the
     *  Healpix index
     */
    private void computeGeoPhysicalParameters() throws Exception {
        final int nside = (int) Math.pow(2, getOrder());
        final double pixRes = HealpixIndex.getPixRes(nside) * ARCSEC2DEG;
        setRadius(pixRes * MULT_FACT);
        setIndex(new HealpixIndex(nside, Scheme.NESTED));
        final Pointing pointing = getIndex().pix2ang(healpix);
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
        Object responseCs;
        try {
            initQuery();
            computeGeoPhysicalParameters();
            final List<Map<Field, String>> result = this.csQuery.getResponseAt(getRightAscension(), getDeclination(), getRadius());
            spatialFilter(result);
            responseCs = result;        
        } catch (Exception ex) {
            if (getSuccessor() == null) {
                responseCs = null;
            } else {
                responseCs = getSuccessor().getResponse();
            }
        }
        return responseCs;
    }
    /**
     * Updates <code>result</code> by only keeping the record inside the Healpix
     *  pixel.
     * @param result the updated pixel
     */
    private void spatialFilter(final List<Map<Field, String>> result) {
        final List<Map<Field, String>> pixelToCheck = new ArrayList<Map<Field, String>>(result);
        for (Map<Field, String> record : pixelToCheck) {
            final Set<Entry<Field, String>> columns = record.entrySet();
            double rightAscension = Double.NaN;
            double declination = Double.NaN;
            for (Entry<Field, String> column : columns) {
                final Field field = column.getKey();
                final ReservedWords ucdWord = ReservedWords.find(field.getUcd());
                switch(ucdWord) {
                    case POS_EQ_RA_MAIN:
                        rightAscension = Utility.parseRaVO(record, field);
                        break;
                    case POS_EQ_DEC_MAIN:
                        declination = Utility.parseDecVO(record, field);
                        break;
                    default:
                        break;
                }
            }
            if (Double.isNaN(rightAscension) || Double.isNaN(declination)) {
                LOG.log(Level.SEVERE, "Cannot find RA or DEC - remove it from the result", record);
                result.remove(record);
            } else if (!isPointIsInsidePixel(rightAscension, declination, getHealpix(), getCoordSystem())) {
                result.remove(record);
            }
         }
    }
    /**
     * Returns <code>true</code> when the point (longitude,latitude) is inside the
     * pixel otherwise <code>false</code>.
     *
     * @param longitude longitude in degree
     * @param latitude latitude in degree
     * @param pixel Healpix pixel
     * @return <code>true</code> when the point (longitude,latitude) is inside the
     * pixel otherwise <code>false</code>
     */
    private boolean isPointIsInsidePixel(final double longitude, final double latitude, final long pixel, final AstroCoordinate.CoordinateSystem coordinateSystem) {
        double longitudeInReferenceFrame;
        double latitudeInReferenceFrame;
        switch(coordinateSystem) {
            case EQUATORIAL:
                longitudeInReferenceFrame = longitude;
                latitudeInReferenceFrame = latitude;
                break;
            case GALACTIC:
                final AstroCoordinate astroCoordinates = new AstroCoordinate(longitude, latitude);
                astroCoordinates.setCoordinateSystem(fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem.EQUATORIAL);
                astroCoordinates.processTo(fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem.GALACTIC);
                longitudeInReferenceFrame = astroCoordinates.getRaAsDecimal();
                latitudeInReferenceFrame = astroCoordinates.getDecAsDecimal();
                break;
            default:
                throw new java.util.NoSuchElementException(coordinateSystem.name() + " is not supported.");
        }
        boolean result;
        try {
            final long healpixFromService = getIndex().ang2pix_nest(Math.PI / 2 - Math.toRadians(latitudeInReferenceFrame), Math.toRadians(longitudeInReferenceFrame));
            result = healpixFromService == pixel;
        } catch (Exception ex) {
            result = false;
            LOG.log(Level.WARNING, null, ex);
        }
        return result;
    }
    /**
     * Returns the VO service URL.
     * @return the url
     */
    protected final String getUrl() {
        return url;
    }
    /**
     * Sets the VO service URL.
     * @param urlVal the url to set
     */
    protected final void setUrl(final String urlVal) {
        this.url = urlVal;
    }
    /**
     * Returns the right ascension in decimal degree.
     * @return the rightAscension
     */
    protected final double getRightAscension() {
        return rightAscension;
    }
    /**
     * Sets the right ascension in decimal degree.
     * @param rightAscensionVal the rightAscension to set
     */
    protected final void setRightAscension(final double rightAscensionVal) {
        this.rightAscension = rightAscensionVal;
    }
    /**
     * Returns the declination in deimal degree.
     * @return the declination
     */
    protected final double getDeclination() {
        return declination;
    }
    /**
     * Sets the declination in decimal degree.
     * @param declinationVal the declination to set
     */
    protected final void setDeclination(final double declinationVal) {
        this.declination = declinationVal;
    }
    /**
     * Returns the radius in decimal degree.
     * @return the radius
     */
    protected final double getRadius() {
        return radius;
    }
    /**
     * Sets the radius in decimal degree.
     * @param radiusVal the radius to set
     */
    protected final void setRadius(final double radiusVal) {
        this.radius = radiusVal;
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
    protected final void setHealpix(final long healpixVal) {
        this.healpix = healpixVal;
    }
    /**
     * Returns the Healpix coordinate system.
     * @return the coordSystem
     */
    protected final AstroCoordinate.CoordinateSystem getCoordSystem() {
        return coordSystem;
    }
    /**
     * Sets the Healpix coordinate system.
     * @param coordSystemVal the coordSystem to set
     */
    protected final void setCoordSystem(final AstroCoordinate.CoordinateSystem coordSystemVal) {
        this.coordSystem = coordSystemVal;
    }

    /**
     * Returns the Healpix index.
     * @return the index
     */
    protected final HealpixIndex getIndex() {
        return index;
    }

    /**
     * Sets the Healpix index.
     * @param indexVal the index to set
     */
    protected final void setIndex(final HealpixIndex indexVal) {
        this.index = indexVal;
    }
    /**
     * Reserved keywords in CSP for which a specific processing is needed.
     */
    public enum ReservedWords {

        /**
         * Right Ascension.
         */
        POS_EQ_RA_MAIN(Arrays.asList("POS_EQ_RA_MAIN", "pos.eq.ra;meta.main", "pos.eq.ra")),
        /**
         * Declination.
         */
        POS_EQ_DEC_MAIN(Arrays.asList("POS_EQ_DEC_MAIN", "pos.eq.dec;meta.main", "pos.eq.dec")),
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
        public static ReservedWords find(final String keyword) {
            ReservedWords response = ReservedWords.NONE;
            final ReservedWords[] words = ReservedWords.values();
            for (ReservedWords word : words) {
                final List<String> reservedName = word.getName();
                if (reservedName.contains(keyword)) {
                    response = word;
                    break;
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
            final List<ReservedWords> required = Arrays.asList(POS_EQ_RA_MAIN, POS_EQ_DEC_MAIN, ID_MAIN);
            boolean result = true;
            final Iterator<ReservedWords> iter = required.iterator();
            while (iter.hasNext() && result) {
                final ReservedWords requiredConcepts = iter.next();
                final List<String> words = requiredConcepts.names;
                for (String word : words) {
                    result = search.contains(word);
                    if (result) {
                        break;
                    }
                }
            }
            return result;
        }
    }
}
