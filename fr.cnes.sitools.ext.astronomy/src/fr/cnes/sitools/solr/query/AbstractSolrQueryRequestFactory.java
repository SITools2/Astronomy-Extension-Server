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

import healpix.essentials.Scheme;

import java.util.Map;
import java.util.Set;

import fr.cnes.sitools.extensions.astro.application.OpenSearchApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.opensearch.OpenSearchSearch;
import fr.cnes.sitools.searchgeometryengine.CoordSystem;
import fr.cnes.sitools.searchgeometryengine.Shape;
import fr.cnes.sitools.util.Util;

/**
 * A factory for building a Solr Query request and a few utility methods to
 * build the SOLR string that is send to the SOLR server.
 *
 * <p>
 * The choice of the implementation is based on the geometry (BBOX, Cone, ...)
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class AbstractSolrQueryRequestFactory {

    /**
     * Maximal order of the Healpix index.
     */
    protected static final int MAX_ORDER = 13;
    /**
     * SOLR query.
     */
    private String query;

    /**
     * Returns a new instance of the SOLR query String.
     *
     * @param queryParameters User query parameters
     * @param coordSystem Coordinate system
     * @param solrBaseUrl URL of the SOLR server
     * @param healpixScheme Healpix Scheme
     * @return SOLR object that contains the SOLR query string.
     */
    public static AbstractSolrQueryRequestFactory createInstance(Map<String, Object> queryParameters, final CoordSystem coordSystem, final String solrBaseUrl, final Scheme healpixScheme) {
        if (queryParameters.containsKey(OpenSearchApplicationPlugin.GeometryShape.BBOX.getShape())) {
            return new QueryBBOXSolrRequest(solrBaseUrl, queryParameters, coordSystem, healpixScheme);
        } else if (queryParameters.containsKey(OpenSearchApplicationPlugin.GeometryShape.CONE.getShape())) {
            return new QueryConeSolrRequest(solrBaseUrl, queryParameters, coordSystem, healpixScheme);
        } else if (queryParameters.containsKey(OpenSearchApplicationPlugin.GeometryShape.POLYGON.getShape())) {
            return new QueryPolygonSolrRequest(solrBaseUrl, queryParameters, coordSystem, healpixScheme);
        } else if (queryParameters.containsKey(OpenSearchApplicationPlugin.GeometryShape.HEALPIX.getShape())) {
            return new QueryPixelSolrRequest(solrBaseUrl, queryParameters, coordSystem, healpixScheme);
        } else {
            return new QueryNoGeometrySolrRequest(solrBaseUrl, queryParameters);
        }
    }

    /**
     * Creates a SOLR query, which is stored in
     * <code>query</code> parameter.
     *
     * <p>
     * This method is doing the following steps:
     * <ul>
     * <li>Gets user query parameters</li>
     * <li>Gets the Solr server URL</li>
     * <li>Creates the geometry from positional search given by the user</li>
     * <li>Transforms a Shape into Healpix pixels</li>
     * <li>Deletes the Geometry parameters</li>
     * <li>Computes and stores the Query String to send to SOLR</li>
     * </ul>
     * Each step is overidden by a subclass.
     * </p>
     */
    public final void createQueryBuilder() {

        // Get user query parameters and configuration
        Map<String, Object> queryParametersToProcess = getUserParametersToProcess();
        String solrServerUrl = getSolrServer();

        // Create the geometry
        Shape shape = createGeometry(queryParametersToProcess);

        // Create the Healpix object
        computeHealpix(shape);

        // Removes the geometry parameters to keep the remaining parameters to process
        removeUserGeometryParameters(queryParametersToProcess);

        query = buildSolrQueryFrom(solrServerUrl, queryParametersToProcess);
    }

    /**
     * Returns the Solr Server URL.
     *
     * @return the URL of the SOLR server
     */
    protected abstract String getSolrServer();

    /**
     * Returns the user parameters to process.
     *
     * @return the updated parameters list to process
     */
    protected abstract Map<String, Object> getUserParametersToProcess();

    /**
     * Returns the Shape that has been created based on user query parameters.
     *
     * @param queryParametersToProcess Query parameters
     * @return a shape representing the geometry at a positional search
     */
    protected abstract Shape createGeometry(Map<String, Object> queryParametersToProcess);

    /**
     * Transforms the shape into a Healpix object.
     *
     * @param shape shape representing the geometry of the positional search
     */
    protected abstract void computeHealpix(Shape shape);

    /**
     * Updates the list of query parameters by removing the geometry parameters.
     *
     * @param queryParameters query parameters
     */
    protected abstract void removeUserGeometryParameters(Map<String, Object> queryParameters);

    /**
     * Returns the SOLR contraint part for the geometry.
     *
     * @return the SOLR constrain for the geometry
     */
    protected abstract String geometryConstraint();

    /**
     * Returns the Solr request String.
     *
     * @return the Solr request
     */
    public final String getSolrQueryRequest() {
        return this.query;
    }

    /**
     * Returns the Solr request String.
     *
     * @param queryParametersToProcess User query parameters
     * @return the solr request
     */
    private String buildSolrQueryFrom(final String solrServerUrl, Map<String, Object> queryParametersToProcess) {
        String queryBuilder = String.format(solrServerUrl + "/select/?version=2.2&start=%s&rows=%s&wt=%s", computeStartSolr(queryParametersToProcess), queryParametersToProcess.get("count"), queryParametersToProcess.get("format"));
        queryParametersToProcess.remove("startIndex");
        queryParametersToProcess.remove("startPage");
        queryParametersToProcess.remove("count");
        queryParametersToProcess.remove("format");
        queryParametersToProcess.remove("coordSystem");
        String searchTerms = searchTermsConstraint(queryParametersToProcess);
        String geometry = geometryConstraint();
        String parameters = parameterConstraint(queryParametersToProcess);

        if (Util.isEmpty(parameters) && Util.isEmpty(geometry) && Util.isEmpty(searchTerms)) {
            queryBuilder = queryBuilder.concat("&q=*:*");
        } else {
            queryBuilder = queryBuilder.concat("&q=");
            boolean hasAlreadyFirstTerm = false;
            if (Util.isNotEmpty(searchTerms)) {
                queryBuilder = queryBuilder.concat(searchTerms);
                hasAlreadyFirstTerm = true;
            }
            if (Util.isNotEmpty(geometry) && hasAlreadyFirstTerm) {
                queryBuilder = queryBuilder.concat(" AND " + geometry);
            } else if (Util.isNotEmpty(geometry)) {
                queryBuilder = queryBuilder.concat(geometry);
                hasAlreadyFirstTerm = true;
            }
            if (Util.isNotEmpty(parameters) && hasAlreadyFirstTerm) {
                queryBuilder = queryBuilder.concat(" AND " + parameters);
            } else if (Util.isNotEmpty(parameters)) {
                queryBuilder = queryBuilder.concat(parameters);
            }
        }
        return queryBuilder;
    }

    /**
     * Returns the search term constraint part that has been built.
     *
     * @param queryParameters Query parameters
     * @return search terms constraint
     */
    protected final String searchTermsConstraint(Map<String, Object> queryParameters) {
        String result = "";
        if (Util.isSet(queryParameters.get("q"))) {
            result = "searchTerms:" + queryParameters.get("q");
            queryParameters.remove("q");
        }
        return result;
    }

    /**
     * Returns the parameter constraint part that has been built.
     *
     * @param queryParametersToProcess User query parameters
     * @return parameter constraint
     */
    protected final String parameterConstraint(Map<String, Object> queryParametersToProcess) {
        String result = "";
        Set<String> params = queryParametersToProcess.keySet();
        assert params != null;
        int i = 0;

        for (String param : params) {
            String espacedParameter = (String) queryParametersToProcess.get(param);
            espacedParameter = AbstractSolrQueryRequestFactory.escapeQueryChars(espacedParameter);
            if (i == 0) {
                String constraintQuery = String.format("%s:%s", param, espacedParameter);
                result = result.concat(constraintQuery);
            } else {
                String constraintQuery = String.format(" AND %s:%s", param, espacedParameter);
                result = result.concat(constraintQuery);
            }
            i++;
        }
        queryParametersToProcess.remove(params);
        return result;
    }

    /**
     * Returns the first item number of the request.
     *
     * @param queryParametersToProcess User query parameters
     * @return the start
     */
    protected final int computeStartSolr(Map<String, Object> queryParametersToProcess) {
        if (!queryParametersToProcess.get("startIndex").equals(OpenSearchSearch.DEFAULT_START_INDEX)) {
            return Integer.valueOf(String.valueOf(queryParametersToProcess.get("startIndex"))) - 1;
        } else if (!queryParametersToProcess.get("startPage").equals(OpenSearchSearch.DEFAULT_START_PAGE)) {
            return (Integer.valueOf(String.valueOf(queryParametersToProcess.get("startPage"))) - 1) * Integer.valueOf(String.valueOf(queryParametersToProcess.get("count")));
        } else {
            return Integer.valueOf(OpenSearchSearch.DEFAULT_START_INDEX) - 1;
        }
    }

    /**
     * Parse shape based on comma.
     *
     * @param shape shape
     * @return an array of parameters
     */
    protected final String[] parseShape(final String shape) {
        return shape.split(",");
    }

    /**
     * Escape a String according to SOLR syntax.
     *
     * @param s String to espace if needed
     * @return Returns an escaped SOLR query
     * @see
     * http://lucene.apache.org/java/docs/queryparsersyntax.html#Escaping%20Special%20Characters
     *
     */
    public static String escapeQueryChars(final String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':'
                    || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~'
                    || c == '*' || c == '?' || c == '|' || c == '&' || c == ';'
                    || Character.isWhitespace(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
