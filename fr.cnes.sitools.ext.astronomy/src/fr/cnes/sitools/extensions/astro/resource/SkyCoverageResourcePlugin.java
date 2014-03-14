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

import java.util.logging.Logger;

import org.restlet.engine.Engine;

import fr.cnes.sitools.plugins.resources.model.DataSetSelectionType;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;

/**
 * Configures the coverage service by the use of Healpix MOC.
 *
 * <p>
 * This service processes a sky coverage by given Healpix MOCs as input
 * parameters. One input parameter is described by a URL. The output is a sky
 * coverage.
 * </p>
 *
 * <p>
 * Model of the coverage service. This model provides:
 * <ul>
 * <li>metadata that is displayed in the administration panel</li>
 * <li>the business class that processes the coverage</li>
 * <li>the DatasetSelection to None. This means this service is not displayed as
 * a plugin of a dataset.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This service answers to the following scenario:<br/>
 * As user, I want to know the sky coverage of my survey and its representation
 * in the sky in order to give me an idea where data are located.
 * <br/>
 * <img src="../../../../../../images/SkyCoverageResourcePlugin.png"/>
 * <br/>
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml (*) --> "Parses moc parameter" if "moc parameter exists and has
 * value" then -->[true] "downloads MOC" as download else -->[false] "Returns
 * Server Internal Error" -->(*) endif download --> if "download is success"
 * then -->[true] "computes MOC coverage" --> "Returns the representation
 * according to the mediaType" -->(*) else -->[false] "Returns Server Internal
 * Error" -->(*) endif
 * @enduml
 */
public class SkyCoverageResourcePlugin extends ResourceModel {

    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(SkyCoverageResourcePlugin.class.getName());

    /**
     * Name of the service's input parameter to get all MOCs URL.
     */
    public static final String INPUT_PARAMETER = "moc";

    /**
     * Constructs a coverage service.
     */
    public SkyCoverageResourcePlugin() {
        super();
        setClassAuthor("J-C Malapert");
        setClassOwner("CNES");
        setClassVersion("1.0");
        setName("Coverage service");
        setDescription("Computes the coverage based on Healpix MOC.");

        //we set to NONE because this is a web service for Virtual Observatory
        // and we do not want to see it in the web user interface
        this.setDataSetSelection(DataSetSelectionType.NONE);
        setResourceClassName(fr.cnes.sitools.extensions.astro.resource.SkyCoverageResource.class.getName());

        this.completeAttachUrlWith("/coverage");
    }
}
