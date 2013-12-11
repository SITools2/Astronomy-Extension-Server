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
/**
 * This class comes from Koders.com (an open source code search engine). This
 * class has been modified to remove the dependence with the package
 * sun.awt.geom.Crossings
 *
 */
package fr.cnes.sitools.astro.graph;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class Polygon2D implements Shape {

    /**
     * Number of points in a quadrilateral.
     */
    private static final int NB_POINTS_QUADRI = 4;
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(Polygon2D.class.getName());
    /**
     * Number of points in the polygon.
     */
    private transient int npoints;
    /**
     * Array of xValue coordinates.
     */
    private transient double[] xpoints;
    /**
     * Array of yValue coordinates.
     */
    private transient double[] ypoints;
    /**
     * Bounds of the rectangle.
     */
    private transient Rectangle bounds;
    /**
     * Path.
     */
    private transient GeneralPath path;
    /**
     * Close path.
     */
    private transient GeneralPath closedPath;

    /**
     * Empty constructor.
     */
    public Polygon2D() {
        xpoints = new double[NB_POINTS_QUADRI];
        ypoints = new double[NB_POINTS_QUADRI];
    }

    /**
     * Constructs a polygon with a list of points.
     *
     * @param pixels points
     */
    public Polygon2D(final List<Point2D.Double> pixels) {
        this.npoints = pixels.size();
        this.xpoints = new double[npoints];
        this.ypoints = new double[npoints];
        int index = 0;
        for (Point2D.Double pixelIter : pixels) {
            this.xpoints[index] = pixelIter.getX();
            this.ypoints[index] = pixelIter.getY();
            index++;
        }
    }

    /**
     * Constructs a polygon with a list of coordinates.
     *
     * @param xpointsVal xValue coordinates of each point
     * @param ypointsVal yValue coordinates of each point
     * @param npointsVal number of points in the polygon
     */
    public Polygon2D(final Double[] xpointsVal, final Double[] ypointsVal, final int npointsVal) {
        if (npointsVal > xpointsVal.length || npointsVal > ypointsVal.length) {
            throw new IndexOutOfBoundsException(
                    "npointsVal > xpoints.length || npointsVal > ypoints.length");
        }
        this.npoints = npointsVal;
        this.xpoints = new double[npointsVal];
        this.ypoints = new double[npointsVal];
        System.arraycopy(xpointsVal, 0, this.xpoints, 0, npointsVal);
        System.arraycopy(ypointsVal, 0, this.ypoints, 0, npointsVal);
    }

    /**
     * Constructs a polygon with an array of xValue/y coordinates and the number of
     * points in the polygon.
     *
     * @param xpointsVal Array of xValue coordinates for each point
     * @param ypointsVal Array of yValue coordinates for each point
     * @param npointsVal number of points in the polygon
     */
    public Polygon2D(final double[] xpointsVal, final double[] ypointsVal, final int npointsVal) {
        if (npointsVal > xpointsVal.length || npointsVal > ypointsVal.length) {
            throw new IndexOutOfBoundsException(
                    "npoints > xpoints.length || npoints > ypoints.length");
        }
        this.npoints = npointsVal;
        this.xpoints = new double[npointsVal];
        this.ypoints = new double[npointsVal];
        System.arraycopy(xpointsVal, 0, this.xpoints, 0, npointsVal);
        System.arraycopy(ypointsVal, 0, this.ypoints, 0, npointsVal);
    }

    /**
     * Constructs a polygon with an array of points.
     *
     * @param pts array of points
     */
    public Polygon2D(final Point2D[] pts) {
        this.npoints = pts.length;
        this.xpoints = new double[this.npoints];
        this.ypoints = new double[this.npoints];
        for (int i = 0; i < this.npoints; i++) {
            xpoints[i] = pts[i].getX();
            ypoints[i] = pts[i].getY();
        }
    }

    /**
     * Set points in the polygon.
     *
     * @param pixels points
     */
    public final void setPoints(final List<Point2D.Double> pixels) {
        this.npoints = pixels.size();
        this.xpoints = new double[npoints];
        this.ypoints = new double[npoints];
        int index = 0;
        for (Point2D.Double pixelIter : pixels) {
            this.xpoints[index] = pixelIter.getX();
            this.ypoints[index] = pixelIter.getY();
            index++;
        }
    }

    /**
     * Translates in deltaX/deltaY.
     * @param deltaX xValue step
     * @param deltaY yValue step
     */
    public final void translate(final int deltaX, final int deltaY) {
        for (int i = 0; i < npoints; i++) {
            xpoints[i] += deltaX;
            ypoints[i] += deltaY;
        }
        if (bounds != null) {
            bounds.translate(deltaX, deltaY);
        }
    }

    /**
     * Computes bounds.
     * @param xpointsVal array of xValue coordinates
     * @param ypointsVal array of yValue coordinates
     * @param npointsVal number of points in the polygon
     */
    public final void calculateBounds(final double[] xpointsVal, final double[] ypointsVal, final int npointsVal) {
        double boundsMinX = Double.MAX_VALUE;
        double boundsMinY = Double.MAX_VALUE;
        double boundsMaxX = Double.MIN_VALUE;
        double boundsMaxY = Double.MIN_VALUE;

        for (int i = 0; i < npointsVal; i++) {
            final double xValue = xpointsVal[i];
            boundsMinX = Math.min(boundsMinX, xValue);
            boundsMaxX = Math.max(boundsMaxX, xValue);
            final double yValue = ypointsVal[i];
            boundsMinY = Math.min(boundsMinY, yValue);
            boundsMaxY = Math.max(boundsMaxY, yValue);
        }
        bounds = new Rectangle((int) Math.floor(boundsMinX), (int) Math.floor(boundsMinY),
                (int) Math.ceil(boundsMaxX - boundsMinX), (int) Math.ceil(boundsMaxY - boundsMinY));
    }

    /**
     * Updates bounds.
     * @param xValue xValue coordinate
     * @param yValue yValue coordinate
     */
    public final void updateBounds(final double xValue, final double yValue) {
        if ((int) xValue < bounds.x) {
            bounds.width += (bounds.x - (int) xValue);
            bounds.x = (int) xValue;
        } else {
            bounds.width = Math.max(bounds.width, (int) Math.ceil(xValue) - bounds.x);
            // bounds.xValue = bounds.xValue;
        }

        if ((int) yValue < bounds.y) {
            bounds.height += (bounds.y - (int) yValue);
            bounds.y = (int) yValue;
        } else {
            bounds.height = Math.max(bounds.height, (int) Math.ceil(yValue) - bounds.y);
            // bounds.yValue = bounds.yValue;
        }
    }

    /**
     * Adss a point in the polygon.
     * @param xValue xValue coordinate
     * @param yValue yValue coordinate
     */
    public final void addPoint(final double xValue, final double yValue) {
        if (npoints == xpoints.length) {
            double[] tmp;

            tmp = new double[npoints * 2];
            System.arraycopy(xpoints, 0, tmp, 0, npoints);
            xpoints = tmp;

            tmp = new double[npoints * 2];
            System.arraycopy(ypoints, 0, tmp, 0, npoints);
            ypoints = tmp;
        }
        xpoints[npoints] = xValue;
        ypoints[npoints] = yValue;
        npoints++;
        if (bounds != null) {
            updateBounds(xValue, yValue);
        }
    }

    @Override
    public final Rectangle getBounds() {
        if (npoints == 0) {
            return new Rectangle();
        }
        if (bounds == null) {
            calculateBounds(xpoints, ypoints, npoints);
        }
        return bounds.getBounds();
    }

    /**
     * Checks if a point is contained.
     * @param point point
     * @return True when <code>point</code> is contained otherwise false.
     */
    public final boolean contains(final Point point) {
        return contains(point.getX(), point.getY());
    }

    /**
     * Checks if xValue/y is contained.
     * @param xValue xValue value
     * @param yValue yValue value
     * @return
     */
    public final boolean contains(final int xValue, final int yValue) {
        return contains((double) xValue, (double) yValue);
    }

    /**
     * Returns the high precision bounding box of the {@link Shape}.
     *
     * @return a {@link Rectangle2D} that precisely bounds the
     * <code>Shape</code>.
     */
    @Override
    public final Rectangle2D getBounds2D() {
        return getBounds();
    }

    /**
     * Determines if the specified coordinates are inside this
     * <code>Polygon</code>. For the definition of <i>insideness</i>, see the
     * class comments of {@link Shape}.
     *
     * @param xValue the specified xValue coordinate
     * @param yValue the specified yValue coordinate
     * @return <code>true</code> if the <code>Polygon</code> contains the
     * specified coordinates; <code>false</code> otherwise.
     */
    @Override
    public final boolean contains(final double xValue, final double yValue) {
        if (npoints <= 2 || !getBounds().contains(xValue, yValue)) {
            return false;
        }
        int hits = 0;

        double lastx = xpoints[npoints - 1];
        double lasty = ypoints[npoints - 1];
        double curx, cury;

        // Walk the edges of the polygon
        for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++) {
            curx = xpoints[i];
            cury = ypoints[i];

            if (cury == lasty) {
                continue;
            }

            double leftx;
            if (curx < lastx) {
                if (xValue >= lastx) {
                    continue;
                }
                leftx = curx;
            } else {
                if (xValue >= curx) {
                    continue;
                }
                leftx = lastx;
            }

            double test1, test2;
            if (cury < lasty) {
                if (yValue < cury || yValue >= lasty) {
                    continue;
                }
                if (xValue < leftx) {
                    hits++;
                    continue;
                }
                test1 = xValue - curx;
                test2 = yValue - cury;
            } else {
                if (yValue < lasty || yValue >= cury) {
                    continue;
                }
                if (xValue < leftx) {
                    hits++;
                    continue;
                }
                test1 = xValue - lastx;
                test2 = yValue - lasty;
            }

            if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
                hits++;
            }
        }

        return ((hits & 1) != 0);
    }

    @Override
    public final boolean contains(final Point2D point) {
        return contains(point.getX(), point.getY());
    }

    /**
     * Tests if the interior of this
     * <code>Polygon</code> intersects the interior of a specified set of
     * rectangular coordinates.
     *
     * @param xValue the xValue coordinate of the specified rectangular shape's
     * top-left corner
     * @param yValue the yValue coordinate of the specified rectangular shape's
     * top-left corner
     * @param width the width of the specified rectangular shape
     * @param height the height of the specified rectangular shape
     * @return <code>true</code> if the interior of this <code>Polygon</code>
     * and the interior of the specified set of rectangular coordinates
     * intersect each other; <code>false</code> otherwise
     * @since 1.2
     */
    @Override
    public final boolean intersects(final double xValue, final double yValue, final double width, final double height) {
        if (npoints <= 0 || !bounds.intersects(xValue, yValue, width, height)) {
            return false;
        }
        updateComputingPath();
        return closedPath.intersects(xValue, yValue, width, height);
    }

    /**
     * Updates the computing path.
     */
    private void updateComputingPath() {
        if (npoints >= 1 && closedPath == null) {
            closedPath = (GeneralPath) path.clone();
            closedPath.closePath();
        }
    }

    /**
     * Tests if the interior of this
     * <code>Polygon</code> intersects the interior of a specified
     * <code>Rectangle2D</code>.
     *
     * @param rectangle a specified <code>Rectangle2D</code>
     * @return <code>true</code> if this <code>Polygon</code> and the interior
     * of the specified <code>Rectangle2D</code> intersect each
     * other; <code>false</code> otherwise.
     */
    @Override
    public final boolean intersects(final Rectangle2D rectangle) {
        return intersects(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
    }

    /**
     * Tests if the interior of this
     * <code>Polygon</code> entirely contains the specified set of rectangular
     * coordinates.
     *
     * @param xValue the xValue coordinate of the top-left corner of the
     * specified set of rectangular coordinates
     * @param yValue the yValue coordinate of the top-left corner of the
     * specified set of rectangular coordinates
     * @param width the width of the set of rectangular coordinates
     * @param height the height of the set of rectangular coordinates
     * @return <code>true</code> if this <code>Polygon</code> entirely contains
     * the specified set of rectangular coordinates; <code>false</code>
     * otherwise
     * @since 1.2
     */
    @Override
    public final boolean contains(final double xValue, final double yValue, final double width, final double height) {
        if (npoints <= 0 || !bounds.intersects(xValue, yValue, width, height)) {
            return false;
        }

        updateComputingPath();
        return closedPath.contains(xValue, yValue, width, height);
    }

    /**
     * Tests if the interior of this
     * <code>Polygon</code> entirely contains the specified
     * <code>Rectangle2D</code>.
     *
     * @param rectangle the specified <code>Rectangle2D</code>
     * @return <code>true</code> if this <code>Polygon</code> entirely contains
     * the specified <code>Rectangle2D</code>; <code>false</code> otherwise.
     * @see #contains(double, double, double, double)
     */
    @Override
    public final boolean contains(final Rectangle2D rectangle) {
        return contains(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
    }

    /**
     * Returns an iterator object that iterates along the boundary of this
     * <code>Polygon</code> and provides access to the geometry of the outline
     * of this
     * <code>Polygon</code>. An optional {@link AffineTransform} can be
     * specified so that the coordinates returned in the iteration are
     * transformed accordingly.
     *
     * @param at an optional <code>AffineTransform</code> to be applied to the
     * coordinates as they are returned in the iteration, or <code>null</code>
     * if untransformed coordinates are desired
     * @return a {@link PathIterator} object that provides access to the
     * geometry of this <code>Polygon</code>.
     */
    @Override
    public final PathIterator getPathIterator(AffineTransform at) {
        return new Polygon2DPathIterator(this, at);
    }

    /**
     * Returns an iterator object that iterates along the boundary of the
     * <code>Shape</code> and provides access to the geometry of the outline of
     * the
     * <code>Shape</code>. Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE point
     * types are returned by the iterator. Since polygons are already flat, the
     * <code>flatness</code> parameter is ignored. An optional
     * <code>AffineTransform</code> can be specified in which case the
     * coordinates returned in the iteration are transformed accordingly.
     *
     * @param affineTransform an optional <code>AffineTransform</code> to be
     * applied to the coordinates as they are returned in the iteration, or
     * <code>null</code> if untransformed coordinates are desired
     * @param flatness the maximum amount that the control points for a given
     * curve can vary from colinear before a subdivided curve is replaced by a
     * straight line connecting the endpoints. Since polygons are already flat
     * the <code>flatness</code> parameter is ignored.
     * @return a <code>PathIterator</code> object that provides access to the
     * <code>Shape</code> object's geometry.
     */
    @Override
    public final PathIterator getPathIterator(final AffineTransform affineTransform, final double flatness) {
        return getPathIterator(affineTransform);
    }

    /**
     * Computes intermediate points on the polygon.
     */
    class Polygon2DPathIterator implements PathIterator {

        /**
         * Polygon.
         */
        private Polygon2D poly;
        /**
         * Affine transformation.
         */
        private AffineTransform transform;
        /**
         * point in polygon.
         */
        private transient int index;

        /**
         * Constructor.
         *
         * @param polygon polygon
         * @param affineTransformation affine transformation
         */
        Polygon2DPathIterator(final Polygon2D polygon, final AffineTransform affineTransformation) {
            poly = polygon;
            transform = affineTransformation;
            if (polygon.npoints == 0) {
                // Prevent a spurious SEG_CLOSE segment
                index = 1;
            }
        }

        /**
         * Returns the winding rule for determining the interior of the path.
         *
         * @return an integer representing the current winding rule.
         * @see PathIterator#WIND_NON_ZERO
         */
        @Override
        public int getWindingRule() {
            return WIND_EVEN_ODD;
        }

        /**
         * Tests if there are more points to read.
         *
         * @return <code>true</code> if there are more points to read;
         * <code>false</code> otherwise.
         */
        @Override
        public boolean isDone() {
            return index > getPoly().npoints;
        }

        /**
         * Moves the iterator forwards, along the primary direction of
         * traversal, to the next segment of the path when there are more points
         * in that direction.
         */
        @Override
        public void next() {
            index++;
        }

        /**
         * Returns the coordinates and type of the current path segment in the
         * iteration. The return value is the path segment type: SEG_MOVETO,
         * SEG_LINETO, or SEG_CLOSE. A
         * <code>float</code> array of length 2 must be passed in and can be
         * used to store the coordinates of the point(s). Each point is stored
         * as a pair of
         * <code>float</code> xValue,&nbsp;yValue coordinates. SEG_MOVETO and SEG_LINETO
         * types return one point, and SEG_CLOSE does not return any points.
         *
         * @param coords a <code>float</code> array that specifies the
         * coordinates of the point(s)
         * @return an integer representing the type and coordinates of the
         * current path segment.
         * @see PathIterator#SEG_MOVETO
         * @see PathIterator#SEG_LINETO
         * @see PathIterator#SEG_CLOSE
         */
        @Override
        public final int currentSegment(final float[] coords) {
            if (index >= getPoly().npoints) {
                return SEG_CLOSE;
            }
            coords[0] = (float) getPoly().xpoints[index];
            coords[1] = (float) getPoly().ypoints[index];
            if (getTransform() != null) {
                getTransform().transform(coords, 0, coords, 0, 1);
            }
            return (index == 0 ? SEG_MOVETO : SEG_LINETO);
        }

        /**
         * Returns the coordinates and type of the current path segment in the
         * iteration. The return value is the path segment type: SEG_MOVETO,
         * SEG_LINETO, or SEG_CLOSE. A
         * <code>double</code> array of length 2 must be passed in and can be
         * used to store the coordinates of the point(s). Each point is stored
         * as a pair of
         * <code>double</code> xValue,&nbsp;yValue coordinates. SEG_MOVETO and SEG_LINETO
         * types return one point, and SEG_CLOSE does not return any points.
         *
         * @param coords a <code>double</code> array that specifies the
         * coordinates of the point(s)
         * @return an integer representing the type and coordinates of the
         * current path segment.
         * @see PathIterator#SEG_MOVETO
         * @see PathIterator#SEG_LINETO
         * @see PathIterator#SEG_CLOSE
         */
        @Override
        public final int currentSegment(final double[] coords) {
            if (index >= getPoly().npoints) {
                return SEG_CLOSE;
            }
            coords[0] = getPoly().xpoints[index];
            coords[1] = getPoly().ypoints[index];
            if (getTransform() != null) {
                getTransform().transform(coords, 0, coords, 0, 1);
            }
            return (index == 0 ? SEG_MOVETO : SEG_LINETO);
        }

        /**
         * Returns the transformation.
         *
         * @return the affine transformation
         */
        public AffineTransform getTransform() {
            return transform;
        }

        /**
         * Sets the affine transformation.
         *
         * @param transformVal the transform to set
         */
        public void setTransform(final AffineTransform transformVal) {
            this.transform = transformVal;
        }

        /**
         * Returns the polygon
         *
         * @return the poly
         */
        public Polygon2D getPoly() {
            return poly;
        }

        /**
         * Sets the polygon.
         *
         * @param polyVal the poly to set
         */
        public void setPoly(final Polygon2D polyVal) {
            this.poly = polyVal;
        }
    }
}
