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
package fr.cnes.sitools.extensions.astro.application.opensearch;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.engine.Engine;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

/**
 * Describes input parameters.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchDescribe extends OpenSearchBase {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(OpenSearchDescribe.class.getName());

  @Override
  public final void doInit() {
    super.doInit();
  }

  @Get
  public final Representation describeQueryParameters() {
    final JSONObject filters = new JSONObject();
    final List<Index> indexes = getIndexedFields();
    final JSONArray filterArray = new JSONArray();
    for (Index index : indexes) {
      try {
        final JSONObject filter = new JSONObject();
        filter.put("id", index.getName());
        filter.put("title", index.getName());
        if (index.isCanBeCategorized()) {
          filter.put("type", "enumeration");
          filter.put("unique", "false");
          filter.put("population", index.getPopulation());
          final JSONArray son = new JSONArray();
          filter.put("son", son);
          final Map<String, Long> topTerms = index.getTopTerms();
          for (Map.Entry<String,Long> entryTerm : topTerms.entrySet()) {
            final JSONObject termEnum = new JSONObject();
            termEnum.put("id", entryTerm.getKey());
            termEnum.put("title", entryTerm.getKey());
            termEnum.put("value", entryTerm.getKey());
            termEnum.put("population", entryTerm.getValue());
            son.put(termEnum);
          }
          filterArray.put(filter);
        } else if (!index.getTopTerms().isEmpty()) {
          filter.put("type", index.getDatatype().name().toLowerCase());
          filterArray.put(filter);
        }
      } catch (JSONException ex) {
        Engine.getLogger(OpenSearchDescribe.class.getName()).log(Level.SEVERE, null, ex);
      }
      try {
        filters.put("filters", filterArray);
      } catch (JSONException ex) {
        Engine.getLogger(OpenSearchDescribe.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    final JsonRepresentation jsonRep = new JsonRepresentation(filters);
    jsonRep.setIndenting(true);
    return jsonRep;
  }
}
