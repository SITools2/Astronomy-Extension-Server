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
package fr.cnes.sitools.astro.graph;

import healpix.tools.SpatialVector;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.engine.Engine;

import com.jhlabs.map.MapMath;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionException;

/**
 * This class implements a shape based on coordinates.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class Coordinates {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(Coordinates.class.getName());
  /**
   * List of points along X.
   */
  private final transient List<Double> xList;
  /**
   * List of points along Y.
   */
  private final transient List<Double> yList;


  /**
   * Constructs a new Coodinates.
   */
  public Coordinates() {
    this.xList = new ArrayList<Double>();
    this.yList = new ArrayList<Double>();
  }

  /**
   * Adds a point to a shape.
   *
   * @param xVal xVal value
   * @param yVal yVal value
   */
  public final void addCoordinate(final double xVal, final double yVal) {
    this.xList.add(xVal);
    this.yList.add(yVal);
  }

  /**
   * Adds a point to a shape.
   *
   * @param coord Coordinate
   */
  public final void addCoordinate(final Coordinates coord) {
    for (int i = 0; i < coord.getLength(); i++) {
      this.xList.add(coord.getCoordinate(i).getX());
      this.yList.add(coord.getCoordinate(i).getY());
    }
  }

  /**
   * Returns an array of x values.
   *
   * @return An array of x values
   */
  public final Double[] getXAsArray() {
    return this.xList.toArray(new Double[this.xList.size()]);
  }

  /**
   * Returns an array of y values.
   *
   * @return An array of y values
   */
  public final Double[] getYAsArray() {
    return this.yList.toArray(new Double[this.yList.size()]);
  }

  /**
   * Returns a point from a shape.
   *
   * @param index index of the point in the shape
   * @return a point
   */
  public final Point2D.Double getCoordinate(final int index) {
    if (index > xList.size()) {
      throw new ArrayIndexOutOfBoundsException("The coordinate index does not exist");
    }
    return new Point2D.Double(xList.get(index), yList.get(index));
  }

  /**
   * Returns the number of points.
   *
   * @return number of points
   */
  public final int getLength() {
    return this.xList.size();
  }

  /**
   * Returns the projected pixels.
   *
   * @param proj selected projection
   * @param range The highest/lowest values for x and y coordinate
   * @param pixelWidth Number of pixels along X axis
   * @param pixelHeight Number of pixels along Y axis
   * @return list of projected points
   */
  public final List<Point2D.Double> getPixelsFromProjection(final Projection proj, final double[] range, final double pixelWidth, final double pixelHeight) {
    assert (proj != null && range.length == Graph.NUMBER_VALUES_RANGE);
    final List<Point2D.Double> listPixels = new ArrayList<Point2D.Double>();
    for (int i = 0; i < this.xList.size(); i++) {
      final Point2D.Double point2D = new Point2D.Double();
      final double rightAscension = (this.xList.get(i) >= Graph.RA_MIN && this.xList.get(i) <= Graph.RA_PI) ? -this.xList.get(i) : Graph.RA_MAX - this.xList.get(i);
      final double declination = this.yList.get(i);
      try {
        if (proj.inside(MapMath.degToRad(rightAscension), MapMath.degToRad(declination))) {
          proj.project(MapMath.degToRad(rightAscension), MapMath.degToRad(declination), point2D);
          point2D.x = scaleX(point2D.getX(), range, pixelWidth);
          point2D.y = scaleY(point2D.getY(), range, pixelHeight);
          listPixels.add(point2D);
        }
      } catch (ProjectionException ex) {
        LOG.log(Level.SEVERE, ex.getMessage());
      }
    }
    return listPixels;
  }

  /**
   * Scales along X axis.
   *
   * @param xPixel coordinate to scale
   * @param range range
   * @param pixelWidth number of pixels along X axis
   * @return projected point along X axis
   */
  private double scaleX(final double xPixel, final double[] range, final double pixelWidth) {
    return (-1 * xPixel * pixelWidth / (range[Graph.X_MAX] - range[Graph.X_MIN]) + pixelWidth / 2.0D);
  }

  /**
   * Scales along Y axis.
   *
   * @param yPixel coordinate to scale
   * @param range range
   * @param pixelHeight number of pixels along Y axis
   * @return projected point along Y axis
   */
  private double scaleY(final double yPixel, final double[] range, final double pixelHeight) {
    return ((range[Graph.Y_MAX] - yPixel) * pixelHeight / (range[Graph.Y_MAX] - range[Graph.Y_MIN]));
  }

  /**
   * Returns the center of the shape.
   *
   * @return center of the shape
   */
  public final SpatialVector getCenter() {
    final Point2D.Double vec = this.getCoordinate(0);
    SpatialVector center = new SpatialVector(vec.x, vec.y);
    for (int i = 1; i < this.xList.size(); i++) {
      center = center.add(new SpatialVector(xList.get(i), yList.get(i)));
    }
    center.normalized();
    return center;
  }

  /**
   * Returns the pixel center.
   *
   * @param proj projection
   * @param range range
   * @param pixelWidth pixel width
   * @param pixelHeight pixel height
   * @return Returns the picel center
   */
  public final Point2D.Double getPixelCenterFromProjection(final Projection proj, final double[] range, final double pixelWidth, final double pixelHeight) {
    assert (proj != null && range.length == Graph.NUMBER_VALUES_RANGE);
    final Point2D.Double point2D = new Point2D.Double();
    final SpatialVector spatialVector = this.getCenter();
    final double rightAscension = (spatialVector.ra() >= Graph.RA_MIN && spatialVector.ra() <= Graph.RA_PI) ? -spatialVector.ra() : Graph.RA_MAX - spatialVector.ra();
    final double declination = spatialVector.dec();
    try {
      if (proj.inside(MapMath.degToRad(rightAscension), MapMath.degToRad(declination))) {
        proj.project(MapMath.degToRad(rightAscension), MapMath.degToRad(declination), point2D);
        point2D.x = scaleX(point2D.getX(), range, pixelWidth);
        point2D.y = scaleY(point2D.getY(), range, pixelHeight);
      }
    } catch (ProjectionException ex) {
      LOG.log(Level.SEVERE, ex.getMessage());
    }
    return point2D;
  }
}
