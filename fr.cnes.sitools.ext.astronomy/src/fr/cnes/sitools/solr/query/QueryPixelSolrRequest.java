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

import fr.cnes.sitools.SearchGeometryEngine.CoordSystem;
import fr.cnes.sitools.SearchGeometryEngine.Shape;
import fr.cnes.sitools.extensions.astro.application.OpenSearchApplicationPlugin;
import healpix.core.HealpixIndex;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a SOLR request based on a Healpix number.
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class QueryPixelSolrRequest extends AbstractSolrQueryRequestFactory {

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
     * Healpix Scheme.
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
     * Constructs a new SOLR string based on Healpix number.
     * @param solrBaseUrl URL of the SOLR server
     * @param queryParameters User query parameters
     * @param coordSystem Coordinate system
     * @param healpixScheme Helapix scheme
     */
    public QueryPixelSolrRequest(final String solrBaseUrl, Map<String, Object> queryParameters, final CoordSystem coordSystem, Scheme healpixScheme) {
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
        return null;
    }

    @Override
    protected final void computeHealpix(final Shape shape) {
        String healpixNested = (String) queryParameters.get(OpenSearchApplicationPlugin.GeometryShape.HEALPIX.getShape());
        int nbHealpixOrder = Integer.valueOf(String.valueOf(queryParameters.get(OpenSearchApplicationPlugin.GeometryShape.HEALPIX.getOrder())));

        // transform Healpix from query parameter to List<Long>
        String[] healpixStringArray = healpixNested.split(",");
        Long[] healpixLongArray = new Long[healpixStringArray.length];
        for (int i = 0; i < healpixStringArray.length; i++) {
            healpixLongArray[i] = Long.valueOf(healpixStringArray[i]);
        }

        List<Long> healpix;
        switch (this.healpixScheme) {
            case RING:
                for (int i = 0; i < healpixStringArray.length; i++) {
                    healpixLongArray[i] = Long.valueOf(healpixStringArray[i]);
                }
                healpix = Arrays.asList(healpixLongArray);
                updateNest2Ring(nbOrder, healpix);
                break;
            case NESTED:
                for (int i = 0; i < healpixStringArray.length; i++) {
                    healpixLongArray[i] = Long.valueOf(healpixStringArray[i]);
                }
                healpix = Arrays.asList(healpixLongArray);                
                break;
            default:
                throw new IllegalArgumentException("Unknown Healpix Scheme");
        }      
        setNbOrder(nbHealpixOrder);
        setObjHealpix(healpix);
    }

    @Override
    protected final void removeUserGeometryParameters(Map<String, Object> queryParameters) {
        queryParameters.remove(OpenSearchApplicationPlugin.GeometryShape.HEALPIX.getShape());
        queryParameters.remove(OpenSearchApplicationPlugin.GeometryShape.HEALPIX.getOrder());
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
     * Sets the Healpix object.
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

    /**
     * Transform Healpix from NESTED to RING.
     * @param nbOrder Healpix order
     * @param healpix Healpix
     * @throws RuntimeException Error when transforming NESTED to RING
     */
    private void updateNest2Ring(int nbOrder, List<Long> healpix) {
        try {
            int nside = (int) Math.pow(2.0, nbOrder);
            HealpixIndex healpixIndex = new HealpixIndex(nside, Scheme.NESTED);
            for (int i = 0; i < healpix.size(); i++) {
                long healpixNumber = healpix.get(i);
                healpixNumber = healpixIndex.nest2ring(healpixNumber);
                healpix.set(0, healpixNumber);
            }
        } catch (Exception ex) {
            Logger.getLogger(QueryPixelSolrRequest.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Error when transforming NESTED to RING");
        }
    }
}
