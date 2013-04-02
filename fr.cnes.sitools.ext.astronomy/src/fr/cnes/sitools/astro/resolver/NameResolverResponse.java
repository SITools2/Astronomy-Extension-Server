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
package fr.cnes.sitools.astro.resolver;

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import healpix.core.AngularPosition;
import healpix.tools.CoordTransform;
import java.util.ArrayList;
import java.util.List;
import org.restlet.data.Status;

/**
 * Provides the response of the server.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class NameResolverResponse {
  /**
   * Laboratory credits.
   */
  private String credits;
  /**
   * List of astroCoordinates for an object name. Two different objects can have the same name.
   * That's why we could have several AstroCoordinate for a given object name
   */
  private List<AstroCoordinate> astroCoordinates;
  /**
   * Service's exception.
   */
  private NameResolverException exception;
  
  /**
   * Constructs a new NameResolverRsponse with credits.
   * @param creditsVal Laboratory credits
   */
  public NameResolverResponse(final String creditsVal) {
    this.credits = creditsVal;
    this.astroCoordinates = new ArrayList<AstroCoordinate>();
  }
  
  /**
   * Constructs a new NameResolverRsponse.
   */
  public NameResolverResponse() {
    this(null);
  }
  
  /**
   * Sets the laboratory credits.
   * @param creditsVal the laboratory credits
   */
  public final void setCredits(final String creditsVal) {
    this.credits = creditsVal;
  }
  
  /**
   * Returns the laboratory credits.
   * @return the laboratory credtis
   */
  public final String getCredits() {
    return this.credits;
  }

  /**
   * Adds a list of AstroCoordinate to the current list.
   * @param astroCoordinatesVal list of AstroCoordinate
   */
  public final void addAstoCoordinates(final List<AstroCoordinate> astroCoordinatesVal) {
    this.astroCoordinates.addAll(astroCoordinatesVal);
  }
  
  /**
   * Adds a AstroCoordinate to the current list.
   * @param astroCoordinateVal AstroCoordinate
   */
  public final void addAstoCoordinate(final AstroCoordinate astroCoordinateVal) {
    this.astroCoordinates.add(astroCoordinateVal);
  }
  
  /**
   * Adds a AstroCoordinate with the right ascension and declination.
   * @param ra right ascension
   * @param dec declination
   */
  public final void addAstroCoordinate(final double ra, final double dec) {
    this.astroCoordinates.add(new AstroCoordinate(ra, dec));
  }
  
  /**
   * Return the list of coordinates.
   * @return the list of coordinates
   */
  public final List<AstroCoordinate> getAstroCoordinates() {
    return this.astroCoordinates;
  }
  
  /**
   * Sets the error.
   * @param ex exception
   */
  public final void setError(final NameResolverException ex) {
    this.exception = ex;
  }
  
  /**
   * Returns the error.
   * @return the error
   */
  public final NameResolverException getError() {
    return this.exception;
  }

  /**
   * Returns <code>true</code> if this object has result other wise <code>false</code>.
   * @return <code>true</code> if this object has result other wise <code>false</code>
   */
  public final boolean hasResult() {
    return (this.getError() == null) ? true : false;
  }


  /**
   * Converts the sky position of an object from a Equatorial reference frame to galactic frame when neeeded.
   *
   * <p> if the coordinates of
   * <code>astroCoord</code> are not in Equatorial frame, then a IllegalArgumentException is thrown. </p>
   *
   * @param astroCoord Sky object position in Equatorial reference frame
   * @param coordinateSystem final reference frame
   * @throws NameResolverException if the transformation Equatorial to galactic failed
   */
  private void processTransformation(final AstroCoordinate astroCoord, final AstroCoordinate.CoordinateSystem coordinateSystem) throws NameResolverException {
    assert astroCoord != null;
    assert coordinateSystem != null;
    if (!AstroCoordinate.CoordinateSystem.EQUATORIAL.equals(astroCoord.getCoordinateSystem())) {
      throw new IllegalArgumentException("astroCoord parameter cannot be null and must be in equatorial reference frame");
    }
    switch (coordinateSystem) {
      case EQUATORIAL:
        break;
      case GALACTIC:
        AngularPosition angularPosition = new AngularPosition(astroCoord.getDecAsDecimal(), astroCoord.getRaAsDecimal());
        try {
          angularPosition = CoordTransform.transformInDeg(angularPosition, CoordTransform.EQ2GAL);
        } catch (Exception ex) {
          throw new NameResolverException(Status.SERVER_ERROR_INTERNAL, ex);
        }
        astroCoord.setRaAsDecimal(angularPosition.phi());
        astroCoord.setDecAsDecimal(angularPosition.theta());
        break;
      default:
        throw new NameResolverException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "The coordinate system " + coordinateSystem.name() + " is not supported");
    }
  }
}
