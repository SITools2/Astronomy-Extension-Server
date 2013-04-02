/*
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
 */
package fr.cnes.sitools.astro.vo.conesearch;

import fr.cnes.sitools.astro.representation.GeoJsonRepresentation;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.Utility;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Field;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * This object contains methods to search solar objects.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConeSearchSolarObjectQuery implements ConeSearchQueryInterface {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ConeSearchSolarObjectQuery.class.getName());
  /**
   * Query.
   */
  private ConeSearchQuery query;
  /**
   * Right ascension.
   */
  private double ra;
  /**
   * Declination.
   */
  private double dec;
  /**
   * Radius in decimal degree.
   */
  private double radius;
  /**
   * URL to query the WS.
   */
  private static final String URL = "http://vo.imcce.fr/webservices/skybot/skybotconesearch_query.php?from=SITools2&EPOCH=<EPOCH>&";
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
   * Conversion arcsec to degree.
   */
  private static final double ARCSEC_2_DEG = ONE_DEG / ONE_DEG_IN_ARSEC;
  /**
   * Multiplation factor to embed the entire Healpix pixel in the ROI.
   */
  private static final double MULT_FACT = 1.5;
  /**
   * True when Healpix mode is used.
   */
  private boolean isHealpixMode;
  /**
   * Healpix is not defined.
   */
  private static final int NOT_DEFINED = -1;
  /**
   * Pixel to check.
   */
  private long pixelToCheck;
  /**
   * Healpix index.
   */
  private HealpixIndex healpixIndex;

  /**
   * keywords.
   */
  public enum ReservedWords {

    /**
     * RA.
     */
    POS_EQ_RA_MAIN("pos.eq.ra;meta.main"),
    /**
     * DEC.
     */
    POS_EQ_DEC_MAIN("pos.eq.dec;meta.main"),
    /**
     * ID.
     */
    IMAGE_TITLE("meta.id;meta.main"),
    /**
     * Type.
     */
    CLASS("meta.code.class;src.class"),
    /**
     * None.
     */
    NONE(null);
    /**
     * ucd name.
     */
    private final String name;

    /**
     * Constructor.
     *
     * @param nameVal ucd name
     */
    ReservedWords(final String nameVal) {
      this.name = nameVal;
    }

    /**
     * Returns the ucd name.
     *
     * @return the ucd name
     */
    public final String getName() {
      return this.name;
    }

    /**
     * Returns the enumeration from the ucd name.
     *
     * @param keyword ucd name.
     * @return the enumeration
     */
    public static ReservedWords find(final String keyword) {
      ReservedWords response = ReservedWords.NONE;
      ReservedWords[] words = ReservedWords.values();
      for (int i = 0; i < words.length; i++) {
        ReservedWords word = words[i];
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
   * Constructs a query.
   *
   * @param raVal right ascension
   * @param decVal declination
   * @param radiusVal radius in degree
   * @param timeVal time
   */
  public ConeSearchSolarObjectQuery(final double raVal, final double decVal, final double radiusVal, final String timeVal) {
    this.query = new ConeSearchQuery(URL.replace("<EPOCH>", timeVal));
    this.ra = raVal;
    this.dec = decVal;
    this.radius = radiusVal;
    this.isHealpixMode = false;
    this.pixelToCheck = NOT_DEFINED;
    this.healpixIndex = null;
  }

  /**
   * Constructs a query.
   *
   * @param healpix healpix
   * @param order helapix order
   * @param time time
   * @throws Exception Exception
   */
  public ConeSearchSolarObjectQuery(final long healpix, final int order, final String time) throws Exception {
    int nside = (int) Math.pow(2, order);
    double pixRes = HealpixIndex.getPixRes(nside);
    this.healpixIndex = new HealpixIndex(nside, Scheme.NESTED);
    this.pixelToCheck = healpix;
    Pointing pointing = healpixIndex.pix2ang(healpix);
    this.ra = Math.toDegrees(pointing.phi);
    this.dec = MAX_DEC - Math.toDegrees(pointing.theta);
    this.radius = pixRes * ARCSEC_2_DEG * MULT_FACT;
    this.query = new ConeSearchQuery(URL.replace("<EPOCH>", time));
    this.isHealpixMode = true;
  }

  @Override
  public final GeoJsonRepresentation getResponse() {
    Map dataModel = new HashMap();

    List features = new ArrayList();
    List<Map<Field, String>> response;
    try {
      response = query.getResponseAt(ra, dec, radius);
    } catch (Exception ex) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No data found");
    }
    if (response == null || response.isEmpty()) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "No data found");
    }

    for (Map<Field, String> record : response) {
      parseRecord(features, record);
    }
    dataModel.put("features", features);
    dataModel.put("totalResults", features.size());
    return new GeoJsonRepresentation(dataModel);

  }

  /**
   * Parses Record and writes the response in features.
   *
   * @param features the response
   * @param record the records to set
   */
  protected final void parseRecord(final List features, final Map<Field, String> record) {
    Map feature = new HashMap();
    Map geometry = new HashMap();
    Map properties = new HashMap();
    String coordinatesRa = null;
    String coordinatesDec = null;

    Set<Field> fields = record.keySet();
    Iterator<Field> iter = fields.iterator();
    while (iter.hasNext()) {
      Field field = iter.next();
      String ucd = field.getUcd();
      net.ivoa.xml.votable.v1.DataType dataType = field.getDatatype();
      String value = record.get(field);
      if (Utility.isSet(ucd) && Utility.isSet(value) && !value.isEmpty()) {
        Object response = Utility.getDataType(dataType, value);
        ReservedWords ucdWord = ReservedWords.find(ucd);
        switch (ucdWord) {
          case POS_EQ_RA_MAIN:
            coordinatesRa = record.get(field);
            break;
          case POS_EQ_DEC_MAIN:
            coordinatesDec = record.get(field);
            break;
          case IMAGE_TITLE:
            String identifier = record.get(field);
            properties.put("identifier", identifier);
            properties.put("seeAlso", String.format("http://vizier.u-strasbg.fr/cgi-bin/VizieR-5?-source=B/astorb/astorb&Name===%s", identifier));
            properties.put("credits", "IMCCE");
            break;
          case CLASS:
            properties.put("type", response);
            break;
          default:
            properties.put(field.getName(), response);
            break;
        }
      } else {
        Object response = Utility.getDataType(dataType, value);
        properties.put(field.getName(), response);
      }
    }
    AstroCoordinate astroCoordinate = new AstroCoordinate(coordinatesRa, coordinatesDec);
    geometry.put("coordinates", String.format("[%s,%s]", astroCoordinate.getRaAsDecimal(), astroCoordinate.getDecAsDecimal()));
    geometry.put("type", "Point");
    geometry.put("crs", "equatorial.ICRS");
    feature.put("geometry", geometry);
    feature.put("properties", properties);
    if (isValid(feature)) {
      if (!isHealpixMode) {
        features.add(feature);
      } else if (isPointIsInsidePixel(astroCoordinate.getRaAsDecimal(), astroCoordinate.getDecAsDecimal())) {
        features.add(feature);
      } else {
        feature.clear();
      }
    } else {
      LOG.log(Level.WARNING, "{0} does not have an identifier. Also, this record is ignored.", feature.toString());
      feature.clear();
    }
  }

  /**
   * Returns true when the point (ra,dec) is inside the pixel otherwise false.
   *
   * @param raSolarObj right ascension in degree
   * @param decSolarObj declination in degree
   * @return true when the point (ra,dec) is inside the pixel otherwise false
   */
  private boolean isPointIsInsidePixel(final double raSolarObj, final double decSolarObj) {
    boolean result;
    try {
      long healpixFromService = this.healpixIndex.ang2pix_nest(Math.PI / 2 - Math.toRadians(decSolarObj), Math.toRadians(raSolarObj));
      result = (healpixFromService == this.pixelToCheck) ? true : false;
    } catch (Exception ex) {
      result = false;
      LOG.log(Level.WARNING, null, ex);
    }
    return result;
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
}