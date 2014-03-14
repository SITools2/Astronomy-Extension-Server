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
package fr.cnes.sitools.searchgeometryengine;


/**
 * Contains an enumeration of different coordinate systems.
 *
 * <p>
 * Two coordinate systems are supported:
 * <ul>
 * <li>Earth observation</li>
 * <li>Astronomy</li>
 * </ul>
 * Moreover, this object contains some utility methods to handle conversion
 * Earth observation <--> astronomy
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public enum CoordSystem {

    /**
     * Earth observation based on (longitude, latitude).
     * Longitude = [-180, 180]
     * Latitude = [-90, 90]
     */
    GEOCENTRIC,

    /**
     * Astronomy is based on (right ascension, declination).
     * Right ascension = [0, 360]
     * Declination = [-90, 90]
     */
    EQUATORIAL, // right ascension, declination
    /**
     * Astronomy is based on (longitude, latitude).
     * longitude = [0, 360]
     * latitude = [-90, 90]
     */
    GALACTIC;

    /**
     * Angle that defines the circumference of the circle.
     */
    protected static final double ANGLE_CIRCLE_CIRCUMFERENCE = 360.;

    /**
     * Angle that defines the semi circumference of the circle.
     */
    protected static final double ANGLE_SEMI_CIRCLE_CIRCUMFERENCE = 180.;

    /**
     * Angle that defines the 1.5xcircumference of the circle.
     */
    protected static final double ANGLE_CIRCLE_CIRCUMFERENCE_MORE_SEMI = 540.;

    /**
     * Angular difference pole vs equator.
     */
    protected static final double ANGLE_DIFFERENCE_POLAR_VS_EQUATOR = 90.;

    /**
     * Convert geocentric longitude to phi (spherical).
     * @param longitude Geocentric longitude in decimal degree
     * @return phi in decimal degree
     */
    public static double convertLongitudeGeoToPhi(final double longitude) {
         return (longitude + ANGLE_CIRCLE_CIRCUMFERENCE)
                 % ANGLE_CIRCLE_CIRCUMFERENCE;
    }

    /**
     * Convert geocentric latitude to theta (spherical).
     * @param latitude latitude in decimal degree
     * @return theta in decimal degree
     */
    public static double convertLatitudeGeoToTheta(final double latitude) {
        return ANGLE_DIFFERENCE_POLAR_VS_EQUATOR - latitude;
    }

    /**
     * Convert Ra (equatorial) to phi (spherical).
     * @param rightAscension Right ascension in decimal degree
     * @return phi in decimal degree
     */
    public static double convertRaToPhi(final double rightAscension) {
         return rightAscension;
    }

    /**
     * Convert Dec (equatorial) to theta (spherical).
     * @param declination Declination in decimal degree
     * @return theta in decimal degree
     */
    public static double convertDecToTheta(final double declination) {
        return ANGLE_DIFFERENCE_POLAR_VS_EQUATOR - declination;
    }

    /**
     * Convert phi (spherical) to Ra (equatorial).
     * @param phi phi in decimal degree
     * @return right ascension in decimal degree
     */
    public static double convertPhiToRa(final double phi) {
        return phi;
    }

    /**
     * Convert theta (spherical) to Dec (equatorial).
     * @param theta theta in decimal degree
     * @return declination in decimal degree
     */
    public static double convertThetaToDec(final double theta) {
        return ANGLE_DIFFERENCE_POLAR_VS_EQUATOR - theta;
    }

    /**
     * Convert phi (spherical) to longitude (geocentric).
     * @param phi phi in decimal degree
     * @return longitude in decimal degree
     */
    public static double convertPhiToLongitudeGeo(final double phi) {
        return (phi + ANGLE_CIRCLE_CIRCUMFERENCE_MORE_SEMI)
                % ANGLE_CIRCLE_CIRCUMFERENCE - ANGLE_SEMI_CIRCLE_CIRCUMFERENCE;
    }

    /**
     * Convert theta (spherical) to latitude (geocentric).
     * @param theta theta in decimal degree
     * @return Returns latitude in decimal degree
     */
    public static double convertThetaToLatitudeGeo(final double theta) {
        return ANGLE_DIFFERENCE_POLAR_VS_EQUATOR - theta;
    }
}
