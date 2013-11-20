/**
 * *****************************************************************************
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
 *****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.astro.vo.conesearch.ConeSearchProtocolLibrary;
import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.plugins.resources.model.DataSetSelectionType;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.plugins.resources.model.ResourceParameter;
import fr.cnes.sitools.plugins.resources.model.ResourceParameterType;
import fr.cnes.sitools.util.Util;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Configures the cone search service.
 *
 * <p>
 * This service searches in a dataset the records that match with cone search on
 * the sphere. To configure this services, a set of information must be provided
 * by the administrator: <ul> <li>dictionary name that maps the Virtual
 * observatory concepts with attributes of the dataset</li> <li>the verbosity of
 * the response (see IVOA document about the Cone Search Protocol</li> <li>x, y,
 * z attributes that are used by the search algorithm</li> <li>general
 * information that is displayed in the response</li> <li>the maximum search
 * radius that is allowed</li> <li>the number of maximum records that is
 * allowed</li> </ul> </p>
 *
 * <p>
 * This service answers to the following scenario:<br/>
 * As user, I want to search on my data using ConeSearch Protocol standard in
 * order to retrieve the result as a VOTable and to use it in Virtual
 * Observatory tool.
 * <br/>
 * <img src="../../../../../../images/CSP-usecase.png"/>
 * <br/>
 * In addition, this plugin has several dependencies with different
 * components:<br/>
 * <img src="../../../../../../images/ConeSearchResourcePlugin.png"/>
 * <br/>
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml CSP-usecase.png title Publishing data through CSP User --> (CSP
 * service) : requests Admin --> (CSP service) : adds and configures the CSP
 * service from the dataset. (CSP service) .. (dataset) : uses
 * @enduml
 * @startuml package "Services" { HTTP - [ConeSearchResourcePlugin] } database
 * "Database" { frame "Data" { [myData] } } package "Dataset" { HTTP - [Dataset]
 * [VODictionary] } [ConeSearchResourcePlugin] --> [Dataset] [Dataset] -->
 * [myData] [Dataset] --> [VODictionary]
 * @enduml
 */
public class ConeSearchResourcePlugin extends ResourceModel {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(ConeSearchResourcePlugin.class.getName());
    /**
     * Maximum radius = 180 degrees.
     */
    private static final int MAX_RADIUS = 180;

    /**
     * Constructs the administration panel.
     */
    public ConeSearchResourcePlugin() {
        super();
        setClassAuthor("J-C Malapert");
        setClassOwner("CNES");
        setClassVersion("1.0");
        setName("Simple Cone Search Protocol");
        setDescription("This plugin provides an access to your dataset through the Simple Cone Search Protocol");
        setResourceClassName(fr.cnes.sitools.extensions.astro.resource.ConeSearchResource.class.getName());

        this.setApplicationClassName(DataSetApplication.class.getName());
        this.setDataSetSelection(DataSetSelectionType.NONE);
        setConfiguration();
        this.completeAttachUrlWith("/conesearch");
    }

    /**
     * Sets the configuration for the administrator.
     */
    private void setConfiguration() {
        final ResourceParameter dictionary = new ResourceParameter(ConeSearchProtocolLibrary.DICTIONARY,
                "Dictionary name that sets up the service", ResourceParameterType.PARAMETER_INTERN);
        dictionary.setValueType("xs:dictionary");
        addParam(dictionary);

        final ResourceParameter verb = new ResourceParameter(ConeSearchProtocolLibrary.VERB,
                "Verbosity determines how many columns are to be returned in the resulting table",
                ResourceParameterType.PARAMETER_INTERN);
        verb.setValueType("xs:enum[1, 2, 3]");
        verb.setValue("1");
        addParam(verb);

        final ResourceParameter xpos = new ResourceParameter(ConeSearchProtocolLibrary.X, "x position of the source point",
                ResourceParameterType.PARAMETER_INTERN);
        xpos.setValueType("xs:dataset.columnAlias");
        addParam(xpos);

        final ResourceParameter ypos = new ResourceParameter(ConeSearchProtocolLibrary.Y, "y position of the source point",
                ResourceParameterType.PARAMETER_INTERN);
        ypos.setValueType("xs:dataset.columnAlias");
        addParam(ypos);

        final ResourceParameter zpos = new ResourceParameter(ConeSearchProtocolLibrary.Z, "z position of the source point",
                ResourceParameterType.PARAMETER_INTERN);
        zpos.setValueType("xs:dataset.columnAlias");
        addParam(zpos);

        final ResourceParameter responsibleParty = new ResourceParameter(ConeSearchProtocolLibrary.RESPONSIBLE_PARTY,
                "The data provider's name and email", ResourceParameterType.PARAMETER_INTERN);
        addParam(responsibleParty);

        final ResourceParameter serviceName = new ResourceParameter(ConeSearchProtocolLibrary.SERVICE_NAME,
                "The name of the catalog served by the service, for example : IRSA.2MASS.ExtendedSources",
                ResourceParameterType.PARAMETER_INTERN);
        addParam(serviceName);

        final ResourceParameter description = new ResourceParameter(ConeSearchProtocolLibrary.DESCRIPTION,
                "A couple of paragraphs of text that describe the nature of the catalog and its wider context",
                ResourceParameterType.PARAMETER_INTERN);
        addParam(description);

        final ResourceParameter instrument = new ResourceParameter(ConeSearchProtocolLibrary.INSTRUMENT,
                "The instrument that made the observations, for example STScI.HST.WFPC2",
                ResourceParameterType.PARAMETER_INTERN);
        addParam(instrument);

        final ResourceParameter waveband = new ResourceParameter(ConeSearchProtocolLibrary.WAVEBAND,
                "The waveband of the observations", ResourceParameterType.PARAMETER_INTERN);
        final String waveBandEnum = "xs:enum-multiple[radio, millimeter, infrared, optical, ultraviolet, xray, gammaray]";
        waveband.setValueType(waveBandEnum);
        addParam(waveband);

        final ResourceParameter epoch = new ResourceParameter(ConeSearchProtocolLibrary.EPOCH,
                "The epoch of the observations, as a free-form string", ResourceParameterType.PARAMETER_INTERN);
        addParam(epoch);

        final ResourceParameter coverage = new ResourceParameter(ConeSearchProtocolLibrary.COVERAGE,
                "The coverage on the sky, as a free-form string", ResourceParameterType.PARAMETER_INTERN);
        addParam(coverage);

        final ResourceParameter maxSR = new ResourceParameter(
                ConeSearchProtocolLibrary.MAX_SR,
                "The largest search radius, given in decimal degrees, that will be accepted by the service without returning an error"
                + " condition. A value of 180.0 indicates that there is no restriction",
                ResourceParameterType.PARAMETER_INTERN);
        maxSR.setValue("180");
        addParam(maxSR);

        final ResourceParameter maxRecords = new ResourceParameter(ConeSearchProtocolLibrary.MAX_RECORDS,
                "The largest number of records that the service will return", ResourceParameterType.PARAMETER_INTERN);
        maxRecords.setValue("-1");
        addParam(maxRecords);

        final ResourceParameter verbosity = new ResourceParameter(ConeSearchProtocolLibrary.VERBOSITY,
                "True or false, depending on whether the service supports the VERB keyword in the request",
                ResourceParameterType.PARAMETER_INTERN);
        verbosity.setValueType("xs:enum[True, False]");
        verbosity.setValue("True");
        addParam(verbosity);
    }

    /**
     * Validates.
     *
     * @return error or warning
     */
    @Override
    public final Validator<ResourceModel> getValidator() {
        return new Validator<ResourceModel>() {
            @Override
            public Set<ConstraintViolation> validate(final ResourceModel item) {
                final Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
                final Map<String, ResourceParameter> params = item.getParametersMap();

                final ResourceParameter maxSr = params.get(ConeSearchProtocolLibrary.MAX_SR);
                if (!Util.isNotEmpty(maxSr.getValue())
                        || !(Double.valueOf(maxSr.getValue()) > 0 && Double.valueOf(maxSr.getValue()) <= MAX_RADIUS)) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("The SR value must be set and a positive value inferior to 180");
                    constraint.setValueName(ConeSearchProtocolLibrary.MAX_SR);
                    constraintList.add(constraint);
                }

                final ResourceParameter dictionary = params.get(ConeSearchProtocolLibrary.DICTIONARY);
                if (!Util.isNotEmpty(dictionary.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("A reference to a VO dictionary must be set");
                    constraint.setValueName(ConeSearchProtocolLibrary.DICTIONARY);
                    constraintList.add(constraint);
                }

                final ResourceParameter verb = params.get(ConeSearchProtocolLibrary.VERB);
                if (!Util.isNotEmpty(verb.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("A verbosity level (1,2,3) must be set");
                    constraint.setValueName(ConeSearchProtocolLibrary.VERB);
                    constraintList.add(constraint);
                }

                final ResourceParameter verbosity = params.get(ConeSearchProtocolLibrary.VERBOSITY);
                if (!Util.isNotEmpty(verbosity.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("Verbosity (true, false) must be set");
                    constraint.setValueName(ConeSearchProtocolLibrary.VERBOSITY);
                    constraintList.add(constraint);
                }

                final ResourceParameter xCol = params.get(ConeSearchProtocolLibrary.X);
                if (!Util.isNotEmpty(xCol.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("A reference to X attribute must be set");
                    constraint.setValueName(ConeSearchProtocolLibrary.X);
                    constraintList.add(constraint);
                }

                final ResourceParameter yCol = params.get(ConeSearchProtocolLibrary.Y);
                if (!Util.isNotEmpty(yCol.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("A reference to Y attribute must be set");
                    constraint.setValueName(ConeSearchProtocolLibrary.Y);
                    constraintList.add(constraint);
                }

                final ResourceParameter zCol = params.get(ConeSearchProtocolLibrary.Z);
                if (!Util.isNotEmpty(zCol.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("A reference to Z attribute must be set");
                    constraint.setValueName(ConeSearchProtocolLibrary.Z);
                    constraintList.add(constraint);
                }

                final ResourceParameter maxRecords = params.get(ConeSearchProtocolLibrary.MAX_RECORDS);
                if (!Util.isNotEmpty(maxRecords.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("A max record must be defined. If set to -1 then maxRecords is equal to the number of records in the dataset");
                    constraint.setValueName(ConeSearchProtocolLibrary.MAX_RECORDS);
                    constraintList.add(constraint);
                }

                return constraintList;
            }
        };
    }
}
