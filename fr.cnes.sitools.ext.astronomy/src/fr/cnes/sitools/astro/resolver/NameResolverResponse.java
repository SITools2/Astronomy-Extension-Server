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
package fr.cnes.sitools.astro.resolver;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sitools.extensions.common.AstroCoordinate;

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
  private transient List<AstroCoordinate> astroCoordinates;
  /**
   * Service's exception.
   */
  private transient NameResolverException exception;
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
   * @param rightAscension right ascension
   * @param declination declination
   */
  public final void addAstroCoordinate(final double rightAscension, final double declination) {
    this.astroCoordinates.add(new AstroCoordinate(rightAscension, declination));
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
   * @param exceptionVal exception
   */
  public final void setError(final NameResolverException exceptionVal) {
    this.exception = exceptionVal;
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
    return (this.getError() == null);
  }
}
