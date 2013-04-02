/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import java.util.logging.Logger;

/**
 * Configures the Solar object service.
 *
 * <p> This service queries the Skybot service from IMCCE with Healpix (Healpix number + order) and a given time. The response is a list of solar objects matching the query
 * parameters. </p>
 * 
 * <p>This service answers to the following scenario:<br/>
 * As user, I want to know if one of my image can contain a system solar object 
 * at the acquisition time of my image in order to detect or analyse system solar objects.
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConeSearchSolarObjectResourcePlugin extends ResourceModel {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ConeSearchSolarObjectResourcePlugin.class.getName());

  /**
   * Constructs the administration panel.
   */
  public ConeSearchSolarObjectResourcePlugin() {
    super();
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    setName("Solar object service");
    setDescription("This service provides access to solar objects from IMCCE");
    setDataSetSelection(DataSetSelectionType.NONE);
    setResourceClassName(fr.cnes.sitools.extensions.astro.resource.ConeSearchSolarObjectResource.class.getName());
    this.completeAttachUrlWith("/solarObjects");
  }
}
