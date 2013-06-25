/*
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES.
 *
 * This file is a part of SITools2
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program inputStream distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary;
import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.plugins.resources.model.DataSetSelectionType;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.plugins.resources.model.ResourceParameter;
import fr.cnes.sitools.plugins.resources.model.ResourceParameterType;
import fr.cnes.sitools.util.Util;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Plugin for publishing a dataset through the Simple Image Access Protocol.
 *
 * <p> 
 * The plugin answers to the need of the following user story:<br/>
 * As administrator, I publish my data through SIAP so that the users
 * can request my images by the use of an interoperability standard.
 * <br/>
 * <img src="../../../../../../images/SIAP-usecase.png"/>
 * <br/>
 * In addition, this plugin has several dependencies with different components:<br/>
 * <img src="../../../../../../images/SimpleImageAccessResourcePlugin.png"/>
 * <br/> 
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 * @startuml SIAP-usecase.png
 * title Publishing data through SIAP
 * User --> (SIAP service) : requests
 * Admin --> (SIAP service) : adds and configures the SIAP service from the dataset.
 * (SIAP service) .. (dataset) : uses
 * @enduml
 * @startuml
 * package "Services" {
 *  HTTP - [SimpleImageAccessResourcePlugin]
 * }
 * database "Database" {
 *   frame "Data" {
 *     [myData]
 *   }
 * }
 * package "Dataset" {
 *  HTTP - [Dataset]
 *  [VODictionary]
 * }
 * folder "DataStorage" {
 *   HTTP - [directory]
 * }
 * [SimpleImageAccessResourcePlugin] --> [Dataset]
 * [Dataset] --> [directory]
 * [Dataset] --> [myData]
 * [Dataset] --> [VODictionary]
 * @enduml
 */
public class SimpleImageAccessResourcePlugin extends ResourceModel {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(SimpleImageAccessResourcePlugin.class.getName());
  

  /**
   * Constructs the configuration panel of the plugin.
   */
  public SimpleImageAccessResourcePlugin() {
    super();
    setClassAuthor("J-C Malapert");
    setClassOwner("CNES");
    setClassVersion("1.0");
    setName("Simple Image Access Protocol");
    setDescription("This plugin provides an access to your data through the Simple Image Access Protocol");
    setResourceClassName(fr.cnes.sitools.extensions.astro.resource.SimpleImageAccessResource.class.getName());

    this.setApplicationClassName(DataSetApplication.class.getName());

    //we set to NONE because this is a web service for Virtual Observatory
    // and we do not want to see it in the web user interface
    this.setDataSetSelection(DataSetSelectionType.NONE);
    setConfiguration();
  }
  
  /**
   * Sets the configuration for the administrator.
   */
  private void setConfiguration() {
    final ResourceParameter dictionary = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.DICTIONARY,
            "Dictionary name that sets up the service", ResourceParameterType.PARAMETER_INTERN);
    dictionary.setValueType("xs:dictionary");
    addParam(dictionary);
        
    final ResourceParameter intersect = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.INTERSECT,
            "how matched images should intersect the region of interest",
            ResourceParameterType.PARAMETER_INTERN);
    //String intersectEnum = "xs:enum[COVERS, ENCLOSED, CENTER, OVERLAPS]";
    intersect.setValueType("xs:enum[CENTER, OVERLAPS]");
    intersect.setValue("OVERLAPS");
    addParam(intersect);
    
   final ResourceParameter geoAttribut = new ResourceParameter(SimpleImageAccessProtocolLibrary.GEO_ATTRIBUT,
            "Geographical attribut for OVERLAPS mode. The geographical attribut must be spoly datatype from pgsphere",
            ResourceParameterType.PARAMETER_INTERN);    
    geoAttribut.setValueType("xs:dataset.columnAlias");    
    addParam(geoAttribut);    

    final ResourceParameter verb = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.VERB,
            "Verbosity determines how many columns are to be returned in the resulting table",
            ResourceParameterType.PARAMETER_INTERN);
    verb.setValueType("xs:enum[0, 1, 2, 3]");
    verb.setValue("1");
    addParam(verb);


    final ResourceParameter responsibleParty = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.RESPONSIBLE_PARTY,
            "The data provider's name and email", ResourceParameterType.PARAMETER_INTERN);
    addParam(responsibleParty);

    final ResourceParameter serviceName = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.SERVICE_NAME,
            "The name of the service",
            ResourceParameterType.PARAMETER_INTERN);
    serviceName.setValueType("xs:enum[Image Cutout Service, Image Mosaicing Service, Atlas Image Archive, Pointed Image Archive]");
    serviceName.setValue("Pointed Image Archive");
    addParam(serviceName);

    final ResourceParameter description = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.DESCRIPTION,
            "A couple of paragraphs of text that describe the nature of the service and its wider context",
            ResourceParameterType.PARAMETER_INTERN);
    addParam(description);

    final ResourceParameter instrument = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.INSTRUMENT,
            "The instrument that made the observations, for example STScI.HST.WFPC2",
            ResourceParameterType.PARAMETER_INTERN);
    addParam(instrument);

    final ResourceParameter waveband = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.WAVEBAND,
            "The waveband of the observations", ResourceParameterType.PARAMETER_INTERN);
    waveband.setValueType("xs:enum[radio, millimeter, infrared, optical, ultraviolet, xray, gammaray]");
    addParam(waveband);


    final ResourceParameter coverage = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.COVERAGE,
            "The coverage on the sky, as a free-form string", ResourceParameterType.PARAMETER_INTERN);
    addParam(coverage);

    final ResourceParameter temporal = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.TEMPORAL,
            "The temporal coverage, as a free-form string", ResourceParameterType.PARAMETER_INTERN);
    addParam(temporal);

    final ResourceParameter maxQuerySize = new ResourceParameter(
            fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.MAX_QUERY_SIZE,
            "The largest search area, given in decimal degrees, that will be accepted by the service without returning an error condition."
            + " A value of 64800 indicates that there is no restriction",
            ResourceParameterType.PARAMETER_INTERN);
    maxQuerySize.setValue("64800");
    addParam(maxQuerySize);

    final ResourceParameter maxImageSize = new ResourceParameter(
            fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.MAX_IMAGE_SIZE,
            "The largest image area, given in decimal degrees, that will be returned by the service",
            ResourceParameterType.PARAMETER_INTERN);
    addParam(maxImageSize);

    final ResourceParameter maxFileSize = new ResourceParameter(
            fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.MAX_FILE_SIZE,
            "The largest file size, given in Bytes, that will be returned by the service",
            ResourceParameterType.PARAMETER_INTERN);
    addParam(maxFileSize);


    final ResourceParameter maxRecords = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.MAX_RECORDS,
            "The largest number of records that the service will return", ResourceParameterType.PARAMETER_INTERN);
    maxRecords.setValue("-1");
    addParam(maxRecords);      
  }

  /**
   * Validates the configuration that has been set by the administrator.
   *
   * @return the error or warning
   */
  @Override
  public final Validator<ResourceModel> getValidator() {
    return new Validator<ResourceModel>() {
      @Override
      public final Set<ConstraintViolation> validate(final ResourceModel item) {
        final Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
        final Map<String, ResourceParameter> params = item.getParametersMap();
        final ResourceParameter dico = params.get(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.DICTIONARY);

        if (!Util.isNotEmpty(dico.getValue())) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setLevel(ConstraintViolationLevel.WARNING);
          constraint.setMessage("A dictionary must be set");
          constraint.setValueName(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.DICTIONARY);
          constraintList.add(constraint);
        }
        
        final ResourceParameter geoAttribut = params.get(SimpleImageAccessProtocolLibrary.GEO_ATTRIBUT);
        final ResourceParameter intersect = params.get(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.INTERSECT);
        if (intersect.getValue().equals("OVERLAPS") && !Util.isNotEmpty(geoAttribut.getValue())) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage(SimpleImageAccessProtocolLibrary.GEO_ATTRIBUT + " must be defined when OVERLAPS mode is used.");
          constraint.setValueName(SimpleImageAccessProtocolLibrary.GEO_ATTRIBUT);
          constraintList.add(constraint);
        } else if (!intersect.getValue().equals("OVERLAPS") && Util.isNotEmpty(geoAttribut.getValue())) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setLevel(ConstraintViolationLevel.WARNING);
          constraint.setMessage(SimpleImageAccessProtocolLibrary.GEO_ATTRIBUT + " is useless when OVERLAPS mode is not used.");
          constraint.setValueName(SimpleImageAccessProtocolLibrary.GEO_ATTRIBUT);
          constraintList.add(constraint);
        }
        return constraintList;
      }
    };
  }
}
