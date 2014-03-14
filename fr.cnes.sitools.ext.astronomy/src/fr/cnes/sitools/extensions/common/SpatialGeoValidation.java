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
package fr.cnes.sitools.extensions.common;

import java.util.Map;

/**
 * Validates a geospatial keyword.
 * @see Package Decorator pattern
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SpatialGeoValidation extends NumberArrayValidation {

    /**
     * Index of the longitude coord in a polygon.
     */
    private int indexLong;
    /**
     * Index of the latitude coord in a polygon.
     */
    private int indexLat;
    /**
     * Ra range to check.
     */
    private double[] raRange;
    /**
     * Dec range to check.
     */
    private double[] decRange;
    /**
     * First element of the range.
     */
    private static final int MIN = 0;
    /**
     * Last element of the range.
     */
    private static final int MAX = 1;

    /**
     * Create a geospatial validation decorator.
     * @param validation validation
     * @param keyword keyword that contains the polygon
     * @param indexLongVal index in the polygon for longitude coord
     * @param indexLatVal index in the polygon for Latitude coord
     * @param raRangeVal RA range validity
     * @param decRangeVal DEC range validity
     */
    public SpatialGeoValidation(final Validation validation, final String keyword, final int indexLongVal, final int indexLatVal,
                                final double[] raRangeVal, final double[] decRangeVal) {
        super(validation, keyword, ",", 2);
        setIndexLat(indexLatVal);
        setIndexLong(indexLongVal);
        setRaRange(raRangeVal);
        setDecRange(decRangeVal);
    }

    @Override
    protected Map<String, String> localValidation() {
        final Map<String, String> error = super.localValidation();
        final String value = getMap().get(this.getKeywordToTest());
        final String[] array = value.split(getSplitChar());
        if (error.isEmpty()) {
            final double valRa = Double.valueOf(array[getIndexLong()]);
            final double valDec = Double.valueOf(array[getIndexLat()]);
            if (valRa < getRaRange()[MIN] || valRa > getRaRange()[MAX]) {
                error.put(getKeywordToTest(), "RA (=" + valRa + ") must be in [" + getRaRange()[MIN] + "," + getRaRange()[MAX] + "]");
            }
            if (valDec < getDecRange()[MIN] || valDec > getDecRange()[MAX]) {
                error.put(getKeywordToTest(), "Dec (=" + valDec + ") must be in [" + getDecRange()[MIN] + "," + getDecRange()[MAX] + "]");
            }
        }
        return error;
    }

    /**
     * Returns the indexLong.
     * @return the indexLong
     */
    protected final int getIndexLong() {
        return indexLong;
    }

    /**
     * Sets the indexLong.
     * @param indexLongVal the indexLong to set
     */
    protected final void setIndexLong(final int indexLongVal) {
        this.indexLong = indexLongVal;
    }

    /**
     * Sets the indexLat.
     * @return the indexLat
     */
    protected final int getIndexLat() {
        return indexLat;
    }

    /**
     * Sets the indexLat.
     * @param indexLatVal the indexLat to set
     */
    protected final void setIndexLat(final int indexLatVal) {
        this.indexLat = indexLatVal;
    }

    /**
     * Returns the RA range.
     * @return the raRange
     */
    protected final double[] getRaRange() {
        return raRange;
    }

    /**
     * Returns the ra range.
     * @param raRangeVal the raRange to set
     */
    protected final void setRaRange(final double[] raRangeVal) {
        this.raRange = raRangeVal;
    }

    /**
     * Returns the dec range.
     * @return the decRange
     */
    protected final double[] getDecRange() {
        return decRange;
    }

    /**
     * Sets the dec range.
     * @param decRangeVal the decRange to set
     */
    protected final void setDecRange(final double[] decRangeVal) {
        this.decRange = decRangeVal;
    }
}
