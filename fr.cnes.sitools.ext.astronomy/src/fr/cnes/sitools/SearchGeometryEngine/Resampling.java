/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.SearchGeometryEngine;

import fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resamples a polygon.
 *
 * <p>The resampling computes intermediate points between points that define the polygon. The number of intermediate points is computed
 * according to the length between the points that define the polygon</p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class Resampling {

  /**
   * Polygon to resample.
   */
  private final Polygon polygon;
  /**
   * Number of points in the polygon to resample.
   */
  private final int nbPointsInPolygon;
  /**
   * The resampled polygon.
   */
  private List<Point> resultPolygon;
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(Resampling.class.getName());

  /**
   * Constructs a Resampling with polygon.
   *
   * @param polygonVal polygon to resample.
   */
  public Resampling(final Polygon polygonVal) {
    this.polygon = polygonVal;
    this.nbPointsInPolygon = polygonVal.getPoints().size();
    this.resultPolygon = new ArrayList<Point>();
  }

  /**
   * Resamples the polygon and stores the result in
   * <code>resultPolygon</code>.
   */
  private void resamplePolygon() {
    List<Point> points = this.polygon.getPoints();
    for (int i = 0; i < nbPointsInPolygon - 1; i++) {
      Point p1 = points.get(i);
      Point p2 = points.get(i + 1);
      this.resultPolygon.addAll(resampleLine(p1, p2));
    }
    this.resultPolygon.addAll(resampleLine(points.get(nbPointsInPolygon - 1), points.get(0)));
  }

  /**
   * Returns a resampled line.
   *
   * @param a first point
   * @param b last point
   * @return resampled line
   */
  protected final List<Point> resampleLine(final Point a, final Point b) {
    //double length = Math.sqrt(Math.pow(a.getLongitude() - b.getLongitude(), 2)
    //        + Math.pow(a.getLatitude() - b.getLatitude(), 2));
    double length = Math.sqrt(Math.pow(a.getAsVector().x() - b.getAsVector().x(), 2.)
            + Math.pow(a.getAsVector().y() - b.getAsVector().y(), 2.)
            + Math.pow(a.getAsVector().z() - b.getAsVector().z(), 2.)) * 60;
    //TODO : calculer lentgh selon que c'est Equatorial ou Earth pour gérer les frontières
    int nbPointsToInterpol = (int) (length / 10);
    if (nbPointsToInterpol < 1) {
      nbPointsToInterpol = 1;
    }

    LOG.log(Level.INFO, "number of intermediate points: {0}", nbPointsToInterpol);
    List<Point> result = new ArrayList<Point>(nbPointsInPolygon);
    //TODO : caluler le resampling pour EQUATORIAL
    //TODO : essayer fixclockwise
    for (int i = 0; i < nbPointsToInterpol; i++) {
      double t = (i / (double) nbPointsToInterpol);
      result.add(interpolate(a, b, t));
    }
    return result;
  }

  /**
   * Returns an interpolated line.
   *
   * @param a first point
   * @param b last point
   * @param t parameter
   * @return interpolated line
   */
  protected final Point interpolate(final Point a, final Point b, final double t) {
    double u = 1 - t;
    double longitude = (a.getLongitude() * u + toto(a.getLongitude(), b.getLongitude()) * t)%360;
    double latitude = a.getLatitude() * u + b.getLatitude() * t;
    return new Point(longitude, latitude, a.getCoordSystem());
  }

  private double toto(double startInit, double stopInit) {
    double current = startInit; // where you are now
    double desired = stopInit; // where you want to go

    while (desired < current) {
      desired += 360;
    }
    double end = desired;
    double start = desired - 360;

    double delta1 = Math.abs(end - current);
    double delta2 = Math.abs(start - current);
    double answer = delta1 < delta2 ? end : current;
    return answer;
  }
  
  /**
   * Resamples Right ascension along the hour great circle 
   * given the start and the stop of Right ascension axis.
   * 
   * <p>
   * Some examples with a step of 10
   * 340 -> 40 returns 340, 350, 0, 10, 20, 30
   * 40 -> 340 return 40, 50, ..., 330
   * </p>
   * 
   * @param startInit the starting right ascension
   * @param stopInit the stoping right ascension
   * @param stepValue resampling value
   * @return Set of right ascension value [startInit, stopInit]
   */
  public static double[] hourCircle(final double startInit, final double stopInit, final double stepValue) {    
    double current = startInit;
    double desired = (startInit > stopInit) ? stopInit + SimpleImageAccessProtocolLibrary.MAX_VALUE_FOR_RIGHT_ASCENSION : stopInit;
    double[] listPointsMax = new double[(int) (SimpleImageAccessProtocolLibrary.MAX_VALUE_FOR_RIGHT_ASCENSION / stepValue) + 1];
    int i = 0;
    while (current < desired) {
      listPointsMax[i] = (current + SimpleImageAccessProtocolLibrary.MAX_VALUE_FOR_RIGHT_ASCENSION) % SimpleImageAccessProtocolLibrary.MAX_VALUE_FOR_RIGHT_ASCENSION;
      i++;
      current += stepValue;
    }
    double[] listPoints = new double[i + 1];
    System.arraycopy(listPointsMax, 0, listPoints, 0, i);
    listPoints[i] = stopInit;
    return listPoints;
  }
  
  /**
   * Resamples declination along the declination axis 
   * given the start and the stop of declination axis
   *
   * <p>
   * Some examples with a step of 10
   * 40 -> -40 returns 40, 30, ... , -30
   * -40 -> 40 return -40, -30, ..., 30
   * </p>
   *
   * @param startInit the starting declination
   * @param stopInit the stoping dclination
   * @param stepValue resampling value
   * @return Set of declination values [startInit, stopInit]
   */
  public static double[] decCircle(final double startInit, final double stopInit, final double stepValue) {
    double current = startInit;
    int step = (int) ((stopInit - startInit) / stepValue);
    int stepSign = (int) Math.signum(step);
    int stepAbsoluteValue = Math.abs(step);
    double[] listPoints = new double[stepAbsoluteValue + 1];
    for (int i = 0; i < stepAbsoluteValue; i++) {
      listPoints[i] = current;
      current = current + stepSign * stepValue;
    }
    listPoints[stepAbsoluteValue] = stopInit;
    return listPoints;
  }

  /**
   * Process the resampling and returns the result.
   *
   * @return the resampled polygon
   */
  public final Polygon processResampling() {
    resamplePolygon();
    return new Polygon(this.resultPolygon);
  }
}
