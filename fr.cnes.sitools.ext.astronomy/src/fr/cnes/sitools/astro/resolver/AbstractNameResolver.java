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

/**
 * Handles in a simple way a name resolver or a set of name resolvers.
 *
 * <p>
 * The AbstractNameResolver lets you retrieve a sky position from a given object name.
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class AbstractNameResolver {
  /**
   * Time out of 10 seconds when calling each name resolver.
   */
  protected static final int SERVER_TIMEOUT = 10000;

  /**
   * The next element in the chain of responsibility.
   */
  private AbstractNameResolver successor;

  /**
   * Sets the next element of the chain of responsability.
   * @param successorVal the next element of the chain of responsability
   */
  public final void setNext(final AbstractNameResolver successorVal) {
        this.setSuccessor(successorVal);
  }

  /**
   * Returns the response of the name resolver.
   * @return the response of the name resolver
   */
  public abstract NameResolverResponse getResponse();

    /**
     * Returns the successor.
     * @return the successor
     */
    public final AbstractNameResolver getSuccessor() {
        return successor;
    }

    /**
     * Sets the successor.
     * @param successorVal the successor to set
     */
    public final void setSuccessor(final AbstractNameResolver successorVal) {
        this.successor = successorVal;
    }

}
