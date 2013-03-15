/**
 * This class comes from Koders.com (an open source code search engine).
 * This class has been modified to remove the dependence with the package
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
 * @author malapert
 */
public class Polygon2D implements Shape {

    /**
     *
     */
    public int npoints;
    /**
     *
     */
    public double xpoints[];
    /**
     *
     */
    public double ypoints[];
    /**
     *
     */
    protected Rectangle bounds;
    
    private GeneralPath path;
    private GeneralPath closedPath;    

    /**
     *
     */
    public Polygon2D() {
        xpoints = new double[4];
        ypoints = new double[4];
    }

    /**
     *
     * @param pixels
     */
    public Polygon2D(List<Point2D.Double> pixels) {
        this.npoints = pixels.size();
        this.xpoints = new double[npoints];
        this.ypoints = new double[npoints];
        int i = 0;
        for (Point2D.Double pixelIter : pixels) {
            this.xpoints[i] = pixelIter.getX();
            this.ypoints[i] = pixelIter.getY();
            i++;
        }
    }

    /**
     *
     * @param xpoints
     * @param ypoints
     * @param npoints
     */
    public Polygon2D(Double xpoints[], Double ypoints[], int npoints) {
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException(
                    "npoints > xpoints.length || npoints > ypoints.length");
        }
        this.npoints = npoints;
        this.xpoints = new double[npoints];
        this.ypoints = new double[npoints];
        for (int i = 0; i < this.npoints; i++) {
            this.xpoints[i] = xpoints[i];
        }
        for (int i = 0; i < this.npoints; i++) {
            this.ypoints[i] = ypoints[i];
        }
    }

    /**
     *
     * @param xpoints
     * @param ypoints
     * @param npoints
     */
    public Polygon2D(double xpoints[], double ypoints[], int npoints) {
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException(
                    "npoints > xpoints.length || npoints > ypoints.length");
        }
        this.npoints = npoints;
        this.xpoints = new double[npoints];
        this.ypoints = new double[npoints];
        System.arraycopy(xpoints, 0, this.xpoints, 0, npoints);
        System.arraycopy(ypoints, 0, this.ypoints, 0, npoints);
    }

    /**
     *
     * @param pts
     */
    public Polygon2D(Point2D[] pts) {
        this.npoints = pts.length;
        this.xpoints = new double[this.npoints];
        this.ypoints = new double[this.npoints];
        for (int i = 0; i < this.npoints; i++) {
            xpoints[i] = pts[i].getX();
            ypoints[i] = pts[i].getY();
        }
    }

    /**
     *
     */
    public void reset() {
        npoints = 0;
        bounds = null;
        path = new GeneralPath();
        closedPath = null;        
    }
      

    /**
     *
     */
    public void invalidate() {
        bounds = null;
    }

    /**
     *
     * @param deltaX
     * @param deltaY
     */
    public void translate(int deltaX, int deltaY) {
        for (int i = 0; i < npoints; i++) {
            xpoints[i] += deltaX;
            ypoints[i] += deltaY;
        }
        if (bounds != null) {
            bounds.translate(deltaX, deltaY);
        }
    }

    void calculateBounds(double xpoints[], double ypoints[], int npoints) {
        double boundsMinX = Double.MAX_VALUE;
        double boundsMinY = Double.MAX_VALUE;
        double boundsMaxX = Double.MIN_VALUE;
        double boundsMaxY = Double.MIN_VALUE;

        for (int i = 0; i < npoints; i++) {
            double x = xpoints[i];
            boundsMinX = Math.min(boundsMinX, x);
            boundsMaxX = Math.max(boundsMaxX, x);
            double y = ypoints[i];
            boundsMinY = Math.min(boundsMinY, y);
            boundsMaxY = Math.max(boundsMaxY, y);
        }
        bounds = new Rectangle((int) Math.floor(boundsMinX), (int) Math.floor(boundsMinY),
                (int) Math.ceil(boundsMaxX - boundsMinX), (int) Math.ceil(boundsMaxY - boundsMinY));
    }

    void updateBounds(double x, double y) {
        if ((int) x < bounds.x) {
            bounds.width += (bounds.x - (int) x);
            bounds.x = (int) x;
        } else {
            bounds.width = Math.max(bounds.width, (int) Math.ceil(x) - bounds.x);
            // bounds.x = bounds.x;
        }

        if ((int) y < bounds.y) {
            bounds.height += (bounds.y - (int) y);
            bounds.y = (int) y;
        } else {
            bounds.height = Math.max(bounds.height, (int) Math.ceil(y) - bounds.y);
            // bounds.y = bounds.y;
        }
    }

    /**
     *
     * @param x
     * @param y
     */
    public void addPoint(double x, double y) {
        if (npoints == xpoints.length) {
            double tmp[];

            tmp = new double[npoints * 2];
            System.arraycopy(xpoints, 0, tmp, 0, npoints);
            xpoints = tmp;

            tmp = new double[npoints * 2];
            System.arraycopy(ypoints, 0, tmp, 0, npoints);
            ypoints = tmp;
        }
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
        if (bounds != null) {
            updateBounds(x, y);
        }
    }

    @Override
    public Rectangle getBounds() {
        if (npoints == 0) {
            return new Rectangle();
        }
        if (bounds == null) {
            calculateBounds(xpoints, ypoints, npoints);
        }
        return bounds.getBounds();
    }

    /**
     *
     * @param p
     * @return
     */
    public boolean contains(Point p) {
        return contains(p.getX(), p.getY());
    }

    /**
     *
     * @param x
     * @param y
     * @return
     */
    public boolean contains(int x, int y) {
        return contains((double) x, (double) y);
    }

    /**
     * Returns the high precision bounding box of the {@link Shape}.
     * 
     * @return a {@link Rectangle2D} that precisely bounds the
     *         <code>Shape</code>.
     */
    @Override
    public Rectangle2D getBounds2D() {
        return getBounds();
    }

    /**
     * Determines if the specified coordinates are inside this
     * <code>Polygon</code>. For the definition of <i>insideness</i>, see
     * the class comments of {@link Shape}.
     * 
     * @param x
     *            the specified x coordinate
     * @param y
     *            the specified y coordinate
     * @return <code>true</code> if the <code>Polygon</code> contains the
     *         specified coordinates; <code>false</code> otherwise.
     */
    @Override
    public boolean contains(double x, double y) {
        if (npoints <= 2 || !getBounds().contains(x, y)) {
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
                if (x >= lastx) {
                    continue;
                }
                leftx = curx;
            } else {
                if (x >= curx) {
                    continue;
                }
                leftx = lastx;
            }

            double test1, test2;
            if (cury < lasty) {
                if (y < cury || y >= lasty) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - curx;
                test2 = y - cury;
            } else {
                if (y < lasty || y >= cury) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - lastx;
                test2 = y - lasty;
            }

            if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
                hits++;
            }
        }

        return ((hits & 1) != 0);
    }

    @Override
    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    /**
     * Tests if the interior of this <code>Polygon</code> intersects the
     * interior of a specified set of rectangular coordinates.
     * 
     * @param x
     *            the x coordinate of the specified rectangular shape's top-left
     *            corner
     * @param y
     *            the y coordinate of the specified rectangular shape's top-left
     *            corner
     * @param w
     *            the width of the specified rectangular shape
     * @param h
     *            the height of the specified rectangular shape
     * @return <code>true</code> if the interior of this <code>Polygon</code>
     *         and the interior of the specified set of rectangular coordinates
     *         intersect each other; <code>false</code> otherwise
     * @since 1.2
     */
    @Override
    public boolean intersects(double x, double y, double w, double h) {
	if (npoints <= 0 || !bounds.intersects(x, y, w, h)) {
	    return false;
	}
        updateComputingPath();
        return closedPath.intersects(x, y, w, h);
    }
    
    private void updateComputingPath() {
        if (npoints >= 1) {
            if (closedPath == null) {
                closedPath = (GeneralPath)path.clone();
                closedPath.closePath();            
            }   
        }
    }    

    /**
     * Tests if the interior of this <code>Polygon</code> intersects the
     * interior of a specified <code>Rectangle2D</code>.
     * 
     * @param r
     *            a specified <code>Rectangle2D</code>
     * @return <code>true</code> if this <code>Polygon</code> and the
     *         interior of the specified <code>Rectangle2D</code> intersect
     *         each other; <code>false</code> otherwise.
     */
    @Override
    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * Tests if the interior of this <code>Polygon</code> entirely contains
     * the specified set of rectangular coordinates.
     * 
     * @param x
     *            the x coordinate of the top-left corner of the specified set
     *            of rectangular coordinates
     * @param y
     *            the y coordinate of the top-left corner of the specified set
     *            of rectangular coordinates
     * @param w
     *            the width of the set of rectangular coordinates
     * @param h
     *            the height of the set of rectangular coordinates
     * @return <code>true</code> if this <code>Polygon</code> entirely
     *         contains the specified set of rectangular coordinates;
     *         <code>false</code> otherwise
     * @since 1.2
     */
    @Override
    public boolean contains(double x, double y, double w, double h) {
	if (npoints <= 0 || !bounds.intersects(x, y, w, h)) {
	    return false;
	}

        updateComputingPath();
        return closedPath.contains(x, y, w, h);
    }

    /**
     * Tests if the interior of this <code>Polygon</code> entirely contains
     * the specified <code>Rectangle2D</code>.
     * 
     * @param r
     *            the specified <code>Rectangle2D</code>
     * @return <code>true</code> if this <code>Polygon</code> entirely
     *         contains the specified <code>Rectangle2D</code>;
     *         <code>false</code> otherwise.
     * @see #contains(double, double, double, double)
     */
    @Override
    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    /**
     * Returns an iterator object that iterates along the boundary of this
     * <code>Polygon</code> and provides access to the geometry of the outline
     * of this <code>Polygon</code>. An optional {@link AffineTransform} can
     * be specified so that the coordinates returned in the iteration are
     * transformed accordingly.
     * 
     * @param at
     *            an optional <code>AffineTransform</code> to be applied to
     *            the coordinates as they are returned in the iteration, or
     *            <code>null</code> if untransformed coordinates are desired
     * @return a {@link PathIterator} object that provides access to the
     *         geometry of this <code>Polygon</code>.
     */
    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return new Polygon2DPathIterator(this, at);
    }

    /**
     * Returns an iterator object that iterates along the boundary of the
     * <code>Shape</code> and provides access to the geometry of the outline
     * of the <code>Shape</code>. Only SEG_MOVETO, SEG_LINETO, and SEG_CLOSE
     * point types are returned by the iterator. Since polygons are already
     * flat, the <code>flatness</code> parameter is ignored. An optional
     * <code>AffineTransform</code> can be specified in which case the
     * coordinates returned in the iteration are transformed accordingly.
     * 
     * @param at
     *            an optional <code>AffineTransform</code> to be applied to
     *            the coordinates as they are returned in the iteration, or
     *            <code>null</code> if untransformed coordinates are desired
     * @param flatness
     *            the maximum amount that the control points for a given curve
     *            can vary from colinear before a subdivided curve is replaced
     *            by a straight line connecting the endpoints. Since polygons
     *            are already flat the <code>flatness</code> parameter is
     *            ignored.
     * @return a <code>PathIterator</code> object that provides access to the
     *         <code>Shape</code> object's geometry.
     */
    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    class Polygon2DPathIterator
            implements PathIterator {

        Polygon2D poly;
        AffineTransform transform;
        int index;

        Polygon2DPathIterator(Polygon2D pg, AffineTransform at) {
            poly = pg;
            transform = at;
            if (pg.npoints == 0) {
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
         *         <code>false</code> otherwise.
         */
        @Override
        public boolean isDone() {
            return index > poly.npoints;
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
         * SEG_LINETO, or SEG_CLOSE. A <code>float</code> array of length 2
         * must be passed in and can be used to store the coordinates of the
         * point(s). Each point is stored as a pair of <code>float</code>
         * x,&nbsp;y coordinates. SEG_MOVETO and SEG_LINETO types return one
         * point, and SEG_CLOSE does not return any points.
         * 
         * @param coords
         *            a <code>float</code> array that specifies the
         *            coordinates of the point(s)
         * @return an integer representing the type and coordinates of the
         *         current path segment.
         * @see PathIterator#SEG_MOVETO
         * @see PathIterator#SEG_LINETO
         * @see PathIterator#SEG_CLOSE
         */
        @Override
        public int currentSegment(float[] coords) {
            if (index >= poly.npoints) {
                return SEG_CLOSE;
            }
            coords[0] = (float) poly.xpoints[index];
            coords[1] = (float) poly.ypoints[index];
            if (transform != null) {
                transform.transform(coords, 0, coords, 0, 1);
            }
            return (index == 0 ? SEG_MOVETO : SEG_LINETO);
        }

        /**
         * Returns the coordinates and type of the current path segment in the
         * iteration. The return value is the path segment type: SEG_MOVETO,
         * SEG_LINETO, or SEG_CLOSE. A <code>double</code> array of length 2
         * must be passed in and can be used to store the coordinates of the
         * point(s). Each point is stored as a pair of <code>double</code>
         * x,&nbsp;y coordinates. SEG_MOVETO and SEG_LINETO types return one
         * point, and SEG_CLOSE does not return any points.
         * 
         * @param coords
         *            a <code>double</code> array that specifies the
         *            coordinates of the point(s)
         * @return an integer representing the type and coordinates of the
         *         current path segment.
         * @see PathIterator#SEG_MOVETO
         * @see PathIterator#SEG_LINETO
         * @see PathIterator#SEG_CLOSE
         */
        @Override
        public int currentSegment(double[] coords) {
            if (index >= poly.npoints) {
                return SEG_CLOSE;
            }
            coords[0] = poly.xpoints[index];
            coords[1] = poly.ypoints[index];
            if (transform != null) {
                transform.transform(coords, 0, coords, 0, 1);
            }
            return (index == 0 ? SEG_MOVETO : SEG_LINETO);
        }
    }
    private static final Logger LOG = Logger.getLogger(Polygon2D.class.getName());
}