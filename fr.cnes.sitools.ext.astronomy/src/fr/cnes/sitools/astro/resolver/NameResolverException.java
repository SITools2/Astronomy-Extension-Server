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
package fr.cnes.sitools.astro.resolver;

import java.util.logging.Logger;
import org.restlet.data.Status;

/**
 * Name Resolver Exception.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class NameResolverException extends Exception {
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(NameResolverException.class.getName());
  /**
   * Status of the Exception.
   */
  private Status status;
  /**
   * Constructs a new NameResolverException with a Status.
   */
  public NameResolverException() {
    this.status = Status.SUCCESS_OK;
  }

  /**
   * Constructs a new NameResolverException with a Status.
   * @param statusVal status of the exception
   */
  public NameResolverException(final Status statusVal) {
    this.status = statusVal;
  }

  /**
   * Constructs a new NameResolverException with a Status, message and an exception.
   * @param statusVal status of the exception
   * @param message message of the exception
   * @param thrw exception
   */
  public NameResolverException(final Status statusVal, final String message, final Throwable thrw) {
    super(message, thrw);
    this.status = statusVal;
  }

  /**
   * Constructs a new NameResolverException with a Status and a message.
   * @param statusVal status of the exception
   * @param message message of the exception
   */
  public NameResolverException(final Status statusVal, final String message) {
    super(message);
    this.status = statusVal;
  }

  /**
   * Constructs a new NameResolverException with a Status and an exception.
   * @param statusVal status of the exception
   * @param thrw exception
   */
  public NameResolverException(final Status statusVal, final Throwable thrw) {
    super(thrw);
    this.status = statusVal;
  }

  /**
   * Returns the status.
   * @return the status
   */
  public final Status getStatus() {
    return this.status;
  }

    /**
     * Sets the status.
     * @param statusVal the status to set
     */
    public final void setStatus(final Status statusVal) {
        this.status = statusVal;
    }
}
