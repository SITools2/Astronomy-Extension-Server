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

import healpix.core.HealpixIndex;
import healpix.core.base.set.LongRangeIterator;
import healpix.core.base.set.LongRangeSet;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import healpix.tools.SpatialVector;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This index is used to find pixels at a given order.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public final class NestedIndex implements Index {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(NestedIndex.class.getName());
  /**
   * Typical choice of fact.
   */
  private static final int TYPICAL_CHOICE_FACT = 4;
  /**
   * Convertion Deg to Arsec.
   */
  private static final double DEG_TO_ARCSEC = 3600.0;
  /**
   * Number of points in a quadrlatere.
   */
  private static final int QUADRI = 4;
  /**
   * Shape.
   */
  private Shape shape;
  /**
   * Healpix order.
   */
  private int order;

  /**
   * Create an indexed shape.
   *
   * @param shapeVal Shape
   */
  public NestedIndex(final Shape shapeVal) {
    assert shapeVal != null;
    setShape(shapeVal);
    computeBestOrder();
  }

  /**
   * Returns the index. <p> For a Point, returns a long For a cone or a polygon, returns a RangeSet. </p>
   *
   * <p>RuntimeException if the shape is unknown.</p>
   *
   * @return the index
   */
  @Override
  public Object getIndex() {
    Object result;
    try {
      final HealpixIndex index = new HealpixIndex((int) Math.pow(2, getOrder()), Scheme.RING);
      switch (getShape().getType()) {
        case POINT:
          result = computePointIndex(index);
          break;
        case POLYGON:
          result = computePolygonIndex(index);
          break;
        case CONE:
          result = computeConeIndex(index);
          break;
        default:
          throw new RuntimeException("Shape : " + getShape() + " not found");
      }
    } catch (Exception ex) {
      Logger.getLogger(NestedIndex.class.getName()).log(Level.SEVERE, null, ex);
      throw new RuntimeException(ex);
    }
    return result;
  }

  /**
   * Computes the pixel that intersects with the point.
   *
   * @param index Healpix index
   * @return the pixel number
   * @throws Exception Healpix
   */
  protected long computePointIndex(final HealpixIndex index) throws Exception {
    final Point point = (Point) getShape();
    return index.ang2pix_nest(point.theta, point.phi);
  }

  /**
   * Computes the pixels that intersect with the cone.
   *
   * @param index Healpix index
   * @return Returns the pixel numbers
   * @throws Exception Healpix
   */
  protected RangeSet computeConeIndex(final HealpixIndex index) throws Exception {
    final Cone cone = (Cone) getShape();
    final RangeSet rangeSet =  index.queryDiscInclusive(cone.getCenter(), cone.getRadius(), TYPICAL_CHOICE_FACT);
    final long[] pixelsNested = new long[(int) rangeSet.nval()];
    final RangeSet.ValueIterator iter = rangeSet.valueIterator();
    int indice = 0;
    while (iter.hasNext()) {
        final long pixel = iter.next();
        pixelsNested[indice] = index.ring2nest(pixel);
        indice++;
    }
    return new RangeSet(pixelsNested);
  }

  /**
   * Compute the pixels that intersect with the polygon.
   *
   * @param index Healpix index
   * @return the rangeSet;
   * @throws Exception Healpix
   */
  protected RangeSet computePolygonIndex(final HealpixIndex index) throws Exception {
    RangeSet rangeSet;
    Polygon polygon = (Polygon) getShape();
    if (polygon.isConvex() && polygon.isCounterClockwised()) {
      try {
        rangeSet = computePolygon(polygon.getPoints(), index);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    } else {
      final Resampling resample = new Resampling(polygon);
      polygon = resample.processResampling();
      final List<Polygon> polygons = polygon.triangulate();
      rangeSet = new RangeSet();
      for (Polygon p : polygons) {
        final RangeSet rangeSetTmp = new RangeSet(rangeSet);
        rangeSet.setToUnion(rangeSetTmp, computePolygon(p.getPoints(), index));
      }
    }

    if (polygon.isClockwised()) {
      final RangeSet rangeFullSky = new RangeSet();
      final long nPixels = index.getNpix();
      rangeFullSky.add(1, nPixels);
      final RangeSet rangeSetTmp = new RangeSet(rangeSet);
      rangeSet.setToDifference(rangeFullSky, rangeSetTmp);
    }
    final long[] pixelsNested = new long[(int) rangeSet.nval()];
    final RangeSet.ValueIterator iter = rangeSet.valueIterator();
    int indice = 0;
    while (iter.hasNext()) {
        final long pixel = iter.next();
        pixelsNested[indice] = index.ring2nest(pixel);
        indice++;
    }
    return new RangeSet(pixelsNested);
  }

  /**
   * Computes the pixels that intersect with a clockwised polygon.
   *
   * @param points List of points of the polygon
   * @param index Healpix index
   * @return the rangeSet of pixels
   * @throws Exception Healpix
   */
  protected RangeSet computePolygon(final List<Point> points, final HealpixIndex index) throws Exception {
    RangeSet result;
    if (points.size() != 3) {
      Pointing[] pointings = new Pointing[points.size()];
      int i = 0;
      double maxTheta = 0;
      double minTheta = Math.PI * 2;
      for (Point p : points) {
        pointings[i] = p;
        i++;
        final double currentTheta = p.theta();
        if (currentTheta > maxTheta) {
          maxTheta = currentTheta;
        }
        if (currentTheta < minTheta) {
          minTheta = currentTheta;
        }
      }

      try {
        result = index.queryPolygonInclusive(pointings, TYPICAL_CHOICE_FACT);
      } catch (Exception ex) {
        result = index.queryStrip(minTheta, maxTheta, true);
      }
    } else {
      final LongRangeSet resultLong = index.query_triangle(index.getNside(), points.get(0).getAsVector(), points.get(1).getAsVector(), points.get(2).getAsVector(), 0, 1);
      final LongRangeIterator iter = resultLong.rangeIterator();
      result = new RangeSet();
      while (iter.moveToNext()) {
        final long first = iter.first();
        final long last = iter.last() + 1;
        result.add(first, last);
      }
    }
    final long[] pixelsNested = new long[(int)result.nval()];
    final RangeSet.ValueIterator iter = result.valueIterator();
    int indice = 0;
    while(iter.hasNext()) {
        final long pixel = iter.next();
        pixelsNested[indice] = index.ring2nest(pixel);
        indice++;
    }
    return new RangeSet(pixelsNested);
  }

  /**
   * Sets the Healpix order.
   *
   * @param orderHealpix Healpix resolution level
   */
  public void setOrder(final int orderHealpix) {
    this.order = orderHealpix;
  }

  /**
   * Returns the order of the Healpix processing.
   *
   * @return the Healpix resolution level
   */
  public int getOrder() {
    return order;
  }

  /**
   * Returns the shape.
   *
   * @return the shape
   */
  public Shape getShape() {
    return shape;
  }

  /**
   * Sets the shape to index.
   *
   * @param val Shape
   */
  protected void setShape(final Shape val) {
    this.shape = val;
  }

  /**
   * Computes an automatic order based on the shape area.
   *
   * @param shapeHealpix polygon
   * @throws Exception Exception
   */
  protected void computeBestOrderPolygon(final Shape shapeHealpix) throws Exception {
    final Polygon polygon = (Polygon) shapeHealpix;
    double pixRes = 0.0; // in degree
    if (polygon.getPoints().size() == QUADRI) {
      final SpatialVector sp1 = new SpatialVector(Math.toDegrees(polygon.getPoints().get(0).phi()), Math.toDegrees(Math.PI / 2 - polygon.getPoints().get(0).theta()));
      final SpatialVector sp2 = new SpatialVector(Math.toDegrees(polygon.getPoints().get(1).phi()), Math.toDegrees(Math.PI / 2 - polygon.getPoints().get(1).theta()));
      final SpatialVector sp3 = new SpatialVector(Math.toDegrees(polygon.getPoints().get(2).phi()), Math.toDegrees(Math.PI / 2 - polygon.getPoints().get(2).theta()));
      pixRes = Math.toDegrees(HealpixIndex.angDist(sp1, sp2));
      pixRes = (Math.toDegrees(HealpixIndex.angDist(sp2, sp3)) < pixRes) ? Math.toDegrees(HealpixIndex.angDist(sp2, sp3)) : pixRes;
    } else {
      SpatialVector spInit = new SpatialVector(Math.toDegrees(polygon.getPoints().get(0).phi()), Math.toDegrees(Math.PI / 2 - polygon.getPoints().get(0).theta()));
      for (int i = 1; i < polygon.getPoints().size(); i++) {
        SpatialVector sp = new SpatialVector(Math.toDegrees(polygon.getPoints().get(i).phi()), Math.toDegrees(Math.PI / 2 - polygon.getPoints().get(i).theta()));
        final double angularDist = Math.toDegrees(HealpixIndex.angDist(spInit, sp));
        if (angularDist < pixRes) {
          pixRes = angularDist;
        }
        spInit = sp;
      }
    }
    final int nside = HealpixIndex.calculateNSide(pixRes * DEG_TO_ARCSEC);
    final int orderFromShape = HealpixIndex.nside2order(nside);
    final int bestOrder = (orderFromShape > HealpixIndex.order_max) ? HealpixIndex.order_max : orderFromShape;
    setOrder(bestOrder);
  }

  /**
   * Computes an automatic order based on the shape area.
   *
   * @param shapeHealpix cone
   * @throws Exception Healpix
   */
  protected void computeBestOrderCone(final Shape shapeHealpix) throws Exception {
    final Cone cone = (Cone) shapeHealpix;
    final double radiusInArcsec = Math.toDegrees(cone.getRadius()) * 2 * DEG_TO_ARCSEC;
    final int nside = HealpixIndex.calculateNSide(radiusInArcsec);
    final int orderFromShape = HealpixIndex.nside2order(nside);
    final int bestOrder = ((orderFromShape) > HealpixIndex.order_max) ? HealpixIndex.order_max : orderFromShape;
    setOrder(bestOrder);
  }

  /**
   * Computes an automatic order based on the shape area.
   *
   * <p>RuntimeException if the shape is unknown or an error.</p>
   */
  protected void computeBestOrder() {
    try {
      switch (getShape().getType()) {
        case POINT:
          setOrder(HealpixIndex.order_max);
          break;
        case POLYGON:
          computeBestOrderPolygon(shape);
          break;
        case CONE:
          computeBestOrderCone(shape);
          break;
        default:
          throw new RuntimeException("Shape : " + getShape() + " not found");
      }
    } catch (Exception ex) {
      throw new RuntimeException("Shape : " + getShape() + " not found");
    }
  }
}
