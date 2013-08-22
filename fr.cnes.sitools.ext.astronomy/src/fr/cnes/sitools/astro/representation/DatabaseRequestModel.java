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
package fr.cnes.sitools.astro.representation;

import fr.cnes.sitools.common.exception.SitoolsException;
import fr.cnes.sitools.dataset.converter.business.ConverterChained;
import fr.cnes.sitools.dataset.database.DatabaseRequest;
import fr.cnes.sitools.datasource.jdbc.model.AttributeValue;
import fr.cnes.sitools.datasource.jdbc.model.Record;
import fr.cnes.sitools.util.Util;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class makes easier the use of DatabaseRequest object in Freemarker.<br/> The
 * connection to the database is closed in this class.
 *
 * <p><pre>
 * Example usage:
 *
 * in your Java source: 
 * <code>TemplateSequenceModel rows = new DatabaseRequestModel(resultSet); 
 * root.put("rows",rows);</code>
 *
 * in your .ftl 
 * <code><#list rows as row> ${row["column1"]} - ${row["column2"]}
 * <#/list></code>
 * </pre>
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class DatabaseRequestModel implements TemplateSequenceModel {
  /**
   * Logger.
   */
    private static final Logger LOG = Logger.getLogger(DatabaseRequestModel.class.getName());
    /**
     * DB Result set.
     */
    private final DatabaseRequest request;
    /**
     * SITools2 converters.
     */
    private final transient ConverterChained converterChained;
    /**
     * Number of rows.
     */
    private transient int size;

    /**
     * Creates a DatabaseRequest Model instance.
     *
     * @param rsVal database connection
     * @param converterChainedVal the converter object
     */
    public DatabaseRequestModel(final DatabaseRequest rsVal, final ConverterChained converterChainedVal) {
        this.request = rsVal;
        this.converterChained = converterChainedVal;
        this.size = request.getTotalCount();

        // we need to close the connection here
        // otherwise the connection will not be free.
        if (this.size == 0) {
            try {
                this.request.close();
            } catch (SitoolsException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Sets the number of rows coming from the database server.
     * @param sizeVal number of rows
     */
    public final void setSize(final int sizeVal) {
        this.size = sizeVal;
    }

    /**
     * Returns a row of the template model.
     * @param rowNumber row number
     * @return the template model
     * @throws TemplateModelException Exception
     */
    @Override
    public final TemplateModel get(final int rowNumber) throws TemplateModelException {
        TemplateModel model = null;
        try {
            final boolean nextResult = request.nextResult();
            if (nextResult) {
                model = new Row(request, this.converterChained);
            }
        } catch (SitoolsException ex) {
            try {
                this.request.close();
            } catch (SitoolsException ex1) {
                LOG.log(Level.WARNING, null, ex1);
            } 
            throw new TemplateModelException(ex);            
        }

        // this is the last record and we need to close the connection
        if (rowNumber == this.size() - 1) {
            try {
                this.request.close();
            } catch (SitoolsException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        return model;
    }

    /**
     * Returns the number of rows.
     * @return the number of rows
     */
    @Override
    public final int size()  {
        return this.size;
    }

    /**
     * Wraps a record from the database to a Row object.
     */
    public class Row implements TemplateHashModel {

        /**
         * Database result set.
         */
        private final DatabaseRequest resultSet;
        /**
         * Mapping with database columns.
         */
        private final Map map;
        /**
         * SITools2 converter.
         */
        private final ConverterChained converterChained;

        /**
         * Contructs a new row.
         * @param rsVal resultSet
         * @param converterChainedVal SITools2 converter
         * @throws SitoolsException Exception
         */
        public Row(final DatabaseRequest rsVal, final ConverterChained converterChainedVal) throws SitoolsException {
            this.resultSet = rsVal;
            this.converterChained = converterChainedVal;
            this.map = new HashMap();
            init();
        }

        /**
         * Creates a HashMap of the different attributes from a record. 
         * 
         * <p>
         * The key of the hash map is the columnAlias
         * </p>
         * @throws SitoolsException Exception
         */
        private void init() throws SitoolsException {
            Record record;
            if (Util.isSet(converterChained)) {
                record = converterChained.getConversionOf(this.resultSet.getRecord());
            } else {
                record = this.resultSet.getRecord();
            }
            final List<AttributeValue> attValueList = record.getAttributeValues();
            for (AttributeValue iter : attValueList) {
                this.map.put(iter.getName(), iter.getValue());
            }
        }

        /**
         * Returns the value of column of a row as a template Model.
         * @param columnAlias database column alias
         * @return the value of column of a row as a template Model
         * @throws TemplateModelException Exception
         */
        @Override
        public final TemplateModel get(final String columnAlias) throws TemplateModelException {
            return new SimpleScalar(String.valueOf(this.map.get(columnAlias)));            
        }

        /**
         * Checks if it is empty.
         * @return true if there is no next row otherwise false
         * @throws TemplateModelException Exception
         */
        @Override
        public final boolean isEmpty() throws TemplateModelException {
            return (this.resultSet == null);
        }
    }    
}
