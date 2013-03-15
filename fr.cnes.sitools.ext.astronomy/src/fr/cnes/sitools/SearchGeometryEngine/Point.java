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
package fr.cnes.sitools.SearchGeometryEngine;

import healpix.core.AngularPosition;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This object contains methods to create a point on the sphere in EQUATORIAL or GEOCENTRIC frame.
 *
 * <p> To create a point in the Geocentric frame:<br/>
 * <pre>
 * <code>
 * Point p = new Point(-20, 20, CoordSystem.GEOCENTRIC);
 * </code>
 * </pre> </p>
 *
 * <p> To create a point in the EQUATORIAL frame:<br/>
 * <pre>
 * <code>
 * Point p = new Point(20, 20, CoordSystem.EQUATORIAL);
 * </code>
 * </pre> </p>
 *
 * @author Jean-Christophe Malapert
 * @see CoordSystem
 */
public class Point extends AngularPosition implements Shape {

  /**
   * Geometry type.
   */
  private static final String TYPE = "POINT";
  /**
   * Default value.
   */
  private static final double DEFAULT_VALUE = 500;
  /**
   * longitude = DEFAULT_VALUE.
   */
  private double longitude = DEFAULT_VALUE;
  /**
   * latitude = DEFAULT_VALUE.
   */
  private double latitude = DEFAULT_VALUE;
  /**
   * Coord system.
   */
  private CoordSystem coordSystem = null;
  /**
   * Maximum value in longitude.
   */
  private static final double MAX_LONG = 180.;
  /**
   * Minimum value in longitude.
   */
  private static final double MIN_LONG = -180.;
  /**
   * Maximum value in latitude.
   */
  private static final double MAX_LAT = 90.;
  /**
   * Minimum value in declination.
   */
  private static final double MIN_LAT = -90.;
    /**
   * Maximum value in right ascension.
   */
  private static final double MAX_RA = 360.;
  /**
   * Minimum value in right ascension.
   */
  private static final double MIN_RA = 0.;
  /**
   * Maximum value in declination.
   */
  private static final double MAX_DEC = 90.;
  /**
   * Minimum value in declination.
   */
  private static final double MIN_DEC = -90.;
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(Point.class.getName());

  /**
   * A point is build by two values : the longitude and the latitude in the reference frame.
   *
   * <p> In the Geocentric frame, longitude is counted from [-180 à 180] and latitude from [-90 to 90]. In the Equatorial frame, longitude
   * is called Right ascension counted from [0 360] while latitude is called declination, counted from [-90 90] </p>
   *
   * @param longitudePoint longitude or right ascension in decimal degrees
   * @param latitudePoint latitude or declination in decimal degrees
   * @param coordSystemPoint reference frame
   */
  public Point(final double longitudePoint, final double latitudePoint, final CoordSystem coordSystemPoint) {
    this();
    LOG.log(Level.FINEST, "Point({0},{1}) in {2} frame", new Object[]{longitudePoint, latitudePoint, coordSystemPoint.name()});
    setPoint(longitudePoint, latitudePoint, coordSystemPoint);
  }

  /**
   * Empty constructor.
   */
  protected Point() {
    super();
  }

  /**
   * Get the latitude or the declination.
   *
   * @return the latitude or the declination
   */
  public final double getLatitude() {
    return latitude;
  }

  /**
   * Get the longitude or the right ascension.
   *
   * @return the longitude or the right ascension
   */
  public final double getLongitude() {
    return longitude;
  }

  /**
   * Get the reference frame.
   *
   * @return the reference frame
   */
  public final CoordSystem getCoordSystem() {
    return coordSystem;
  }

  /**
   * This shape is not surface.
   *
   * @return false
   */
  @Override
  public final boolean isSurface() {
    return false;
  }

  /**
   * Inputs validation.
   *
   * @param longitudeVal longitude
   * @param latitudeVal latitude
   */
  protected final void checkCoordinatesInGeocentric(final double longitudeVal, final double latitudeVal) {
    StringBuilder errorMessage = new StringBuilder();
    if (longitudeVal > MAX_LONG || longitudeVal < MIN_LONG) {
      String message = String.format("long=%s - longitude must be included in the following range [-180 180]\n", longitudeVal);
      errorMessage = errorMessage.append(message);
    }
    if (latitudeVal > MAX_LAT || latitudeVal < MIN_LAT) {
      String message = String.format("lat=%s - latitude must be included in the following range [-90 90]\n", latitudeVal);      
      errorMessage = errorMessage.append(message);
    }
    if (!errorMessage.toString().isEmpty()) {
      throw new IllegalArgumentException(errorMessage.toString());
    }
  }

  /**
   * Inputs validation.
   *
   * @param longitudeVal right ascension in decimal degrees
   * @param latitudeVal declination in decimal degrees
   */
  protected final void checkCoordinatesInEquatorial(final double longitudeVal, final double latitudeVal) {
    StringBuilder errorMessage = new StringBuilder();
    if (longitudeVal > MAX_RA || longitudeVal < MIN_RA) {
      String message = String.format("RA=%s - right ascension must be included in the following range [0 360]\n", longitudeVal);
      errorMessage = errorMessage.append(message);
    }
    if (latitudeVal > MAX_DEC || latitudeVal < MIN_DEC) {
      String message = String.format("DEC=%s - declination must be included in the following range [-90 90]\n", latitudeVal);
      errorMessage = errorMessage.append(message);
    }
    if (!errorMessage.toString().isEmpty()) {
      throw new IllegalArgumentException(errorMessage.toString());
    }
  }

  /**
   * Sets the point in a specific reference frame.
   *
   * @param longitudeVal longitude or right ascension in decimal degrees
   * @param latitudeVal latitude or declination in decimal degrees
   * @param coordSystemVal the reference frame
   */
  protected final void setPoint(final double longitudeVal, final double latitudeVal, final CoordSystem coordSystemVal) {
    if (coordSystemVal == null) {
      throw new IllegalArgumentException("Reference frame cannot be null");
    }

    if (coordSystemVal.equals(CoordSystem.EQUATORIAL)) {
      checkCoordinatesInEquatorial(longitudeVal, latitudeVal);
      setTheta(Math.toRadians(CoordSystem.convertDecToTheta(latitudeVal)));
      setPhi(Math.toRadians(CoordSystem.convertRaToPhi(longitudeVal)));
    } else if (coordSystemVal.equals(CoordSystem.GEOCENTRIC)) {
      checkCoordinatesInGeocentric(longitudeVal, latitudeVal);
      setTheta(Math.toRadians(CoordSystem.convertLatitudeGeoToTheta(latitudeVal)));
      setPhi(Math.toRadians(CoordSystem.convertLongitudeGeoToPhi(longitudeVal)));
    } else {
      throw new IllegalArgumentException("Referential is unknown");
    }
    setLongitude(longitudeVal);
    setLatitude(latitudeVal);
    setcoordSystem(coordSystemVal);
  }

  /**
   * Sets the longitude or right ascension.
   *
   * @param val longitude or right ascension in decimal degrees
   */
  private void setLongitude(final double val) {
    this.longitude = val;
  }

  /**
   * Sets the latitude or declination.
   *
   * @param val latitude or declination in decimal degrees
   */
  private void setLatitude(final double val) {
    this.latitude = val;
  }

  /**
   * Sets the reference frame.
   *
   * @param val reference frame
   */
  private void setcoordSystem(final CoordSystem val) {
    this.coordSystem = val;
  }

  /**
   * Returns the type.
   *
   * @return the type
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

    if (!(obj instanceof Point)) {
      return false;
    }

    //cast to native object is now safe
    Point that = (Point) obj;

    //now a proper field-by-field evaluation can be made
    return that.getLatitude() == this.latitude
            && that.getLongitude() == this.longitude
            && that.coordSystem.equals(this.coordSystem);
  }

  @Override
  public final int hashCode() {
    int hash = 3;
    hash = 73 * hash + (int) (Double.doubleToLongBits(this.longitude) ^ (Double.doubleToLongBits(this.longitude) >>> 32));
    hash = 73 * hash + (int) (Double.doubleToLongBits(this.latitude) ^ (Double.doubleToLongBits(this.latitude) >>> 32));
    hash = 73 * hash + (this.coordSystem != null ? this.coordSystem.hashCode() : 0);
    return hash;
  }
}
