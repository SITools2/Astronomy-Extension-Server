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
package fr.cnes.sitools.extensions.security;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Authorizer;

import fr.cnes.sitools.plugins.filters.model.FilterModel;

/**
 * Authorizes the file access to a datastorage when the url matches the pattern of the file.
 *
 * <p>
 * Business class implementing the FilterFilenamePattern plugin.
 * </p>
 *
 * <br/>
 * <img src="../../../../../images/FilterFilenamePattern.png"/>
 * <br/>
 * @see FilterFilenamePatternPlugin The plugin that calls this class.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml
 * FilterFilenamePatternPlugin o-- FilterFilenamePattern : attachs
 *
 * FilterFilenamePattern : boolean authorize(final Request request, final Response response)
 *
 * FilterFilenamePatternPlugin : setConfigurationParameters()
 * FilterFilenamePatternPlugin : Validator<FilterModel> getValidator()
 * @enduml
 */
public class FilterFilenamePattern extends Authorizer {

  /**
   * Application data model.
   */
  private final transient FilterModel filterModel;
  /**
   * Constructor.
   * @param context context
   */
  public FilterFilenamePattern(final Context context) {
    this.filterModel = (FilterModel) context.getAttributes().get("FILTER_MODEL");
  }

  @Override
  public final boolean authorize(final Request request, final Response response) {
    final String urlStr = request.getResourceRef().getIdentifier(true);
    final String pattern = this.filterModel.getParameterByName(FilterFilenamePatternPlugin.PATTERN_KEYWORD).getValue();
    return urlStr.matches(pattern);
  }
}
