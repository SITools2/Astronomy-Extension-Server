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

import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.OpenSearchApplicationPlugin;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.util.RIAPUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

/**
 * Base Resource that computes the Solr indexes from Luke.
 * @author Jean-Christophe Malapert
 */
public class OpenSearchBase extends SitoolsParameterizedResource {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OpenSearchBase.class.getName());
  
  /**
   * List of indexed fields.
   */
    private transient List<Index> indexedFields;
    /**
     * SOLR base URL.
     */
    private transient String solrBaseUrl;
    /**
     * Data model.
     */
    private transient ApplicationPluginModel paramModel;    

    /**
     * Init.
     */
    @Override
    public void doInit() {
        super.doInit();   
        setAutoDescribing(false);
        this.paramModel = ((OpenSearchApplicationPlugin) getApplication()).getModel();
        final String attach = getSitoolsSetting(Consts.APP_SOLR_URL);
        this.solrBaseUrl = RIAPUtils.getRiapBase() + attach + "/" + getPluginParameters().get("solrCore").getValue();
        try {
            computeIndexedFields();
        } catch (JSONException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }
    }

    /**
     * Computes the list of SOLR indexes.
     * @throws JSONException Exception
     * @throws IOException  Exception
     */
    protected final void computeIndexedFields() throws JSONException, IOException {
        this.indexedFields = new ArrayList<Index>();
        // Use Luke to get the index definition
        final ClientResource client = new ClientResource(getSolrBaseUrl() + "/admin/luke?wt=json&numTerms=" + Index.MAX_TOP_TERMS);               
        
        final JSONObject json = new JSONObject(client.get().getText());
        final JSONObject fields = json.getJSONObject("fields");
        
        // iter on all fields of the solr index
        final Iterator iter = fields.keys();
        while (iter.hasNext()) {
            final String key = (String) iter.next();
            final JSONObject node = fields.getJSONObject(key);
            // parse index
            final boolean isIndexField = node.has("index");
            if (isIndexField && node.getString("index").contains("S")) {
                final String indexType = node.getString("type");
                final JSONArray topTermsArray = node.getJSONArray("topTerms");
                final Map<String, Long> terms = new HashMap<String, Long>();
                for (int i = 0; i < topTermsArray.length(); i += 2) {
                    terms.put(topTermsArray.getString(i), topTermsArray.getLong(i + 1));
                }
                this.indexedFields.add(new Index(key, true, Index.DataType.getDataTypeFromSolrDataTypeName(indexType), terms));
            }
        }
        client.release();
    }

    /**
     * Returns indexes.
     * @return indexes
     */
    public final List<Index> getIndexedFields() {
        return Collections.unmodifiableList(this.indexedFields);
    }

    /**
     * Returns the Solr URL.
     * @return the Solr URL
     */
    public final String getSolrBaseUrl() {
        return this.solrBaseUrl;
    }

    /**
     * Returns the plugin data model.
     * @return the plugin data model
     */
    public final Map<String, ApplicationPluginParameter> getPluginParameters() {
        return this.paramModel.getParametersMap();
    }

    /**
     * Returns the plugin data model.
     * @return the plugin data model
     */
    public final ApplicationPluginModel getPluginModel() {
        return this.paramModel;
    }
}
