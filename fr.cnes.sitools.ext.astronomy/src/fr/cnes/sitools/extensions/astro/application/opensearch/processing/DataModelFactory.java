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
package fr.cnes.sitools.extensions.astro.application.opensearch.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.ivoa.xml.votable.v1.Field;

import org.restlet.engine.Engine;

import fr.cnes.sitools.extensions.astro.application.opensearch.responsibility.ConeSearchHealpix;
import fr.cnes.sitools.extensions.astro.application.opensearch.responsibility.SiaHealpix;
import fr.cnes.sitools.extensions.common.AstroCoordinate;

/**
 * Factory to create the data model from SIAP or CSP.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class DataModelFactory {

    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(DataModelFactory.class.getName());

    /**
     * Returns the right implementation of the data model.
     * @param response server response
     * @param coordinateSystem coordinate system
     * @return the right implementation of the data model
     */
    public static final AbstractJsonDataModel jsonProcessor(final List<Map<Field, String>> response, final AstroCoordinate.CoordinateSystem coordinateSystem) {

        final List<String> ucds = new ArrayList<String>();
        if (response.isEmpty()) {
            return new JsonDataModelSIA(response, coordinateSystem);
        } else {
            final Set<Field> fields = response.get(0).keySet();
            for (Field field : fields) {
                ucds.add(field.getUcd());
            }
            if (SiaHealpix.ReservedWords.requiredConceptsIsContainedIn(ucds)) {
                return new JsonDataModelSIA(response, coordinateSystem);
            } else if (ConeSearchHealpix.ReservedWords.requiredConceptsIsContainedIn(ucds)) {
                return new JsonDataModelCs(response, coordinateSystem);
            } else {
                throw new IllegalArgumentException("Unknown response");
            }
        }
    }
}
