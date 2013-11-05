/**
 * *****************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SITools2. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.plugins.resources.model.DataSetSelectionType;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import java.util.logging.Logger;

/**
 * Provides a services to convert a VOTable to GeoSJON.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class Votable2GeoJsonResourcePlugin extends ResourceModel {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(Votable2GeoJsonResourcePlugin.class.getName());

    /**
     * Constructs the administation panel.
     */
    public Votable2GeoJsonResourcePlugin() {
        super();
        setClassAuthor("J-C Malapert");
        setClassOwner("CNES");
        setClassVersion("1.0");
        setName("VOTable2GeoJson");
        setDescription("Provides a capability to convert a VOTable to GeoJson");
        setDataSetSelection(DataSetSelectionType.NONE);
        setResourceClassName(fr.cnes.sitools.extensions.astro.resource.Votable2GeoJsonResource.class.getName());
        this.completeAttachUrlWith("/votable2geojson");
    }
}
