/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

    double ra;
    double dec;
    String coordinateType = CoordinateType.DECIMAL.name();
    private String type;

    /**
     * Type of the object
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Type of the object
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Representation of astronomical coordinates : decimal or sexagesimal
     */
    public enum CoordinateType {

        /**
         * Decimal representation 
         */
        DECIMAL,
        /**
         * Sexagesimal representation
         */
        SEXAGESIMAL
    }

    /**
     * Create a point on the sphere.
     * @param ra Right ascension in decimal degree.
     * @param dec Declination in decimal degree.
     */
    public AstroCoordinate(double ra, double dec) {
        this.ra = ra;
        this.dec = dec;
        this.type = null;
    }

    /**
     * Create a point on the sphere.
     * @param ra Right ascension in sexagesimal.
     * @param dec Declination in sexagesimal.
     * @RuntimeException - if coordinates format is not valid
     * @see The conversion from sexagesimal to decimal coordinates
     * is done by the use of <a href="http://jsky.sourceforge.net/">jsky</a>.
     */
    public AstroCoordinate(final String ra, final String dec) {
        HMS hms = new HMS(ra);
        this.ra = hms.getVal() * 360. / 24.;
        DMS dms = new DMS(dec);
        this.dec = dms.getVal();
        this.type = null;
        this.coordinateType = CoordinateType.DECIMAL.name();
    }

    /**
     * Get the right ascension as decimal degree.
     * @return right ascension.
     */
    public double getRaAsDecimal() {
        return this.ra;
    }

    /**
     * Get the declination as decimal degree.
     * @return declination.
     */
    public double getDecAsDecimal() {
        return this.dec;
    }

    /**
     * Get the right ascension as sexagesimal.
     * @return right ascension.
     * @RuntimeException - if coordinate format is not valid   
     */
    public String getRaAsSexagesimal() {
        HMS hms = new HMS(this.ra * 24. / 360.);
        return hms.toString(true);
    }

    /**
     * Get the declination as sexagesimal.
     * @return declination.
     * @RuntimeException - if coordinate format is not valid   
     */
    public String getDecAsSexagesimal() {
        DMS dms = new DMS(this.dec);
        return dms.toString(true);
    }

    /**
     * Set the right ascension.
     * @param ra right ascension in decimal degree.
     */
    public void setRaAsDecimal(double ra) {
        this.ra = ra;
    }

    /**
     * Set the declination.
     * @param dec declination in decimal degree.
     */
    public void setDecAsDecimal(double dec) {
        this.dec = dec;
    }

    /**
     * Set the right ascension. The right ascension is then stored in decimal degree.
     * @param ra right ascension in sexagesimal.
     * @RuntimeException - if coordinate format is not valid   
     * @see The conversion from sexagesimal to decimal coordinates
     * is done by the use of <a href="http://jsky.sourceforge.net/">jsky</a>.
     */
    public void setRaAsSexagesimal(final String ra) {
        HMS hms = new HMS(ra);
        this.ra = hms.getVal() * 360.0 / 24.0;
    }

    /**
     * Set the declination. The declination is then stored in decimal degree.
     * @param dec declination in sexagesimal.
     * @RuntimeException - if coordinate format is not valid  
     * @see The conversion from sexagesimal to decimal coordinates
     * is done by the use of <a href="http://jsky.sourceforge.net/">jsky</a>.   
     */
    public void setDecAsSexagesimal(final String dec) {
        DMS dms = new DMS(dec);
        this.dec = dms.getVal();
    }

    /**
     * Display the coordinates as follows:
     * Coordinate: (ra_decimal,dec_decimal) or (ra_sexagecimal,dec_sexagesimal)
     * @return
     */
    @Override
    public String toString() {
        return "Coordinate: (" + getRaAsDecimal() + "," + getDecAsDecimal() + ") or (" + getRaAsSexagesimal() + ","
                + getDecAsSexagesimal() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        //check for self-comparison
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof AstroCoordinate)) {
            return false;
        }

        //cast to native object is now safe
        AstroCoordinate that = (AstroCoordinate) obj;

        //now a proper field-by-field evaluation can be made
        return that.coordinateType.equals(this.coordinateType)
                && that.dec == this.dec
                && that.ra == this.ra
                && (that.type!=null && this.type!=null && that.type.equals(this.type));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.ra) ^ (Double.doubleToLongBits(this.ra) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.dec) ^ (Double.doubleToLongBits(this.dec) >>> 32));
        hash = 29 * hash + (this.coordinateType != null ? this.coordinateType.hashCode() : 0);
        hash = 29 * hash + (this.type != null ? this.type.hashCode() : 0);
        return hash;
    }
    private static final Logger LOG = Logger.getLogger(AstroCoordinate.class.getName());
}
