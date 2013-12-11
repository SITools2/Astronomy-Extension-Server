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
package fr.cnes.sitools.solr.query;

import fr.cnes.sitools.extensions.astro.application.OpenSearchApplicationPlugin;
import fr.cnes.sitools.searchgeometryengine.AbstractGeometryIndex;
import fr.cnes.sitools.searchgeometryengine.CoordSystem;
import fr.cnes.sitools.searchgeometryengine.Index;
import fr.cnes.sitools.searchgeometryengine.Point;
import fr.cnes.sitools.searchgeometryengine.Polygon;
import fr.cnes.sitools.searchgeometryengine.RingIndex;
import fr.cnes.sitools.searchgeometryengine.Shape;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a SOLR request based on a polygon.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class QueryPolygonSolrRequest extends AbstractSolrQueryRequestFactory {

    /**
     * Solr base URL.
     */
    private final String solrBaseUrl;
    /**
     * Query parameters to process.
     */
    private Map<String, Object> queryParameters;
    /**
     * Coordinates system.
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
     * Constructs a new SOLR string based on a polygon.
     *
     * @param solrBaseUrl URL of the SOLR server
     * @param queryParameters User query parameters
     * @param coordSystem Coordinate system
     * @param healpixScheme Helapix scheme
     */
    public QueryPolygonSolrRequest(final String solrBaseUrl, Map<String, Object> queryParameters, final CoordSystem coordSystem, Scheme healpixScheme) {
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
        String polygon = (String) queryParameters.get(OpenSearchApplicationPlugin.GeometryShape.POLYGON.getShape());
        String[] parameters = parseShape(polygon);
        List<Point> points = new ArrayList<Point>();
        for (int i = 0; i < parameters.length; i += 2) {
            points.add(new Point(Double.valueOf(parameters[i]), Double.valueOf(parameters[i + 1]), coordSystem));
        }

        // intersection algorithm
        Shape shape = new Polygon(points);
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
    protected void removeUserGeometryParameters(Map<String, Object> queryParameters) {
        queryParameters.remove(OpenSearchApplicationPlugin.GeometryShape.POLYGON.getShape());
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
        }
        return constraint;
    }

    /**
     * Returns the healpix numbers that intersect with the shape.
     *
     * @param shape shape
     * @return healpix number on RING scheme
     * @throws UnsupportedOperationException when NESTED is asked
     */
    private Index getIntersectedHealpixWithShape(Shape shape, Scheme healpixScheme) throws Exception {
        Index index = AbstractGeometryIndex.createIndex(shape, fr.cnes.sitools.searchgeometryengine.Scheme.valueOf(healpixScheme.name()));
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
     * Sets the Healpix object
     *
     * @param objHealpix the objHealpix to set
     */
    public void setObjHealpix(Object objHealpix) {
        this.objHealpix = objHealpix;
    }

    /**
     * Sets the Healpix order
     *
     * @param nbOrder the nbOrder to set
     */
    public void setNbOrder(int nbOrder) {
        this.nbOrder = nbOrder;
    }

}
