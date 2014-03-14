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

import healpix.core.HealpixIndex;
import healpix.essentials.RangeSet;

/**
 * Factory to create an geometry index based on a shape and a Healpix Scheme.
 *
 * <p>
 * The geometry index depends on the shape and the Scheme. Here is an example
 * showing how to create a AbstractGeometryIndex.<br/>
 * <pre>
 * <code>
 * Index index = AbstractGeometryIndex.createIndex(shape, Scheme.RING);
 * Long pixelNumber = (Long) index.getIndex(); // for point
 *
 * Index index = AbstractGeometryIndex.createIndex(shape, Scheme.RING);
 * RangeSet pixels = (RangeSet) index.getIndex(); // for polygon
 * </code>
 * </pre>
 * </p>
 *
 * @author Jean-Christophe Malapert
 */
public abstract class AbstractGeometryIndex {

    /**
     * Library version.
     */
    public static final String VERSION_LIBRARY = "1.0";

    /**
     * Creates the index with a shape and a Healpix Scheme.
     *
     * <p>
     * A Runtime Exception is raised when a problem happens</p>
     *
     * @param shape shape
     * @param scheme Healpix Scheme
     * @return the index
     */
    public static Index createIndex(final Shape shape, final Scheme scheme) {
        Index index;
        switch (scheme) {
            case MOC:
                index = new MocIndex(shape);
                break;

            case RING:
                index = new RingIndex(shape);
                break;

            case NESTED:
                index = new NestedIndex(shape);
                break;

            default:
                throw new RuntimeException("Cannot manage this scheme");
        }
        return index;
    }

    /**
     * Returns the pixel resolution at a given order.
     *
     * @param order Healpix order
     * @return the pixel resolution in arcsec
     */
    public static double getPixelResolution(final int order) {
        return HealpixIndex.getPixRes((long) Math.pow(2, order));
    }

    /**
     * Transforms a RangetSet in an array of long.
     *
     * @param rangeSet rangeSet
     * @return an array of pixels at a given order
     */
    public static long[] decodeRangeSet(final RangeSet rangeSet) {
        assert rangeSet != null;
        long[] pixels = new long[(int) rangeSet.nval()];
        RangeSet.ValueIterator valueIter = rangeSet.valueIterator();
        int i = 0;
        while (valueIter.hasNext()) {
            pixels[i] = valueIter.next();
            i++;
        }
        return pixels;
    }
}
