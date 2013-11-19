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
 * Plugin for the reverse name resolver service from CDS.
 *
 * <p>
 * Finds the object's coordinate based on the object's name.</p>
 *
 * <p>
 * This service answers to the following scenario:<br/>
 * Based on a click on a map, I want to find the name of the object that
 * corresponds to my click.
 * <br/>
 * <img src="../../../../../../images/reverseNameResolver-usecase.png"/>
 * <br/>
 * </p>
 * <p>
 * In addition, the service has some dependancies with an external service
 * <br/>
 * <img src="../../../../../../images/ReverseNameResolverResourcePlugin.png"/>
 * <br/>
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml reverseNameResolver-usecase.png title Reverse Name Resolver User
 * --> (Reverse Name Resolver) : requests Admin --> (Reverse Name Resolver) :
 * adds and configures the Reverse Name Resolver. (Reverse Name Resolver) ..
 * (project) : uses
 * @enduml
 * @startuml package "Services" { HTTP - [ReverseNameResolverResourcePlugin] }
 * cloud { [CDS] } package "Project/Dataset" { HTTP - [Project/Dataset] }
 * [ReverseNameResolverResourcePlugin] --> [Project/Dataset] : "attached to"
 * [ReverseNameResolverResourcePlugin] --> [CDS] : "uses"
 * @enduml
 *
 */
public class ReverseNameResolverResourcePlugin extends ResourceModel {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(ReverseNameResolverResourcePlugin.class.getName());

    /**
     * Cronstructs the administration panel.
     */
    public ReverseNameResolverResourcePlugin() {
        super();
        setClassAuthor("J-C Malapert");
        setClassOwner("CNES");
        setClassVersion("1.1");
        setName("Reverse Name Resolver service");
        setDescription("This service provides a resource (/reverseNameResolver/{coordSystem}/{coordinates-order}) to reverse the query name resolver service.");
        setDataSetSelection(DataSetSelectionType.NONE);
        setResourceClassName(fr.cnes.sitools.extensions.astro.resource.ReverseNameResolverResource.class.getName());
        this.completeAttachUrlWith("/reverseNameResolver/{coordSystem}/{coordinates-order}");
    }
}
