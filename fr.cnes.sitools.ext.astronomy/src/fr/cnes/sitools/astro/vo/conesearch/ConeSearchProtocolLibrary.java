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
package fr.cnes.sitools.astro.vo.conesearch;

import fr.cnes.sitools.astro.representation.VOTableRepresentation;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.Request;

/**
 * Library implementing the Cone Search Protocol
 * 
 * @author Jean-Christophe Malapert
 */
public class ConeSearchProtocolLibrary {

  public static final double RA_MIN = 0.;
  public static final double RA_MAX = 360.;
  public static final double DEC_MIN = -90.;
  public static final double DEC_MAX = 90.;
  public static final String DICTIONARY = "PARAM_Dictionary";
  public static final String RA = "RA";
  public static final String DEC = "DEC";
  public static final String SR = "SR";
  public static final String VERB = "VERB";
  public static final String X = "COLUMN_X";
  public static final String Y = "COLUMN_Y";
  public static final String Z = "COLUMN_Z";
  public static final String RESPONSIBLE_PARTY = "METADATA_RESPONSIBLE_PARTY";
  public static final String SERVICE_NAME = "METADATA_SERVICE_NAME";
  public static final String DESCRIPTION = "METADATA_DESCRIPTION";
  public static final String INSTRUMENT = "METADATA_INSTRUMENT";
  public static final String WAVEBAND = "METADATA_WAVEBAND";
  public static final String EPOCH = "METADATA_EPOCH";
  public static final String COVERAGE = "METADATA_COVERAGE";
  public static final String MAX_SR = "METADATA_MAX_SR";
  public static final String MAX_RECORDS = "METADATA_MAX_RECORDS";
  public static final String VERBOSITY = "METADATA_VERBOSITY";
  public static final List REQUIRED_UCD_CONCEPTS = Arrays.asList("ID_MAIN", "POS_EQ_RA_MAIN", "POS_EQ_DEC_MAIN");

  private final DataSetApplication datasetApp;
  private final ResourceModel resourceModel;
  private final Request request;
  private final Context context;

  /**
   * Constructor
   * 
   * @param datasetApp
   *          Dataset Application
   * @param resourceModel
   *          Data model
   * @param request
   *          Request
   * @param context
   *          Context
   */
  public ConeSearchProtocolLibrary(final DataSetApplication datasetApp, final ResourceModel resourceModel, final Request request,
      final Context context) {
    this.datasetApp = datasetApp;
    this.resourceModel = resourceModel;
    this.request = request;
    this.context = context;
  }

  /**
   * Fill data Model that will be used in the template
   * 
   * @return data model for the template
   */
  private Map fillDataModel() {
    // init
    Map dataModel;
    double maxSr = Double.valueOf(this.resourceModel.getParameterByName(ConeSearchProtocolLibrary.MAX_SR).getValue());
    boolean verbosity = Boolean.valueOf(this.resourceModel.getParameterByName(ConeSearchProtocolLibrary.VERBOSITY)
        .getValue());
    int verb = Integer.valueOf(this.resourceModel.getParameterByName(ConeSearchProtocolLibrary.VERB).getValue());

    // Handling input parameters
    ConeSearchDataModelInterface inputParameters = new ConeSearchInputParameters(this.datasetApp, this.request,
        this.context, maxSr, verbosity, verb);

    // data model response
    if (inputParameters.getDataModel().containsKey("infos")) {
      dataModel = inputParameters.getDataModel();
    }
    else {
      ConeSearchDataModelInterface response = new ConeSearchResponse((ConeSearchInputParameters) inputParameters,
          this.resourceModel);
      dataModel = response.getDataModel();
    }
    return dataModel;
  }

  /**
   * VOTable response
   * 
   * @return VOTable response
   */
  public VOTableRepresentation getResponse() {
    Map dataModel = fillDataModel();
    return new VOTableRepresentation(dataModel);
  }
    private static final Logger LOG = Logger.getLogger(ConeSearchProtocolLibrary.class.getName());
}
