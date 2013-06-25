/**
 * *****************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
package fr.cnes.sitools.astro.vo.sia;

import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.extensions.common.InputsValidation;
import fr.cnes.sitools.extensions.common.NotNullAndNotEmptyValidation;
import fr.cnes.sitools.extensions.common.SpatialGeoValidation;
import fr.cnes.sitools.extensions.common.StatusValidation;
import fr.cnes.sitools.extensions.common.Validation;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.AnyTEXT;
import net.ivoa.xml.votable.v1.DataType;
import net.ivoa.xml.votable.v1.Info;
import net.ivoa.xml.votable.v1.Option;
import net.ivoa.xml.votable.v1.Param;
import net.ivoa.xml.votable.v1.Values;
import org.restlet.Context;
import org.restlet.Request;

/**
 * Input parameters for SIA.
 *
 * @author Jean-Crhistophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class SimpleImageAccessInputParameters implements DataModelInterface {
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(SimpleImageAccessInputParameters.class.getName());
  /**
   * Init value for right ascension parameter of the user input.
   */
  private transient double ra = 0.0;
  /**
   * Init value for declination parameter of the user input.
   */
  private transient double dec = 0.0;
  /**
   * Array that stores the size parameter of the user input.
   */
  private transient double[] size;
  /**
   * Default value for the verb parameter of the user input.
   */
  private transient int verb = 0;
  /**
   * Data model that stores the metadata response of the service.
   */
  private final transient Map dataModel = new HashMap();
  /**
   * Request.
   */
  private final transient Request request;
  /**
   * Context.
   */
  private final transient Context context;
  /**
   * Application where this resources is linked.
   */
  private final transient DataSetApplication datasetApp;
  /**
   * Configuration parameters of this resource.
   */
  private final transient ResourceModel resourceModel;

  /**
   * Constructs the objet that returns the metadata of the service.
   * @param datasetAppVal application
   * @param requestVal request
   * @param contextVal context
   * @param resourceModelVal configuration parameters
   */
  public SimpleImageAccessInputParameters(final DataSetApplication datasetAppVal, final Request requestVal, final Context contextVal, final ResourceModel resourceModelVal) {
    this.datasetApp = datasetAppVal;
    this.context = contextVal;
    this.request = requestVal;
    this.resourceModel = resourceModelVal;
    final String posInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.POS);
    final String sizeInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.SIZE);
    final String format = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.FORMAT);
    final String intersect = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.INTERSECT);
    final String verbosity = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.VERB);
    //TODO check the differentParameters
    if (Util.isSet(format) && format.equals(SimpleImageAccessProtocolLibrary.ParamStandardFormat.METADATA.name())) {
      fillMetadataFormat();
    } else {
      checkInputParameters(posInput, sizeInput);
    }
  }

  /**
   * Fills metadata response.
   */
  private void fillMetadataFormat() {
    this.dataModel.put("description", this.resourceModel.getParameterByName(SimpleImageAccessProtocolLibrary.DESCRIPTION).getValue());

    final Info info = new Info();
    info.setName("QUERY_STATUS");
    info.setValueAttribute("OK");
    final List<Info> listInfos = new ArrayList<Info>();
    listInfos.add(info);
    this.dataModel.put("infos", listInfos);

    final List<Param> listParam = new ArrayList<Param>();
    Param param = new Param();
    param.setName("INPUT:POS");
    param.setValue("0,0");
    param.setDatatype(DataType.DOUBLE);
    AnyTEXT anyText = new AnyTEXT();
    anyText.getContent().add("Search Position in the form ra,dec where ra and dec are given in decimal degrees in the ICRS coordinate system.");
    param.setDESCRIPTION(anyText);
    listParam.add(param);

    param = new Param();
    param.setName("INPUT:SIZE");
    param.setValue("0.05");
    param.setDatatype(DataType.DOUBLE);
    anyText = new AnyTEXT();
    anyText.getContent().add("Size of search region in the RA and Dec directions.");
    param.setDESCRIPTION(anyText);
    listParam.add(param);

    param = new Param();
    param.setName("INPUT:FORMAT");
    param.setValue(SimpleImageAccessProtocolLibrary.ParamStandardFormat.ALL.name());
    param.setDatatype(DataType.CHAR);
    param.setArraysize("*");
    anyText = new AnyTEXT();
    anyText.getContent().add("Requested format of images.");
    param.setDESCRIPTION(anyText);
    final List<String> formatList = SimpleImageAccessProtocolLibrary.ParamStandardFormat.getCtes();
    final Values values = new Values();
    for (String formatIter : formatList) {
      final Option option = new Option();
      option.setValue(formatIter);
      values.getOPTION().add(option);
    }
    param.setVALUES(values);
    //TODO : le faire pour chaque format
    listParam.add(param);

    param = new Param();
    param.setName("INPUT:INTERSECT");
    param.setValue(this.resourceModel.getParameterByName(SimpleImageAccessProtocolLibrary.INTERSECT).getValue());
    param.setDatatype(DataType.CHAR);
    anyText = new AnyTEXT();
    anyText.getContent().add("Choice of overlap with requested region.");
    param.setDESCRIPTION(anyText);
    listParam.add(param);

    param = new Param();
    param.setName("INPUT:VERB");
    param.setValue(this.resourceModel.getParameterByName(SimpleImageAccessProtocolLibrary.VERB).getValue());
    param.setDatatype(DataType.INT);
    anyText = new AnyTEXT();
    anyText.getContent().add("Verbosity level, controlling the number of columns returned.");
    param.setDESCRIPTION(anyText);
    listParam.add(param);

    dataModel.put("params", listParam);
  }

  /**
   * Checks input parameters.
   * @param posInput input parameter for POS
   * @param sizeInput input parameter for SIZE
   */
  private void checkInputParameters(final String posInput, final String sizeInput) {
    final List<Info> infos = new ArrayList<Info>();
    final Map<String, String> validationMap = new HashMap<String, String>();
    validationMap.put(SimpleImageAccessProtocolLibrary.POS, posInput);
    validationMap.put(SimpleImageAccessProtocolLibrary.SIZE, sizeInput);    
    Validation validation = new InputsValidation(validationMap);
    validation = new NotNullAndNotEmptyValidation(validation, SimpleImageAccessProtocolLibrary.POS);
    validation = new NotNullAndNotEmptyValidation(validation, SimpleImageAccessProtocolLibrary.SIZE);
    validation = new SpatialGeoValidation(validation, SimpleImageAccessProtocolLibrary.POS, 0, 1, new double[]{0.0, 360.0}, new double[]{-90.0, 90.0});
    StatusValidation status = validation.validate();
    if (status.isValid()) {
        final String pos = validation.getMap().get(SimpleImageAccessProtocolLibrary.POS);
        final String[] arrayPos = pos.split(",");
        this.ra = Double.valueOf(arrayPos[0]);
        this.dec = Double.valueOf(arrayPos[1]);
        final String size = validation.getMap().get(SimpleImageAccessProtocolLibrary.SIZE);
        final String[] arraySize = size.split(",");
        if(arraySize.length == 1) {
            this.size = new double[1];
            this.size[0] = Double.valueOf(arraySize[0]);
        } else {
            this.size = new double[2];
            this.size[0] = Double.valueOf(arraySize[0]);
            this.size[1] = Double.valueOf(arraySize[1]);
        }        
    } else {
        final Map<String, String> errors = status.getMessages();
        final Set<Map.Entry<String, String>> entries = errors.entrySet();        
        for (Map.Entry<String, String> entry : entries) {
            final Info info = new Info();
            info.setID(entry.getKey());
            info.setName("Error in " + entry.getKey());
            info.setValueAttribute("Error in input " + entry.getKey() + ": " + entry.getValue());
            infos.add(info);
            LOG.log(Level.FINEST, "{0}: {1}", new Object[]{entry.getKey(), entry.getValue()});            
        }        
    }
    
    if (!infos.isEmpty()) {
      this.dataModel.put("infos", infos);
    }

  }

  @Override
  public final Map getDataModel() {
    return Collections.unmodifiableMap(this.dataModel);
  }

  /**
   * Get Ra.
   *
   * @return Ra
   */
  public final double getRa() {
    return this.ra;
  }

  /**
   * Get Dec.
   *
   * @return Dec
   */
  public final double getDec() {
    return this.dec;
  }

  /**
   * Get Sr.
   *
   * @return Sr
   */
  public final double[] getSize() {
    return this.size;
  }

  /**
   * Get the request.
   *
   * @return Request
   */
  public final Request getRequest() {
    return this.request;
  }

  /**
   * Get the context.
   *
   * @return Context
   */
  public final Context getContext() {
    return this.context;
  }

  /**
   * Get DatasetApplication.
   *
   * @return Dataset application
   */
  public final DataSetApplication getDatasetApplication() {
    return this.datasetApp;
  }

  /**
   * Get verb.
   * @return verb
   */
  public final int getVerb() {
    return this.verb;
  }  
}
