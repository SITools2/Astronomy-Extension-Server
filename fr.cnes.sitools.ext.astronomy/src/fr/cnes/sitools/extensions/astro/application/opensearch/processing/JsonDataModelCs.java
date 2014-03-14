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
package fr.cnes.sitools.extensions.astro.application.opensearch.processing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ivoa.xml.votable.v1.Field;

import org.restlet.engine.Engine;

import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeatureDataModel;
import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeaturesDataModel;
import fr.cnes.sitools.extensions.astro.application.opensearch.responsibility.ConeSearchHealpix;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.Utility;

/**
 * JSON data model for Cone search.
 * @author malapert
 */
class JsonDataModelCs extends AbstractJsonDataModel {
    /**
     * Coordinate system.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystem;
    /**
     * Server response.
     */
    private List<Map<Field, String>> response;
   /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(JsonDataModelCs.class.getName());
    /**
     * Constructor.
     * @param responseVal server response
     * @param coordinateSystemVal coordinate system
     */
    public JsonDataModelCs(final List<Map<Field, String>> responseVal, final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        setResponse(responseVal);
        setCoordinateSystem(coordinateSystemVal);
    }

    @Override
    public FeaturesDataModel getDataModel() {
        return createGeoJsonDataModel(getResponse(), getCoordinateSystem());
    }
    /**
     * Creates and returns the GeoJson data response from the response.
     *
     * @param response response coming from the cache or the VO server
     * @return GeoJson data response
     */
    private FeaturesDataModel createGeoJsonDataModel(final List<Map<Field, String>> response, AstroCoordinate.CoordinateSystem coordinateSystem) {
        final FeaturesDataModel dataModel = new FeaturesDataModel();
        final AstroCoordinate astroCoordinates = new AstroCoordinate();
        for (Map<Field, String> iterDoc : response) {
            final FeatureDataModel feature = new FeatureDataModel();
            final Set<Map.Entry<Field, String>> columns = iterDoc.entrySet();
            double raResponse = Double.NaN;
            double decResponse = Double.NaN;
            for (Map.Entry<Field, String> column : columns) {
                final Field field = column.getKey();
                final net.ivoa.xml.votable.v1.DataType dataType = field.getDatatype();
                final String ucd = field.getUcd();
                final String value = iterDoc.get(field);
                if (Utility.isSet(value) && !value.isEmpty()) {
                    final Object responseDataType;
                    try {
                        responseDataType = Utility.getDataType(dataType, value);
                    } catch (NumberFormatException ex) {
                        //TO DO : need to parse for not a value number
                        LOG.log(Level.SEVERE, "No number has been provided for " + field.getName() +" - skip the attribute", ex);
                        continue;
                    }
                    final ConeSearchHealpix.ReservedWords ucdWord = ConeSearchHealpix.ReservedWords.find(ucd);
                    switch (ucdWord) {
                        case POS_EQ_RA_MAIN:
                            raResponse = Utility.parseRaVO(iterDoc, field);
                            break;
                        case POS_EQ_DEC_MAIN:
                            decResponse = Utility.parseDecVO(iterDoc, field);
                            break;
                        case ID_MAIN:
                            feature.setIdentifier(iterDoc.get(field));
                            break;
                        default:
                            feature.addProperty(field.getName(), responseDataType);
                            break;
                    }
                }
            }
            switch (coordinateSystem) {
                case GALACTIC:
                    astroCoordinates.setRaAsDecimal(raResponse);
                    astroCoordinates.setDecAsDecimal(decResponse);
                    astroCoordinates.setCoordinateSystem(fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem.EQUATORIAL);
                    astroCoordinates.processTo(fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem.GALACTIC);
                    raResponse = astroCoordinates.getRaAsDecimal();
                    decResponse = astroCoordinates.getDecAsDecimal();
                    break;
                case EQUATORIAL:
                    break;
                default:
                    break;
            }
            feature.createGeometry(String.format("[%s,%s]", raResponse, decResponse), "Point");
            feature.createCrs(coordinateSystem.getCrs());
            if (isValid(feature)) {
                dataModel.addFeature(feature);
            } else {
                LOG.log(Level.SEVERE, "Feaure not valid - skip it", feature);
            }
        }
        return dataModel;
    }
    /**
     * Returns true when identifier is set.
     *
     * @param feature data response
     * @return true when identifier os set
     */
    private boolean isValid(final FeatureDataModel feature) {
        return !feature.getIdentifier().isEmpty();
    }

    /**
     * Returns the coordinate system.
     * @return the coordinateSystem
     */
    protected final AstroCoordinate.CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Sets the coordinate system.
     * @param coordinateSystemVal the coordinateSystem to set
     */
    protected final void setCoordinateSystem(final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        this.coordinateSystem = coordinateSystemVal;
    }

    /**
     * Returns the response.
     * @return the response
     */
    protected final List<Map<Field, String>> getResponse() {
        return response;
    }

    /**
     * Sets the response.
     * @param responseVal the response to set
     */
    protected final void setResponse(final List<Map<Field, String>> responseVal) {
        this.response = responseVal;
    }
}
