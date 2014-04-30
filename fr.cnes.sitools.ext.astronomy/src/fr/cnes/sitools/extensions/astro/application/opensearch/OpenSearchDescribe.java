/**
 * *****************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * ****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.application.opensearch;

import fr.cnes.sitools.extensions.common.Utility;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.data.MediaType;

import org.restlet.engine.Engine;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
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
        final JsonNode filters = Utility.mapper.createObjectNode();
        final List<Index> indexes = getIndexedFields();
        final ArrayNode filterArray = Utility.mapper.createArrayNode();
        for (Index index : indexes) {
            final JsonNode filter = Utility.mapper.createObjectNode();
            ((ObjectNode) filter).put("id", index.getName());
            ((ObjectNode) filter).put("title", index.getName());
            if (index.isCanBeCategorized()) {
                ((ObjectNode) filter).put("type", "enumeration");
                ((ObjectNode) filter).put("unique", "false");
                ((ObjectNode) filter).put("population", index.getPopulation());
                final ArrayNode son = Utility.mapper.createArrayNode();
                ((ObjectNode) filter).put("son", son);
                final Map<String, Long> topTerms = index.getTopTerms();
                for (Map.Entry<String, Long> entryTerm : topTerms.entrySet()) {
                    final JsonNode termEnum = Utility.mapper.createObjectNode();
                    ((ObjectNode) termEnum).put("id", entryTerm.getKey());
                    ((ObjectNode) termEnum).put("title", entryTerm.getKey());
                    ((ObjectNode) termEnum).put("value", entryTerm.getKey());
                    ((ObjectNode) termEnum).put("population", entryTerm.getValue());
                    ((ArrayNode) son).add(termEnum);
                }
                ((ArrayNode) filterArray).add(filter);
            } else if (!index.getTopTerms().isEmpty()) {
                ((ObjectNode) filter).put("type", index.getDatatype().name().toLowerCase());
                ((ArrayNode) filterArray).add(filter);
            }
        }
        ((ObjectNode) filters).put("filters", filterArray);
        final JacksonRepresentation jsonRep = new JacksonRepresentation(MediaType.APPLICATION_JSON, filters);
        //jsonRep.setIndenting(true);
        return jsonRep;
    }
}
