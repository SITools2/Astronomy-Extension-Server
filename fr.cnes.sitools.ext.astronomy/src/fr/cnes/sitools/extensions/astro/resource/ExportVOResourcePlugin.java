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
package fr.cnes.sitools.extensions.astro.resource;

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
 * Configures the VO export service.
 *
 * <p>
 * This service export a dataset as a VOTable. </p>
 *
 * <p>
 * This service answers to the following scenario:<br/>
 * As user, I want to select rows in my dataset and export them as a VOTable
 * file in order to use my exported data in a Virtual Obervatory tool.
 * <br/>
 * <img src="../../../../../../images/VOExport-usecase.png"/>
 * <br/>
 * In addition, this plugin has several dependencies with different
 * components:<br/>
 * <img src="../../../../../../images/ExportVOResourcePlugin.png"/>
 * <br/>
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml VOExport-usecase.png title Exporting data through VOTable User -->
 * (VO export service) : requests Admin --> (VO export service) : adds and
 * configures the VO export service from the dataset. (VO export service) ..
 * (dataset) : uses
 * @enduml
 * @startuml package "Services" { HTTP - [ExportVOResourcePlugin] } database
 * "Database" { frame "Data" { [myData] } } package "Dataset" { HTTP - [Dataset]
 * [VODictionary] } [ExportVOResourcePlugin] --> [Dataset] [Dataset] -->
 * [myData] [Dataset] --> [VODictionary]
 * @enduml
 */
public class ExportVOResourcePlugin extends ResourceModel {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(ExportVOResourcePlugin.class.getName());
    /**
     * Dictionary parameter that is used in the administration panel.
     */
    public static final String DICTIONARY = "Dictionary";
    /**
     * Description parameter that is dislayed in the VOTable.
     */
    public static final String DESCRIPTION = "Description";

    /**
     * Constructs the administration panel.
     */
    public ExportVOResourcePlugin() {
        super();
        setClassAuthor("J-C Malapert");
        setClassOwner("CNES");
        setClassVersion("1.0");
        setName("Export VOTable");
        setDescription("This service provides a VOTable export of selected records from a dataset.");
        setResourceClassName(fr.cnes.sitools.extensions.astro.resource.ExportVOResource.class.getName());
        this.setApplicationClassName(DataSetApplication.class.getName());
        this.setDataSetSelection(DataSetSelectionType.MULTIPLE);
        // this.getParameterByName("methods").setValue("GET");
        this.completeAttachUrlWith("/voexport");
        setConfiguration();
    }

    /**
     * Sets the configuration for the administrator.
     */
    private void setConfiguration() {
        final ResourceParameter dictionary = new ResourceParameter(ExportVOResourcePlugin.DICTIONARY,
                "Dictionary name that sets up the service", ResourceParameterType.PARAMETER_INTERN);
        dictionary.setValueType("xs:dictionary");
        addParam(dictionary);
        final ResourceParameter description = new ResourceParameter(ExportVOResourcePlugin.DESCRIPTION,
                "Description name in the VOTable", ResourceParameterType.PARAMETER_INTERN);
        addParam(description);
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
            public final Set<ConstraintViolation> validate(final ResourceModel item) {
                final Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
                final Map<String, ResourceParameter> params = item.getParametersMap();
                final ResourceParameter dico = params.get(ExportVOResourcePlugin.DICTIONARY);
                final ResourceParameter url = params.get("url");
                final ResourceParameter description = params.get(ExportVOResourcePlugin.DESCRIPTION);

                if (!Util.isNotEmpty(description.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.WARNING);
                    constraint
                            .setMessage("The description describes the VOTable. In the current configuration, you have not defined a description");
                    constraint.setValueName(ExportVOResourcePlugin.DESCRIPTION);
                    constraintList.add(constraint);
                }

                if (!Util.isNotEmpty(url.getValue()) || (Util.isNotEmpty(url.getValue()) && !url.getValue().startsWith("/"))) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("the Url must be set and to start by '/'");
                    constraint.setValueName("url");
                    constraintList.add(constraint);
                }

                if (!Util.isNotEmpty(dico.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("The dictionary for VOTable must be set");
                    constraint.setValueName(ExportVOResourcePlugin.DICTIONARY);
                    constraintList.add(constraint);
                }
                return constraintList;
            }
        };
    }
}
