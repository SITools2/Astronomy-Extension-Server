/**
 * *****************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *****************************************************************************
 */
package fr.cnes.sitools.astro.graph;

import com.jhlabs.map.MapMath;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionException;
import healpix.tools.SpatialVector;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a shape based on coordinates.
 *
 * @author Jean-Christophe Malapert
 */
public class Coordinates {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(Coordinates.class.getName());
  /**
   * List of points along X.
   */
  private List<Double> xList;
  /**
   * List of points along Y.
   */
  private List<Double> yList;


  /**
   * Constructor.
   */
  public Coordinates() {
    this.xList = new ArrayList<Double>();
    this.yList = new ArrayList<Double>();
  }

  /**
   * Add a coordinate to a shape.
   *
   * @param x x value
   * @param y y value
   */
  public final void addCoordinate(final double x, final double y) {
    this.xList.add(x);
    this.yList.add(y);
  }

  /**
   * Add a coordinate to a shape.
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
   * Get an array of x values.
   *
   * @return An array of x values
   */
  public final Double[] getXAsArray() {
    return this.xList.toArray(new Double[this.xList.size()]);
  }

  /**
   * Get an array of y values.
   *
   * @return An array of y values
   */
  public final Double[] getYAsArray() {
    return this.yList.toArray(new Double[this.yList.size()]);
  }

  /**
   * Get a coordinate for a specific index.
   *
   * @param i index
   * @return a point
   */
  public final Point2D.Double getCoordinate(final int i) {
    if (i > xList.size()) {
      throw new ArrayIndexOutOfBoundsException("The coordinate index does not exist");
    }
    return new Point2D.Double(xList.get(i), yList.get(i));
  }

  /**
   * Number of coordinates.
   *
   * @return Number of coordinates
   */
  public final int getLength() {
    return this.xList.size();
  }

  /**
   * Get the projected pixels.
   *
   * @param proj selected projection
   * @param range The highest/lowest values for x and y coordinate
   * @param pixelWidth Number of pixels along X axis
   * @param pixelHeight Number of pixels along Y axis
   * @return List of projected points
   */
  public final List<Point2D.Double> getPixelsFromProjection(final Projection proj, final double[] range, final double pixelWidth, final double pixelHeight) {
    assert (proj != null && range.length == Graph.NUMBER_VALUES_RANGE);
    List<Point2D.Double> listPixels = new ArrayList<Point2D.Double>();
    for (int i = 0; i < this.xList.size(); i++) {
      Point2D.Double point2D = new Point2D.Double();
      double ra = this.xList.get(i);
      double dec = this.yList.get(i);
      ra = (ra >= Graph.RA_MIN && ra <= Graph.RA_PI) ? -ra : Graph.RA_MAX - ra;
      try {
        if (proj.inside(MapMath.degToRad(ra), MapMath.degToRad(dec))) {
          proj.project(MapMath.degToRad(ra), MapMath.degToRad(dec), point2D);
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
   * Scale along X axis.
   *
   * @param o coordinate to scale
   * @param range range
   * @param pixelWidth number of pixels along X axis
   * @return projected point along X axis
   */
  private double scaleX(final double o, final double[] range, final double pixelWidth) {
    return (-1 * o * pixelWidth / (range[Graph.X_MAX] - range[Graph.X_MIN]) + pixelWidth / 2.0D);
  }

  /**
   * Scale along Y axis.
   *
   * @param o coordinate to scale
   * @param range range
   * @param pixelHeight number of pixels along Y axis
   * @return projected point along Y axis
   */
  private double scaleY(final double o, final double[] range, final double pixelHeight) {
    return ((range[Graph.Y_MAX] - o) * pixelHeight / (range[Graph.Y_MAX] - range[Graph.Y_MIN]));
  }

  /**
   * Returns the center of the shape.
   *
   * @return Center of the shape
   */
  public final SpatialVector getCenter() {
    Point2D.Double vec = this.getCoordinate(0);
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
  public Point2D.Double getPixelCenterFromProjection(final Projection proj, final double[] range, final double pixelWidth, final double pixelHeight) {
    assert (proj != null && range.length == Graph.NUMBER_VALUES_RANGE);
    Point2D.Double point2D = new Point2D.Double();
    SpatialVector sv = this.getCenter();
    double ra = sv.ra();
    double dec = sv.dec();
    ra = (ra >= Graph.RA_MIN && ra <= Graph.RA_PI) ? -ra : Graph.RA_MAX - ra;
    try {
      if (proj.inside(MapMath.degToRad(ra), MapMath.degToRad(dec))) {
        proj.project(MapMath.degToRad(ra), MapMath.degToRad(dec), point2D);
        point2D.x = scaleX(point2D.getX(), range, pixelWidth);
        point2D.y = scaleY(point2D.getY(), range, pixelHeight);
      }
    } catch (ProjectionException ex) {
      LOG.log(Level.SEVERE, ex.getMessage());
    }
    return point2D;
  }
}
