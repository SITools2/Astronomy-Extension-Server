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
package fr.cnes.sitools.astro.vo.conesearch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ivoa.xml.votable.v1.AnyTEXT;
import net.ivoa.xml.votable.v1.DataType;
import net.ivoa.xml.votable.v1.Field;
import net.ivoa.xml.votable.v1.Info;
import net.ivoa.xml.votable.v1.Param;

import org.restlet.engine.Engine;

import fr.cnes.sitools.astro.representation.DatabaseRequestModel;
import fr.cnes.sitools.common.exception.SitoolsException;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.dataset.converter.business.ConverterChained;
import fr.cnes.sitools.dataset.database.DatabaseRequest;
import fr.cnes.sitools.dataset.database.DatabaseRequestFactory;
import fr.cnes.sitools.dataset.database.DatabaseRequestParameters;
import fr.cnes.sitools.dataset.database.common.DataSetExplorerUtil;
import fr.cnes.sitools.dataset.dto.ColumnConceptMappingDTO;
import fr.cnes.sitools.dataset.dto.DictionaryMappingDTO;
import fr.cnes.sitools.dataset.model.Predicat;
import fr.cnes.sitools.dictionary.model.Concept;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.util.Util;
import freemarker.template.TemplateSequenceModel;

/**
 * Votable response for cone search.
 *
 * @author Jean-Christophe Malapert
 */
public class ConeSearchResponse implements ConeSearchDataModelInterface {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(ConeSearchResponse.class.getName());
  /**
   * Data model that constains the conesearch response.
   */
  private final transient Map dataModel = new HashMap();

  /**
   * Constructor.
   *
   * @param inputParameters input parameters
   * @param model data model
   */
  public ConeSearchResponse(final ConeSearchInputParameters inputParameters, final ResourceModel model) {
    createResponse(inputParameters, model);
  }

  /**
   * Create VOTable response.
   *
   * @param inputParameters Input parameters
   * @param model data model
   */
  private void createResponse(final ConeSearchInputParameters inputParameters, final ResourceModel model) {
    // createResponse
    final String dictionaryName = model.getParameterByName(ConeSearchProtocolLibrary.DICTIONARY).getValue();

    inputParameters.getDatasetApplication().getLogger().log(Level.FINEST, "DICO: {0}", dictionaryName);

    // Set Votable parameters
    setVotableParametersFromConfiguration(model);

    // Set Votable resources
    setVotableResource(inputParameters.getDatasetApplication(), inputParameters, model, dictionaryName);
  }

  /**
   * Set VOTable parameters coming from administration configuration.
   *
   * @param modelVal parameters from administration
   */
  private void setVotableParametersFromConfiguration(final ResourceModel modelVal) {
    final List<Param> params = new ArrayList<Param>();
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.COVERAGE, DataType.CHAR);
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.EPOCH, DataType.CHAR);
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.INSTRUMENT, DataType.CHAR);
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.MAX_SR, DataType.DOUBLE);
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.RESPONSIBLE_PARTY, DataType.CHAR);
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.SERVICE_NAME, DataType.CHAR);
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.WAVEBAND, DataType.CHAR);
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.MAX_RECORDS, DataType.INT);
    setVotableParam(params, modelVal, ConeSearchProtocolLibrary.VERBOSITY, DataType.BOOLEAN);
    if (Util.isSet(params)) {
      dataModel.put("params", params);
    }
  }

  /**
   * Sets Votable Param.
   *
   * @param params List of params
   * @param model data model
   * @param parameterName parameter name
   * @param datatype datatype
   */
  private void setVotableParam(final List<Param> params, final ResourceModel model, final String parameterName, final DataType datatype) {
    final String parameterValue = model.getParameterByName(parameterName).getValue();
    if (Util.isNotEmpty(parameterValue)) {
      final Param param = new Param();
      param.setName(parameterName);
      param.setValue(parameterValue);
      param.setDatatype(datatype);
      params.add(param);
    }
  }

  /**
   * Create the response based on Table.
   *
   * @param datasetApp Dataset application
   * @param inputParameters Input Parameters
   * @param model data model
   * @param dictionaryName Cone search dictionary
   */
  private void setVotableResource(final DataSetApplication datasetApp, final ConeSearchInputParameters inputParameters,
          final ResourceModel model, final String dictionaryName) {
    final List<Info> infos = new ArrayList<Info>();
    final List<Field> fieldList = new ArrayList<Field>();
    final List<String> columnList = new ArrayList<String>();
    DatabaseRequest databaseRequest = null;

    try {
      // Get the concepts from the dictionary
      List<ColumnConceptMappingDTO> mappingList = getDicoFromConfiguration(datasetApp, dictionaryName);

      mappingList = checkRequiredMapping(mappingList, inputParameters.getVerb());

      // Get query parameters
      final DatabaseRequestParameters dbParams = setQueryParameters(datasetApp, model, inputParameters);
      databaseRequest = DatabaseRequestFactory.getDatabaseRequest(dbParams);

      // Execute query
      databaseRequest.createRequest();

      datasetApp.getLogger().log(Level.FINEST, "DB request: {0}", databaseRequest.getRequestAsString());

      // complete data model with fields
      setFields(fieldList, columnList, mappingList);
      dataModel.put("fields", fieldList);
      dataModel.put("sqlColAlias", columnList);

      // Complete data model with data
      final int count = (databaseRequest.getCount() > dbParams.getPaginationExtend()) ? dbParams.getPaginationExtend() : databaseRequest.getCount();
      final ConverterChained converterChained = datasetApp.getConverterChained();
      final TemplateSequenceModel rows = new DatabaseRequestModel(databaseRequest, converterChained);
      ((DatabaseRequestModel) rows).setSize(count);
      setVotableOK(infos);
      dataModel.put("infos", infos);
      dataModel.put("rows", rows);
    } catch (SitoolsException ex) {
      LOG.log(Level.FINER, null, ex);
      try {
        databaseRequest.close();
      } catch (SitoolsException ex1) {
          LOG.log(Level.FINER, null, ex1);
      } finally {
        setVotableError(infos, "Query", "Error in query", "Error in input query: " + ex.getMessage());
        if (!infos.isEmpty()) {
          this.dataModel.put("infos", infos);
        }
      }
    }
  }

  /**
   * Sets Query parameters to the database.
   *
   * @param datasetApp Dataset Application
   * @param model Data model
   * @param inputParameters Input Parameters
   * @return DatabaseRequestParamerters object
   */
  private DatabaseRequestParameters setQueryParameters(final DataSetApplication datasetApp, final ResourceModel model,
          final ConeSearchInputParameters inputParameters) {
    final String xCol = model.getParameterByName(ConeSearchProtocolLibrary.X).getValue();
    final String yCol = model.getParameterByName(ConeSearchProtocolLibrary.Y).getValue();
    final String zCol = model.getParameterByName(ConeSearchProtocolLibrary.Z).getValue();

    // Get the dataset
    final DataSetExplorerUtil dsExplorerUtil = new DataSetExplorerUtil(datasetApp, inputParameters.getRequest(),
            inputParameters.getContext());

    // Get query parameters
    final DatabaseRequestParameters dbParams = dsExplorerUtil.getDatabaseParams();

    // Get dataset records
    final int nbRecordsInDataSet = datasetApp.getDataSet().getNbRecords();

    // Get max records that is defined by admin
    int nbMaxRecords = Integer.valueOf(model.getParameterByName(ConeSearchProtocolLibrary.MAX_RECORDS).getValue());
    nbMaxRecords = (nbMaxRecords > nbRecordsInDataSet || nbMaxRecords == -1) ? nbRecordsInDataSet : nbMaxRecords;

    // Set max records
    dbParams.setPaginationExtend(nbMaxRecords);

    final double[] xyz = computeXyzFromRaDec(inputParameters.getRa(), inputParameters.getDec());
    final double zmin = ((inputParameters.getDec() - inputParameters.getSr()) <= -90) ? -1 : Math.sin(Math
            .toRadians(inputParameters.getDec() - inputParameters.getSr()));
    final double zmax = ((inputParameters.getDec() + inputParameters.getSr()) >= 90) ? 1 : Math.sin(Math
            .toRadians(inputParameters.getDec() + inputParameters.getSr()));

    // Create predicat definition
    final String predicatDefinition = String.format(" AND %s <= %s AND %s >= %s AND %s*%s + %s*%s + %s*%s >= %s", zCol,
            String.valueOf(zmax), zCol, String.valueOf(zmin), xCol, xyz[0], yCol, xyz[1], zCol, xyz[2],
            String.valueOf(Math.cos(Math.toRadians(inputParameters.getSr()))));

    final Predicat predicat = new Predicat();
    predicat.setStringDefinition(predicatDefinition);
    final List<Predicat> predicatList = dbParams.getPredicats();
    predicatList.add(predicat);
    dbParams.setPredicats(predicatList);
    return dbParams;
  }

  /**
   * Sets the votable error.
   *
   * @param infos infos
   * @param id id
   * @param name name
   * @param value value
   */
  private void setVotableError(final List<Info> infos, final String id, final String name, final String value) {
    if (Util.isNotEmpty(name)) {
      final Info info = new Info();
      info.setID(id);
      info.setName(name);
      info.setValueAttribute(value);
      infos.add(info);
    }
  }

  /**
   * Set Query_status=OK.
   *
   * @param infos infos
   */
  private void setVotableOK(final List<Info> infos) {
    final Info info = new Info();
    info.setName("QUERY_STATUS");
    info.setValueAttribute("OK");
    infos.add(info);
  }

  @Override
  public final Map getDataModel() {
    return Collections.unmodifiableMap(this.dataModel);
  }

  /**
   * Computes Equatorial to cartesian.
   * @param rightAscension right ascension
   * @param declination declination
   * @return (x,y,z) coordinates
   */
  private double[] computeXyzFromRaDec(final double rightAscension, final double declination) {
    final double[] matrix = new double[]{
    Math.cos(Math.toRadians(declination)) * Math.cos(Math.toRadians(rightAscension)),
    Math.cos(Math.toRadians(declination)) * Math.sin(Math.toRadians(rightAscension)),
    Math.sin(Math.toRadians(declination))};
    return matrix;
  }

  /**
   * Provide the mapping between SQL column/concept for a given dictionary.
   *
   * @param datasetApp Application where this service is attached
   * @param dicoToFind Dictionary name to find
   * @return Returns a mapping SQL column/Concept
   * @throws SitoolsException No mapping has been done or cannot find the dico
   */
  private List<ColumnConceptMappingDTO> getDicoFromConfiguration(final DataSetApplication datasetApp,
          final String dicoToFind) throws SitoolsException {
    List<ColumnConceptMappingDTO> colConceptMappingDTOList = null;

    // Get the list of dictionnaries related to the datasetApplication
    final List<DictionaryMappingDTO> dicoMappingList = datasetApp.getDictionaryMappings();
    if (!Util.isSet(dicoMappingList) || dicoMappingList.isEmpty()) {
      throw new SitoolsException("No mapping with VO concepts has been done. please contact the administrator");
    }

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
   * Set Fields and columnSqlAliasList.
   *
   * @param fieldList List of fields to display on the VOTable
   * @param columnList List of SQL column
   * @param mappingList List of SQL column/concept
   */
  private void setFields(final List<Field> fieldList, final List<String> columnList, final List<ColumnConceptMappingDTO> mappingList) {

    for (ColumnConceptMappingDTO mappingIter : mappingList) {
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
      final Concept concept = mappingIter.getConcept();
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

  /**
   * Check required mapping and filter columns to map according to VERB.
   *
   * @param mappingList list of mapping defined by the administrator
   * @param verb VERB of Cone search protocol
   * @return Returns the new mapping according to VERB
   * @throws SitoolsException columns with UCD ID_MAIN, POS_EQ_RA_MAIN, POS_EQ_DEC_MAIN must be mapped
   */
  private List<ColumnConceptMappingDTO> checkRequiredMapping(final List<ColumnConceptMappingDTO> mappingList, final int verb)
          throws SitoolsException {
    final int nbConceptToMap = ConeSearchProtocolLibrary.REQUIRED_UCD_CONCEPTS.size();
    int nbConcept = 0;
    final List<ColumnConceptMappingDTO> conceptToMap = new ArrayList<ColumnConceptMappingDTO>(mappingList);
    for (ColumnConceptMappingDTO mappingIter : mappingList) {
      final Concept concept = mappingIter.getConcept();
      final String ucdValue = concept.getPropertyFromName("ucd").getValue();
      if (Util.isNotEmpty(ucdValue) && ConeSearchProtocolLibrary.REQUIRED_UCD_CONCEPTS.contains(ucdValue)) {
        nbConcept++;
      } else if (verb == 1) {
        conceptToMap.remove(mappingIter);
      }
    }

    if (nbConceptToMap != nbConcept) {
      throw new SitoolsException("columns with UCD ID_MAIN, POS_EQ_RA_MAIN, POS_EQ_DEC_MAIN must be mapped");
    }
    return conceptToMap;
  }
}
