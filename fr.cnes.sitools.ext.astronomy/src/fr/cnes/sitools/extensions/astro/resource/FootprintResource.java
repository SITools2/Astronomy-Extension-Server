/*******************************************************************************
 * Copyright 2011 2012 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * 
 * This directory is part of SITools2.
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

import cds.moc.HealpixMoc;
import fr.cnes.sitools.common.exception.SitoolsException;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.dataset.converter.business.ConverterChained;
import fr.cnes.sitools.dataset.database.DatabaseRequest;
import fr.cnes.sitools.dataset.database.DatabaseRequestFactory;
import fr.cnes.sitools.dataset.database.DatabaseRequestParameters;
import fr.cnes.sitools.dataset.database.common.DataSetExplorerUtil;
import fr.cnes.sitools.dataset.model.Column;
import fr.cnes.sitools.datasource.jdbc.model.AttributeValue;
import fr.cnes.sitools.datasource.jdbc.model.Record;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

/**
 * 
 * @author Jean-Christophe Malapert
 */
public class FootprintResource extends SitoolsParameterizedResource {

  private String featureType;
  private String search;
  private File directory;
  private static String filename = "Mox.txt";

    /**
     * Initialize
     */
    @Override
  public void doInit() {
    super.doInit();
    this.featureType = String.valueOf(this.getRequestAttributes().get("featureType"));
    this.search = String.valueOf(this.getRequestAttributes().get("search"));
    this.directory = new File(this.getModel().getParameterByName("CacheDirectory").getValue());
  }

    /**
     * Returns the footprint
     * @return the representation
     */
    @Get
  public Representation getFootprintResponse() {
    try {
      HealpixMoc moc = new HealpixMoc(new FileInputStream(directory + File.separator + filename), HealpixMoc.ASCII);
      if (this.featureType.isEmpty()) {
        return new FileRepresentation(directory + File.separator + filename, MediaType.APPLICATION_JSON);
      }
      else if (this.featureType.equals("coverage")) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("moc_coverage", pourcent(moc.getCoverage()));
        jsonObject.put("moc_resolution", (int) (moc.getAngularRes() * 6000) / 100. + " arcmin");
        return JSONRepresentation(jsonObject.toString());
      }
      else if (this.featureType.equals("intersect")) {
        // TO DO : search with query disc and box
        return new EmptyRepresentation();
      }
    }
    catch (Exception ex) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex.getMessage());
    }
    throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
  }

    /**
     * Creates a cache
     */
    @Put
  public void processCache() {
    // Get the datasetApplication
    DataSetApplication datasetApp = (DataSetApplication) getApplication();

    // Get the pipeline to convert the information
    ConverterChained converterChained = datasetApp.getConverterChained();

    // Get the dataset
    DataSetExplorerUtil dsExplorerUtil = new DataSetExplorerUtil((DataSetApplication) getApplication(), getRequest(),
        getContext());

    // Get query parameters and build the request
    DatabaseRequestParameters dbParams = dsExplorerUtil.getDatabaseParams();
    dbParams.setPaginationExtend(datasetApp.getDataSet().getNbRecords());
    final String raCol = this.getModel().getParameterByName("RA").getValue();
    final String decCol = this.getModel().getParameterByName("DEC").getValue();
    List<Column> columnsToQuery = getColumnsFromName(datasetApp, new String[] {raCol, decCol});
    columnsToQuery.addAll(getPrimaryKeys(datasetApp));
    dbParams.setSqlVisibleColumns(columnsToQuery);
    DatabaseRequest databaseRequest = DatabaseRequestFactory.getDatabaseRequest(dbParams);

    try {
      if (dbParams.getDistinct()) {
        databaseRequest.createDistinctRequest();
      }
      else {
        databaseRequest.createRequest();
      }

      while (databaseRequest.nextResult()) {
        Record record = databaseRequest.getRecord();
        String ra = String.valueOf(getValueFromKey(record, raCol));
        String dec = String.valueOf(getValueFromKey(record, decCol));
      }

    }
    catch (Exception ex) {
    }
    finally {
      try {
        databaseRequest.close();
      }
      catch (SitoolsException ex) {
        Logger.getLogger(FootprintResource.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

    /**
     * Delete a cache
     */
    @Delete
  public void deleteCache() {
  }

  private String pourcent(double d) {
    return (int) (1000 * d) / 100. + "%";
  }

  /**
   * Get columns from a list of column name
   * 
   * @param datasetApp
   *          dataset Application
   * @param columnsName
   *          columns name
   * @return Returns the columns from the list of columns name
   */
  private List<Column> getColumnsFromName(DataSetApplication datasetApp, String[] columnsName) {
    List<Column> columnsKey = new ArrayList<Column>();
    List<Column> columns = datasetApp.getDataSet().getColumnModel();
    for (Column columnIter : columns) {
      for (int i = 0; i < columnsName.length; i++) {
        if (columnIter.getColumnAlias().equals(columnsName[i])) {
          columnsKey.add(columnIter);
        }
      }
    }
    return columnsKey;
  }

  private List<Column> getPrimaryKeys(DataSetApplication datasetApp) {
    List<Column> columnsKey = new ArrayList<Column>();
    List<Column> columns = datasetApp.getDataSet().getColumnModel();
    for (Column columnIter : columns) {
      if (columnIter.isPrimaryKey()) {
        columnsKey.add(columnIter);
      }
    }
    return columnsKey;
  }

  private Object getValueFromKey(Record record, String key) {
    Object value = null;
    List<AttributeValue> attributes = record.getAttributeValues();
    for (AttributeValue attributeIter : attributes) {
      if (attributeIter.getName().equals(key)) {
        value = attributeIter.getValue();
        break;
      }
    }
    return value;
  }

  private Representation JSONRepresentation(String content) {
    return new StringRepresentation(content, MediaType.APPLICATION_JSON);
  }
    private static final Logger LOG = Logger.getLogger(FootprintResource.class.getName());
}
