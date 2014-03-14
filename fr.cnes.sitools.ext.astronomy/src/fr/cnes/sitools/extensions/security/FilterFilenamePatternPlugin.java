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
package fr.cnes.sitools.extensions.security;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.plugins.filters.model.FilterModel;
import fr.cnes.sitools.plugins.filters.model.FilterParameter;
import fr.cnes.sitools.plugins.filters.model.FilterParameterType;
import fr.cnes.sitools.util.Util;

/**
 * Filters the access by a pattern.
 *
 * <p>
 * A data storage is a directory from the file system that is put online on the
 * web.
 *
 * When the administrator configures a data storage, all files in this data
 * storage are available. This extension allows to configure the file to access
 * by the use of a pattern.
 * <br/>
 * <img src="../../../../../images/FilterFilenamePatternPlugin.png"/>
 * <br/>
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 *
 * @startuml title Access to files by matching a pattern in the URL (use a data
 * storage) as (datastorage) (check the pattern) as (accessRight) (configure
 * the\n datastorage filter) as (filter) User -> (datastorage) (datastorage) ..>
 * (accessRight) Admin -> (datastorage) Admin -> (filter) note "The pattern is
 * configured." as Note (filter) .. Note Note .. (accessRight)
 * @enduml
 */
public class FilterFilenamePatternPlugin extends FilterModel {

    /**
     * Input keyword that checks the pattern.
     */
    public static final String PATTERN_KEYWORD = "pattern";

    /**
     * Constructs a filter.
     */
    public FilterFilenamePatternPlugin() {
        super();
        setName("FilterByFilenameExtension");
        setDescription("Customizable datastorage directory authorizer by filename extension. Give access to the files matching the pattern.");
        setClassAuthor("Jean-Christophe Malapert");
        setClassOwner("CNES");
        setClassVersion("1.0");
        setClassName(fr.cnes.sitools.extensions.security.FilterFilenamePatternPlugin.class.getName());
        setFilterClassName(fr.cnes.sitools.extensions.security.FilterFilenamePattern.class.getName());
        setConfiguration();
    }

    /**
     * Sets the configuration parameters for the administrator.
     */
    private void setConfiguration() {
        final FilterParameter pattern = new FilterParameter(FilterFilenamePatternPlugin.PATTERN_KEYWORD, "pattern to match", FilterParameterType.PARAMETER_INTERN);
        pattern.setValue("(.*\\.fits)|(.*\\.txt)");
        pattern.setValueType("xs:string");
        addParam(pattern);
    }

    @Override
    public final Validator<FilterModel> getValidator() {
        return new Validator<FilterModel>() {
            @Override
            public Set<ConstraintViolation> validate(final FilterModel item) {
                final Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
                final Map<String, FilterParameter> params = item.getParametersMap();
                final String value = params.get(PATTERN_KEYWORD).getValue();
                if (Util.isEmpty(value)) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setMessage("A pattern must be set");
                    constraint.setValueName(FilterFilenamePatternPlugin.PATTERN_KEYWORD);
                    constraintList.add(constraint);
                } else {
                    try {
                        Pattern.compile(value);
                    } catch (PatternSyntaxException exception) {
                        final ConstraintViolation constraint = new ConstraintViolation();
                        constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                        constraint.setMessage("the pattern is not valid.");
                        constraint.setValueName(FilterFilenamePatternPlugin.PATTERN_KEYWORD);
                        constraintList.add(constraint);
                    }
                }
                return constraintList;
            }
        };
    }
}
