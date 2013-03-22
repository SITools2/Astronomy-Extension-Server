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
package fr.cnes.sitools.astro.vo.sia;

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

/**
 * Votable response for cone search
 * 
 * @author Jean-Christophe Malapert
 */
public class SimpleImageAccessResponse implements SimpleImageAccessDataModelInterface {

    private Map dataModel = new HashMap();

    /**
     * Constructor
     *
     * @param inputParameters
     *          input parameters
     * @param model
     *          data model
     */
    public SimpleImageAccessResponse(final SimpleImageAccessInputParameters inputParameters, final ResourceModel model) {
        createResponse(inputParameters, model);
    }

    /**
     * Create VOTable response
     *
     * @param inputParameters
     *          Input parameters
     * @param model
     *          data model
     */
    private void createResponse(final SimpleImageAccessInputParameters inputParameters, final ResourceModel model) {
        // createResponse
        String dictionaryName = model.getParameterByName(SimpleImageAccessProtocolLibrary.DICTIONARY).getValue();

        inputParameters.getDatasetApplication().getLogger().log(Level.FINEST, "DICO: {0}", dictionaryName);

        // Set Votable parameters
        setVotableParametersFromConfiguration(dataModel, model);

        // Set Votable resources
        setVotableResource(inputParameters.getDatasetApplication(), inputParameters, model, dictionaryName);
    }

    /**
     * Set VOTable parameters coming from administration configuration
     *
     * @param dataModel
     *          data model to set
     * @param model
     *          parameters from administration
     */
    private void setVotableParametersFromConfiguration(Map dataModel, final ResourceModel model) {
        List<Param> params = new ArrayList<Param>();
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.COVERAGE, DataType.CHAR);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.TEMPORAL, DataType.CHAR);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.INSTRUMENT, DataType.CHAR);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.MAX_FILE_SIZE, DataType.LONG);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.RESPONSIBLE_PARTY, DataType.CHAR);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.SERVICE_NAME, DataType.CHAR);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.WAVEBAND, DataType.CHAR);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.MAX_RECORDS, DataType.INT);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.MAX_QUERY_SIZE, DataType.CHAR);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.MAX_IMAGE_SIZE, DataType.CHAR);
        setVotableParam(params, model, SimpleImageAccessProtocolLibrary.VERB, DataType.BOOLEAN);
        if (Util.isSet(params)) {
            dataModel.put("params", params);
        }
    }

    /**
     * Set Votable Param
     *
     * @param params
     *          List of params
     * @param model
     *          data model
     * @param parameterName
     *          parameter name
     * @param datatype
     *          datatype
     */
    private void setVotableParam(List<Param> params, final ResourceModel model, final String parameterName,
            final DataType datatype) {
        String parameterValue = model.getParameterByName(parameterName).getValue();
        if (Util.isNotEmpty(parameterValue)) {
            Param param = new Param();
            param.setName(parameterName);
            param.setValue(parameterValue);
            param.setDatatype(datatype);
            params.add(param);
        }
    }

    /**
     * Create the response based on Table
     *
     * @param datasetApp
     *          Dataset application
     * @param inputParameters
     *          Input Parameters
     * @param model
     *          data model
     * @param dictionaryName
     *          Cone search dictionary
     */
    private void setVotableResource(final DataSetApplication datasetApp, final SimpleImageAccessInputParameters inputParameters,
            final ResourceModel model, final String dictionaryName) {
        List<Field> fieldList = new ArrayList<Field>();
        List<String> columnList = new ArrayList<String>();
        DatabaseRequest databaseRequest = null;

        try {
            // Get the concepts from the dictionary
            List<ColumnConceptMappingDTO> mappingList = getDicoFromConfiguration(datasetApp, dictionaryName);

            mappingList = checkRequiredMapping(mappingList, inputParameters.getVerb());

            // Get query parameters
            DatabaseRequestParameters dbParams = setQueryParameters(datasetApp, model, inputParameters, mappingList);
            databaseRequest = DatabaseRequestFactory.getDatabaseRequest(dbParams);

            // Execute query
            databaseRequest.createRequest();

            datasetApp.getLogger().log(Level.FINEST, "DB request: {0}", databaseRequest.getRequestAsString());

            // complete data model with fields
            setFields(fieldList, columnList, mappingList);
            dataModel.put("fields", fieldList);
            dataModel.put("sqlColAlias", columnList);

            // Complete data model with data
            int count = databaseRequest.getCount();
            count = (count > dbParams.getPaginationExtend()) ? dbParams.getPaginationExtend() : count;
            ConverterChained converterChained = datasetApp.getConverterChained();
            TemplateSequenceModel rows = new DatabaseRequestModel(databaseRequest, converterChained);
            ((DatabaseRequestModel) rows).setSize(count);
            dataModel.put("rows", rows);

        } catch (SitoolsException ex) {
            try {
                if (Util.isSet(databaseRequest)) {
                    databaseRequest.close();
                }
            } catch (SitoolsException ex1) {
            } finally {
                List<Info> infos = new ArrayList<Info>();
                datasetApp.getLogger().log(Level.FINEST, "ERROR: {0}", ex.getMessage());
                setVotableError(infos, "Query", "Error in query", "Error in input query: " + ex.getMessage());
                if (!infos.isEmpty()) {
                    this.dataModel.put("infos", infos);
                }
            }
        }
    }

    /**
     * Set Query parameters to the database
     *
     * @param datasetApp
     *          Dataset Application
     * @param model
     *          Data model
     * @param inputParameters
     *          Input Parameters
     * @return DatabaseRequestParamerters object
     */
    private DatabaseRequestParameters setQueryParameters(final DataSetApplication datasetApp, final ResourceModel model,
            final SimpleImageAccessInputParameters inputParameters, List<ColumnConceptMappingDTO> mappingList) {

        // Get the dataset
        DataSetExplorerUtil dsExplorerUtil = new DataSetExplorerUtil(datasetApp, inputParameters.getRequest(),
                inputParameters.getContext());

        // Get query parameters
        DatabaseRequestParameters dbParams = dsExplorerUtil.getDatabaseParams();

        // Get dataset records
        int nbRecordsInDataSet = datasetApp.getDataSet().getNbRecords();

        // Get max records that is defined by admin
        int nbMaxRecords = Integer.valueOf(model.getParameterByName(SimpleImageAccessProtocolLibrary.MAX_RECORDS).getValue());
        nbMaxRecords = (nbMaxRecords > nbRecordsInDataSet || nbMaxRecords == -1) ? nbRecordsInDataSet : nbMaxRecords;

        // Set max records
        dbParams.setPaginationExtend(nbMaxRecords);

        // Create predicat definition
        double raUser = inputParameters.getRa();
        double decUser = inputParameters.getDec();
        double[] sizeArray = inputParameters.getSize();
        double decmin, decmax;
        double[] raRange = {-1.0, -1.0, -1.0, -1.0};
        if (sizeArray.length == 2) {
            if (decUser - sizeArray[1]/2.0 < -90) {
                decmin = -90.0;
                decmax = (decUser + sizeArray[1]/2.0 > 90.0)?90.0:decUser + sizeArray[1]/2.0;
                raRange[0] = 0.0;
                raRange[1] = 360.0;
            } else if (decUser + sizeArray[1]/2.0 > 90) {
                decmin = (decUser - sizeArray[1]/2.0 < -90.0)?-90.0:decUser - sizeArray[1]/2.0;
                decmax = 90;
                raRange[0] = 0.0;
                raRange[1] = 360.0;
            } else {
                decmin = decUser - sizeArray[1]/2.0;
                decmax = decUser + sizeArray[1]/2.0;
                double ramin = raUser - sizeArray[0]/2.0;
                double ramax = raUser + sizeArray[0]/2.0;
                if (ramin < 0) {
                    raRange[0] = 0;
                    raRange[1] = raUser - sizeArray[0]/2.0;
                    raRange[2] = 360.0 - (raUser - sizeArray[0]/2.0);
                    raRange[3] = 360.0;
                } else if (ramax > 360) {
                    raRange[0] = raUser - sizeArray[0]/2.0;
                    raRange[1] = 360.0;
                    raRange[2] = 0.0;
                    raRange[3] = (raUser + sizeArray[0]/2.0)-360.0;
                } else {
                    raRange[0] = raUser - sizeArray[0]/2.0;
                    raRange[1] = raUser + sizeArray[0]/2.0;
                }
            }
        } else {
            if (decUser - sizeArray[0]/2.0 < -90) {
                decmin = -90.0;
                decmax = (decUser + sizeArray[0]/2.0>90.0)?90.0:decUser + sizeArray[0]/2.0;
                raRange[0] = 0.0;
                raRange[1] = 360.0;
            } else if (decUser + sizeArray[0]/2.0 > 90) {
                decmin = (decUser - sizeArray[0]/2.0 < -90.0)?-90.0:decUser - sizeArray[0]/2.0;
                decmax = 90;
                raRange[0] = 0.0;
                raRange[1] = 360.0;
            } else {
                decmin = decUser - sizeArray[0]/2.0;
                decmax = decUser + sizeArray[0]/2.0;
                double ramin = raUser - sizeArray[0]/2.0;
                double ramax = raUser + sizeArray[0]/2.0;
                if (ramin < 0) {
                    raRange[0] = 0;
                    raRange[1] = raUser + sizeArray[0]/2.0;
                    raRange[2] = 360.0;
                    raRange[3] = 360.0 - (raUser - sizeArray[0]/2.0);
                } else if (ramax > 360) {
                    raRange[0] = raUser - sizeArray[0]/2.0;
                    raRange[1] = 360.0;
                    raRange[2] = 0.0;
                    raRange[3] = (raUser + sizeArray[0]/2.0)-360.0;
                }else {
                    raRange[0] = raUser - sizeArray[0]/2.0;
                    raRange[1] = raUser + sizeArray[0]/2.0;
                }
            }
        }

        String raColTarget = null;
        String decColTarget = null;
        for (ColumnConceptMappingDTO mapIter : mappingList) {
            String ucd = mapIter.getConcept().getPropertyFromName("ucd").getValue();
            if (ucd.equals(SimpleImageAccessProtocolLibrary.REQUIRED_UCD_CONCEPTS.get(1))) {
                raColTarget = mapIter.getColumnAlias();
            }
            if (ucd.equals(SimpleImageAccessProtocolLibrary.REQUIRED_UCD_CONCEPTS.get(2))) {
                decColTarget = mapIter.getColumnAlias();
            }
        }

        String predicatDefinition;
        if(raRange[3] < 0.0) {
            predicatDefinition = String.format(" AND ( %s BETWEEN %s AND %s ) AND ( %s BETWEEN %s AND %s )", decColTarget,decmin,decmax,raColTarget,raRange[0],raRange[1]);
        } else {
            predicatDefinition = String.format(" AND ( %s BETWEEN %s AND %s ) AND (( %s BETWEEN %s AND %s ) OR ( %s BETWEEN %s AND %s ))", decColTarget,decmin,decmax,raColTarget,raRange[0],raRange[1],raColTarget,raRange[2],raRange[3]);
        }

         Predicat predicat = new Predicat();
         predicat.setStringDefinition(predicatDefinition);
         List<Predicat> predicatList = dbParams.getPredicats();
         predicatList.add(predicat);
         dbParams.setPredicats(predicatList);
        return dbParams;
    }

    /**
     * Set the votable error
     *
     * @param infos
     * @param id
     * @param name
     * @param value
     */
    private void setVotableError(List<Info> infos, final String id, final String name, final String value) {
        if (Util.isNotEmpty(name)) {
            Info info = new Info();
            info.setID(id);
            info.setName(name);
            info.setValueAttribute(value);
            infos.add(info);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public Map getDataModel() {
        return Collections.unmodifiableMap(this.dataModel);
    }

    /**
     * Provide the mapping between SQL column/concept for a given dictionary
     *
     * @param datasetApp
     *          Application where this service is attached
     * @param dicoToFind
     *          Dictionary name to find
     * @return Returns a mapping SQL column/Concept
     * @throws SitoolsException
     *           No mapping has been done or cannot find the dico
     */
    private List<ColumnConceptMappingDTO> getDicoFromConfiguration(final DataSetApplication datasetApp,
            final String dicoToFind) throws SitoolsException {
        List<ColumnConceptMappingDTO> colConceptMappingDTOList = null;

        // Get the list of dictionnaries related to the datasetApplication
        List<DictionaryMappingDTO> dicoMappingList = datasetApp.getDictionaryMappings();
        if (!Util.isSet(dicoMappingList) || dicoMappingList.isEmpty()) {
            throw new SitoolsException("No mapping with VO concepts has been done. please contact the administrator");
        }

        // For each dictionary, find the interesting one and return the mapping SQLcolumn/concept
        for (DictionaryMappingDTO dicoMappingIter : dicoMappingList) {
            String dicoName = dicoMappingIter.getDictionaryName();
            if (dicoToFind.equals(dicoName)) {
                colConceptMappingDTOList = dicoMappingIter.getMapping();
                break;
            }
        }
        return colConceptMappingDTOList;
    }

    /**
     * Set Fields and columnSqlAliasList
     *
     * @param fieldList
     *          List of fields to display on the VOTable
     * @param columnList
     *          List of SQL column
     * @param mappingList
     *          List of SQL column/concept
     */
    private void setFields(List<Field> fieldList, List<String> columnList, final List<ColumnConceptMappingDTO> mappingList) {

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
            Field field = new Field();
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
            AnyTEXT anyText = new AnyTEXT();
            anyText.getContent().add(descriptionValue);
            field.setDESCRIPTION(anyText);
            fieldList.add(field);

        }
    }

    /**
     * Check required mapping and filter columns to map according to VERB
     *
     * @param mappingList
     *          list of mapping defined by the administrator
     * @param verb
     *          VERB of Cone search protocol
     * @return Returns the new mapping according to VERB
     * @throws SitoolsException
     *           columns with UCD ID_MAIN, POS_EQ_RA_MAIN, POS_EQ_DEC_MAIN must be mapped
     */
    private List<ColumnConceptMappingDTO> checkRequiredMapping(final List<ColumnConceptMappingDTO> mappingList, int verb)
            throws SitoolsException {
        final int nbConceptToMap = SimpleImageAccessProtocolLibrary.REQUIRED_UCD_CONCEPTS.size();
        int nbConcept = 0;
        List<ColumnConceptMappingDTO> conceptToMap = new ArrayList<ColumnConceptMappingDTO>(mappingList);
        for (ColumnConceptMappingDTO mappingIter : mappingList) {
            Concept concept = mappingIter.getConcept();
            String ucdValue = concept.getPropertyFromName("ucd").getValue();
            if (Util.isNotEmpty(ucdValue) && SimpleImageAccessProtocolLibrary.REQUIRED_UCD_CONCEPTS.contains(ucdValue)) {
                nbConcept++;
            } else if (verb == 1) {
                conceptToMap.remove(mappingIter);
            }
        }

        if (nbConceptToMap != nbConcept) {
            StringBuilder buffer = new StringBuilder("columns with ");
            for (ColumnConceptMappingDTO mappingIter : mappingList) {
                buffer.append(mappingIter.getConcept().getName()).append(" ");
            }
            buffer.append("must be mapped");
            throw new SitoolsException(buffer.toString());
        }

        return conceptToMap;

    }
    private static final Logger LOG = Logger.getLogger(SimpleImageAccessResponse.class.getName());
}
