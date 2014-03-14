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
package fr.cnes.sitools.extensions.astro.application;

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
import org.restlet.engine.Engine;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import fr.cnes.sitools.common.model.Category;
import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.extensions.common.VoDictionary;
import fr.cnes.sitools.plugins.applications.business.AbstractApplicationPlugin;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;

/**
 * Plugin to access to the Simple Cone Search service.
 *
 * <p> Application for MIZAR Module. This application queries a Simple Cone Search service by the use of (Healpix,order) parameters
 * and it returns a GeoJson file.<br/> The cache directive is set to FOREVER </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOApplicationPlugin extends AbstractApplicationPlugin {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(OpenSearchVOApplicationPlugin.class.getName());
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
  private final transient Map<String, VoDictionary> dico = new HashMap<String, VoDictionary>();

  /**
   * List of supported protocols by the service.
   */
  public enum Protocol {
      /**
       * Detects automatically the protocol.
       */
      DETECT_AUTOMATICALLY,
      /**
       * Cone search protocol.
       */
      CONE_SEARCH_PROTOCOL,
      /**
       * Simple Image Access Protocol.
       */
      SIMPLE_IMAGE_ACCESS_PROTOCOL;
  }
  /**
   * Constructor.
   */
  public OpenSearchVOApplicationPlugin() {
    super();
    constructor();
  }

  /**
   * Constructor.
   *
   * @param context Context
   */
  public OpenSearchVOApplicationPlugin(final Context context) {
    super(context);
    constructor();
  }

  /**
   * Constructor.
   *
   * @param context Context
   * @param model Plugin model
   */
  public OpenSearchVOApplicationPlugin(final Context context, final ApplicationPluginModel model) {
    super(context, model);
    try {
      final Category category = Category.valueOf(getParameter("category").getValue());
      if (model.getCategory() == null) {
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
    this.getModel().setClassName("VO OpenSearch Application");
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
    param.setName("serviceURL");
    param.setDescription("URL of the VO service");
    param.setValueType("String");
    param.setValue("http://vizier.u-strasbg.fr/viz-bin/votable/-A?-source=I/284&");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("mocdescribe");
    param.setDescription("MOC URL");
    param.setValueType("String");
    param.setValue("http://alasky.u-strasbg.fr/footprints/tables/vizier/II_306_sdss8/MOC");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("protocol");
    param.setDescription("Set the service protocol");
    param.setValueType("xs:enum[" + Protocol.DETECT_AUTOMATICALLY.name() + "," + Protocol.CONE_SEARCH_PROTOCOL.name() + "," + Protocol.SIMPLE_IMAGE_ACCESS_PROTOCOL.name() + "]");
    param.setValue(Protocol.DETECT_AUTOMATICALLY.name());
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
    this.setName("VO OpenSearch Application");
    this.setAuthor("J-C Malapert");
    this.setOwner("CNES");
    this.setDescription("This application allows to configure an open search application based on a Cone Search Protocol or Simple Image Access Protocol.");
  }

  @Override
  public final Restlet createInboundRoot() {
    final Router router = new Router(getContext());
    router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
    router.attachDefault(fr.cnes.sitools.extensions.astro.application.opensearch.OpenSearchVODescription.class);
    if (!getParameter("syndicationRight").getValue().equals("closed")) {
      //router.attach("/describe", fr.cnes.sitools.extensions.astro.application.OpenSearchDescribe.class);
      router.attach("/search", fr.cnes.sitools.extensions.astro.application.opensearch.OpenSearchVOSearch.class);
      router.attach("/dico/{name}", fr.cnes.sitools.extensions.astro.application.opensearch.OpenSearchVODico.class);
      router.attach("/moc", fr.cnes.sitools.extensions.astro.application.opensearch.VoMocDescription.class);      
      attachParameterizedResources(router);
    }
    return router;
  }

  /**
   * Returns the dictionary.
   *
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
        if (!shortName.getValue().isEmpty() && (shortName.getValue().length() > MAX_LENGTH_SHORTNAME || shortName.getValue().contains("<") || shortName.getValue().contains(">"))) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("shortName");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 16 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter description = params.get("description");
        if (!description.getValue().isEmpty() && (description.getValue().length() > MAX_LENGTH_DESCRIPTION
                || description.getValue().contains("<") || description.getValue().contains(">"))) {
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
        if (tags.getValue().length() > MAX_LENGTH_TAGS || tags.getValue().contains("<")
                || tags.getValue().contains(">")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("tags");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 256 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter longName = params.get("longName");
        if (longName.getValue().length() > MAX_LENGTH_LONGNAME || longName.getValue().contains("<")
                || longName.getValue().contains(">")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("longName");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 48 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter imagePng = params.get("imagePng");
        try {
          if (!imagePng.getValue().isEmpty()) {
            new URL(imagePng.getValue());
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
            new URL(imageIcon.getValue());
          }
        } catch (MalformedURLException ex) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("imageIcon");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage(ex.getMessage());
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter syndicationRight = params.get("syndicationRight");
        if (!syndicationRight.getValue().equals("open") && !syndicationRight.getValue().equals("closed") && !syndicationRight.getValue().equals("private")
                && !syndicationRight.getValue().equals("limited")) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("syndicationRight");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("syndicationRight must take one of the following values : open, private, limited, closed");
          constraintList.add(constraint);
        }
        final ApplicationPluginParameter serviceURL = params.get("serviceURL");
        if (serviceURL.getValue().isEmpty()) {
          final ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("serviceURL");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("A service URL must be set.");
          constraintList.add(constraint);
        }
        return constraintList;
      }
    };
  }
}
