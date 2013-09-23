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

import fr.cnes.sitools.extensions.astro.application.opensearch.datamodel.FeaturesDataModel;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Field;

/**
 * Transforms the response from the VO server to JSON.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class JsonDataModelDecorator extends VORequestDecorator {

    /**
     * Healpix coordinate system.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystem;
   /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(JsonDataModelDecorator.class.getName());
    /**
     * Constructor.
     * @param decorateVORequestVal VO response
     * @param coordinateSystemVal coordinate System of the output.
     */
    public JsonDataModelDecorator(final VORequestInterface decorateVORequestVal, final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        super(decorateVORequestVal);
        setCoordinateSystem(coordinateSystemVal);
    }
  @Override
    public final Object getOutput() {
        final Object object = super.getOutput();
        final List<Map<Field, String>> model = (List<Map<Field, String>>) object;
        final AbstractJsonDataModel jsonReader = DataModelFactory.jsonProcessor(model, getCoordinateSystem());
        return jsonReader.getDataModel();
    }
  /**
   * Computes the JSON data model from the server response.
   * @param model server response
   * @param coordinateSystem coordinate system
   * @return the JSON data model from the server response
   */
  public static FeaturesDataModel computeJsonDataModel(final List<Map<Field, String>> model, final AstroCoordinate.CoordinateSystem coordinateSystem) {
        final AbstractJsonDataModel jsonReader = DataModelFactory.jsonProcessor(model, coordinateSystem);
        return jsonReader.getDataModel();
  }
    /**
     * Returns the coordinate system of the output.
     * @return the coordinateSystem
     */
    protected final AstroCoordinate.CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }
    /**
     * Sets the coordinate system of the output.
     * @param coordinateSystemVal the coordinateSystem to set
     */
    protected final void setCoordinateSystem(final AstroCoordinate.CoordinateSystem coordinateSystemVal) {
        this.coordinateSystem = coordinateSystemVal;
    }
}
