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
package fr.cnes.sitools.extensions.astro.application;

import fr.cnes.sitools.common.model.Category;
import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.plugins.applications.business.AbstractApplicationPlugin;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 * Plugin to access to observations from SOLR server.
 *
 * <p> Application for AstroGlobWeb Module. This application queries a SOLR server by the use of (Healpix,order) parameters and it returns a
 * GeoJson file. </p>.
 *
 * @author Jean-Christophe Malapert
 */
public class OpenSearchApplicationPlugin extends AbstractApplicationPlugin {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OpenSearchApplicationPlugin.class.getName());

  /**
   * Supported keywords by open search.
   */
  public enum Standard_Open_Search {

    /**
     * Identifer of the GeoJson record.
     */
    PROPERTIES_IDENTIFIER("properties.identifier", "identifier", "properties"),
    /**
     * Title of the GeoJson record.
     */
    PROPERTIES_TITLE("properties.title", "title", "properties"),
    /**
     * Description of the GeoJson record.
     */
    PROPERTIES_DESCRIPTION("properties.description", "description", "properties"),
    /**
     * Elevation (for Earth Observation).
     */
    PROPERTIES_ELE("properties.ele", "ele", "properties"),
    /**
     * URL to the thumbnail.
     */
    PROPERTIES_THUMBNAIL("properties.thumbnail", "thumbnail", "properties"),
    /**
     * URL to the quicklook.
     */
    PROPERTIES_QUICKLOOK("properties.quicklook", "quicklook", "properties"),
    /**
     * URL to the icon.
     */
    PROPERTIES_ICON("properties.icon", "icon", "properties"),
    /**
     * URL to the file to dowbload.
     */
    PROPERTIES_SERVICES_DOWNLOAD("properties.services.download.url", "url", "download"),
    /**
     * Mime type to the file to download.
     */
    PROPERTIES_SERVICES_DOWNLOAD_MIMETYPE("properties.services.download.mimetype", "mimetype", "download"),
    /**
     * Title of the Tiled file.
     */
    PROPERTIES_SERVICES_BROWSE_TITLE("properties.services.browse.title", "title", "browse"),
    /**
     * Title of the layer type.
     */
    PROPERTIES_SERVICES_BROWSE_LAYER_TYPE("properties.services.browse.layer.type", "type", "layer"),
    /**
     * URL to layer service.
     */
    PROPERTIES_SERVICES_BROWSE_LAYER_URL("properties.services.browse.layer.url", "url", "layer"),
    /**
     * Opacity of the layer.
     */
    PROPERTIES_SERVICES_BROWSE_LAYER_OPACITY("properties.services.browse.layer.opacity", "opacity", "layer"),
    /**
     * Healpix order from which data is requested from the client.
     */
    PROPERTIES_SERVICES_BROWSE_LAYER_MINLEVEL("properties.services.browse.layer.minlevel", "minLevel", "layer"),
    PROPERTIES_SERVICES_BROWSE_LAYER_LAYERS("properties.services.browse.layer.layers", "layers", "layer"),
    /**
     * Layer version.
     */
    PROPERTIES_SERVICES_BROWSE_LAYER_VERSION("properties.services.browse.layer.version", "version", "layer"),
    /**
     * BBOX of the geometry.
     */
    PROPERTIES_SERVICES_BROWSE_LAYER_BBOX("properties.services.browse.layer.bbox", "bbox", "layer"),
    /**
     * Coordinates reference system.
     */
    PROPERTIES_SERVICES_BROWSE_LAYER_SRS("properties.services.browse.layer.srs", "srs", "layer"),
    /**
     * Geometry of the observation.
     */
    GEOMETRY_COORDINATES("geometry.coordinates", "coordinates", "geometry"),
    /**
     * Geometry type.
     */
    GEOMETRY_COORDINATES_TYPE("geometry.coordinates.type", "type", "geometry"),
    /**
     * Right ascension in degree.
     */
    PROPERTIES_RA("properties.ra", "ra", "properties"),
    /**
     * Declination in degree.
     */
    PROPERTIES_DEC("properties.dec", "dec", "properties");
    private final String keywordSolr;
    private final String keyword;
    private final String node;

    Standard_Open_Search(final String keywordSolr, final String keyword, final String node) {
      this.keywordSolr = keywordSolr;
      this.keyword = keyword;
      this.node = node;
    }

    public String getKeywordSolr() {
      return this.keywordSolr;
    }

    public String getKeyword() {
      return this.keyword;
    }

    public String getNode() {
      return this.node;
    }

    /**
     * Get the node and the keyword relative to a node.
     *
     * @param keywordSolr Keyword from SOLR data model
     * @return Returns an array [node name, keyword]
     */
    public static String[] getKeywordProperties(final String keywordSolr) {
      String result[] = new String[2];
      result[0] = "properties"; // no standard fields are in properties node
      Standard_Open_Search[] standardOpenSearchArray = Standard_Open_Search.values();
      for (int i = 0; i < standardOpenSearchArray.length; i++) {
        Standard_Open_Search standardOpenSearch = standardOpenSearchArray[i];
        if (standardOpenSearch.getKeywordSolr().equals(keywordSolr)) {
          result[0] = standardOpenSearch.getNode();
          result[1] = standardOpenSearch.getKeyword();
          return result;
        }
      }
      result[1] = keywordSolr.replace("properties.nostandard.", "");
      return result;
    }
  }

  public enum GeometryShape {

    CONE("cone", "radius"),
    POLYGON("polygon", null),
    BBOX("bbox", null),
    HEALPIX("healpix", "order");
    private final String shape;
    private final String order;

    GeometryShape(String shape, String order) {
      this.shape = shape;
      this.order = order;
    }

    public String getShape() {
      return this.shape;
    }

    public String getOrder() {
      return this.order;
    }

    public static GeometryShape getGeometryShapeFrom(String value) {
      GeometryShape result = null;
      GeometryShape[] shapes = GeometryShape.values();
      for (GeometryShape shape : shapes) {
        if (shape.getShape().equals(value) || value.equals(shape.getOrder())) {
          result = shape;
        }
      }
      return result;
    }

    public String getOpenSearchDescription(String coordsystem) {
      String result;
      if (getOrder() != null) {
        result = String.format("%s={%s:%s?}&amp;%s={%s:%s?}", getShape(), coordsystem, getShape(), getOrder(), coordsystem, getOrder());
      } else {
        result = String.format("%s={%s:%s?}", getShape(), coordsystem, getShape());
      }
      return result;
    }
  }

  /**
   * Constructor
   */
  public OpenSearchApplicationPlugin() {
    super();
    constructor();
  }

  /**
   * Constructor
   *
   * @param context Context
   */
  public OpenSearchApplicationPlugin(Context context) {
    super(context);
    constructor();
  }

  /**
   * Constructor
   *
   * @param context Context
   * @param model Plugin model
   */
  public OpenSearchApplicationPlugin(Context context, ApplicationPluginModel model) {
    super(context, model);
    try {
      Category category = Category.valueOf(getParameter("category").getValue());
      if (model.getCategory() == null) {
        model.setCategory(category);
      }
      setCategory(category);
    } catch (Exception ex) {
    }
    register();
  }

  /**
   * Constructor with all parameters
   */
  private void constructor() {
    this.getModel().setClassAuthor("J-C Malapert");
    this.getModel().setClassName("OpenSearch Application");
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
    param.setDescription("Contains a set of words that are used as keywords to identify and categorize this search content. Tags must be a single word and are delimited by the space character.");
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
    param.setName("referenceSystem");
    param.setDescription("Contains a value that indicates the reference system that is used. It could be geographic or ICRS");
    param.setValueType("xs:enum[geographic, ICRS]");
    param.setValue("ICRS");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("queryShape");
    param.setDescription("Contains a value that indicates the shape that will be given by the user.");
    param.setValueType(String.format("xs:enum[%s, %s, %s, %s]", GeometryShape.BBOX.getShape(), GeometryShape.CONE.getShape(), GeometryShape.HEALPIX.getShape(), GeometryShape.POLYGON.getShape()));
    param.setValue(GeometryShape.BBOX.getShape());
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("solrCore");
    param.setDescription("Set the SOLR core");
    this.addParameter(param);

    param = new ApplicationPluginParameter();
    param.setName("healpixScheme");
    param.setDescription("Set the Healpix Scheme sored in SOLR core");
    param.setValueType("xs:enum[RING, NESTED]");
    param.setValue("RING");
    this.addParameter(param);

  }

  @Override
  public void sitoolsDescribe() {
    this.setName("OpenSearch Application");
    this.setAuthor("J-C Malapert");
    this.setOwner("CNES");
    this.setDescription("This application allows to configure an open search application based on a SOLR index.");

  }

  @Override
  public Restlet createInboundRoot() {
    Router router = new Router(getContext());
    router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
    router.attachDefault(fr.cnes.sitools.extensions.astro.application.OpenSearchDescription.class);
    if (!getParameter("syndicationRight").getValue().equals("closed")) {
      router.attach("/describe", fr.cnes.sitools.extensions.astro.application.OpenSearchDescribe.class);
      router.attach("/search", fr.cnes.sitools.extensions.astro.application.OpenSearchSearch.class);
      router.attach("/cluster/search", fr.cnes.sitools.extensions.astro.application.OpenSearchClusterSearch.class);
      router.attach("/moc", fr.cnes.sitools.extensions.astro.application.MocDescription.class);
      attachParameterizedResources(router);
    }
    attachParameterizedResources(router);
    return router;
  }

  @Override
  public Validator<AbstractApplicationPlugin> getValidator() {
    return new Validator<AbstractApplicationPlugin>() {
      @Override
      public Set<ConstraintViolation> validate(AbstractApplicationPlugin item) {
        Set<ConstraintViolation> constraintList = new HashSet<ConstraintViolation>();
        Map<String, ApplicationPluginParameter> params = item.getModel().getParametersMap();
        ApplicationPluginParameter shortName = params.get("shortName");
        if (!shortName.getValue().isEmpty() && (shortName.getValue().length() > 16 || shortName.getValue().contains("<") || shortName.getValue().contains(">"))) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("shortName");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 16 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        ApplicationPluginParameter description = params.get("description");
        if (!description.getValue().isEmpty() && (description.getValue().length() > 1024 || description.getValue().contains("<") || description.getValue().contains(">"))) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("description");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 1024 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        ApplicationPluginParameter contact = params.get("contact");
        if (contact.getValue().contains("@") && contact.getValue().contains(".")) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("contact");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must be an email address");
          constraintList.add(constraint);
        }
        ApplicationPluginParameter tags = params.get("tags");
        if (tags.getValue().length() > 256 || tags.getValue().contains("<") || tags.getValue().contains(">")) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("tags");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 256 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        ApplicationPluginParameter longName = params.get("longName");
        if (longName.getValue().length() > 48 || longName.getValue().contains("<") || longName.getValue().contains(">")) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("longName");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("The value must contain 48 of fewer characters of plain text. The value must not contain HTML or other markup");
          constraintList.add(constraint);
        }
        ApplicationPluginParameter imagePng = params.get("imagePng");
        try {
          if (!imagePng.getValue().isEmpty()) {
            URL url = new URL(imagePng.getValue());
          }
        } catch (MalformedURLException ex) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("imagePng");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage(ex.getMessage());
          constraintList.add(constraint);
        }
        ApplicationPluginParameter imageIcon = params.get("imageIcon");
        try {
          if (!imageIcon.getValue().isEmpty()) {
            URL url = new URL(imageIcon.getValue());
          }
        } catch (MalformedURLException ex) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("imageIcon");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage(ex.getMessage());
          constraintList.add(constraint);
        }
        ApplicationPluginParameter syndicationRight = params.get("syndicationRight");
        if (!syndicationRight.getValue().equals("open") && !syndicationRight.getValue().equals("closed") && !syndicationRight.getValue().equals("private") && !syndicationRight.getValue().equals("limited")) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("syndicationRight");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("syndicationRight must take one of the following values : open, private, limited, closed");
          constraintList.add(constraint);
        }
        ApplicationPluginParameter solrCore = params.get("solrCore");
        if (solrCore.getValue().isEmpty()) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("solrCore");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("A SOLR core must be set");
          constraintList.add(constraint);
        }
        ApplicationPluginParameter referencesystem = params.get("referenceSystem");
        if (!referencesystem.getValue().equals("ICRS") && !referencesystem.getValue().equals("geographic")) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("referenceSystem");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("referenceSystem must take one of the following values : ICRS, geographic");
          constraintList.add(constraint);
        }
        ApplicationPluginParameter healpixScheme = params.get("healpixScheme");
        if (!healpixScheme.getValue().equals("RING") && !healpixScheme.getValue().equals("NESTED")) {
          ConstraintViolation constraint = new ConstraintViolation();
          constraint.setValueName("healpixScheme");
          constraint.setLevel(ConstraintViolationLevel.CRITICAL);
          constraint.setMessage("healpixScheme must take one of the following values : RING, NESTED");
          constraintList.add(constraint);
        }
        return constraintList;
      }
    };
  }
}
