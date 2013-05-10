/**
 * *****************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.astro.resolver;

import java.util.logging.Logger;
import jsky.coords.DMS;
import jsky.coords.HMS;

/**
 * This object contains utility methods to store astronomical coordinates.<br/>
 * An AstroCoordinate allows you to store information about a sky position
 *
 * @author Jean-Christophe Malapert
 */
public class AstroCoordinate {
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(AstroCoordinate.class.getName());
    /**
     * Right ascension.
     */
    private double rightAscension;
    /**
     * Declination.
     */
    private double declination;
    /**
     * Type of coordinate.
     */
    private transient String coordinateType = CoordinateType.DECIMAL.name();
    /**
     * Type.
     */
    private transient String type;

    /**
     * Type of the object.
     *
     * @return the type
     */
    public final String getType() {
        return type;
    }

    /**
     * Type of the object.
     *
     * @param typeVal the type to set
     */
    public final void setType(final String typeVal) {
        this.type = typeVal;
    }

    /**
     * Returns the right ascension.
     * @return the rightAscension
     */
    public final double getRightAscension() {
        return rightAscension;
    }

    /**
     * Sets the right ascension.
     * @param rightAscensionVal the rightAscension to set
     */
    public final void setRightAscension(final double rightAscensionVal) {
        this.rightAscension = rightAscensionVal;
    }

    /**
     * Returns the declination.
     * @return the declination
     */
    public final double getDeclination() {
        return declination;
    }

    /**
     * Sets the declination.
     * @param declinationVal the declination to set
     */
    public final void setDeclination(final double declinationVal) {
        this.declination = declinationVal;
    }

    /**
     * Representation of astronomical coordinates : decimal or sexagesimal.
     */
    public enum CoordinateType {

        /**
         * Decimal representation .
         */
        DECIMAL,
        /**
         * Sexagesimal representation.
         */
        SEXAGESIMAL
    }

    /**
     * Create a point on the sphere.
     */
    public AstroCoordinate() {
        this(0, 0);
    }

    /**
     * Create a point on the sphere.
     *
     * @param rightAscensionVal Right ascension in decimal degree.
     * @param declinationVal Declination in decimal degree.
     */
    public AstroCoordinate(final double rightAscensionVal, final double declinationVal) {
        setRightAscension(rightAscensionVal);
        setDeclination(declinationVal);
        setType(null);
    }

    /**
     * Create a point on the sphere.
     *
     * @param rightAscensionVal Right ascension in sexagesimal.
     * @param declinationVal Declination in sexagesimal.
     * @RuntimeException - if coordinates format is not valid
     * @see The conversion from sexagesimal to decimal coordinates is done by
     * the use of <a href="http://jsky.sourceforge.net/">jsky</a>.
     */
    public AstroCoordinate(final String rightAscensionVal, final String declinationVal) {
        final HMS hms = new HMS(rightAscensionVal);
        setRightAscension(hms.getVal() * 360. / 24.);
        final DMS dms = new DMS(declinationVal);
        setDeclination(dms.getVal());
        setType(null);
        this.coordinateType = CoordinateType.DECIMAL.name();
    }

    /**
     * Returns the right ascension as decimal degree.
     *
     * @return right ascension.
     */
    public final double getRaAsDecimal() {
        return this.getRightAscension();
    }

    /**
     * Returns the declination as decimal degree.
     *
     * @return declination.
     */
    public final double getDecAsDecimal() {
        return this.getDeclination();
    }

    /**
     * Returns the right ascension as sexagesimal.
     *
     * @return right ascension.
     * @RuntimeException - if coordinate format is not valid
     */
    public final String getRaAsSexagesimal() {
        final HMS hms = new HMS(this.getRightAscension() * 24. / 360.);
        return hms.toString(true);
    }

    /**
     * Returns the declination as sexagesimal.
     *
     * @return declination.
     * @RuntimeException - if coordinate format is not valid
     */
    public final String getDecAsSexagesimal() {
        final DMS dms = new DMS(this.getDeclination());
        return dms.toString(true);
    }

    /**
     * Sets the right ascension.
     *
     * @param rightAscensionVal right ascension in decimal degree.
     */
    public final void setRaAsDecimal(final double rightAscensionVal) {
        this.setRightAscension(rightAscensionVal);
    }

    /**
     * Sets the declination.
     *
     * @param declinationVal declination in decimal degree.
     */
    public final void setDecAsDecimal(final double declinationVal) {
        this.setDeclination(declinationVal);
    }

    /**
     * Sets the right ascension. The right ascension is then stored in decimal
     * degree.
     *
     * @param rightAscensionVal right ascension in sexagesimal.
     * @RuntimeException - if coordinate format is not valid
     * @see The conversion from sexagesimal to decimal coordinates is done by
     * the use of <a href="http://jsky.sourceforge.net/">jsky</a>.
     */
    public final void setRaAsSexagesimal(final String rightAscensionVal) {
        final HMS hms = new HMS(rightAscensionVal);
        this.setRightAscension(hms.getVal() * 360.0 / 24.0);
    }

    /**
     * Sets the declination. The declination is then stored in decimal degree.
     *
     * @param declinationVal declination in sexagesimal.
     * @RuntimeException - if coordinate format is not valid
     * @see The conversion from sexagesimal to decimal coordinates is done by
     * the use of <a href="http://jsky.sourceforge.net/">jsky</a>.
     */
    public final void setDecAsSexagesimal(final String declinationVal) {
        final DMS dms = new DMS(declinationVal);
        this.setDeclination(dms.getVal());
    }

    /**
     * Display the coordinates as follows: Coordinate: (ra_decimal,dec_decimal)
     * or (ra_sexagecimal,dec_sexagesimal).
     *
     * @return ra_decimal, dec_decimal
     */
    @Override
    public final String toString() {
        return "Coordinate: (" + getRaAsDecimal() + "," + getDecAsDecimal() + ") or (" + getRaAsSexagesimal() + ","
                + getDecAsSexagesimal() + ")";
    }

    @Override
    public final boolean equals(final Object obj) {
        //check for self-comparison
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof AstroCoordinate)) {
            return false;
        }

        //cast to native object is now safe
        final AstroCoordinate that = (AstroCoordinate) obj;

        //now a proper field-by-field evaluation can be made
        return that.coordinateType.equals(this.coordinateType)
                && that.getDeclination() == this.getDeclination()
                && that.getRightAscension() == this.getRightAscension()
                && (that.type != null && this.type != null && that.type.equals(this.type));
    }

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.getRightAscension()) ^ (Double.doubleToLongBits(this.getRightAscension()) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.getDeclination()) ^ (Double.doubleToLongBits(this.getDeclination()) >>> 32));
        hash = 29 * hash + (this.coordinateType != null ? this.coordinateType.hashCode() : 0);
        hash = 29 * hash + (this.type != null ? this.type.hashCode() : 0);
        return hash;
    }    
}
