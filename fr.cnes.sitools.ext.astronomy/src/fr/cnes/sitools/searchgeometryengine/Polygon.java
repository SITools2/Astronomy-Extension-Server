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
package fr.cnes.sitools.searchgeometryengine;

import healpix.essentials.Vec3;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Contains methods to create a polygon
 *
 * <p> A polygon is described by at a set points (>3).<br/> The last point of the polygon IS NOT the first one. </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class Polygon implements Shape {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(Polygon.class.getName());
  /**
   * Numerical precision for double operation.
   */
  private static final double NUMERICAL_PRECISION = 1e-15;
  /**
   * Number of sides for a triangle.
   */
  private static final int POINTS_TRIANGLE = 3;
  /**
   * List of points that determines the shape.
   */
  private List<Point> points;

  /**
   * Copy a polygon.
   *
   * @param polygon polygon
   */
  public Polygon(final Polygon polygon) {
    this.points = polygon.getPoints();
  }

  /**
   * Creates a polygon from a list of points.
   *
   * <p>IllegalArgumentException if the number of points in the shape < 3</p>
   *
   * @param pointsShape list of points
   */
  public Polygon(final List<Point> pointsShape) {
    if (pointsShape.size() < POINTS_TRIANGLE) {
      throw new IllegalArgumentException("This is not a polygon");
    }
    this.points = pointsShape;
  }

  /**
   * Creates a polygon from two points from a rectangle.
   *
   * <p>IllegalArgumentException if it is not a polygon.</p>
   *
   * @param lowerLeft upper right corner
   * @param upperRight lower left corner
   */
  public Polygon(final Point lowerLeft, final Point upperRight) {
    if (lowerLeft == null || upperRight == null
            || lowerLeft.getLongitude() == upperRight.getLongitude()
            || lowerLeft.getLatitude() == upperRight.getLatitude()) {
      throw new IllegalArgumentException("This is not a polygon.");
    }
    Point p1 = lowerLeft;
    Point p2 = new Point(lowerLeft.getLongitude(), upperRight.getLatitude(),
            upperRight.getCoordSystem());
    Point p3 = upperRight;
    Point p4 = new Point(upperRight.getLongitude(), lowerLeft.getLatitude(),
            lowerLeft.getCoordSystem());
    setPoints(Arrays.asList(p1, p4, p3, p2));
    checkCoordSystem(getPoints());
  }

  /**
   * Empty constructor.
   */
  protected Polygon() {
  }

    /**
     * Test if shape is clockwised.
     * @return true if clockwise
     */
    public final boolean isClockwise() {
        final Vec3 a = new Vec3(points.get(0).getAsVector().x(), points.get(0).getAsVector().y(), points.get(0).getAsVector().z());
        final Vec3 b = new Vec3(points.get(1).getAsVector().x(), points.get(1).getAsVector().y(), points.get(1).getAsVector().z());
        final Vec3 c = new Vec3(points.get(2).getAsVector().x(), points.get(2).getAsVector().y(), points.get(2).getAsVector().z());
        Vec3 ba = new Vec3(a);
        ba = ba.sub(b);
        Vec3 bc = new Vec3(c);
        bc = bc.sub(b);
        ba = ba.cross(bc);
        boolean returnedValue;
        if(ba.dot(a)>=0)
            returnedValue = true;
        else
            returnedValue = false;
        return returnedValue;
    }

  /**
   * Returns
   * <code>true</code> when the polygon is convex otherwise
   * <code>false</code>.
   *
   * @return <code>true</code> when the polygon is convex otherwise <code>false</code>
   */
  public final boolean isConvex() {
    Vec3 p0 = new Vec3(getPoints().get(getPoints().size() - 1));
    Vec3 firstNormal = p0.cross(new Vec3(getPoints().get(0)));
    for (int i = 0; i < getPoints().size() - 1; i++) {
      Vec3 p1 = new Vec3(getPoints().get(i));
      Vec3 p2 = new Vec3(getPoints().get(i + 1));
      Vec3 normal = p1.cross(p2);
      if (normal.dot(firstNormal) < 0) {
        return false;
      } else {
        p0 = new Vec3(getPoints().get(i));
        firstNormal = p0.cross(new Vec3(getPoints().get(i+1)));
      }
    }
    return true;
  }

  /**
   * Returns the points of the polygon.
   *
   * @return the points of the polygon
   */
  public final List<Point> getPoints() {
    return Collections.unmodifiableList(points);
  }

  /**
   * Tests if the shape is clockwised.
   *
   * @return true if clockwise
   */
  public final boolean isClockwised() {
    boolean isClockwise = true;
    final int nbPoints = getPoints().size();
    for (int i = 0; i < nbPoints - 2; i++) {
      if (!isClockWised(getPoints().get(i), getPoints().get(i + 1), getPoints().get(i + 2))) {
        isClockwise = false;
        break;
      }
    }
    if (isClockwise == true && !isClockWised(getPoints().get(nbPoints - 2), getPoints().get(nbPoints - 1), getPoints().get(0))) {
      isClockwise = false;
    }
    return isClockwise;
  }

  /**
   * Tests if the shape is counter clockwised.
   *
   * @return true if clockwise
   */
  public final boolean isCounterClockwised() {
    return !isClockwised();
  }

  private static double numericPrecision(final double a) {
    double res;
    if (a < NUMERICAL_PRECISION && a > -1 * NUMERICAL_PRECISION) {
      res = 0;
    } else {
      res = a;
    }
    return res;
  }

  /**
   * Tests if the shape is clockwised.
   *
   * @param p0 1st point of the shape
   * @param p1 2nd point of the shape
   * @param p2 3rd point of the shape
   * @return true if clockwise
   */
  public static boolean isClockWised(final Point p0, final Point p1, final Point p2) {
    boolean returnedValue;
    if (p0.getLatitude() == p1.getLatitude() &&  p1.getLatitude() == p2.getLatitude()) {
      returnedValue = true;
    } else if (p0.getLongitude() == p1.getLongitude() &&  p1.getLongitude() == p2.getLongitude()) {
      returnedValue = true;
    } else {
      Vec3 a = new Vec3(p0);
      Vec3 b = new Vec3(p1);
      Vec3 c = new Vec3(p2);
      Vec3 ba = new Vec3(a);
      ba.sub(b);
      Vec3 bc = new Vec3(c);
      bc.sub(b);
      Vec3 abc = ba.cross(bc);
      returnedValue = numericPrecision(abc.dot(b)) >= 0;
    }
    return returnedValue;
  }

  /**
   * CounterClockwises a shape.
   *
   * @return the list of points in a counterclockwise way
   */
  private List<Point> fixCounterClockwise() {
    final List<Point> counterClockwisePoints = new ArrayList<Point>(this.points);
    if (this.isClockwised()) {
      Collections.reverse(counterClockwisePoints);
    }
    return counterClockwisePoints;
  }

  /**
   * Returns
   * <code>true</code> when
   * <code>p</code> is included in the triangle otherwise
   * <code>false</code>.
   *
   * @param p point to test
   * @param p0 First point of the triangle
   * @param p1 Second point of the triangle
   * @param p2 Third point of the triangle
   * @return <code>true</code> when p is included in the triangle otherwise <code>false</code>
   */
  private boolean testPointInTriangle(final Point p, final Point p0, final Point p1, final Point p2) {
    final Vec3 p0n = new Vec3(p0);
    final Vec3 p0p1n = p0n.cross(new Vec3(p1));
    final Vec3 p1n = new Vec3(p1);
    final Vec3 p1p2n = p1n.cross(new Vec3(p2));
    final Vec3 p2n = new Vec3(p2);
    final Vec3 p2p0n = p2n.cross(new Vec3(p0));
    return (p0p1n.dot(new Vec3(p)) >= 0) && (p1p2n.dot(new Vec3(p)) >= 0) && (p2p0n.dot(new Vec3(p)) >= 0);
  }

  /**
   * Detected the North and South pole
   * @param points
   * @return 
   */
  protected List<Point> detectPole(final List<Point> points) {
    final double latNorthPole = 90.;
    final double latSouthPole = -90.;
    List<Point> cleanedPoints = new ArrayList<Point>();
    boolean isDetectedNorthPole = false;
    boolean isDetectedSouthPole = false;
    for (Point iter : points) {      
      double lat = iter.getLatitude();
      if (lat == latNorthPole && isDetectedNorthPole == false) {
        isDetectedNorthPole = true;
        cleanedPoints.add(iter);
      } else if (lat == latNorthPole && isDetectedNorthPole == true) {
      } else if (lat == latSouthPole && isDetectedSouthPole == false) {
        isDetectedSouthPole = true;
        cleanedPoints.add(iter);
      } else if (lat == latSouthPole && isDetectedSouthPole == true) {
      } else {
        cleanedPoints.add(iter);
      }
    }
    return cleanedPoints;
  }

  /**
   * Triangulates a whathever polygon by the use of EarCutting algorithm.
   *
   * @return Returns the list of triangles from the polygon
   */
  public final List<Polygon> triangulate() {
    List<Point> ccPoints = fixCounterClockwise();
    ccPoints = detectPole(ccPoints);
    List<Polygon> polygons = new ArrayList<Polygon>();
    List<Point> v = ccPoints;
    int n = v.size();

    int[] prev = new int[n];
    int[] next = new int[n];
    for (int i = 0; i < n; i++) {
      prev[i] = i - 1;
      next[i] = i + 1;
    }
    prev[0] = n - 1;
    next[n - 1] = 0;

    int i = 0;
    while (n > POINTS_TRIANGLE) {
      boolean isEar = true;
      if (!isClockWised(v.get(prev[i]), v.get(i), v.get(next[i]))) {
        int k = next[next[i]];
        do {
          if (testPointInTriangle(v.get(k), v.get(prev[i]), v.get(i), v.get(next[i]))) {
            isEar = false;
            break;
          }
          k = next[k];
        } while (k != prev[i]);
      } else {
        isEar = false;
      }

      if (isEar) {
        polygons.add(new Polygon(Arrays.asList(v.get(i), v.get(prev[i]), v.get(next[i]))));
        next[prev[i]] = next[i];
        prev[next[i]] = prev[i];
        n--;
        i = prev[i];
      } else {
        i = next[i];
      }
    }
    polygons.add(new Polygon(Arrays.asList(v.get(i), v.get(prev[i]), v.get(next[i]))));
    return polygons;
  }

  /**
   * Compute normal's vectors.
   *
   * @param shapePoints list of points
   * @return Normal vector
   */
  protected final Vec3 computeNormalToPolygon(final List<Point> shapePoints) {
    final Vec3 a = new Vec3(shapePoints.get(0));
    final Vec3 b = new Vec3(shapePoints.get(1));
    final Vec3 c = new Vec3(shapePoints.get(2));
    Vec3 ba = new Vec3(a);
    ba.sub(b);
    Vec3 bc = new Vec3(c);
    bc.sub(b);
    Vec3 abc = ba.cross(bc);
    return abc;
  }

  /**
   * Compute the barycenter of the polygon.
   *
   * @param shapePoints list of points
   * @return the barycenter
   */
  protected final Vec3 getBarycenter(final List<Point> shapePoints) {
    Vec3 barycenter = new Vec3();
    for (Point it : shapePoints) {
      barycenter = barycenter.add(new Vec3(it));
    }
    barycenter.scale(1.0 / getPoints().size());
    return barycenter;
  }

  /**
   * Returns true.
   *
   * @return true
   */
  @Override
  public final boolean isSurface() {
    return true;
  }

  /**
   * Set the points of the polygon in a clockwise way.
   *
   * <p>IllegalArgumentException is the number of points < 3.</p>
   *
   * @param val points of the polygon
   */
  protected final void setPoints(final List<Point> val) {
    if (val.size() < POINTS_TRIANGLE) {
      throw new IllegalArgumentException("This is not a polygon");
    }
    this.points = val;
  }

  /**
   * Check if all points have the same reference frame.
   *
   * @param pointsVal list of points
   */
  private void checkCoordSystem(final List<Point> pointsVal) {
    final Point p1 = pointsVal.get(0);
    for (Point p : pointsVal) {
      if (!p1.getCoordSystem().equals(p.getCoordSystem())) {
        throw new IllegalArgumentException("Each point of the polygon must have the same reference frame");
      }
    }
  }

  /**
   * Returns the type of the shape.
   *
   * @return type of the shape
   */
  @Override
  public final Type getType() {
    return Shape.Type.POLYGON;
  }

  @Override
  public final boolean equals(final Object obj) {
    //check for self-comparison
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Polygon)) {
      return false;
    }

    //cast to native object is now safe
    Polygon that = (Polygon) obj;

    //now a proper field-by-field evaluation can be made
    for (int i = 0; i < points.size(); i++) {
      if (!points.get(i).equals(that.points.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public final int hashCode() {
    int hash = 3;
    hash = 67 * hash + (this.points != null ? this.points.hashCode() : 0);
    return hash;
  }

  @Override
  public final String toString() {
    String result = "(";
    for (Point point : points) {
      result = result.concat(String.format(" (%s , %s)", point.getLongitude(), point.getLatitude()));
    }
    return result.concat(")");
  }
}
