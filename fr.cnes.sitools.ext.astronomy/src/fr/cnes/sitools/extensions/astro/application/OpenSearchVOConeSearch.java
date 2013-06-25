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
package fr.cnes.sitools.extensions.astro.application;

import fr.cnes.sitools.astro.representation.GeoJsonRepresentation;
import fr.cnes.sitools.astro.vo.conesearch.ConeSearchProtocolLibrary;
import fr.cnes.sitools.astro.vo.conesearch.ConeSearchQuery;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.cache.CacheBrowser;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.AstroCoordinate.CoordinateSystem;
import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.extensions.common.VoDictionary;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Field;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Provides a search capability on a Simple Cone Search service by the use of (healpix,order) parameters.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOConeSearch extends SitoolsParameterizedResource {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OpenSearchVOConeSearch.class.getName());
  /**
   * Coordinate system for both inputs and result.
   */
  private CoordinateSystem coordinatesSystem;  
  /**
   * Query.
   */
  private transient ConeSearchQuery query;
  /**
   * User parameters.
   */
  private transient UserParameters userParameters;
  /**
   * Dictionary.
   */
  private transient Map<String, VoDictionary> dico;

  @Override
  public final void doInit() {
    try {
      super.doInit();
      final String url = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getModel().getParametersMap().get("coneSearchURL").getValue();
      this.query = new ConeSearchQuery(url);
      if (!getRequest().getMethod().equals(Method.OPTIONS)) {
        if (getRequestAttributes().containsKey("coordSystem")) {
            this.coordinatesSystem = CoordinateSystem.valueOf(String.valueOf(getRequestAttributes().get("coordSystem")));                    
        } else {
            throw new Exception("coordSystem attribute must exist.");
        }           
        this.userParameters = new UserParameters(getRequest().getResourceRef().getQueryAsForm());
        this.dico = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getDico();
      }
    } catch (Exception ex) {
      LOG.log(Level.WARNING, null, ex);
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, ex.getMessage());
    }
  }

  /**
   * Reserved keywords in CSP for which a specific processing is needed.
   */
  public enum ReservedWords {

    /**
     * Right Ascension.
     */
    POS_EQ_RA_MAIN(Arrays.asList("POS_EQ_RA_MAIN", "pos.eq.ra;meta.main")),
    /**
     * Declination.
     */
    POS_EQ_DEC_MAIN(Arrays.asList("POS_EQ_DEC_MAIN", "pos.eq.dec;meta.main")),
    /**
     * ID.
     */
    ID_MAIN(Arrays.asList("ID_MAIN", "meta.id;meta.main")),
    /**
     * None.
     */
    NONE(Arrays.asList(""));
    /**
     * List of keywords for an item.
     */
    private final List<String> names;

    /**
     * Constructor.
     *
     * @param nameVals List of keywords
     */
    ReservedWords(final List<String> nameVals) {
      this.names = nameVals;
    }

    /**
     * Returns a list of keywords related to an item.
     *
     * @return list of keywords
     */
    public List<String> getName() {
      return this.names;
    }

    /**
     * Finds the enum from one of its keywords.
     *
     * @param keyword keyword to match
     * @return the enum
     */
    public static OpenSearchVOConeSearch.ReservedWords find(final String keyword) {
      OpenSearchVOConeSearch.ReservedWords response = OpenSearchVOConeSearch.ReservedWords.NONE;
      final OpenSearchVOConeSearch.ReservedWords[] words = OpenSearchVOConeSearch.ReservedWords.values();
      for (int i = 0; i < words.length; i++) {
        final OpenSearchVOConeSearch.ReservedWords word = words[i];
        final List<String> reservedName = word.getName();
        if (reservedName.contains(keyword)) {
          response = word;
          break;
        }
      }
      return response;
    }
  }

  /**
   * Returns the JSON representation.
   *
   * @return the JSON representation
   */
  @Get
  public final Representation getJsonResponse() {
    try {
      double rightAscension = userParameters.getRa();
      double declination = userParameters.getDec();
      if (this.coordinatesSystem == CoordinateSystem.GALACTIC) {
          AstroCoordinate astroCoordinates = new AstroCoordinate(rightAscension, declination);
          astroCoordinates.transformTo(CoordinateSystem.EQUATORIAL);
          rightAscension = astroCoordinates.getRaAsDecimal();
          declination = astroCoordinates.getDecAsDecimal();
      }         
      final double radius = userParameters.getSr();
      final List<Map<Field, String>> response = useCacheHealpix(query, rightAscension, declination, radius, true);
      final Map dataModel = createGeoJsonDataModel(response);
      Representation rep = new GeoJsonRepresentation(dataModel);
      rep = useCacheBrowser(rep, cacheIsEnabled());
      return rep;
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
    }
  }

  /**
   * Creates and returns the GeoJson data model from the response.
   * @param response response coming from the cache or the VO server
   * @return GeoJson data model
   */
  private Map createGeoJsonDataModel(final List<Map<Field, String>> response) {
    final Map dataModel = new HashMap();
    final List features = new ArrayList();
    final AstroCoordinate astroCoordinates = new AstroCoordinate();
    final Set<Field> fields = getFields(response);
    fillDictionary(fields);
    for (Map<Field, String> iterDoc : response) {
      final Map geometry = new HashMap();
      final Map properties = new HashMap();
      final Map feature = new HashMap();
      final Iterator<Field> fieldIter = fields.iterator();
      double raResponse = Double.NaN;
      double decResponse = Double.NaN;

      while (fieldIter.hasNext()) {
        final Field field = fieldIter.next();
        final net.ivoa.xml.votable.v1.DataType dataType = field.getDatatype();
        final String ucd = field.getUcd();
        final String value = iterDoc.get(field);
        if (Utility.isSet(value) && !value.isEmpty()) {
          final Object responseDataType = Utility.getDataType(dataType, value);
          final OpenSearchVOConeSearch.ReservedWords ucdWord = OpenSearchVOConeSearch.ReservedWords.find(ucd);
          switch (ucdWord) {
            case POS_EQ_RA_MAIN:
              raResponse = Utility.parseRaVO(iterDoc, field);
              break;
            case POS_EQ_DEC_MAIN:
              decResponse = Utility.parseDecVO(iterDoc, field);
              break;
            case ID_MAIN:
              properties.put("identifier", iterDoc.get(field));
              break;
            default:
              properties.put(field.getName(), responseDataType);
              break;
          }
        }
      }
      if (this.coordinatesSystem == CoordinateSystem.GALACTIC) {          
          astroCoordinates.setRaAsDecimal(raResponse);
          astroCoordinates.setDecAsDecimal(decResponse);
          astroCoordinates.processTo(CoordinateSystem.GALACTIC);
          raResponse = astroCoordinates.getRaAsDecimal();
          decResponse = astroCoordinates.getDecAsDecimal();
      }
      geometry.put("coordinates", String.format("[%s,%s]", raResponse, decResponse));
      geometry.put("type", "Point");
      geometry.put("crs", this.coordinatesSystem.getCrs());
      feature.put("geometry", geometry);
      feature.put("properties", properties);
      spatialFilter(feature, raResponse, decResponse);
      if (!feature.isEmpty()) {
        features.add(feature);
      }
    }
    dataModel.put("features", features);
    dataModel.put("totalResults", features.size());    
    return dataModel;
  }

  /**
   * Returns the representation with cache directives cache parameter is set to enable.
   *
   * @param rep representation to cache
   * @param isEnabled True when the cache is enabled
   * @return the representation with the cache directive when the cache is enabled
   */
  private Representation useCacheBrowser(final Representation rep, final boolean isEnabled) {
    Representation cachedRepresentation = rep;
    if (isEnabled) {
      final CacheBrowser cache = CacheBrowser.createCache(CacheBrowser.CacheDirectiveBrowser.DAILY, rep);
      getResponse().setCacheDirectives(cache.getCacheDirectives());
      cachedRepresentation = cache.getRepresentation();
    }
    return cachedRepresentation;
  }

  /**
   * Returns the response from the cache or from the VO service.
   * 
   * <p>
   * A cache is always done with the option cacheable=false.<br/>
   * When cacheable is true then the cache VOservices is used. In this cache
   * the results are loaded to the VO servers once a day.<br/>
   * When cacheable is false then the cache VOservices#solarBodies is used. In this cache
   * the results are loaded to the VO servers each two minutes.
   * </p>
   * @param csQuery cone search query
   * @param rightAscension right ascension of the cone's center
   * @param declination declination of the cone's center
   * @param radius radius of the cone
   * @param isEnabled cache enable or disable
   * @return the response from the cache or from the VO service
   * @throws Exception - an error occurs dwhen calling the server
   */
  private List<Map<Field, String>> useCacheHealpix(final ConeSearchQuery csQuery, final double rightAscension, final double declination, final double radius, final boolean isEnabled) throws Exception {
    final String applicationID = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getId();
    final String cacheID = SingletonCacheHealpixDataAccess.generateId(applicationID, String.valueOf(userParameters.getOrder()), String.valueOf(userParameters.getHealpix()));
    final CacheManager cacheManager = SingletonCacheHealpixDataAccess.getInstance();
    final Cache cache = (isEnabled) ? cacheManager.getCache("VOservices") : cacheManager.getCache("VOservices#solarBodies");
    List<Map<Field, String>> response;
    if (cache.isKeyInCache(cacheID)) {
      LOG.log(Level.INFO, "Use of the cache for ID {0}", cacheID);
      response = (List<Map<Field, String>>) cache.get(cacheID).getObjectValue();
    } else {
      response = csQuery.getResponseAt(rightAscension, declination, radius);
      LOG.log(Level.INFO, "Caching result for ID {0}", cacheID);
      final Element element = new Element(cacheID, response);
      cache.put(element);
    }
    return response;
  }

  /**
   * Returns True when the cache is enabled otherwise False.
   *
   * @return True when the cache is enabled otherwise False
   */
  private boolean cacheIsEnabled() {
    return Boolean.parseBoolean(((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getParameter("cacheable").getValue());
  }

  /**
   * Filters the response.
   *
   * <p> the
   * <code>feature</code> is cleared : <ul> <li>when the query mode is Healpix and the record is not in the queried Healpix pixel</li>
   * <li>when is not valid</li> </ul> </p>
   *
   * @param feature record
   * @param rightAscension right asciension
   * @param declination declination
   * @see #isValid(java.util.Map)
   */
  private void spatialFilter(final Map feature, final double rightAscension, final double declination) {
    if (isValid(feature)) {
      if (this.userParameters.isHealpixMode() && !isPointIsInsidePixel(rightAscension, declination)) {
        LOG.log(Level.FINE, "This record {0} is ignored.", feature.toString());
        feature.clear();
      }
    } else {
      LOG.log(Level.WARNING, "The record record {0} is not valid : No identifier found in the response.", feature.toString());
      feature.clear();
    }
  }

  /**
   * Returns the list of distinct fields from the response. <p> if response is empty, then returns an empty list. </p>
   *
   * @param response respone
   * @return the list of distinct fields from the response
   */
  private Set<Field> getFields(final List<Map<Field, String>> response) {
    Set<Field> fields = new HashSet<Field>();
    if (!response.isEmpty()) {
      final Map<Field, String> map = response.get(0);
      fields = map.keySet();
    }
    return fields;
  }

  /**
   * Parses the description TAG of each field and sets it to
   * <code>dico</code>.
   *
   * @param fields keywords of the response
   */
  private void fillDictionary(final Set<Field> fields) {
    final Iterator<Field> fieldIter = fields.iterator();
    while (fieldIter.hasNext()) {
      final Field field = fieldIter.next();
      final String description = (field.getDESCRIPTION() == null) ? null : field.getDESCRIPTION().getContent().get(0).toString();
      this.dico.put(field.getName(), new VoDictionary(description, field.getUnit()));
    }
  }

  /**
   * Returns true when identifier is set.
   *
   * @param feature data model
   * @return true when identifier os set
   */
  private boolean isValid(final Map feature) {
    return ((Map) feature.get("properties")).containsKey("identifier");
  }

  /**
   * Returns true when the point (rightAscension,declination) is inside the pixel otherwise false.
   *
   * @param rightAscension right ascension in degree
   * @param declination declination in degree
   * @return true when the point (rightAscension,declination) is inside the pixel otherwise false
   */
  private boolean isPointIsInsidePixel(final double rightAscension, final double declination) {
    boolean result;
    try {
      final long healpixFromService = this.userParameters.getHealpixIndex().ang2pix_nest(Math.PI / 2 - Math.toRadians(declination), Math.toRadians(rightAscension));
      result = (healpixFromService == this.userParameters.getHealpix()) ? true : false;
    } catch (Exception ex) {
      result = false;
      LOG.log(Level.WARNING, null, ex);
    }
    return result;
  }

  /**
   * User parameters.
   */
  public class UserParameters {

    /**
     * Value when Healpix is not defined.
     */
    public static final int NOT_DEFINED = -1;
    /**
     * Multiplation factor to embed the entire Healpix pixel in the ROI.
     */
    private static final double MULT_FACT = 1.5;
    /**
     * Max value in degree of declination axis.
     */
    private static final double MAX_DEC = 90.;
    /**
     * One degree.
     */
    private static final double ONE_DEG = 1.0;
    /**
     * One degree in arsec.
     */
    private static final double ONE_DEG_IN_ARSEC = 3600.;
    /**
     * Arcsec to degree conversion.
     */
    private static final double ARCSEC2DEG = ONE_DEG / ONE_DEG_IN_ARSEC;
    /**
     * right ascension.
     */
    private transient double rightAscension;
    /**
     * declination.
     */
    private transient double declination;
    /**
     * radius.
     */
    private transient double radius;
    /**
     * Healpix order.
     */
    private transient int order;
    /**
     * Healpix pixel.
     */
    private transient long healpix;
    /**
     * Healpix index.
     */
    private transient HealpixIndex healpixIndex;
    /**
     * Healpix is used.
     */
    private transient boolean isHealpixMode;

    /**
     * Constructor.
     *
     * @param form form
     * @throws Exception Exception
     */
    public UserParameters(final Form form) throws Exception {
      computeInputChoice(form);
    }

    /**
     * Computes input parameters.
     *
     * @param form form
     * @throws Exception Exception
     */
    private void computeInputChoice(final Form form) throws Exception {
      final String raInput = form.getFirstValue(ConeSearchProtocolLibrary.RA);
      final String decInput = form.getFirstValue(ConeSearchProtocolLibrary.DEC);
      final String srInput = form.getFirstValue(ConeSearchProtocolLibrary.SR);
      final String healpixInput = form.getFirstValue("healpix");
      final String orderInput = form.getFirstValue("order");
      if (raInput != null && decInput != null && srInput != null) {
        this.rightAscension = Double.valueOf(raInput);
        this.declination = Double.valueOf(decInput);
        this.radius = Double.valueOf(srInput);
        this.isHealpixMode = false;
        this.order = NOT_DEFINED;
        this.healpix = NOT_DEFINED;
        this.healpixIndex = null;
      } else if (healpixInput != null && orderInput != null) {
        this.healpix = Long.valueOf(healpixInput);
        this.order = Integer.valueOf(orderInput);
        final int nside = (int) Math.pow(2, order);
        final double pixRes = HealpixIndex.getPixRes(nside) * ARCSEC2DEG;
        this.healpixIndex = new HealpixIndex(nside, Scheme.NESTED);
        this.isHealpixMode = true;
        final Pointing pointing = healpixIndex.pix2ang(healpix);
        this.rightAscension = Math.toDegrees(pointing.phi);
        this.declination = MAX_DEC - Math.toDegrees(pointing.theta);
        this.radius = pixRes * MULT_FACT;
      } else {
        throw new Exception("wrong input parameters");
      }
    }

    /**
     * Returns the Healpix index.
     *
     * @return the Healpix index
     */
    public final HealpixIndex getHealpixIndex() {
      return this.healpixIndex;
    }

    /**
     * Returns true when Healpix mode is used otherwise false.
     *
     * @return true when Healpix mode is used otherwise false
     */
    public final boolean isHealpixMode() {
      return this.isHealpixMode;
    }

    /**
     * Returns the Healpix order.
     *
     * @return the Healpix order
     */
    public final int getOrder() {
      return this.order;
    }

    /**
     * Returns the Healpix pixel.
     *
     * @return the Healpix pixel
     */
    public final long getHealpix() {
      return this.healpix;
    }

    /**
     * Returns the right ascension in degree.
     *
     * @return the right ascension
     */
    public final double getRa() {
      return this.rightAscension;
    }

    /**
     * Returns the declination in degree.
     *
     * @return the declination
     */
    public final double getDec() {
      return this.declination;
    }

    /**
     * Return the radius in degree.
     *
     * @return the radius
     */
    public final double getSr() {
      return this.radius;
    }
  }

  @Override
  public final void sitoolsDescribe() {
    setName("Cone Search service.");
    setDescription("Retrieves and transforms a response from a Cone Search service.");
  }

  /**
   * Describes GET method in the WADL.
   *
   * @param info information
   */
  @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("ConeSearchProtocolJSON");
    info.setDocumentation("Interoperability service to distribute images through a converted format of the Cone Search Protocol");

    final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo("healpix", true, "long", ParameterStyle.QUERY,
            "Healpix number"));
    parametersInfo.add(new ParameterInfo("order", true, "integer", ParameterStyle.QUERY,
            "Healpix order"));
    ParameterInfo json = new ParameterInfo("format", true, "string", ParameterStyle.QUERY, "JSON format");
    json.setFixed("json");
    parametersInfo.add(json);

    info.getRequest().setParameters(parametersInfo);

    // represensation when the response is fine
    final ResponseInfo responseOK = new ResponseInfo();

    final DocumentationInfo documentation = new DocumentationInfo();
    documentation.setTitle("GeoJSON");
    documentation.setTextContent("<pre>{\n"
            + "totalResults: 1,\n"
            + "type: \"FeatureCollection\",\n"
            + "features: [\n"
            + "  geometry: {\n"
            + "    coordinates: [10.6847083,41.26875],\n"
            + "    type: \"Point\"\n"
            + "  },\n"
            + "properties: {\n"
            + "  crs: {\n"
            + "    type: \"name\",\n"
            + "    properties: {\n"
            + "      name: \"EQUATORIAL.ICRS\"\n"
            + "    }\n"
            + "  },\n"
            + "  identifier: \"CDS0\"\n"
            + "}\n"
            + "}]}</pre>");

    final List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    final RepresentationInfo representationInfo = new RepresentationInfo(MediaType.APPLICATION_JSON);
    representationInfo.setDocumentation(documentation);
    representationsInfo.add(representationInfo);
    responseOK.setRepresentations(representationsInfo);
    responseOK.getStatuses().add(Status.SUCCESS_OK);

    // represensation when the response is not fine
    final ResponseInfo responseNOK = new ResponseInfo();
    final RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
    representationInfoError.setReference("error");

    responseNOK.getRepresentations().add(representationInfoError);
    responseNOK.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
    responseNOK.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);

    info.setResponses(Arrays.asList(responseOK, responseNOK));
  }
}
