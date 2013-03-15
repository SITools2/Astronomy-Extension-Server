/**
 * *****************************************************************************
 * Copyright 2012, 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package fr.cnes.sitools.SearchGeometryEngine;

import healpix.essentials.Vec3;
import healpix.tools.SpatialVector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * This object contains methods to create a polygon
 *
 * <p> A polygon is described by at a set points (>3).<br/> The last point of the polygon IS NOT the first one. </p>
 *
 * @author Jean-Christophe Malapert
 */
public class Polygon implements Shape {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(Polygon.class.getName());
  /**
   * Type of shape.
   */
  private static final String TYPE = "POLYGON";
  /**
   * Minimum number of points to define a quadrilatere.
   */
  private static final int QUADRI = 4;
  /**
   * Number of sides for a triangle.
   */
  private static final int POINTS_TRIANGLE = 3;
  /**
   * 2 * PI.
   */
  private static final double PI2 = Math.PI * 2;
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
   * Create a polygon from a list of points.
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
   * Create a polygon from two points from a rectangle.
   *
   * @param lowerLeft upper right corner
   * @param upperRight lower left corner
   */
  public Polygon(final Point lowerLeft, final Point upperRight) {
    Point p1 = lowerLeft;
    Point p2 = new Point(lowerLeft.getLongitude(), upperRight.getLatitude(),
            upperRight.getCoordSystem());
    Point p3 = upperRight;
    Point p4 = new Point(upperRight.getLongitude(), lowerLeft.getLatitude(),
            lowerLeft.getCoordSystem());
    setPoints(Arrays.asList(p1, p4, p3, p2));
    checkCoordSystem(getPoints());
  }

  public final List<Polygon> splitEquatorialPolygon() {
    Polygon polygon = new Polygon(getPoints());
    return polygon.splitInTriangles();
  }

  public final List<Polygon> splitGeocentricPolygon() {
    List<Polygon> polygons = new ArrayList<Polygon>();
    double maxLatitude = getPoints().get(0).getLatitude();
    double minLatitude = getPoints().get(0).getLatitude();
    double maxLongitude = getPoints().get(0).getLongitude();
    double minLongitude = getPoints().get(0).getLongitude();
    for (int i = 1; i < getPoints().size(); i++) {
      if (getPoints().get(i).getLongitude() > maxLongitude) {
        maxLongitude = getPoints().get(i).getLongitude();
      } else if (getPoints().get(i).getLongitude() < minLongitude) {
        minLongitude = getPoints().get(i).getLongitude();
      }
      if (getPoints().get(i).getLatitude() > maxLatitude) {
        maxLatitude = getPoints().get(i).getLatitude();
      } else if (getPoints().get(i).getLatitude() < minLatitude) {
        minLatitude = getPoints().get(i).getLatitude();
      }
    }
    Point p1 = getPoints().get(0);
    Point p4 = getPoints().get(1);
    Point p3 = getPoints().get(2);
    Point p2 = getPoints().get(3);
    
    for (double longitude = minLongitude + 5; longitude < maxLongitude; longitude += 5) {
      Point pHight = new Point(longitude, maxLatitude, CoordSystem.GEOCENTRIC);
      Point pLow = new Point(longitude, minLatitude, CoordSystem.GEOCENTRIC);
      Polygon poly = new Polygon(Arrays.asList(p1, p2, pHight, pLow));
      polygons.add(poly);
      p1 = new Point(pLow.getLongitude(), pLow.getLatitude(), pLow.getCoordSystem());
      p2 = new Point(pHight.getLongitude(), pHight.getLatitude(), pHight.getCoordSystem());
    }
    Polygon poly = new Polygon(Arrays.asList(p1, p2, p3, p4));
    polygons.add(poly);
    return polygons;
  }

  /**
   * Split a no clockwised polygon in two polygons.
   *
   * @return a list of polygons
   */
  public final List<Polygon> splitPolygon() {
    CoordSystem coord = getPoints().get(0).getCoordSystem();
    List<Polygon> polygons;
    switch (coord) {
      case EQUATORIAL:
        polygons = splitEquatorialPolygon();
        break;
      case GEOCENTRIC:
        polygons = splitGeocentricPolygon();
        break;
      default:
        throw new IllegalArgumentException("Reference system is not recognized.");
    }
    return polygons;
  }

  /**
   * Empty constructor.
   */
  protected Polygon() {
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
   * Test if the shape is clockwised.
   *
   * @return true if clockwise
   */
  public final boolean isClockwise() {
    Vec3 a = new Vec3(getPoints().get(0));
    Vec3 b = new Vec3(getPoints().get(1));
    Vec3 c = new Vec3(getPoints().get(2));
    Vec3 ba = new Vec3(a);
    ba.sub(b);
    Vec3 bc = new Vec3(c);
    bc.sub(b);
    Vec3 abc = ba.cross(bc);
    return (abc.dot(b) >= 0);
  }

  /**
   * Clockwise a shape.
   */
  public final void fixClockwise() {
    if (!this.isClockwise()) {
      List<Point> tmp = new ArrayList<Point>(getPoints());
      for (int i = 0; i < tmp.size(); i++) {
        getPoints().set(i, tmp.get(tmp.size() - 1 - i));
      }
    }
  }

  /**
   * Clockwise a shape.
   *
   * @param polygon polygon
   * @return a clockwised polygon
   */
  public final Polygon fixClockwise(final Polygon polygon) {
    Polygon polygonClockWised;
    if (!polygon.isClockwise()) {
      List<Point> pointsClockWised = new ArrayList<Point>(polygon.getPoints());
      int i = polygon.getPoints().size() - 1;
      for (Point p : polygon.getPoints()) {
        pointsClockWised.set(i, p);
        i--;
      }
      polygonClockWised = new Polygon(pointsClockWised);
    } else {
      polygonClockWised = polygon;
    }
    return polygonClockWised;
  }

  /**
   * Compute normal's vectors.
   *
   * @param shapePoints list of points
   * @return Normal vector
   */
  protected final Vec3 computeNormalToPolygon(final List<Point> shapePoints) {
    Vec3 a = new Vec3(shapePoints.get(0));
    Vec3 b = new Vec3(shapePoints.get(1));
    Vec3 c = new Vec3(shapePoints.get(2));
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
   * Splitting whatever large polygon into triangles.
   *
   * @return Returns a list of triangles
   */
  public final List<Polygon> splitInTriangles() {
    Vec3 barycenter = getBarycenter(getPoints());
    barycenter.normalize();
    barycenter.scale(-1);
    double theta = Math.atan2(Math.sqrt(barycenter.x * barycenter.x + barycenter.y * barycenter.y), barycenter.z);
    double phi = Math.atan2(barycenter.y, barycenter.x);
    if (phi < 0.) {
      phi += 2 * Math.PI;
    }
    if (phi >= 2 * Math.PI) {
      phi -= 2 * Math.PI;

    }
    Point pTriangle = null;
    if (getPoints().get(0).getCoordSystem().equals(CoordSystem.GEOCENTRIC)) {
      pTriangle = new Point(CoordSystem.convertPhiToLongitudeGeo(Math.toDegrees(phi)), CoordSystem.convertThetaToLatitudeGeo(Math.toDegrees(theta)), CoordSystem.GEOCENTRIC);
    } else if (getPoints().get(0).getCoordSystem().equals(CoordSystem.EQUATORIAL)) {
      pTriangle = new Point(CoordSystem.convertPhiToRa(Math.toDegrees(phi)), CoordSystem.convertThetaToDec(Math.toDegrees(theta)), CoordSystem.EQUATORIAL);
    } else {
      throw new IllegalAccessError("Unknown coord system");
    }
    List<Polygon> triangles = new ArrayList<Polygon>();
    for (int i = 0; i < getPoints().size() - 1; i++) {
      Polygon triangle = new Polygon(Arrays.asList(getPoints().get(i), getPoints().get(i + 1), pTriangle));
      triangles.add(triangle);
    }
    triangles.add(new Polygon(Arrays.asList(getPoints().get(getPoints().size() - 1), getPoints().get(0), pTriangle)));
    return triangles;
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
    Point p1 = pointsVal.get(0);
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
  public final String getType() {
    return TYPE;
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
      result += String.format(" (%s , %s)", point.getLongitude(), point.getLatitude());
    }
    return result.concat(")");
  }
}
