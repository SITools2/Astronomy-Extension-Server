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
package fr.cnes.sitools.extensions.astro.application.opensearch.responsibility;

/**
 * Handles in a simple way the VO request.
 *
 * <p>
 * This implementation is designed by a chain of responsability pattern.
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class AbstractVORequest {

    /**
     * The next element in the chain of responsibility.
     */
    private AbstractVORequest successor;

    /**
     * Sets the next element of the chain of responsability.
     *
     * @param successorVal the next element of the chain of responsability
     */
    public final void setNext(final AbstractVORequest successorVal) {
        this.setSuccessor(successorVal);
    }

    /**
     * Returns the response of the request.
     *
     * @return the response of the request
     */
    public abstract Object getResponse();
    /**
     * Returns the successor.
     * @return the successor
     */
    public final AbstractVORequest getSuccessor() {
        return successor;
    }
    /**
     * Sets the successor.
     *
     * @param successorVal the successor to set
     */
    public final void setSuccessor(final AbstractVORequest successorVal) {
        this.successor = successorVal;
    }
}
