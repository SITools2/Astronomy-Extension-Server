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
package fr.cnes.sitools.extensions.astro.application.opensearch.datamodel;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Data model for a record.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class FeatureDataModel {
    /**
     * Geometry node of the data model.
     */
    public static final String GEOMETRY = "geometry";
    /**
     * Sets of coordinates describing the FOV.
     */
    public static final String GEOMETRY_COORDINATES = "coordinates";
    /**
     * Geometry type : Point or Polygon.
     */
    public static final String GEOMETRY_TYPE = "type";
    /**
     * Properties node.
     */
    public static final String PROPERTIES = "properties";
    /**
     * Coordinate system node.
     */
    public static final String PROPERTIES_CRS = "crs";
    /**
     * Coordinate system type.
     */
    public static final String PROPERTIES_CRS_TYPE = "type";
    /**
     * Coordinate system name.
     */
    public static final String PROPERTIES_CRS_TYPE_VALUE = "name";
    /**
     * Coordinate system properties.
     */
    public static final String PROPERTIES_CRS_PROPERTIES = "properties";
    /**
     * Coordinate system name.
     */
    public static final String PROPERTIES_CRS_PROPERTIES_NAME = "name";
    /**
     * Unique identifier.
     */
    public static final String PROPERTIES_ID = "identifier";
    /**
     * Observation date.
     */
    public static final String PROPERTIES_DATEOBS = "date-obs";
    /**
     * Quicklook.
     */
    public static final String PROPERTIES_QUICKLOOK = "quicklook";
    /**
     * Service node.
     */
    public static final String SERVICES = "services";
    /**
     * Download node.
     */
    public static final String SERVICES_DOWNLOAD = "download";
    /**
     * URL where the file can be downloaded.
     */
    public static final String SERVICES_DOWNLOAD_URL = "url";
    /**
     * Mimetype of the URL.
     */
    public static final String SERVICES_DOWNLOAD_MIMETYPE = "mimetype";
    /**
     * One record.
     */
    private Map feature = new HashMap();
    /**
     * geometry information.
     */
    private final Map geometry = new HashMap();
    /**
     * properties information.
     */
    private final Map properties = new HashMap();
    /**
     * services information.
     */
    private final Map services = new HashMap();
    /**
     * crs information.
     */
    private final Map crs = new HashMap();
    /**
     * crs properties information.
     */
    private final Map crsProperties = new HashMap();
    /**
     * download information.
     */
    private final Map download = new HashMap(); 
    
    /**
     * Constructs and initializes the data model.
     */
    public FeatureDataModel() {
        geometry.clear();
        properties.clear();
        services.clear();
        crs.clear();
        crsProperties.clear();
        download.clear();
        feature.put(PROPERTIES, properties);
        properties.put(PROPERTIES_ID, null);
    }
    /**
     * Constructs and initializes the data model with an record ID.
     * @param identifier Identifier
     */
    public FeatureDataModel(final String identifier) {
        geometry.clear();
        properties.clear();
        services.clear();
        crs.clear();
        crsProperties.clear();
        download.clear();        
        feature.put(PROPERTIES, properties);
        properties.put(PROPERTIES_ID, identifier);
    }
    /**
     * Sets the identifier.
     * @param identifier the identifier to set
     */
    public final void setIdentifier(final String identifier) {
        properties.put(PROPERTIES_ID, identifier);
    }
    /**
     * Returns the identifier.
     * @return the identifier
     */
    public final String getIdentifier() {
        return (String) properties.get(PROPERTIES_ID);
    }
    /**
     * Sets the quicklook.
     * @param url URL of the quicklook
     */
    public final void setQuicklook(final URL url) {
        properties.put(PROPERTIES_QUICKLOOK, url);
    }
    /**
     * Returns the quicklook.
     * @return the quicklook URL
     */
    public final String getQuicklook() {
        return (String) properties.get(PROPERTIES_QUICKLOOK);
    }
    /**
     * Creates the geometry in the data model.
     * @param coordinates coordinates
     * @param shapeName shape (Polygon or Point)
     */
    public final void createGeometry(final String coordinates, final String shapeName) {
        geometry.put(GEOMETRY_COORDINATES, coordinates);
        geometry.put(GEOMETRY_TYPE, shapeName);
        getFeature().put(GEOMETRY, geometry);
    }
    /**
     * Returns the geometry from the data model.
     * @return the geometry from the data model
     */
    public final Map getGeometry() {
        return geometry;
    }
    /**
     * Creates crs in the data model.
     * @param crsName crs name
     */
    public final void createCrs(final String crsName) {
        crs.put(PROPERTIES_CRS_TYPE, PROPERTIES_CRS_TYPE_VALUE);
        crs.put(PROPERTIES_CRS_PROPERTIES, crsProperties);
        crsProperties.put(PROPERTIES_CRS_PROPERTIES_NAME, crsName);
        properties.put(PROPERTIES_CRS, crs);
        getFeature().put(PROPERTIES, properties);
    }
    /**
     * Creates services in the data model.
     * @param mimeType mime type
     * @param url URL of the file to download
     */
    public final void createServices(final String mimeType, final URL url) {
        download.put(SERVICES_DOWNLOAD_MIMETYPE, mimeType);
        download.put(SERVICES_DOWNLOAD_URL, url.toString());
        services.put(SERVICES_DOWNLOAD, download);
        getFeature().put(SERVICES, services);
    }
    /**
     * Returns the services from the data model.
     * @return the services from the data model
     */
    public final Map getServices() {
        return services;
    }
    /**
     * Adds the observation date in the data model.
     * @param obsDate observation date
     */
    public final void addDateObs(final String obsDate) {
        properties.put(PROPERTIES_DATEOBS, obsDate);
    }
    /**
     * Adds a property.
     * @param keyword keyword
     * @param val value
     */
    public final void addProperty(final String keyword, final String val) {
        properties.put(keyword, val);
    }
    /**
     * Adds a property.
     * @param keyword keyword
     * @param val value
     */
    public final void addProperty(final String keyword, final Object val) {
        properties.put(keyword, val);
    }
    /**
     * Adds a property.
     * @param keyword keyword
     * @param val value
     */
    public final void addProperty(final String keyword, final int val) {
        properties.put(keyword, val);
    }
    /**
     * Adds a property.
     * @param keyword keyword
     * @param val value
     */
    public final void addProperty(final String keyword, final float val) {
        properties.put(keyword, val);
    }
    /**
     * Adds a property.
     * @param keyword keyword
     * @param val value
     */
    public final void addProperty(final String keyword, final double val) {
        properties.put(keyword, val);
    }
    /**
     * Returns the properties.
     * @return the properties
     */
    public final Map getProperties() {
        return this.properties;
    }
    /**
     * Sets the feature.
     * @param featureVal feature to set
     */
    public final void setFeature(final Map featureVal) {
        this.feature = featureVal;
    }
    /**
     * Returns the feature.
     * @return the feature
     */
    public final Map getFeature() {
        return this.feature;
    }
    /**
     * Returns True when feature is empty otherwise False.
     * @return True when feature is empty otherwise False
     */
    public final boolean isEmpty() {
        return this.feature.isEmpty();
    }
    /**
     * Clears the feature.
     */
    public final void clear() {
        geometry.clear();
        properties.clear();
        services.clear();
        crs.clear();
        crsProperties.clear();
        download.clear();
        feature.clear();
    }
}
