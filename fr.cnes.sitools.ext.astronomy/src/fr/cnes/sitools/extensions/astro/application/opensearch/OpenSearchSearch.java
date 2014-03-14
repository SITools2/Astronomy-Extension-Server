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

package fr.cnes.sitools.extensions.astro.application.opensearch;

import healpix.essentials.Scheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.extensions.astro.application.OpenSearchApplicationPlugin;
import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeatureDataModel;
import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeaturesDataModel;
import fr.cnes.sitools.searchgeometryengine.CoordSystem;
import fr.cnes.sitools.solr.query.AbstractSolrQueryRequestFactory;

/**
 * Search resource for OpenSearch.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchSearch extends OpenSearchBase {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(OpenSearchSearch.class.getName());
  /**
   * Default count.
   */
  public static final String DEFAULT_COUNT = "100";
  /**
   * Default start index.
   */
  public static final String DEFAULT_START_INDEX = "1";
  /**
   * Default start page.
   */
  public static final String DEFAULT_START_PAGE = "1";
  /**
   * Query parameters.
   */
  private Map<String, Object> queryParameters;

  /**
   * Init.
   */
  @Override
  public final void doInit() {
    super.doInit();
    setQueryParameters();
    //TO DO : vérifier que count startPage et startIndex sont des entiers
    //TO DO : vérifier le format de la date pour time:start et time:stop
  }

  /**
   * Returns the JSON reprepsentation.
   *
   * @return the JSON reprepsentation
   */
  @Get
  public Representation getJsonResponse() {
    if (!isValidInputParameters()) {
      LOG.log(Level.WARNING, null, "Input parameters are not valid");
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
    }
    try {
      final String referenceSystem = getPluginParameters().get("referenceSystem").getValue();
      final CoordSystem coordSystem = referenceSystem.equals("geocentric") ? CoordSystem.GEOCENTRIC : CoordSystem.EQUATORIAL;
      final String healpixSchemeParam = getPluginParameters().get("healpixScheme").getValue();
      final Scheme healpixScheme = Scheme.valueOf(healpixSchemeParam);
      final AbstractSolrQueryRequestFactory querySolr = AbstractSolrQueryRequestFactory.createInstance(queryParameters, coordSystem, getSolrBaseUrl(), healpixScheme);
      querySolr.createQueryBuilder();
      final String query = querySolr.getSolrQueryRequest();
      LOG.log(Level.INFO, query);
      final ClientResource client = new ClientResource(query);
      final JsonRepresentation jsonRep = new JsonRepresentation(buildJsonResponse(new JSONObject(client.get().getText())));
      jsonRep.setIndenting(true);
      return jsonRep;
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex);
    }
  }

  /**
   * Sets query parameters for SOLR.
   */
  private void setQueryParameters() {
    final Form form = this.getRequest().getResourceRef().getQueryAsForm();
    final Set<String> parameters = form.getNames();

    this.queryParameters = new HashMap<String, Object>(parameters.size());

    for (String parameterIter : parameters) {
      final Object value = form.getFirstValue(parameterIter);
      this.queryParameters.put(parameterIter, value);
    }
    if (!this.queryParameters.containsKey("count")) {
      this.queryParameters.put("count", DEFAULT_COUNT);
    }
    if (!this.queryParameters.containsKey("startIndex")) {
      this.queryParameters.put("startIndex", DEFAULT_START_INDEX);
    }
    if (!this.queryParameters.containsKey("startPage")) {
      this.queryParameters.put("startPage", DEFAULT_START_PAGE);
    }
    this.queryParameters.put("format", "json");
  }

  /**
   * Returns the user query parameters.
   *
   * @return user query parameters
   */
  protected final Map<String, Object> getQueryParameters() {
    return this.queryParameters;
  }

  /**
   * Check if input parameters is really included in all query parameters. The whole parameters contains indexed fields, q, count,
   * startPage, startIndex, format
   *
   * @return Returns true when input parameters is included in all query parameters
   */
  private boolean isValidInputParameters() {
    final List<String> allQueryParameters = new ArrayList<String>();

    // get Indexed fields from LUKA interface
    final List<Index> indexes = getIndexedFields();
    for (Index index : indexes) {
      allQueryParameters.add(index.getName());
    }

    // Get the query shape. The shape is defined by the administrator in the plugin
    final String shape = getPluginParameters().get("queryShape").getValue();
    final OpenSearchApplicationPlugin.GeometryShape geometry = OpenSearchApplicationPlugin.GeometryShape.getGeometryShapeFrom(shape);
    allQueryParameters.add(geometry.getShape());
    if (geometry.getOrder() != null) {
      allQueryParameters.add(geometry.getOrder());
    }

    // Add fields from no indexed fields
    allQueryParameters.add("q");
    allQueryParameters.add("count");
    allQueryParameters.add("startPage");
    allQueryParameters.add("startIndex");
    allQueryParameters.add("format");

    // Check if queryParameter is incuded in QllQueryParameters
    final Set<String> queryUserParameters = this.queryParameters.keySet();
    for (String queryUserParameter : queryUserParameters) {
      if (!allQueryParameters.contains(queryUserParameter)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the footprint is a point.
   *
   * @param footprint footprint of the shape
   * @return Returns true when the footprint is a Point
   */
  private boolean isPoint(final String footprint) {
    return (footprint.split(",").length == 2) ? true : false;
  }
  /**
   * Returns the first point.
   * @param footprint footprint
   * @return the first point
   * @throws JSONException Exception
   */
  private JSONArray getFirstPoint(final String footprint) throws JSONException {
    return new JSONArray(footprint);
  }

  /**
   * Transforms the response from SOLR to OpenSearch.
   *
   * @param json JSON output from SOLR
   * @return Return the openSearch response
   * @throws JSONException Exception
   */
  private JSONObject buildJsonResponse(final JSONObject json) throws JSONException {
    final JSONObject responseService = new JSONObject();
    responseService.put("type", "FeatureCollection");
    final JSONObject response = json.getJSONObject("response");
    responseService.put(FeaturesDataModel.TOTAL_RESULTS, response.getLong("numFound"));

    final JSONArray docsArray = (JSONArray) response.get("docs");

    final List<Map> records = new ArrayList<Map>();
    for (int i = 0; i < docsArray.length(); i++) {
      final Map<String, Object> dataModelRecord = new HashMap<String, Object>();
      final JSONObject doc = docsArray.getJSONObject(i);
      dataModelRecord.put("type", "Feature");
      final Map geometry = new HashMap();
      final String referenceSystem = getPluginParameters().get("referenceSystem").getValue();
      final String footprint = (String) doc.get(OpenSearchApplicationPlugin.Standard_Open_Search.GEOMETRY_COORDINATES.getKeywordSolr());
      final JSONArray pointArray = getFirstPoint(footprint);
      if (isPoint(footprint)) {
        geometry.put(FeatureDataModel.GEOMETRY_TYPE, "Point");
        geometry.put(FeatureDataModel.GEOMETRY_COORDINATES, Arrays.asList(pointArray.get(0), pointArray.get(1)));
      } else {
        geometry.put(FeatureDataModel.GEOMETRY_TYPE, "Polygon");
        final String[] points = footprint.split("],");
        final List responsePoints = new ArrayList();
        for (int j = 0; j < points.length; j++) {
          String point = points[j];
          point = point.replace("[", "");
          point = point.replace("]", "");
          final String[] coordinates = point.split(",");
          final List responsePoint = Arrays.asList(Double.valueOf(coordinates[0]), Double.valueOf(coordinates[1]));
          responsePoints.add(responsePoint);
        }
        geometry.put(FeatureDataModel.GEOMETRY_COORDINATES, Arrays.asList(responsePoints));
      }
      dataModelRecord.put(FeatureDataModel.GEOMETRY, geometry);

      final Map properties = new HashMap();
      final Map services = new HashMap();
      final Map download = new HashMap();
      final Map browse = new HashMap();
      final Map layer = new HashMap();
      final Map crs = new HashMap();
      final Map crsProperties = new HashMap();
      crsProperties.put("name", "ICRS".equals(referenceSystem) ? "equatorial.ICRS" : "urn:ogc:def:crs:OGC:1.3:CRS84");
      crs.put("properties", crsProperties);
      crs.put("type", "name");
      properties.put("crs", crs);
      //TODO check as above
      final Iterator iter = doc.keys();
      while (iter.hasNext()) {
        final String key = (String) iter.next();
        if (!key.equals(OpenSearchApplicationPlugin.Standard_Open_Search.GEOMETRY_COORDINATES.getKeywordSolr())
                && !key.equals(OpenSearchApplicationPlugin.Standard_Open_Search.GEOMETRY_COORDINATES_TYPE.getKeywordSolr())) {
          final String[] result = OpenSearchApplicationPlugin.Standard_Open_Search.getKeywordProperties(key);
          final String node = result[0];
          final String val = result[1];
          if ("properties".equals(node)) {
            properties.put(val, doc.get(key));
          } else if ("layer".equals(node)) {
            layer.put(val, doc.get(key));
          } else if ("browse".equals(node)) {
            browse.put(val, doc.get(key));
          } else if ("download".equals(node)) {
            download.put(val, doc.get(key));
          } else {
            throw new IllegalArgumentException();
          }
        }
      }
      if (!layer.isEmpty()) {
        browse.put("layer", layer);
      }
      if (!browse.isEmpty()) {
        services.put("browse", browse);
      }
      if (!download.isEmpty()) {
        services.put("download", download);
      }
      if (!services.isEmpty()) {
        dataModelRecord.put(FeatureDataModel.SERVICES, services);
      }
      dataModelRecord.put(FeatureDataModel.PROPERTIES, properties);

      records.add(dataModelRecord);
    }
    responseService.put(FeaturesDataModel.FEATURES, records);
    return responseService;
  }
}
