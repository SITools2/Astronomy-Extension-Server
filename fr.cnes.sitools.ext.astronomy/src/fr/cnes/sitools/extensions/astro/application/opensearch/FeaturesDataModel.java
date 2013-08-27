/*******************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 ******************************************************************************/
package fr.cnes.sitools.extensions.astro.application.opensearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data model of the collection of feature.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class FeaturesDataModel {
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(FeaturesDataModel.class.getName());    
    /**
     * Total result of records in the response.
     */
    public static final String TOTAL_RESULTS = "totalResults";
    /**
     * Feature type of the JSON file.
     */
    public static final String TYPE = "type";
    /**
     * Value of the feature type.
     */
    public static final String TYPE_VALUE = "FeatureCollection";
    /**
     * Features node of the data model.
     */
    public static final String FEATURES = "features";    
    /**
     * Result.
     */
    private final Map features = new HashMap();
    /**
     * List of feature.
     */
    private final List<Map> featureList = new ArrayList<Map>();
    /**
     * Constructs.
     */
    public FeaturesDataModel() {
        this.features.put(TOTAL_RESULTS, 0);
        this.features.put(TYPE, TYPE_VALUE);
        this.features.put(FEATURES, featureList);
    }
    /**
     * Adds a feature.
     * @param featureDM the feature
     */
    public final void addFeature(final FeatureDataModel featureDM) {
        if (!featureDM.isEmpty()) {
            this.featureList.add(featureDM.getFeature());
        }
    }
    /**
     * Returns the features.
     * @return the features
     */
    public final Map getFeatures() {        
        this.features.put(TOTAL_RESULTS, featureList.size());
        return features;
    }
    /**
     * Returns True when the identifier is already in the list of stored records otherwise False.
     * @param identifier identifier
     * @return True when the identifier is already in the list of stored records otherwise False
     */
    public final boolean hasIdentifier(final String identifier) {
        boolean result = false;
        for (final Map feature : featureList) {          
            final Map properties = (Map) feature.get(FeatureDataModel.PROPERTIES);
            assert properties != null;
            if (properties.containsKey(FeatureDataModel.PROPERTIES_ID) && properties.get(FeatureDataModel.PROPERTIES_ID).equals(identifier)) {
                result = true;
                break;
            }
        }    
        return result;
    }
    /**
     * Returns a feature based on a identifier.
     * <p>
     * An IllegalArgumentException is raised when the identifier is unknown.
     * </p>
     * @param identifier identifier
     * @return a feature based on a identifier
     */
    public final Map getFeature(final String identifier) {        
        for (final Map feature : featureList) {           
            final Map properties = (Map) feature.get(FeatureDataModel.PROPERTIES);
            assert properties != null;
            if (properties.containsKey(FeatureDataModel.PROPERTIES_ID) && properties.get(FeatureDataModel.PROPERTIES_ID).equals(identifier)) {                
                return feature;
            }
        }
        throw new IllegalArgumentException(identifier + " is unknown.");
    }
    
    /**
     * Updates the feature when the server returns a response that contains
     * duplicates ID.
     * <p>
     * The update is based on the download and quicklook.
     * </p>
     *
     * @param feature feature to test
     */
    public final void updateFeatureWithSpecialCase(final FeatureDataModel feature) {
        if (feature.isEmpty()) {
            return;
        }
        final String identifier = feature.getIdentifier();
        if (hasIdentifier(identifier)) {
            final Map storedFeature = this.getFeature(identifier);
            final Map storedProperties = (Map) storedFeature.get(FeatureDataModel.PROPERTIES);
            final Map currentProperties = feature.getProperties();
            if (currentProperties.containsKey(FeatureDataModel.PROPERTIES_QUICKLOOK) && !storedProperties.containsKey(FeatureDataModel.PROPERTIES_QUICKLOOK)) {
                storedProperties.put(FeatureDataModel.PROPERTIES_QUICKLOOK, currentProperties.get(FeatureDataModel.PROPERTIES_QUICKLOOK));               
            }
            if (currentProperties.containsKey(FeatureDataModel.SERVICES) && storedFeature.containsKey(FeatureDataModel.SERVICES)) {
                final Map storedServices = (Map) storedFeature.get(FeatureDataModel.SERVICES);
                final Map currentServices = (Map) feature.getServices();
                final Map storedDownload = (Map) storedServices.get(FeatureDataModel.SERVICES_DOWNLOAD);
                final Map currentDownload = (Map) currentServices.get(FeatureDataModel.SERVICES_DOWNLOAD);
                final String storedMimeType = String.valueOf(storedDownload.get(FeatureDataModel.SERVICES_DOWNLOAD_MIMETYPE));
                final String currentMimeType = String.valueOf(currentDownload.get(FeatureDataModel.SERVICES_DOWNLOAD_MIMETYPE));
                if ("image/fits".equals(storedMimeType) && !storedMimeType.equals(currentMimeType)) {
                    storedProperties.put(FeatureDataModel.PROPERTIES_QUICKLOOK, currentDownload.get(FeatureDataModel.SERVICES_DOWNLOAD_URL));
                }
            } else if (feature.getFeature().containsKey(FeatureDataModel.SERVICES) && !storedFeature.containsKey(FeatureDataModel.SERVICES)) {
                storedFeature.put(FeatureDataModel.SERVICES, (Map) feature.getServices());
            }
            feature.clear();
        }
    }
}
