/*******************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.sitools.SearchGeometryEngine.Cone;
import fr.cnes.sitools.SearchGeometryEngine.CoordSystem;
import fr.cnes.sitools.SearchGeometryEngine.GeometryIndex;
import fr.cnes.sitools.SearchGeometryEngine.Index;
import fr.cnes.sitools.SearchGeometryEngine.Point;
import fr.cnes.sitools.SearchGeometryEngine.RingIndex;
import fr.cnes.sitools.SearchGeometryEngine.Shape;
import fr.cnes.sitools.extensions.astro.application.OpenSearchApplicationPlugin;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a SOLR request based on a Cone.
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class QueryConeSolrRequest extends AbstractSolrQueryRequestFactory {

    /**
     * Solr base URL.
     */
    private final String solrBaseUrl;
    /**
     * Query parameters to process.
     */
    private Map<String, Object> queryParameters;
    /**
     * Coordinate system.
     */
    private final CoordSystem coordSystem;
    /**
     * Healpix scheme.
     */
    private Scheme healpixScheme;
    /**
     * Healpix result.
     */
    private Object objHealpix;
    /**
     * Healpix order.
     */
    private int nbOrder;

    /**
     * Constructs a new Solr string based on a cone.
     * @param solrBaseUrl URL of the SOLR server
     * @param queryParameters User query parameters
     * @param coordSystem Coordinate system
     * @param healpixScheme Helapix scheme
     */
    public QueryConeSolrRequest(final String solrBaseUrl, Map<String, Object> queryParameters, final CoordSystem coordSystem, Scheme healpixScheme) {
        this.solrBaseUrl = solrBaseUrl;
        this.queryParameters = queryParameters;
        this.coordSystem = coordSystem;
        this.healpixScheme = healpixScheme;
    }

    @Override
    protected final String getSolrServer() {
        return this.solrBaseUrl;
    }

    @Override
    protected Map<String, Object> getUserParametersToProcess() {
        return this.queryParameters;
    }

    @Override
    protected final Shape createGeometry(Map<String, Object> queryParametersToProcess) {
        // query parameter
        String cone = (String) queryParameters.get(OpenSearchApplicationPlugin.GeometryShape.CONE.getShape());
        String radius = (String) queryParameters.get(OpenSearchApplicationPlugin.GeometryShape.CONE.getOrder());
        String[] parameters = parseShape(cone);

        // intersection algorithm
        Shape shape = new Cone(new Point(Double.valueOf(parameters[0]), Double.valueOf(parameters[1]), coordSystem), Math.toRadians(Double.valueOf(radius)));
        return shape;
    }

    @Override
    protected final void computeHealpix(final Shape shape) {
        Object obj = null;
        try {
            Index index = getIntersectedHealpixWithShape(shape, this.healpixScheme);
            obj = index.getIndex();
        } catch (Exception ex) {
            Logger.getLogger(QueryBBOXSolrRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        setObjHealpix(obj);
    }

    @Override
    protected final void removeUserGeometryParameters(Map<String, Object> queryParameters) {
        queryParameters.remove(OpenSearchApplicationPlugin.GeometryShape.CONE.getShape());
        queryParameters.remove(OpenSearchApplicationPlugin.GeometryShape.CONE.getOrder());
    }

    @Override
    protected final String geometryConstraint() {
        String constraint = "";
        if (this.objHealpix != null) {
            if (this.objHealpix instanceof List) {
                List<Long> healpixList = (List<Long>) this.objHealpix;
                constraint = String.format("order%s:(", nbOrder);
                for (int i = 0; i < healpixList.size(); i++) {
                    constraint = constraint.concat(String.valueOf(healpixList.get(i)));
                    if (i < healpixList.size() - 1) {
                        constraint = constraint.concat(" OR ");
                    }
                }
                constraint = constraint.concat(")");
            } else if (this.objHealpix instanceof RangeSet) {
                RangeSet range = (RangeSet) this.objHealpix;
                int nbRanges = range.size();
                constraint = String.format("order%s:(", nbOrder);
                for (int i = 0; i < nbRanges; i++) {
                    constraint = constraint.concat(String.format("[%s TO %s]", range.ivbegin(i), range.ivend(i) - 1));
                    if (i < nbRanges - 1) {
                        constraint = constraint.concat(" OR ");
                    }
                }
                constraint = constraint.concat(")");
            }
            //constraint = String.format("(%s)", constraint);
        }
        return constraint;  
    }

    /**
     * Returns the healpix numbers that intersect with the shape.
     * @param shape shape
     * @return healpix number on RING scheme
     * @throws UnsupportedOperationException when NESTED is asked
     */
    private Index getIntersectedHealpixWithShape(Shape shape, Scheme healpixScheme) throws Exception {
        Index index = GeometryIndex.createIndex(shape, healpixScheme);
        int nbHealpixOrder;
        switch (healpixScheme) {
            case RING:
                nbHealpixOrder = ((RingIndex) index).getOrder();
                nbHealpixOrder = (nbHealpixOrder > MAX_ORDER) ? MAX_ORDER : nbHealpixOrder;
                ((RingIndex) index).setOrder(nbHealpixOrder);
                this.setNbOrder(nbHealpixOrder);
                break;
            case NESTED:
                throw new UnsupportedOperationException("Not supported yet.");
            //TODO : faire le buildSQLRequest en fonction de la multi-resolution
            //break;
            default:
                throw new UnsupportedOperationException("Not supported yet.");
        }

        return index;
    }

    /**
     * Sets a Healpix object.
     * @param objHealpixVal the objHealpix to set
     */
    public final void setObjHealpix(final Object objHealpixVal) {
        this.objHealpix = objHealpixVal;
    }

    /**
     * Sets the Healpix order.
     * @param nbOrderVal the nbOrder to set
     */
    public final void setNbOrder(final int nbOrderVal) {
        this.nbOrder = nbOrderVal;
    }
}