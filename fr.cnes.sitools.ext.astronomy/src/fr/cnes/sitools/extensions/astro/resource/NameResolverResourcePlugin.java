/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is a part of SITools2
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program inputStream distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.plugins.resources.model.DataSetSelectionType;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.plugins.resources.model.ResourceParameter;
import fr.cnes.sitools.plugins.resources.model.ResourceParameterType;
import java.util.logging.Logger;

/**
 * A name resolver service provides object coordinates from its name.
 *
 * <p> The parameters are coordinates system and object name for deep sky objects and stars. The parameters are coordinates system, solar
 * object name and time for solar objects </p>
 *
 * <p>This service answers to the following scenario:<br/> As user, I want to set the name and the service returns its coordinates in order
 * to integrate this service in a tool. Moreover several output can be supported. This means it is possible to get several coordinates for
 * on single name. 
 * <br/>
 * <img src="../../../../../../images/NameResolver-usecase.png"/>
 * <br/>
 * </p>
 * <p>
 * In addition, the service has some dependancies with external services
 * <br/>
 * <img src="../../../../../../images/NameResolverResourcePlugin.png"/>
 * <br/>
 * </p>
 *
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml NameResolver-usecase.png
 * title Name Resolver
 * User --> (Name Resolver) : requests
 * Admin --> (Name Resolver) : adds and configures the Name Resolver.
 * (Name Resolver) .. (project) : uses
 * @enduml
 * @startuml
 * package "Services" {
 *  HTTP - [NameResolverResourcePlugin]
 *  [Cache]
 * }
 * cloud {
 * [CDS]
 * [IMCCE]
 * [IAS]
 * }
 * package "Project/Dataset" {
 *  HTTP - [Project/Dataset]
 * }
 * [NameResolverResourcePlugin] --> [Project/Dataset] : "attached to"
 * [NameResolverResourcePlugin] --> [CDS] : "uses"
 * [NameResolverResourcePlugin] --> [IMCCE] : "uses"
 * [NameResolverResourcePlugin] --> [IAS] : "uses"
 * [NameResolverResourcePlugin] .. [Cache]
 * @enduml
 */
public class NameResolverResourcePlugin extends ResourceModel {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(NameResolverResourcePlugin.class.getName());

  /**
   * Constructor of the administration panel.
   */
  public NameResolverResourcePlugin() {
    super();
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    setName("Name Resolver service");
    setDescription("This service provides a resource (/nameResolver/{objectName}/{coordSystem})"
            + " to query name resolver services. coordSystem variable is either GALACTIC or EQUATORIAL");
    setDataSetSelection(DataSetSelectionType.NONE);
    setResourceClassName(fr.cnes.sitools.extensions.astro.resource.NameResolverResource.class.getName());
    setConfiguration();
    this.completeAttachUrlWith("/nameResolver/{objectName}/{coordSystem}");    
  }
  
  /**
   * Sets the configuration for the administrator.
   */
  private void setConfiguration() {
    final ResourceParameter xsEnumEditable = new ResourceParameter("nameResolver",
            "Select your resolver name service for avoiding to add resolverName as URL parameter",
            ResourceParameterType.PARAMETER_USER_INPUT);
    xsEnumEditable.setValue("CDS"); // default value
    xsEnumEditable.setValueType("xs:enum-multiple[IMCCE,CDS,IAS,SITools2,ALL]");
    this.addParam(xsEnumEditable);
    final ResourceParameter epoch = new ResourceParameter("epoch", "Set an epoch for avoiding to add epoch as URL parameter",
            ResourceParameterType.PARAMETER_USER_INPUT);
    epoch.setValueType("String");
    epoch.setValue("now");
    this.addParam(epoch);      
  }  
}
