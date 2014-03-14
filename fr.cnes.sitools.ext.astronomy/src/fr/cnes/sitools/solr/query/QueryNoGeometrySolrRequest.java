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
package fr.cnes.sitools.solr.query;

import java.util.Map;

import fr.cnes.sitools.searchgeometryengine.Shape;

/**
 * Creates a SOLR request based on no geometry parameters.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class QueryNoGeometrySolrRequest extends AbstractSolrQueryRequestFactory {

    /**
     * Solr base URL.
     */
    private final String solrBaseUrl;
    /**
     * Query parameters to process.
     */
    private Map<String, Object> queryParametersToProcess;

    /**
     * Constructs a new SOLR string based on no geometry parameters.
     *
     * @param solrBaseUrl URL of the SOLR server
     * @param queryParametersToProcess User query parameters
     */
    public QueryNoGeometrySolrRequest(final String solrBaseUrl, Map<String, Object> queryParametersToProcess) {
        this.solrBaseUrl = solrBaseUrl;
        this.queryParametersToProcess = queryParametersToProcess;
    }

    @Override
    protected final String getSolrServer() {
        return this.solrBaseUrl;
    }

    @Override
    protected Map<String, Object> getUserParametersToProcess() {
        return this.queryParametersToProcess;
    }

    @Override
    protected final Shape createGeometry(Map<String, Object> queryParametersToProcess) {
        return null;
    }

    @Override
    protected void computeHealpix(final Shape shape) {
    }

    @Override
    protected void removeUserGeometryParameters(Map<String, Object> queryParameters) {
    }

    @Override
    protected final String geometryConstraint() {
        return null;
    }
}
