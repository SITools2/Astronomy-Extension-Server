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

import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
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
 * Provides the MIZAR configuration file.
 *
 * <p>
 * This service answers to the following scenario:<br/>
 * As administrator, I want to change the GlobWeb configuration file by the
 * administration pannel in order to change easily the configuration file.
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class GlobWebResourcePlugin extends ResourceModel {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(GlobWebResourcePlugin.class.getName());
    /**
     * Conf parameter that has been used in the administration panel to select
     * the GlobWeb configuration file.
     */
    public static final String CONF_ADM = "conf";

    /**
     * Constructs the administation panel.
     */
    public GlobWebResourcePlugin() {
        super();
        setClassAuthor("J-C Malapert");
        setClassOwner("CNES");
        setClassVersion("1.0");
        setName("GlobWeb Server");
        setDescription("Provides the configuration file of the Globweb module");
        setDataSetSelection(DataSetSelectionType.NONE);
        setResourceClassName(fr.cnes.sitools.extensions.astro.resource.GlobWebResource.class.getName());
        setConfiguration();
        this.completeAttachUrlWith("/globWeb");
    }

    /**
     * Sets the configuration for the administrator.
     */
    private void setConfiguration() {
        final ResourceParameter configurationFile = new ResourceParameter(CONF_ADM, "Filename located in <root>/data/freemarker",
                ResourceParameterType.PARAMETER_USER_INPUT);
        configurationFile.setValueType("String");
        this.addParam(configurationFile);
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
                final ResourceParameter resourceParam = params.get(CONF_ADM);
                if (!Util.isNotEmpty(resourceParam.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("The configuration filename must be set");
                    constraint.setValueName("conf");
                    constraintList.add(constraint);
                }
                return constraintList;
            }
        };
    }
}
