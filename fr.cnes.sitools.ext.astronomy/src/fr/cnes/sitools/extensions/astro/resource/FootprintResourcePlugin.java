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
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.plugins.resources.model.DataSetSelectionType;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.plugins.resources.model.ResourceParameter;
import fr.cnes.sitools.plugins.resources.model.ResourceParameterType;
import java.util.logging.Logger;

/**
 * Provides some footprint capabilities.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class FootprintResourcePlugin extends ResourceModel {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(FootprintResourcePlugin.class.getName());

    /**
     * Constructs the administration panel.
     */
    public FootprintResourcePlugin() {
        super();
        setClassAuthor("J-C Malapert");
        setClassOwner("CNES");
        setClassVersion("0.1");
        setName("Footprint Computing");
        setDescription("This service provides some footprint capabilities (area, coverage, filter)");
        setResourceClassName(fr.cnes.sitools.extensions.astro.resource.FootprintResource.class.getName());
        this.setApplicationClassName(DataSetApplication.class.getName());
        this.setDataSetSelection(DataSetSelectionType.NONE);
        this.completeAttachUrlWith("/footprint/{featureType}");
        setConfiguration();
    }

    /**
     * Sets the configuration for the administrator.
     */
    private void setConfiguration() {
        final ResourceParameter raParam = new ResourceParameter("RA", "Right ascension in decimal degree",
                ResourceParameterType.PARAMETER_INTERN);
        raParam.setValueType("xs:dataset.columnAlias");
        this.addParam(raParam);

        final ResourceParameter decParam = new ResourceParameter("DEC", "Declination in decimal degree",
                ResourceParameterType.PARAMETER_INTERN);
        decParam.setValueType("xs:dataset.columnAlias");
        this.addParam(decParam);

        final ResourceParameter dicoParam = new ResourceParameter("WCS_DICO", "World Coordinate System dictionary",
                ResourceParameterType.PARAMETER_INTERN);
        dicoParam.setValueType("xs:String");
        this.addParam(dicoParam);

        final ResourceParameter cacheDirectory = new ResourceParameter("CacheDirectory",
                "Specify a directory where MOC will be computed and cached", ResourceParameterType.PARAMETER_INTERN);
        cacheDirectory.setValueType("String");
        this.addParam(cacheDirectory);
    }
}
