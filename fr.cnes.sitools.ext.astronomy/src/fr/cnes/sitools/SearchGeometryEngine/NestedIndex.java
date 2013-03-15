/*******************************************************************************
* Copyright 2012, 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.SearchGeometryEngine;


import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nested Index for hierarchical resolution.
 * @author Jean-Christophe Malapert
 */
public class NestedIndex implements Index {

    /**
     * Shape for which the index must be computed.
     */
    private Shape shape;

    /**
     * Initialize Max order = Max order of Healpix in JAVA.
     */
    private int orderMax = HealpixMoc.MAXORDER;

    /**
     * Initialize min order to 0.
     */
    private int orderMin = 0;

    /**
     * Typical choice of fact.
     */
    protected static final int TYPICAL_CHOICE_FACT = 4;

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(NestedIndex.class.getName());

    /**
     * Cronstructs a new Index based with a shape.
     * @param geometryShape shape for which the Healpix index must be computed.
     */
    public NestedIndex(final Shape geometryShape) {
        this.shape = geometryShape;
    }

    /**
     * Empty constructor.
     */
    protected NestedIndex() {
    }

    /**
     * Returns the Healpix index of the shape.
     * <p>
     * A long is returned when the geometry is a point.
     * A HealpixMoc is returned when the geometry is a polygon or a cone.
     * </p>
     * @return the index
     */
    @Override
    public final Object getIndex() {
        assert getShape() != null;
        Object result = null;
        try {
            HealpixIndex index;
            index = new HealpixIndex((int)
                    Math.pow(2, getOrderMax()), Scheme.RING);
            if (getShape().getType().equals("POINT")) {
                result = computePointIndex(index);
            } else if (getShape().getType().equals("POLYGON")) {
                result = computePolygonIndex(index);
            } else if (getShape().getType().equals("CONE")) {
                result = computeConeIndex(index);
            } else {
                throw new IllegalAccessException("Shape : "
                        + getShape() + " not found");
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return result;
    }

    /**
     * Transforms a pixel from RING to NESTED.
     * @param index Healpix index
     * @return the pixel number
     * @throws Exception Healpix Exception
     */
    protected final long computePointIndex(final HealpixIndex index) throws Exception {
        Point point = (Point) getShape();
        return index.ring2nest(index.ang2pix(point));
    }

    /**
     * Returns a hierarchical index of a cone.
     * @param index Healpix index
     * @return the pixel numbers as a hierarchical index
     * @throws Exception Healpix Exception
     */
    protected final HealpixMoc computeConeIndex(final HealpixIndex index)
            throws Exception {
        HealpixMoc moc = new HealpixMoc();
        Cone cone = (Cone) getShape();
        RangeSet rangeSet = index.queryDiscInclusive(cone.getCenter(), cone.getRadius(), TYPICAL_CHOICE_FACT);
        RangeSet.ValueIterator valueIter = rangeSet.valueIterator();
        while (valueIter.hasNext()) {
            long pixNest = index.ring2nest(valueIter.next());
            moc.add(new MocCell(getOrderMax(), pixNest));
        }
        return moc;
    }

    /**
     * Returns a hierarchical index of a polygon.
     * 
     * <p>
     * When the polygon is clockwise, the index is computed on this polygon.<br/>
     * When the polygon is counterclockwised, the index is computed on the complement of this polygon
     * </p>
     * @param index Healpix index
     * @return the HealpixMoc;
     * @throws Exception Healpix Exception
     */
    protected final HealpixMoc computePolygonIndex(final HealpixIndex index)
            throws Exception {
        RangeSet rangeSet;
        HealpixMoc moc = new HealpixMoc();
        Polygon polygon = (Polygon) getShape();
        if (polygon.isClockwise()) {
            rangeSet = computePolygon(polygon.getPoints(), index);
        } else {
            List<Polygon> polygons = polygon.splitPolygon();
            rangeSet = new RangeSet();
            for (Polygon p : polygons) {
                rangeSet.setToUnion(rangeSet,
                        computePolygon(p.getPoints(), index));
            }
        }

        RangeSet.ValueIterator valueIter = rangeSet.valueIterator();
        while (valueIter.hasNext()) {
            long pixNest = index.ring2nest(valueIter.next());
            moc.add(new MocCell(getOrderMax(), pixNest));
        }
        return moc;
    }

    /**
     * Returns a range of Healpix pixels from the a list of points.
     * @param points List of points of the polygon
     * @param index Healpix index
     * @return the rangeSet of pixels
     * @throws Exception Helapix Exception
     */
    protected final RangeSet computePolygon(final List<Point> points,
            final HealpixIndex index) throws Exception {
        RangeSet result;
        Pointing[] pointings = new Pointing[points.size()];

        int i = 0;
        double maxTheta = 0;
        double minTheta = Math.PI * 2;
        for (Point p : points) {
            pointings[i] = p;
            i++;
            double currentTheta = p.theta();
            if (currentTheta > maxTheta) {
                maxTheta = currentTheta;
            }
            if (currentTheta < minTheta) {
                minTheta = currentTheta;
            }
        }

        try {
            result = index.queryPolygonInclusive(pointings,
                    TYPICAL_CHOICE_FACT);
        } catch (Exception ex) {
            result = index.queryStrip(minTheta, maxTheta, true);
        }

        return result;
    }

    /**
     * Returns the shape.
     * @return the shape
     */
    public final Shape getShape() {
        return shape;
    }

    /**
     * Sets the shape.
     * @param val shape
     */
    public final void setShape(final Shape val) {
        this.shape = val;
    }

    /**
     * Returns the Max order.
     * @return max order
     */
    public final int getOrderMax() {
        return this.orderMax;
    }

    /**
     * Returns the min order.
     * @return min order.
     */
    public final int getOrderMin() {
        return this.orderMin;
    }

    /**
     * Sets the max order.
     * @param val max order
     */
    public final void setOrderMax(final int val) {
        this.orderMax = val;
    }

    /**
     * Sets the min order.
     * @param val mion order
     */
    public final void setOrderMin(final int val) {
        this.orderMin = val;
    }
}

