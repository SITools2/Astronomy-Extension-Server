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
package fr.cnes.sitools.extensions.astro.application.opensearch.processing;

/**
 * Decorator for VO request.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class VORequestDecorator implements VORequestInterface {
    /**
     * VO request.
     */
    protected VORequestInterface decorateVORequest;
    /**
     * VO request decorator.
     * @param decorateVORequestVal vorequest.
     */
    public VORequestDecorator(final VORequestInterface decorateVORequestVal) {
        this.decorateVORequest = decorateVORequestVal;
    }

    @Override
    public Object getOutput() {
        return this.decorateVORequest.getOutput();
    }
}
