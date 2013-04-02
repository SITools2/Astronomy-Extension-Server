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
 * ****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.resource;

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
 * <p> This plugin allows a data provider to publish his images in the Virtual Observatory. </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
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
    setClassVersion("0.1");
    setName("Simple Image Access Protocol");
    setDescription("This plugin provides an access to your data through the Simple Image Access Protocol");
    setResourceClassName(fr.cnes.sitools.extensions.astro.resource.SimpleImageAccessResource.class.getName());

    this.setApplicationClassName(DataSetApplication.class.getName());

    //we set to NONE because this is a web service for Virtual Observatory
    // and we do not want to see it in the web user interface
    this.setDataSetSelection(DataSetSelectionType.NONE);

    ResourceParameter dictionary = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.DICTIONARY,
            "Dictionary name that sets up the service", ResourceParameterType.PARAMETER_INTERN);
    dictionary.setValueType("xs:dictionary");
    addParam(dictionary);

    ResourceParameter intersect = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.INTERSECT,
            "how matched images should intersect the region of interest",
            ResourceParameterType.PARAMETER_INTERN);
    String intersectEnum = "xs:enum[COVERS, ENCLOSED, CENTER, OVERLAPS]";
    intersect.setValueType(intersectEnum);
    intersect.setValue("OVERLAPS");
    addParam(intersect);

    ResourceParameter verb = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.VERB,
            "Verbosity determines how many columns are to be returned in the resulting table",
            ResourceParameterType.PARAMETER_INTERN);
    String verbEnum = "xs:enum[0, 1, 2, 3]";
    verb.setValueType(verbEnum);
    verb.setValue("1");
    addParam(verb);


    ResourceParameter responsibleParty = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.RESPONSIBLE_PARTY,
            "The data provider's name and email", ResourceParameterType.PARAMETER_INTERN);
    addParam(responsibleParty);

    ResourceParameter serviceName = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.SERVICE_NAME,
            "The name of the service",
            ResourceParameterType.PARAMETER_INTERN);
    String serviceNameEnum = "xs:enum[Image Cutout Service, Image Mosaicing Service, Atlas Image Archive, Pointed Image Archive]";
    serviceName.setValueType(serviceNameEnum);
    serviceName.setValue("Pointed Image Archive");
    addParam(serviceName);

    ResourceParameter description = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.DESCRIPTION,
            "A couple of paragraphs of text that describe the nature of the service and its wider context",
            ResourceParameterType.PARAMETER_INTERN);
    addParam(description);

    ResourceParameter instrument = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.INSTRUMENT,
            "The instrument that made the observations, for example STScI.HST.WFPC2",
            ResourceParameterType.PARAMETER_INTERN);
    addParam(instrument);

    ResourceParameter waveband = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.WAVEBAND,
            "The waveband of the observations", ResourceParameterType.PARAMETER_INTERN);
    String waveBandEnum = "xs:enum[radio, millimeter, infrared, optical, ultraviolet, xray, gammaray]";
    waveband.setValueType(waveBandEnum);
    addParam(waveband);


    ResourceParameter coverage = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.COVERAGE,
            "The coverage on the sky, as a free-form string", ResourceParameterType.PARAMETER_INTERN);
    addParam(coverage);

    ResourceParameter temporal = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.TEMPORAL,
            "The temporal coverage, as a free-form string", ResourceParameterType.PARAMETER_INTERN);
    addParam(temporal);

    ResourceParameter maxQuerySize = new ResourceParameter(
            fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.MAX_QUERY_SIZE,
            "The largest search area, given in decimal degrees, that will be accepted by the service without returning an error condition."
            + " A value of 64800 indicates that there is no restriction",
            ResourceParameterType.PARAMETER_INTERN);
    maxQuerySize.setValue("64800");
    addParam(maxQuerySize);

    ResourceParameter maxImageSize = new ResourceParameter(
            fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.MAX_IMAGE_SIZE,
            "The largest image area, given in decimal degrees, that will be returned by the service",
            ResourceParameterType.PARAMETER_INTERN);
    addParam(maxImageSize);

    ResourceParameter maxFileSize = new ResourceParameter(
            fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.MAX_FILE_SIZE,
            "The largest file size, given in Bytes, that will be returned by the service",
            ResourceParameterType.PARAMETER_INTERN);
    addParam(maxFileSize);


    ResourceParameter maxRecords = new ResourceParameter(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.MAX_RECORDS,
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
        Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
        Map<String, ResourceParameter> params = item.getParametersMap();
        ResourceParameter dico = params.get(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.DICTIONARY);

        if (!Util.isNotEmpty(dico.getValue())) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setLevel(ConstraintViolationLevel.WARNING);
          constraint.setMessage("A dictionary must be set");
          constraint.setValueName(fr.cnes.sitools.astro.vo.sia.SimpleImageAccessProtocolLibrary.DICTIONARY);
          constraintList.add(constraint);
        }
        return constraintList;
      }
    };
  }
}
