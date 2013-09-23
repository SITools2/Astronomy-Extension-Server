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
 * Library implementing the Cone Search Protocol.
 *
 * @author Jean-Christophe Malapert
 */
public class ConeSearchProtocolLibrary {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(ConeSearchProtocolLibrary.class.getName());
    /**
     * RA min.
     */
    public static final double RA_MIN = 0.;
    /**
     * RA max.
     */
    public static final double RA_MAX = 360.;
    /**
     * Dec min.
     */
    public static final double DEC_MIN = -90.;
    /**
     * Dec max.
     */
    public static final double DEC_MAX = 90.;
    /**
     * dico.
     */
    public static final String DICTIONARY = "PARAM_Dictionary";
    /**
     * RA.
     */
    public static final String RA = "RA";
    /**
     * Dec.
     */
    public static final String DEC = "DEC";
    /**
     * radius.
     */
    public static final String SR = "SR";
    /**
     * verbosity.
     */
    public static final String VERB = "VERB";
    /**
     * x column.
     */
    public static final String X = "COLUMN_X";
    /**
     * y column.
     */
    public static final String Y = "COLUMN_Y";
    /**
     * z column.
     */
    public static final String Z = "COLUMN_Z";
    /**
     * Responsible party.
     */
    public static final String RESPONSIBLE_PARTY = "METADATA_RESPONSIBLE_PARTY";
    /**
     * service name.
     */
    public static final String SERVICE_NAME = "METADATA_SERVICE_NAME";
    /**
     * description.
     */
    public static final String DESCRIPTION = "METADATA_DESCRIPTION";
    /**
     * instrument.
     */
    public static final String INSTRUMENT = "METADATA_INSTRUMENT";
    /**
     * waveband.
     */
    public static final String WAVEBAND = "METADATA_WAVEBAND";
    /**
     * epoch.
     */
    public static final String EPOCH = "METADATA_EPOCH";
    /**
     * coverage.
     */
    public static final String COVERAGE = "METADATA_COVERAGE";
    /**
     * max sr.
     */
    public static final String MAX_SR = "METADATA_MAX_SR";
    /**
     * max records.
     */
    public static final String MAX_RECORDS = "METADATA_MAX_RECORDS";
    /**
     * verbosity.
     */
    public static final String VERBOSITY = "METADATA_VERBOSITY";
    /**
     * ucd concepts.
     */
    public static final List REQUIRED_UCD_CONCEPTS = Arrays.asList("ID_MAIN", "POS_EQ_RA_MAIN", "POS_EQ_DEC_MAIN");
    /**
     * dataset application.
     */
    private final transient DataSetApplication datasetApp;
    /**
     * resource model.
     */
    private final transient ResourceModel resourceModel;
    /**
     * request.
     */
    private final transient Request request;
    /**
     * context.
     */
    private final transient Context context;

    /**
     * Constructor.
     *
     * @param datasetAppVal Dataset Application
     * @param resourceModelVal Data model
     * @param requestVal Request
     * @param contextVal Context
     */
    public ConeSearchProtocolLibrary(final DataSetApplication datasetAppVal, final ResourceModel resourceModelVal, final Request requestVal,
            final Context contextVal) {
        this.datasetApp = datasetAppVal;
        this.resourceModel = resourceModelVal;
        this.request = requestVal;
        this.context = contextVal;
    }

    /**
     * Fill data Model that will be used in the template.
     *
     * @return data model for the template
     */
    private Map fillDataModel() {
        // init
        Map dataModel;
        final double maxSr = Double.valueOf(this.resourceModel.getParameterByName(ConeSearchProtocolLibrary.MAX_SR).getValue());
        final boolean verbosity = Boolean.valueOf(this.resourceModel.getParameterByName(ConeSearchProtocolLibrary.VERBOSITY)
                .getValue());
        final int verb = Integer.valueOf(this.resourceModel.getParameterByName(ConeSearchProtocolLibrary.VERB).getValue());

        // Handling input parameters
        final ConeSearchDataModelInterface inputParameters = new ConeSearchInputParameters(this.datasetApp, this.request,
                this.context, maxSr, verbosity, verb);

        // data model response
        if (inputParameters.getDataModel().containsKey("infos")) {
            dataModel = inputParameters.getDataModel();
        } else {
            final ConeSearchDataModelInterface response = new ConeSearchResponse((ConeSearchInputParameters) inputParameters,
                    this.resourceModel);
            dataModel = response.getDataModel();
        }
        return dataModel;
    }

    /**
     * VOTable response.
     *
     * @return VOTable response
     */
    public final VOTableRepresentation getResponse() {
        final Map dataModel = fillDataModel();
        return new VOTableRepresentation(dataModel);
    }
}
