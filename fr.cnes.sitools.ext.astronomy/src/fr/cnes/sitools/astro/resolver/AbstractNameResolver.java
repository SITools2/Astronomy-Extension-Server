/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import healpix.core.AngularPosition;
import healpix.tools.CoordTransform;
import java.util.List;
import org.restlet.data.Status;

/**
 * This object contains methods to handle in a simple way a name resolver.<br/>
 * An AbstractNameResolver allows you to get a sky position given an object name
 * 
 * @author Jean-Christophe Malapert 
 */
public abstract class AbstractNameResolver {

    /**
     * Coordinate system
     */
    public enum CoordinateSystem {

        /**
         * Galactic coordinates
         */
        GALACTIC,
        /**
         * Equatorial coordinates
         */
        EQUATORIAL
    }

    /**
     * Process coordinates EQUATORIAL <--> GALACTIC
     * @param astroCoord Object position
     * @param coordinateSystem Choice of the coordinate system
     * @throws NameResolverException when the transformation Equatorial to galactic failed
     */
    public void processTransformation(AstroCoordinate astroCoord, CoordinateSystem coordinateSystem) throws NameResolverException {
        // transform in the right coordinate system if needed
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
        }
    }

    /**
     * Get coordinates. The result can contain from one to several positions. 
     * For instance "IO" can be an asteroid or a natural satellite. In this case, 
     * 2 positions will be returned.
     * @param coordinateSystem Coordinate system
     * @return Positions on the sky
     * @throws NameResolverException 
     */
    public abstract List<AstroCoordinate> getCoordinates(CoordinateSystem coordinateSystem) throws NameResolverException;

    /**
     * Get the whole response of the target server
     * @return the response
     */
    public abstract Object getCompleteResponse();

    /**
     * Get credits of the server
     * @return credits
     */
    public abstract String getCreditsName();
}
