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
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.engine.Engine;

import cds.moc.HealpixMoc;
import cds.moc.MocCell;

/**
 * Nested Index for hierarchical resolution.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class MocIndex implements Index {

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
  protected static final int TYPICAL_CHOICE_FACT = 128;
  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(MocIndex.class.getName());

  /**
   * Cronstructs a new Index based with a shape.
   *
   * @param geometryShape shape for which the Healpix index must be computed.
   */
  public MocIndex(final Shape geometryShape) {
    this.shape = geometryShape;
  }

  /**
   * Empty constructor.
   */
  protected MocIndex() {
  }

  /**
   * Returns the Healpix index of the shape. <p> A long is returned when the geometry is a point. A HealpixMoc is returned when the geometry
   * is a polygon or a cone. </p>
   *
   * @return the index
   */
  @Override
  public final Object getIndex() {
    try {
      Object result = null;
      final HealpixIndex index = new HealpixIndex((int) Math.pow(2, getOrderMax()), Scheme.NESTED);
      switch (getShape().getType()) {
        case POINT:
          result = computePointIndex(index);
          break;
        case POLYGON:
          result = computePolygonIndex(index);
          break;
        case CONE:
          result = computePolygonIndex(index);
          break;
        default:
          throw new RuntimeException("Shape : " + getShape() + " not found");
      }
      return result;
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new RuntimeException("Cannot compute the index.");
    }
  }

  /**
   * Computes the pixel related to the point.
   *
   * @param index Healpix index
   * @return the pixel number
   * @throws Exception Healpix Exception
   */
  protected final long computePointIndex(final HealpixIndex index) throws Exception {
    return computePointIndex(index, (Point) getShape());
  }

  /**
   * Computes the pixel related to the point.
   *
   * @param index Healpix index
   * @param shape point
   * @return the pixel number
   * @throws Exception Healpix Exception
   */
  protected static long computePointIndex(final HealpixIndex index, final Shape shape) throws Exception {
    Point point = (Point) shape;
    return index.ang2pix(point);
  }

  /**
   * Returns a hierarchical index of a cone.
   *
   * @param index Healpix index
   * @return the pixel numbers as a hierarchical index
   * @throws Exception Healpix Exception
   */
  protected final HealpixMoc computeConeIndex(final HealpixIndex index) throws Exception {
    return computeConeIndex(index, (Cone) getShape());
  }

  /**
   * Returns a hierarchical index of a cone.
   *
   * @param index Healpix index
   * @param shape cone
   * @return the pixel numbers as a hierarchical index
   * @throws Exception Healpix Exception
   */
  protected static HealpixMoc computeConeIndex(final HealpixIndex index, final Shape shape) throws Exception {
    final HealpixMoc moc = new HealpixMoc();
    final Cone cone = (Cone) shape;
    final RangeSet rangeSet = index.queryDiscInclusive(cone.getCenter(), cone.getRadius(), TYPICAL_CHOICE_FACT);
    final RangeSet.ValueIterator valueIter = rangeSet.valueIterator();
    while (valueIter.hasNext()) {
      final long pixNest = valueIter.next();
      moc.add(new MocCell(index.getOrder(), pixNest));
    }
    return moc;
  }

  /**
   * Returns a hierarchical index of a polygon.
   *
   * <p> When the polygon is clockwise, the index is computed on this polygon.<br/> When the polygon is counterclockwised, the index is
   * computed on the complement of this polygon </p>
   *
   * @param index Healpix index
   * @return the HealpixMoc;
   * @throws Exception Healpix Exception
   */
  protected final HealpixMoc computePolygonIndex(final HealpixIndex index) throws Exception {
    return computePolygonIndex(index, (Polygon) getShape());
  }

  /**
   * Returns a hierarchical index of a polygon.
   *
   * <p> When the polygon is clockwise, the index is computed on this polygon.<br/> When the polygon is counterclockwised, the index is
   * computed on the complement of this polygon </p>
   *
   * @param index Healpix index
   * @param shape polygon
   * @return the HealpixMoc;
   * @throws Exception Healpix Exception
   */
  protected static HealpixMoc computePolygonIndex(final HealpixIndex index, final Shape shape) throws Exception {
    RangeSet rangeSet;
    HealpixMoc moc = new HealpixMoc();
    Polygon polygon = (Polygon) shape;

    final Resampling resample = new Resampling(polygon);
    polygon = resample.processResampling();
    final List<Polygon> polygons = polygon.triangulate();
    rangeSet = new RangeSet();
    for (Polygon p : polygons) {
      final RangeSet rangeSetTmp = new RangeSet(rangeSet);
      rangeSet.setToUnion(rangeSetTmp, computePolygon(p.getPoints(), index));
    }

    final RangeSet.ValueIterator valueIter = rangeSet.valueIterator();
    while (valueIter.hasNext()) {
      moc.add(new MocCell(index.getOrder(), valueIter.next()));
    }
    if (polygon.isClockwised()) {
      moc = moc.complement();
    }
    return moc;
  }

  /**
   * Returns a range of Healpix pixels from the a list of points.
   *
   * @param points List of points of the polygon
   * @param index Healpix index
   * @return the rangeSet of pixels
   * @throws Exception Helapix Exception
   */
  protected static RangeSet computePolygon(final List<Point> points, final HealpixIndex index) throws Exception {
    RangeSet result;
    final Pointing[] pointings = new Pointing[points.size()];
    int i = 0;
    for (Point p : points) {
      pointings[i] = p;
      i++;
    }
    result = index.queryPolygonInclusive(pointings, TYPICAL_CHOICE_FACT);
    return result;
  }

  /**
   * Returns the shape.
   *
   * @return the shape
   */
  public final Shape getShape() {
    return shape;
  }

  /**
   * Sets the shape.
   *
   * @param val shape
   */
  public final void setShape(final Shape val) {
    this.shape = val;
  }

  /**
   * Returns the Max order.
   *
   * @return max order
   */
  public final int getOrderMax() {
    return this.orderMax;
  }

  /**
   * Returns the min order.
   *
   * @return min order.
   */
  public final int getOrderMin() {
    return this.orderMin;
  }

  /**
   * Sets the max order.
   *
   * @param val max order
   */
  public final void setOrderMax(final int val) {
    this.orderMax = val;
  }

  /**
   * Sets the min order.
   *
   * @param val mion order
   */
  public final void setOrderMin(final int val) {
    this.orderMin = val;
  }
}
