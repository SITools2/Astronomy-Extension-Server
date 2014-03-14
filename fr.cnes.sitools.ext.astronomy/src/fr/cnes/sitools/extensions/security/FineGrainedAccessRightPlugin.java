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

import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.plugins.filters.model.FilterModel;
import fr.cnes.sitools.plugins.filters.model.FilterParameter;
import fr.cnes.sitools.plugins.filters.model.FilterParameterType;
import fr.cnes.sitools.util.Util;

/**
 * Filters the access by delegating the responsability to an external database
 * <p>
 * A data storage is a directory from the file system that is put online on the web.
 *
 * When the administrator configures a data storage, all files in this data storage are
 * available. This extension allows to configure the file to access by delegating
 * the access configuration to a SQL database.<br/>
 *
 * To make it works, the database must contain two columns at least :<br/>
 * <ul>
 * <li>one for filename</li>
 * <li>another for the profiles.</li>
 * </ul>
 * The profiles must be an array of string.
 * <br/>
 * <img src="../../../../../images/FineGrainedAccessRightPlugin.png"/>
 * <br/>
 * Here is an example on how to insert data in the access right table:
 * <pre>
 * <code>
 * INSERT INTO "accessRight"( filename, profile) VALUES
 * ('Images/Webcam/2012-12-29-193736.jpg', '{"Administrator"}');
 * </code>
 * </pre>
 * </p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml
 * title Access to files by delegating\n the logic to the database
 * (use a data storage) as (datastorage)
 * (check access rights) as (accessRight)
 * (configure the\n datastorage filter) as (filter)
 * User -> (datastorage)
 * (datastorage) ..> (accessRight)
 * Admin -> (datastorage)
 * Admin -> (filter)
 * note "The data source, schema name,\n table name, columns name\n are configured." as Note
 * (filter) .. Note
 * Note .. (accessRight)
 * note "database with two columns :\n filename and profile[]" as Note1
 * (accessRight) .. Note1
 * @enduml
 */
public class FineGrainedAccessRightPlugin extends FilterModel {
    /**
     * Data source name that contains the table responsibles for the access rights.
     */
    public static final String DATASOURCE = "dataSourceName";
    /**
     * Table name that contains filenameColumn and profilesColumns.
     */
    public static final String TABLE = "tableName";
    /**
     * Filename column.
     */
    public static final String FILENAME = "filenameColumn";
    /**
     * Profiles column.
     */
    public static final String PROFILES = "profilesColumn";
    /**
     * Schema name.
     */
    public static final String SCHEMA = "schema";
    /**
     * Empty constructor.
     */
    public FineGrainedAccessRightPlugin() {
        super();
        setName("FineGrainedAccessRightExtension");
        setDescription("Provides a customizable datastorage directory authorizer by setting the responsibility of the access to an external database.");
        setClassAuthor("Jean-Christophe Malapert");
        setClassOwner("CNES");
        setClassVersion("1.0");
        setClassName(fr.cnes.sitools.extensions.security.FineGrainedAccessRightPlugin.class.getName());
        setFilterClassName(fr.cnes.sitools.extensions.security.FineGrainedAccessRight.class.getName());
        setConfigurationParameters();
    }
    /**
     * Sets the configuration parameters for the administrator.
     */
    private void setConfigurationParameters() {
        final FilterParameter dataSource = new FilterParameter(
                DATASOURCE,
                "Data source name where the table that contains access right is located",
                FilterParameterType.PARAMETER_INTERN);
        dataSource.setValueType("xs:string");
        addParam(dataSource);
        final FilterParameter table = new FilterParameter(
                TABLE,
                "Table name that contains the access rights.",
                FilterParameterType.PARAMETER_INTERN);
        table.setValueType("xs:string");
        addParam(table);

        final FilterParameter schema = new FilterParameter(
                SCHEMA,
                "Sets the schema name if PostGreSQL is used.",
                FilterParameterType.PARAMETER_INTERN);
        schema.setValueType("xs:string");
        addParam(schema);

        final FilterParameter filenameColumn = new FilterParameter(
                FILENAME,
                "Sets the column name that contains the filename.",
                FilterParameterType.PARAMETER_INTERN);
        filenameColumn.setValueType("xs:string");
        addParam(filenameColumn);

        final FilterParameter profilesColumn = new FilterParameter(
                PROFILES,
                "Sets the profiles column name that contains the profiles.",
                FilterParameterType.PARAMETER_INTERN);
        profilesColumn.setValueType("xs:string");
        addParam(profilesColumn);
    }

    @Override
    public final Validator<FilterModel> getValidator() {
        return new Validator<FilterModel>() {
            @Override
            public Set<ConstraintViolation> validate(final FilterModel item) {
                final Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
                final Map<String, FilterParameter> params = item.getParametersMap();
                String value = params.get(DATASOURCE).getValue();
                if (Util.isEmpty(value)) {
                  final ConstraintViolation constraint = new ConstraintViolation();
                  constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                  constraint.setMessage("A datasource must be set");
                  constraint.setValueName(DATASOURCE);
                  constraintList.add(constraint);
                }
                value = params.get(TABLE).getValue();
                if (Util.isEmpty(value)) {
                  final ConstraintViolation constraint = new ConstraintViolation();
                  constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                  constraint.setMessage("A table must be set");
                  constraint.setValueName(TABLE);
                  constraintList.add(constraint);
                }
                value = params.get(FILENAME).getValue();
                if (Util.isEmpty(value)) {
                  final ConstraintViolation constraint = new ConstraintViolation();
                  constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                  constraint.setMessage("A filename column name must be set");
                  constraint.setValueName(FILENAME);
                  constraintList.add(constraint);
                }
                value = params.get(PROFILES).getValue();
                if (Util.isEmpty(value)) {
                  final ConstraintViolation constraint = new ConstraintViolation();
                  constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                  constraint.setMessage("A profiles column name must be set");
                  constraint.setValueName(PROFILES);
                  constraintList.add(constraint);
                }
                return constraintList;
            }
        };
    }
}
