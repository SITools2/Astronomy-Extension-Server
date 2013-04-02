/************************************************************************
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is part of SITools2.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *************************************************************************/
package fr.cnes.sitools.extensions.security;

import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.plugins.filters.model.FilterModel;
import fr.cnes.sitools.plugins.filters.model.FilterParameter;
import fr.cnes.sitools.plugins.filters.model.FilterParameterType;
import fr.cnes.sitools.util.Util;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Filters the access by a pattern.
 *
 * <p>
 * This service answers to the following scenario:<br/> As administrator, I want to make accessible some filenames through a pattern 
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class FilterFilenamePatternPlugin extends FilterModel {

  /**
   * Constructs a filter.
   */
  public FilterFilenamePatternPlugin() {
    super();
    setName("FilterByFilenameExtension");
    setDescription("Customizable datastorage directory authorizer by filename extension");
    setClassAuthor("Jean-Christophe Malapert");
    setClassOwner("CNES");
    setClassVersion("0.1");
    setClassName(fr.cnes.sitools.extensions.security.FilterFilenamePatternPlugin.class.getName());
    setFilterClassName(fr.cnes.sitools.extensions.security.FilterFilenamePattern.class.getName());

    /**
     * Parameter for the log directory
     */
    FilterParameter pattern = new FilterParameter("pattern", "pattern to match", FilterParameterType.PARAMETER_INTERN);
    pattern.setValue("(.*\\.fits)|(.*\\.txt)");
    pattern.setValueType("xs:string");
    addParam(pattern);
  }

  @Override
  public final Validator<FilterModel> getValidator() {
    return new Validator<FilterModel>() {
      @Override
      public Set<ConstraintViolation> validate(final FilterModel item) {
        Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
        Map<String, FilterParameter> params = item.getParametersMap();
        String value = params.get("pattern").getValue();
        if (!Util.isNotEmpty(value)) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("A pattern must be set");
          constraint.setValueName("pattern");
          constraintList.add(constraint);
        } else {
          try {
            Pattern.compile(value);
          } catch (PatternSyntaxException exception) {
            ConstraintViolation constraint = new ConstraintViolation();
            constraint.setLevel(ConstraintViolationLevel.CRITICAL);
            constraint.setMessage("the pattern is not valid.");
            constraint.setValueName("pattern");
            constraintList.add(constraint);
          }
        }
        return constraintList;
      }
    };
  }
}