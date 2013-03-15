/**
 * *****************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.sitools.astro.resolver.AbstractNameResolver.CoordinateSystem;
import fr.cnes.sitools.astro.vo.conesearch.ConeSearchProtocolLibrary;
import fr.cnes.sitools.astro.vo.conesearch.ConeSearchQuery;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.common.Utility;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Field;
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
   * Query.
   */
  private ConeSearchQuery query;
  /**
   * User parameters.
   */
  private UserParameters userParameters;

  @Override
  public final void doInit() {
    try {
      super.doInit();
      String url = ((OpenSearchVOConeSearchApplicationPlugin) getApplication()).getModel().getParametersMap().get("coneSearchURL").getValue();
      this.query = new ConeSearchQuery(url);
      if (!getRequest().getMethod().equals(Method.OPTIONS)) {
        this.userParameters = new UserParameters(getRequest().getResourceRef().getQueryAsForm());
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
    POS_EQ_RA_MAIN("POS_EQ_RA_MAIN"),
    /**
     * Declination.
     */
    POS_EQ_DEC_MAIN("POS_EQ_DEC_MAIN"),
    /**
     * ID.
     */
    ID_MAIN("ID_MAIN"),
    /**
     * None.
     */
    NONE("");
    /**
     * name.
     */
    private final String name;

    /**
     * Constructor.
     *
     * @param nameVal name
     */
    ReservedWords(final String nameVal) {
      this.name = nameVal;
    }

    /**
     * Returns name.
     *
     * @return name
     */
    public String getName() {
      return this.name;
    }

    /**
     * Finds the enum from its keyword.
     *
     * @param keyword keyword
     * @return the enum
     */
    public static OpenSearchVOConeSearch.ReservedWords find(final String keyword) {
      OpenSearchVOConeSearch.ReservedWords response = OpenSearchVOConeSearch.ReservedWords.NONE;
      OpenSearchVOConeSearch.ReservedWords[] words = OpenSearchVOConeSearch.ReservedWords.values();
      for (int i = 0; i < words.length; i++) {
        OpenSearchVOConeSearch.ReservedWords word = words[i];
        String reservedName = word.getName();
        if (keyword.equals(reservedName)) {
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
      double ra = userParameters.getRa();
      double dec = userParameters.getDec();
      double sr = userParameters.getSr();
      List<Map<Field, String>> response = this.query.getResponseAt(ra, dec, sr);
      Map dataModel = new HashMap();
      List features = new ArrayList();
      dataModel.put("totalResults", response.size());
      for (Map<Field, String> iterDoc : response) {
        Map geometry = new HashMap();
        Map properties = new HashMap();
        Map feature = new HashMap();
        Set<Field> fields = iterDoc.keySet();
        Iterator<Field> fieldIter = fields.iterator();
        double raValue = Double.NaN;
        double decValue = Double.NaN;

        while (fieldIter.hasNext()) {
          Field field = fieldIter.next();
          net.ivoa.xml.votable.v1.DataType dataType = field.getDatatype();
          String ucd = field.getUcd();
          String value = iterDoc.get(field);
          if (Utility.isSet(value) && !value.isEmpty()) {
            Object responseDataType = Utility.getDataType(dataType, value);
            OpenSearchVOConeSearch.ReservedWords ucdWord = OpenSearchVOConeSearch.ReservedWords.find(ucd);
            switch (ucdWord) {
              case POS_EQ_RA_MAIN:
                raValue = Utility.parseRa(iterDoc, field);
                break;
              case POS_EQ_DEC_MAIN:
                decValue = Utility.parseDec(iterDoc, field);
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
        geometry.put("coordinates", String.format("[%s,%s]", raValue, decValue));
        geometry.put("type", "Point");
        geometry.put("crs", CoordinateSystem.EQUATORIAL.name().concat(".ICRS"));
        feature.put("geometry", geometry);
        feature.put("properties", properties);
        features.add(feature);
      }
      dataModel.put("features", features);
      return new GeoJsonRepresentation(dataModel, "GeoJson.ftl");
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
    }
  }

  /**
   * User parameters.
   */
  public class UserParameters {

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
    private double ra;
    /**
     * declination.
     */
    private double dec;
    /**
     * radius.
     */
    private double sr;

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
      String raInput = form.getFirstValue(ConeSearchProtocolLibrary.RA);
      String decInput = form.getFirstValue(ConeSearchProtocolLibrary.DEC);
      String srInput = form.getFirstValue(ConeSearchProtocolLibrary.SR);
      String healpixInput = form.getFirstValue("healpix");
      String orderInput = form.getFirstValue("order");
      if (raInput != null && decInput != null && srInput != null) {
        this.ra = Double.valueOf(raInput);
        this.dec = Double.valueOf(decInput);
        this.sr = Double.valueOf(srInput);
      } else if (healpixInput != null && orderInput != null) {
        long healpix = Long.valueOf(healpixInput);
        int order = Integer.valueOf(orderInput);
        int nside = (int) Math.pow(2, order);
        double pixRes = HealpixIndex.getPixRes(nside) * ARCSEC2DEG;
        HealpixIndex healpixIndex = new HealpixIndex(nside, Scheme.NESTED);
        Pointing pointing = healpixIndex.pix2ang(healpix);
        this.ra = Math.toDegrees(pointing.phi);
        this.dec = MAX_DEC - Math.toDegrees(pointing.theta);
        this.sr = pixRes;
      } else {
        throw new Exception("wrong input parameters");
      }
    }

    /**
     * Returns the right ascension in degree.
     *
     * @return the right ascension
     */
    public final double getRa() {
      return this.ra;
    }

    /**
     * Returns the declination in degree.
     *
     * @return the declination
     */
    public final double getDec() {
      return this.dec;
    }

    /**
     * Return the radius in degree.
     *
     * @return the radius
     */
    public final double getSr() {
      return this.sr;
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

    List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo("healpix", true, "long", ParameterStyle.QUERY,
            "Healpix number"));
    parametersInfo.add(new ParameterInfo("order", true, "integer", ParameterStyle.QUERY,
            "Healpix order"));
    ParameterInfo json = new ParameterInfo("format", true, "string", ParameterStyle.QUERY, "JSON format");
    json.setFixed("json");
    parametersInfo.add(json);

    info.getRequest().setParameters(parametersInfo);
    
    // represensation when the response is fine
    ResponseInfo responseOK = new ResponseInfo();    

    DocumentationInfo documentation = new DocumentationInfo();
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

    List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    RepresentationInfo representationInfo = new RepresentationInfo(MediaType.APPLICATION_JSON);
    representationInfo.setDocumentation(documentation);    
    representationsInfo.add(representationInfo);
    responseOK.setRepresentations(representationsInfo);
    responseOK.getStatuses().add(Status.SUCCESS_OK);

    // represensation when the response is not fine
    ResponseInfo responseNOK = new ResponseInfo();
    RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
    representationInfoError.setReference("error");

    responseNOK.getRepresentations().add(representationInfoError);
    responseNOK.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
    responseNOK.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);

    info.setResponses(Arrays.asList(responseOK, responseNOK));
  }
}
