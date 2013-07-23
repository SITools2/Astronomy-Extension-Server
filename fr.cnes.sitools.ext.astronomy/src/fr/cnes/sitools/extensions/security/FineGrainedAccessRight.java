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
package fr.cnes.sitools.extensions.security;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.common.application.ContextAttributes;
import fr.cnes.sitools.datasource.jdbc.business.SitoolsSQLDataSourceFactory;
import fr.cnes.sitools.datasource.jdbc.model.JDBCDataSource;
import fr.cnes.sitools.plugins.filters.model.FilterModel;
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.util.RIAPUtils;
import fr.cnes.sitools.util.Util;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Authorizer;
import org.restlet.security.Role;

/**
 * Filters the access by delegating the responsability to an extrernal database.
 * 
 * <p>
 * Business class implementing the FineGrainedAccessRight plugin.
 * </p>
 * 
 * <br/>
 * <img src="../../../../../images/FineGrainedAccessRight.png"/>
 * <br/>
 * @see FineGrainedAccessRightPlugin The plugin that calls this class.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml
 * FineGrainedAccessRightPlugin o-- FineGrainedAccessRight : attachs
 * 
 * FineGrainedAccessRight : boolean authorize(final Request request, final Response response)
 * 
 * FineGrainedAccessRightPlugin : setConfigurationParameters()
 * FineGrainedAccessRightPlugin : Validator<FilterModel> getValidator()
 * @enduml
 */
public class FineGrainedAccessRight extends Authorizer {

    /**
     * Application data model.
     */
    private final transient FilterModel filterModel;
    
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(FineGrainedAccessRight.class.getName());
    
    /**
     * Constructor.
     * @param context context
     */
    public FineGrainedAccessRight(final Context context) {
        this.filterModel = (FilterModel) context.getAttributes().get("FILTER_MODEL");
    }

    /**
     * Returns the data source ID from its name.
     * @param dsName data source name
     * @param context context
     * @return the data source ID
     */
    private String findDataSourceId(final String dsName, final Context context) {
        final SitoolsSettings sitoolsSettings = (SitoolsSettings) context.getAttributes().get(ContextAttributes.SETTINGS);
        final String dataSourceUrl = sitoolsSettings.getString(Consts.APP_DATASOURCES_URL);
        final JDBCDataSource dataSource = RIAPUtils.getObjectFromName(dataSourceUrl, dsName, getApplication().getContext());
        return dataSource.getId();
    }

    /**
     * Returns the prepare statement String.
     * @param table table name
     * @param schema schema name if PostgreSQL is used
     * @param filename filename column
     * @param profile profile column
     * @param roles the different roles of a user
     * @return the prepare statement String
     */
    private String prepareStatementString(final String table, final String schema, final String filename, final String profile, final List<Role> roles) {
        final String dsSchema = (Util.isNotEmpty(schema)) ? schema + "." : "";
        StringBuilder prepareSt = new StringBuilder("SELECT count(*) as result FROM " + dsSchema + "\"" + table + "\" WHERE " + filename + " = ? AND ");
        prepareSt = prepareSt.append("( ");
        for (final Iterator<Role> it = roles.iterator(); it.hasNext();) {
            it.next();            
            prepareSt = prepareSt.append("? = ANY(").append(profile).append(")");
            if (it.hasNext()) {
                prepareSt = prepareSt.append(" OR ");
            }
        }
        prepareSt = prepareSt.append(" )");
        return prepareSt.toString();
    }

    /**
     * Returns the filename from the request.
     * 
     * <p>
     * The filename is given in the request after the application URI of the data storage.
     * </p>
     * @param request request
     * @return the filename
     */
    private String getFilename(final Request request) {
        final String filename = request.getResourceRef().getRemainingPart(true);
        return filename.substring(1, filename.length()); // remove the "/"        
    }
    
    /**
     * Sets the SQL parameters to the prepare statement.
     * @param stmt prepare statement
     * @param roles roles of the user
     * @param filename filename
     * @throws SQLException SQL exception
     */
    private void setSqlParameters(final PreparedStatement stmt, final List<Role> roles, final String filename) throws SQLException {
        stmt.setString(1, filename);        
        int sqlParameterIndex = 2;
        for (Role role : roles) {
            stmt.setString(sqlParameterIndex++, role.getName());
        }        
    }

    @Override
    public final boolean authorize(final Request request, final Response response) {
        boolean responseAuthorize = false;
        final Context context = getApplication().getContext();
        final String dsName = this.filterModel.getParameterByName(FineGrainedAccessRightPlugin.DATASOURCE).getValue();
        final String dsTable = this.filterModel.getParameterByName(FineGrainedAccessRightPlugin.TABLE).getValue();
        final String dsColFilename = this.filterModel.getParameterByName(FineGrainedAccessRightPlugin.FILENAME).getValue();
        final String dsColProfile = this.filterModel.getParameterByName(FineGrainedAccessRightPlugin.PROFILES).getValue();
        final String dsSchema = this.filterModel.getParameterByName(FineGrainedAccessRightPlugin.SCHEMA).getValue();
        final String dsId = findDataSourceId(dsName, context);
        final String filename = getFilename(request);
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            final List<Role> roles = request.getClientInfo().getRoles();
            conn = SitoolsSQLDataSourceFactory.getDataSource(dsId).getConnection();
            stmt = conn.prepareStatement(prepareStatementString(dsTable, dsSchema, dsColFilename, dsColProfile, roles));
            setSqlParameters(stmt, roles, filename);
            final ResultSet resultSet = stmt.executeQuery();
            if (!resultSet.next()) {
                resultSet.close();
                throw new SQLException("SQL syntax is wrong");
            }
            final int nbReturned = resultSet.getInt("result");
            resultSet.close();
            if (nbReturned > 1) {
                throw new SQLException("The table structure of " + dsTable + " is not the expected table structure");
            } else if (nbReturned == 1) {
                responseAuthorize = true;
            } else {
                responseAuthorize = false;
            }
        } catch (SQLException ex) {
            LOG.log(Level.SEVERE, null, ex);
            responseAuthorize = false;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        }
        return responseAuthorize;
    }
}
