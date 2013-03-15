/*
 * Copyright 2013 - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.sitools.astro.resolver.AstroCoordinate;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
   * Conversion arcsec to degree.
   */
  private static final double ARCSEC_2_DEG = 1 / 3600.;

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
    HealpixIndex healpixIndex = new HealpixIndex(nside, Scheme.NESTED);
    Pointing pointing = healpixIndex.pix2ang(healpix);
    this.ra = Math.toDegrees(pointing.phi);
    this.dec = 90.0 - Math.toDegrees(pointing.theta);
    this.radius = pixRes * ARCSEC_2_DEG;
    this.query = new ConeSearchQuery(URL.replace("<EPOCH>", time));
  }

  @Override
  public final GeoJsonRepresentation getResponse() {
    Map dataModel = new HashMap();
    int totalResults = 0;

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
      totalResults++;
    }
    dataModel.put("features", features);
    dataModel.put("totalResults", totalResults);
    return new GeoJsonRepresentation(dataModel, "GeoJson.ftl");

  }

  /**
   * Returns the value in the right datatype.
   *
   * @param dataType VOTable datatype
   * @param value value to cast
   * @return the value in the right datatype
   */
  private Object getDataType(final net.ivoa.xml.votable.v1.DataType dataType, final String value) {
    Object response;
    switch (dataType) {
      case DOUBLE:
        response = Double.valueOf(value);
        break;
      case DOUBLE_COMPLEX:
        response = Double.valueOf(value);
        break;
      case FLOAT:
        response = Float.valueOf(value);
        break;
      case FLOAT_COMPLEX:
        response = Float.valueOf(value);
        break;
      case INT:
        response = Integer.valueOf(value);
        break;
      case LONG:
        response = Long.valueOf(value);
        break;
      default:
        response = value;
        break;
    }
    return response;
  }

  /**
   * Parses Record and writes the response in features.
   * @param features the response
   * @param record the records to set
   */
  protected final void parseRecord(List features, final Map<Field, String> record) {
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
      if (ucd != null && value != null && !value.isEmpty()) {
        Object response = getDataType(dataType, value);
        ReservedWords ucdWord = ReservedWords.find(ucd);
        switch (ucdWord) {
          case POS_EQ_RA_MAIN:
            coordinatesRa = record.get(field).replace(" ", ":");
            break;
          case POS_EQ_DEC_MAIN:
            coordinatesDec = record.get(field).replace(" ", ":");
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
        Object response = getDataType(dataType, value);
        properties.put(field.getName(), response);
      }
    }
    AstroCoordinate astroCoordinate = new AstroCoordinate(coordinatesRa, coordinatesDec);
    geometry.put("coordinates", String.format("[%s,%s]", astroCoordinate.getRaAsDecimal(), astroCoordinate.getDecAsDecimal()));
    geometry.put("type", "Point");
    geometry.put("crs", "equatorial.ICRS");
    feature.put("geometry", geometry);
    feature.put("properties", properties);
    features.add(feature);
  }
}