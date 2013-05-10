/******************************************************************************
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.sitools.extensions.astro.application;

import fr.cnes.sitools.common.model.Category;
import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.extensions.common.AbstractUtility;
import fr.cnes.sitools.extensions.common.VoDictionary;
import fr.cnes.sitools.plugins.applications.business.AbstractApplicationPlugin;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 * Plugin to access to the Simple Image Access service.
 * 
 * <p>
 * Application for AstroGlobWeb Module. This application queries a Simple Image Access service 
 * by the use of (Healpix,order) parameters and it returns a GeoJson file.<br/>
 * <p>The cache directive is set to FOREVER</p>
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOSiaSearchApplicationPlugin extends AbstractApplicationPlugin {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OpenSearchVOSiaSearchApplicationPlugin.class.getName());

  /**
   * Maximum of characters that is allowed for the short name by the open search standard.
   */
  private static final int MAX_LENGTH_SHORTNAME = 16;
  /**
   * Maximum of characters that is allowed for the description by the open search standard.
   */
  private static final int MAX_LENGTH_DESCRIPTION = 1024;
  /**
   * Maximum of characters that is allowed for the tags by the open search standard.
   */
  private static final int MAX_LENGTH_TAGS = 256;  
  /**
   * Maximum of characters that is allowed for the longname by the open search standard.
   */
  private static final int MAX_LENGTH_LONGNAME = 48;  
  /**
   * Dictionary.
   */
  private transient Map<String, VoDictionary> dico = new HashMap<String, VoDictionary>();  
  /**
   * Constructor.
   */
  public OpenSearchVOSiaSearchApplicationPlugin() {
    super();
    constructor();
  }

  /**
   * Constructor.
   *
   * @param context Context
   */
  public OpenSearchVOSiaSearchApplicationPlugin(final Context context) {
    super(context);
    constructor();
  }

  /**
   * Constructor.
   *
   * @param context Context
   * @param model Plugin model
   */
  public OpenSearchVOSiaSearchApplicationPlugin(final Context context, final ApplicationPluginModel model) {
    super(context, model);
    try {
      final Category category = Category.valueOf(getParameter("category").getValue());
      if (!AbstractUtility.isSet(model.getCategory())) {
        model.setCategory(category);
      }
      setCategory(category);
    } catch (Exception ex) {
      LOG.log(Level.FINEST, ex.getMessage());
    }
    register();
  }

  /**
   * Constructor with all parameters.
   */
  private void constructor() {
    this.getModel().setClassAuthor("J-C Malapert");
    this.getModel().setClassName("VO OpenSearch Application for SIA");
    this.getModel().setClassOwner("CNES");
    this.getModel().setClassVersion("1.0");

    ApplicationPluginParameter param = new ApplicationPluginParameter();
    param.setName("category");
    param.setValue("user");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("shortName");
    param.setDescription("Contains a brief human-readable title that identifies this search engine");
    param.setValue("SITools2 search");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("description");
    param.setDescription("Contains a human-readable text description of the search engine.");
    param.setValue("SITools2 connector providing an open search capability");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("contact");
    param.setDescription("Contains an email address at which the maintener of the description document can be reached.");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("tags");
    param.setDescription("Contains a set of words that are used as keywords to identify and categorize this search content. "
            + "Tags must be a single word and are delimited by the space character.");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("longName");
    param.setDescription("Contains an extended human-readable title that identifies the search engine.");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("imagePng");
    param.setDescription("Contains a URL that identifies the location of an image that can be used in association with this search content.");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("imageIcon");
    param.setDescription("Contains a URL that identifies the location of an image that can be used in association with this search content.");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("syndicationRight");
    param.setDescription("Contains a value that indicates the degree to which the search result provided by this search engine can be queried, displayed, and redistributed.");
    param.setValueType("xs:enum[open, closed]");
    param.setValue("open");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("queryShape");
    param.setDescription("Contains a value that indicates the shape that will be given by the user.");
    param.setValueType(String.format("xs:enum[%s]", OpenSearchApplicationPlugin.GeometryShape.HEALPIX.getShape()));
    param.setValue(OpenSearchApplicationPlugin.GeometryShape.HEALPIX.getShape());
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("siaSearchURL");
    param.setDescription("The SIA Search URL");
    param.setValueType("String");
    param.setValue("http://archives.esac.esa.int/hst/hst-vo/hla_sia?REQUEST=queryData&");
    this.addParameter(param);
    
    param = new ApplicationPluginParameter();
    param.setName("cacheable");
    param.setDescription("Set to true when the result can be cached");
    param.setValueType("xs:enum[True,False]");
    param.setValue("False");
    this.addParameter(param);     
    SingletonCacheHealpixDataAccess.create();
  }

  @Override
  public final void sitoolsDescribe() {
    this.setName("VO OpenSearch Application for SIA");
    this.setAuthor("J-C Malapert");
    this.setOwner("CNES");
    this.setDescription("This application allows to configure an open search application based on a SIA Protocol.");
  }

  @Override
  public final Restlet createInboundRoot() {
    final Router router = new Router(getContext());
    router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
    router.attachDefault(fr.cnes.sitools.extensions.astro.application.OpenSearchVOSiaDescription.class);
    if (!getParameter("syndicationRight").getValue().equals("closed")) {
      //router.attach("/describe", fr.cnes.sitools.extensions.astro.application.OpenSearchDescribe.class);
      router.attach("/dico/{name}", fr.cnes.sitools.extensions.astro.application.OpenSearchVOSiaSearchDico.class);
      router.attach("/search", fr.cnes.sitools.extensions.astro.application.OpenSearchVOSiaSearch.class);
      attachParameterizedResources(router);
    }
    return router;
  }
  
  /**
   * Returns the dictionary.
   * @return the dictonary
   */
  public final Map<String, VoDictionary> getDico() {
    return this.dico;
  }  

  @Override
  public final Validator<AbstractApplicationPlugin> getValidator() {
    return new Validator<AbstractApplicationPlugin>() {
      @Override
      public final Set<ConstraintViolation> validate(final AbstractApplicationPlugin item) {
        final Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
        final Map<String, ApplicationPluginParameter> params = item.getModel().getParametersMap();
        final ApplicationPluginParameter shortName = params.get("shortName");
        if (!shortName.getValue().isEmpty() && (shortName.getValue().length() > MAX_LENGTH_SHORTNAME || shortName.getValue().contains("<")
                || shortName.getValue().contains(">"))) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("shortName");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 16 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter description = params.get("description");
        if (!description.getValue().isEmpty() && (description.getValue().length() > MAX_LENGTH_DESCRIPTION || description.getValue().contains("<")
                || description.getValue().contains(">"))) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("description");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 1024 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter contact = params.get("contact");
        if (contact.getValue().contains("@") && contact.getValue().contains(".")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("contact");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must be an email address");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter tags = params.get("tags");
        if (tags.getValue().length() > MAX_LENGTH_TAGS || tags.getValue().contains("<") || tags.getValue().contains(">")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("tags");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 256 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter longName = params.get("longName");
        if (longName.getValue().length() > MAX_LENGTH_LONGNAME || longName.getValue().contains("<") || longName.getValue().contains(">")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("longName");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 48 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter imagePng = params.get("imagePng");
        try {
          if (!imagePng.getValue().isEmpty()) {
            final URL url = new URL(imagePng.getValue());
          }
        } catch (MalformedURLException ex) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("imagePng");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage(ex.getMessage());
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter imageIcon = params.get("imageIcon");
        try {
          if (!imageIcon.getValue().isEmpty()) {
            final URL url = new URL(imageIcon.getValue());
          }
        } catch (MalformedURLException ex) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("imageIcon");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage(ex.getMessage());
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter syndicationRight = params.get("syndicationRight");
        if (!syndicationRight.getValue().equals("open") && !syndicationRight.getValue().equals("closed")
                && !syndicationRight.getValue().equals("private") && !syndicationRight.getValue().equals("limited")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("syndicationRight");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("syndicationRight must take one of the following values : open, private, limited, closed");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter siaSearchURL = params.get("siaSearchURL");
        if (siaSearchURL.getValue().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("siaSearchURL");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("A SIA search URL must be set.");
          constraintList.add(constraint);
        }
        return constraintList;
      }
    };
  }
}
