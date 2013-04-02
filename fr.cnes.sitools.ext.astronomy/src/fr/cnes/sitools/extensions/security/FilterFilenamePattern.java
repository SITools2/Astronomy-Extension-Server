/***********************************************************************
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
 *************************************************************************/
package fr.cnes.sitools.extensions.security;

import fr.cnes.sitools.plugins.filters.model.FilterModel;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Authorizer;

/**
 * Authorizes to access to the file in a datastorage when the url matches the pattern of the file.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class FilterFilenamePattern extends Authorizer {
  
  /**
   * configuration parameters.
   */
  private FilterModel filterModel;
  
  /**
   * Constructor.
   * @param context context
   */
  public FilterFilenamePattern(final Context context) {       
    this.filterModel = (FilterModel) context.getAttributes().get("FILTER_MODEL");
  }

  @Override
  public final boolean authorize(final Request request, final Response response) {
    String urlStr = request.getResourceRef().getIdentifier(true);
    String pattern = this.filterModel.getParameterByName("pattern").getValue();
    return urlStr.matches(pattern);
  }
}