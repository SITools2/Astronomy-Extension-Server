/*******************************************************************************
 * Copyright 2012 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.sitools.SearchGeometryEngine.Shape;
import java.util.Map;

/**
 * Creates a SOLR request based on no geometry parameters
 * 
 * @author Jean-Christophe Malapert
 */
public class QueryNoGeometrySolrRequest extends AbstractSolrQueryRequestFactory {
    
    private final String solrBaseUrl;
    private Map<String, Object> queryParametersToProcess;

    /**
     * Constructs a new SOLR string based on no geometry parameters
     * @param solrBaseUrl URL of the SOLR server
     * @param queryParametersToProcess User query parameters
     */
    public QueryNoGeometrySolrRequest(final String solrBaseUrl, Map<String, Object> queryParametersToProcess) {
        this.solrBaseUrl = solrBaseUrl;
        this.queryParametersToProcess = queryParametersToProcess;      
    }    

    @Override
    protected String getSolrServer() {
        return this.solrBaseUrl;
    }

    @Override
    protected Map<String, Object> getUserParametersToProcess() {
        return this.queryParametersToProcess;
    }

    @Override
    protected Shape createGeometry(Map<String, Object> queryParametersToProcess) {
        return null;
    }

    @Override
    protected void computeHealpix(Shape shape) {        
    }

    @Override
    protected void removeUserGeometryParameters(Map<String, Object> queryParameters) {       
    }

    @Override
    protected String geometryConstraint() {
        return null;
    }
        
}
