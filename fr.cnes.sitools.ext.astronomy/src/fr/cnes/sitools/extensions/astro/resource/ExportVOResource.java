/*******************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.sitools.astro.representation.DatabaseRequestModel;
import fr.cnes.sitools.astro.representation.VOTableRepresentation;
import fr.cnes.sitools.common.exception.SitoolsException;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.dataset.converter.business.ConverterChained;
import fr.cnes.sitools.dataset.database.DatabaseRequest;
import fr.cnes.sitools.dataset.database.DatabaseRequestFactory;
import fr.cnes.sitools.dataset.database.DatabaseRequestParameters;
import fr.cnes.sitools.dataset.database.common.DataSetExplorerUtil;
import fr.cnes.sitools.dataset.dto.ColumnConceptMappingDTO;
import fr.cnes.sitools.dataset.dto.DictionaryMappingDTO;
import fr.cnes.sitools.dataset.model.Column;
import fr.cnes.sitools.dictionary.model.Concept;
import freemarker.template.TemplateSequenceModel;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.AnyTEXT;
import net.ivoa.xml.votable.v1.DataType;
import net.ivoa.xml.votable.v1.Field;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Gets selected rows from the dataset and export them as a VOTable.
 * 
 * <p>
 * This resource exports column that have been asked by the query but also
 * having a VOTable concept
 * </p>
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ExportVOResource extends SitoolsParameterizedResource {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ExportVOResource.class.getName());
  /**
   * Name of the dictionary that has been used to map VO concepts with dataset's attributes.
   */
  private transient String dictionaryName = null;
  /**
   * Description that is displayed in the VOTable.
   */
  private transient String description = null;

    /**
     * Initialize dictionary name and description.
     */
    @Override
  public final void doInit() {
    super.doInit();
    this.dictionaryName = this.getModel().getParameterByName(ExportVOResourcePlugin.DICTIONARY).getValue();
    this.description = this.getModel().getParameterByName(ExportVOResourcePlugin.DESCRIPTION).getValue();
  }

    /**
     * Returns the supported representation.
     * @param variant variant
     * @return the representation
     */
    @Override
  protected final Representation head(final Variant variant) {
    final Representation repr = super.head();
    repr.setMediaType(MediaType.TEXT_XML);
    return repr;
  }

    /**
     * Returns the response.
     * @return the representation
     */
    @Get
  public final Representation getVoExport() {

    // Get the datasetApplication
    final DataSetApplication datasetApp = (DataSetApplication) getApplication();

    // Get the pipeline to convert the information
    final ConverterChained converterChained = datasetApp.getConverterChained();

    // Create the data model for the template and add a description for the resource
    final Map dataModel = new HashMap();
    dataModel.put("description", this.description);

    // Get the dataset
    final DataSetExplorerUtil dsExplorerUtil = new DataSetExplorerUtil((DataSetApplication) getApplication(), getRequest(),
        getContext());

    // Get the concepts from the dictionary
    final List<ColumnConceptMappingDTO> mappingList = getDicoFromConfiguration((DataSetApplication) getApplication(),
        this.dictionaryName);

    // Get column list that have been mapped with VOTable concepts
    final List<Column> columnListHavingConcept = getColumnList((DataSetApplication) getApplication(), mappingList);
    if (columnListHavingConcept.isEmpty()) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
          "VO semantic is not associated to data, please contact the administrator");
    }

    // Get query parameters
    final DatabaseRequestParameters dbParams = dsExplorerUtil.getDatabaseParams();
    dbParams.setPaginationExtend(datasetApp.getDataSet().getNbRecords());

    // Get columns that have ben queried and having a concept
    // We use this method instead of getVisibleColumn because getVisibleColumns returns
    // the visble columns of the livegrid + primary key
    // By using this method, we are not allowed to use the primary as output column   
    String colModel = getRequest().getResourceRef().getQueryAsForm().getFirstValue("colModel");

    List<Column> columnListToDisplay;
    // If no data model is transfered    
    if (colModel != null) {
      colModel = colModel.replaceAll("\"", "");
      String[] cols = colModel.split(", ");
      columnListToDisplay = getColumnsFromName(datasetApp, cols);
    } else {
      columnListToDisplay = dbParams.getSqlVisibleColumns();
    }

    // Fine : query must have the primary key
    final List<Column> columnListToQuery = dbParams.getSqlVisibleColumns();

    // Keep all column having a VOTable concept
    columnListToDisplay.retainAll(columnListHavingConcept);
    if (columnListToDisplay.isEmpty()) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
          "Cannot do the export: There is no VO semantic related to selected columns");
    }

    // Set the column to display in the VOTable format
    // After, we will remove the primary key. For the moment,
    // we need the primary key for the query
    dbParams.setSqlVisibleColumns(columnListToQuery);
    final DatabaseRequest databaseRequest = DatabaseRequestFactory.getDatabaseRequest(dbParams);

    // complete data model with fields
    final List<Field> fieldList = new ArrayList<Field>();
    final List<String> columnList = new ArrayList<String>();
    setFields(fieldList, columnList, mappingList, columnListToDisplay);
    dataModel.put("fields", fieldList);
    dataModel.put("sqlColAlias", columnList);

    // Execute query
    try {
      if (dbParams.getDistinct()) {
        databaseRequest.createDistinctRequest();
      } else {
        databaseRequest.createRequest();
      }
    } catch (SitoolsException ex) {
      LOG.log(Level.SEVERE, null, ex);
      try {
        databaseRequest.close();
      } catch (SitoolsException ex1) {
        LOG.log(Level.SEVERE, null, ex1);
      } finally {
        throw new ResourceException(ex);
      }
    }

    // Complete data model
    final TemplateSequenceModel rows = new DatabaseRequestModel(databaseRequest, converterChained);
    dataModel.put("rows", rows);

    // Return the response
    final Representation rep =  new VOTableRepresentation(dataModel);
    if (fileName != null && !"".equals(fileName)) {
      final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
      disp.setFilename(fileName);
      rep.setDisposition(disp);
    }
    return rep;
  }

    /**
     * General WADL description.
     */
    @Override
  public final void sitoolsDescribe() {
    setName("VOExport Service");
    setDescription("Resource for VO export");
  }

    /**
     * Describes the GET method in WADL.
     * @param info information
     */
    @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("ExportVO");
    info.setDocumentation("Export data as a VOTable format");

    final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo("colModel", true, "String", ParameterStyle.QUERY,
        "List of columnName as colModel=\"col1,col2,...\""));
    info.getRequest().setParameters(parametersInfo);

    info.getResponse().getStatuses().add(Status.SUCCESS_OK);

    final DocumentationInfo documentation = new DocumentationInfo();
    documentation.setTitle("VOTable");
    documentation.setTextContent("VOTable format for interoperability");

    final List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    final RepresentationInfo representationInfo = new RepresentationInfo(MediaType.TEXT_XML);
    representationInfo.setDocumentation(documentation);
    representationsInfo.add(representationInfo);
    info.getResponse().setRepresentations(representationsInfo);
  }

  /**
   * Provides the mapping between SQL column/concept for a given dictionary.
   * 
   * @param datasetApp Application where this service is attached
   * @param dicoToFind Dictionary name to find
   * @return a mapping SQL column/Concept
   */
  private List<ColumnConceptMappingDTO> getDicoFromConfiguration(final DataSetApplication datasetApp, final String dicoToFind) {
    List<ColumnConceptMappingDTO> colConceptMappingDTOList = null;

    // Get the list of dictionnaries related to the datasetApplication
    final List<DictionaryMappingDTO> dicoMappingList = datasetApp.getDictionaryMappings();

    // For each dictionary, find the interesting one and return the mapping SQLcolumn/concept
    for (DictionaryMappingDTO dicoMappingIter : dicoMappingList) {
      final String dicoName = dicoMappingIter.getDictionaryName();
      if (dicoToFind.equals(dicoName)) {
        colConceptMappingDTOList = dicoMappingIter.getMapping();
        break;
      }
    }
    return colConceptMappingDTOList;
  }

  /**
   * Extracts the list of column model from the mapping.
   * 
   * @param datasetApp Application where the service is attached
   * @param mappingList mapping
   * @return the list of Columns
   */
  private List<Column> getColumnList(final DataSetApplication datasetApp, final List<ColumnConceptMappingDTO> mappingList) {
    final List<Column> columnList = new ArrayList<Column>();
    for (ColumnConceptMappingDTO mappingIter : mappingList) {
      final String sqlColumnAlias = mappingIter.getColumnAlias();
      final Column column = datasetApp.getDataSet().findByColumnAlias(sqlColumnAlias);
      columnList.add(column);
    }
    return columnList;
  }

  /**
   * Returns searched columns from a list of column name.
   *
   * @param datasetApp dataset Application
   * @param columnsName columns name
   * @return the columns from the list of columns name
   */
  private List<Column> getColumnsFromName(final DataSetApplication datasetApp, final String[] columnsName) {
    final List<Column> columnsKey = new ArrayList<Column>();
    final List<Column> columns = datasetApp.getDataSet().getColumnModel();
    for (Column columnIter : columns) {
      for (int i = 0; i < columnsName.length; i++) {
        if (columnIter.getColumnAlias().equals(columnsName[i])) {
          columnsKey.add(columnIter);
        }
      }
    }
    return columnsKey;
  }

  /**
   * Sets Fields and columnSqlAliasList.
   *
   * @param fieldList List of fields to display on the VOTable
   * @param columnList List of SQL column
   * @param mappingList List of SQL column/concept
   * @param colToQuery ColumnList to query
   */
  private void setFields(final List<Field> fieldList, final List<String> columnList, final List<ColumnConceptMappingDTO> mappingList, final List<Column> colToQuery) {
    for (Column colToQueryIter : colToQuery) {
      for (ColumnConceptMappingDTO mappingIter : mappingList) {
        if (colToQueryIter.getColumnAlias().equals(mappingIter.getColumnAlias())) {
          String id = null;
          String name = null;
          String ucd = null;
          String utype = null;
          String ref = null;
          String datatype = null;
          String width = null;
          String precision = null;
          String unit = null;
          String type = null;
          String xtype = null;
          String arraysize = null;
          String descriptionValue = null;
          columnList.add(mappingIter.getColumnAlias());
          Concept concept = mappingIter.getConcept();
          if (concept.getName() != null) {
            name = concept.getName();
          }
          if (concept.getPropertyFromName("ID").getValue() != null) {
            id = concept.getPropertyFromName("ID").getValue();
          }
          if (concept.getPropertyFromName("ucd").getValue() != null) {
            ucd = concept.getPropertyFromName("ucd").getValue();
          }
          if (concept.getPropertyFromName("utype").getValue() != null) {
            utype = concept.getPropertyFromName("utype").getValue();
          }
          if (concept.getPropertyFromName("ref").getValue() != null) {
            ref = concept.getPropertyFromName("ref").getValue();
          }
          if (concept.getPropertyFromName("datatype").getValue() != null) {
            datatype = concept.getPropertyFromName("datatype").getValue();
          }
          if (concept.getPropertyFromName("width").getValue() != null) {
            width = concept.getPropertyFromName("width").getValue();
          }
          if (concept.getPropertyFromName("precision").getValue() != null) {
            precision = concept.getPropertyFromName("precision").getValue();
          }
          if (concept.getPropertyFromName("unit").getValue() != null) {
            unit = concept.getPropertyFromName("unit").getValue();
          }
          if (concept.getPropertyFromName("type").getValue() != null) {
            type = concept.getPropertyFromName("type").getValue();
          }
          if (concept.getPropertyFromName("xtype").getValue() != null) {
            xtype = concept.getPropertyFromName("xtype").getValue();
          }
          if (concept.getPropertyFromName("arraysize").getValue() != null) {
            arraysize = concept.getPropertyFromName("arraysize").getValue();
          }
          if (concept.getDescription() != null) {
            descriptionValue = concept.getDescription();
          }
          final Field field = new Field();
          field.setID(id);
          field.setName(name);
          field.setUcd(ucd);
          field.setUtype(utype);
          field.setRef(ref);
          field.setDatatype(DataType.fromValue(datatype));
          if (width != null) {
            field.setWidth(BigInteger.valueOf(Long.valueOf(width)));
          }
          field.setPrecision(precision);
          field.setUnit(unit);
          field.setType(type);
          field.setXtype(xtype);
          field.setArraysize(arraysize);
          final AnyTEXT anyText = new AnyTEXT();
          anyText.getContent().add(descriptionValue);
          field.setDESCRIPTION(anyText);
          fieldList.add(field);
        }
      }
    }
  }
}
