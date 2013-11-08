 /*******************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.solr.transformer;

import fr.cnes.sitools.SearchGeometryEngine.CoordSystem;
import fr.cnes.sitools.SearchGeometryEngine.GeometryIndex;
import fr.cnes.sitools.SearchGeometryEngine.Index;
import fr.cnes.sitools.SearchGeometryEngine.Point;
import fr.cnes.sitools.SearchGeometryEngine.Polygon;
import fr.cnes.sitools.SearchGeometryEngine.RingIndex;
import fr.cnes.sitools.SearchGeometryEngine.Shape;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import healpix.core.HealpixIndex;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jsky.coords.WCSKeywordProvider;
import jsky.coords.WCSTransform;
import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImportHandlerException;
import org.apache.solr.handler.dataimport.DataImporter;
import org.apache.solr.handler.dataimport.Transformer;

/**
 * Computes some geographical parameters from the Word Coordinates System (WCS) keywords.
 *
 * <p> When WCS keywords exist, the DIH computes: 
 * <ul> 
 * <li>the WCS center of the frame</li> 
 * <li>the Ra/Dec of each corner of the FOV</li>
 * <li>the Healpix index of the FOV</li> 
 * </ul> 
 * </p>
 *
 * <p> To set up the Healpix configuration, some attributes must be set to entity tag:
 * <ul> 
 * <li>transformer, transformer name</li>
 * <li>minOrder (optional, default: 0), minimum order for which Healpix is computed</li>
 * <li>maxOrder (optional, default: 13), maximum order
 * for which Healpix is computed</li>
 * <li>scheme, Healpix Scheme</li>
 * </ul> Here is an example of a Healpix configuration:
 * <pre>
 * <code>
 * entity name="headers" query="select * from fuse.headers" transformer="fr.cnes.sitools.solr.transformer.WcsTransformer" minOrder="3" maxOrder="13" scheme="NESTED">
 * </code>
 * </pre>
 * </p>
 *
 * <p> To map, a solrColumn with a wcs element, configure the solr fields as follow <field column="<name>" ... wcs="..."/> where wcs keyword
 * can take the following values : NAXIS1,NAXIS2,EQUINOX,CTYPE1,CTYPE2,CRPIX1,CRPIX2,CRVAL1,CRVAL2,CDELT1,CDELT2
 * CROTA1,CROTA2,CD1_1,CD1_2,CD2_1,CD2_2,DATE-OBS,EPOCH,RADECSYS Moreover, three new columns are added (ra, dec, coordinates) and can be
 * used in SOLR schema
 *
 * About Healpix index, new columns are computed: <field column="<name>" .../> where <name> is from order0 to order13 </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class WcsTransformer extends Transformer implements WCSKeywordProvider {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(WcsTransformer.class.getName());
  /**
   * Origin of FITS along X.
   */
  private static final double ORGIN_X = 0.5;
  /**
   * Origin of FITS along Y.
   */
  private static final double ORGIN_Y = 0.5;
  /**
   * keyword for geometrical coordinates in the response.
   */
  private static final String COORDINATES = "geometry.coordinates";
  /**
   * keyword for geometrical coordinates type in the response.
   */
  private static final String COORDINATES_TYPE = "geometry.coordinates.type";
  /**
   * right ascension keyword in the response.
   */
  private static final String RA = "properties.ra";
  /**
   * declination keyword in the response.
   */
  private static final String DEC = "properties.dec";
  /**
   * right ascension keyword.
   */
  private static final String RA_WCS = "RA";
  /**
   * declination keyword.
   */
  private static final String DEC_WCS = "DEC";
  /**
   * Initialize a default value the WCS interface.
   */
  private static final double DEFAULT_VALUE = 500;
  /**
   * Min Healpix order (0).
   */
  private static final int DEFAULT_MIN_ORDER = 0;
  /**
   * Max Healpix order (13).
   */
  private static final int DEFAULT_MAX_ORDER = 13;
  /**
   * Healpix Scheme.
   */
  private Scheme scheme;
  /**
   * data.
   */
  private List<Map<String, String>> fields;
  /**
   * data in row.
   */
  private Map<String, Object> row;
  /**
   * min Healpix order.
   */
  private int minOrder;
  /**
   * max Healpix order.
   */
  private int maxOrder;

  /**
   * Returns the transformation that is applied in this method.
   * 
   * <p>This transformer computes the footprint and its Healpix index.</p>
   * 
   *
   * @param rowVal row to convert
   * @param context Data Handler context
   * @return new converted row
   */
  @Override
  public final Object transformRow(final Map<String, Object> rowVal, final Context context) {
    List<Point2D.Double> points;
    WCSTransform wcs;
    setupConfiguration(context);
    this.row = rowVal;
    try {
      wcs = new WCSTransform(this);
      computeWcsCenter(wcs);
      points = computeFootprint(wcs);
    } catch (IllegalArgumentException ex) {
      LOG.log(Level.WARNING, null, ex);
      points = computePointSource();
    } catch (RuntimeException ex) {
      LOG.log(Level.WARNING, null, ex);
      points = computePointSource();
    }
    computeHealpix(points);
    return this.row;
  }

  /**
   * Sets up the Solr transformation's configuration.
   *
   * @param context context
   */
  private void setupConfiguration(final Context context) {
    minOrder = (context.getEntityAttribute("minOrder") == null) ? DEFAULT_MIN_ORDER : Integer.valueOf(context.getEntityAttribute("minOrder"));
    maxOrder = (context.getEntityAttribute("maxOrder") == null) ? DEFAULT_MAX_ORDER : Integer.valueOf(context.getEntityAttribute("maxOrder"));
    String healpixScheme = context.getEntityAttribute("scheme");
    try {
      scheme = Scheme.valueOf(healpixScheme);
    } catch (IllegalArgumentException ex) {
      throw new RuntimeException("Healpix scheme must be set by defining scheme (=RING or NESTED) variable");
    }
    fields = context.getAllEntityFields();
  }

  /**
   * Returns the point source based on RA and DEC keywords.
   * 
   * <p><code>null</code> is returned when <code>RA_WCS</code> or <code>DEC_WCS</code> cannot be retrieved.</p>
   *
   * @return one point in the list or null when RA or DEC is not set
   */
  private List<Point2D.Double> computePointSource() {
    double ra = getDoubleValue(RA_WCS, DEFAULT_VALUE);
    double dec = getDoubleValue(DEC_WCS, DEFAULT_VALUE);
    if (ra != DEFAULT_VALUE && dec != DEFAULT_VALUE) {
      Point2D.Double p = new Point2D.Double(ra, dec);
      row.put(COORDINATES, "[" + ra + "," + dec + "]");
      row.put(COORDINATES_TYPE, "point");
      row.put(RA, ra);
      row.put(DEC, dec);
      return Arrays.asList(p);
    } else {
      return null;
    }
  }

  /**
   * Returns <code>true</code> if the xml-data-config file contains attributes with a specific wcs key.
   *
   * @param field field
   * @param key WCS keyword
   * @return true when the keyword is detected otherwise false
   */
  private boolean containWcsKey(final Map<String, String> field, final String key) {
    return (field.containsKey("wcs") && field.get("wcs").equals(key)) ? true : false;
  }

  /**
   * Computes and stores the center of the image in <code>row</code>.
   *
   * @param wcs wcs
   */
  private void computeWcsCenter(final WCSTransform wcs) {
    if (wcs.isWCS() && wcs.isValid()) {
      try {
        Point2D.Double wcsCenter = wcs.getWCSCenter();
        row.put(RA, wcsCenter.x);
        row.put(DEC, wcsCenter.y);
      } catch (IllegalArgumentException ex) {
        LOG.log(Level.WARNING, null, ex);
      } catch (RuntimeException ex) {
        LOG.log(Level.WARNING, null, ex);
      }
    }
  }

  /**
   * Computes, stores and returns the footprint of the image.
   *
   * @param wcs wcs
   * @return the center of the image
   */
  private List<Point2D.Double> computeFootprint(final WCSTransform wcs) {
    List<Point2D.Double> points = null;
    if (wcs != null && wcs.isWCS() && wcs.isValid()) {
      try {
        int naxis1 = this.getIntValue("NAXIS1");
        int naxis2 = this.getIntValue("NAXIS2");
        if (naxis1 != 0 && naxis2 != 0) {
          double heightPix = wcs.getHeight();
          double widthPix = wcs.getWidth();
          Point2D.Double p1 = wcs.pix2wcs(ORGIN_X, ORGIN_Y);
          Point2D.Double p2 = wcs.pix2wcs(ORGIN_X, heightPix + ORGIN_Y);
          Point2D.Double p3 = wcs.pix2wcs(widthPix + ORGIN_X, heightPix + ORGIN_Y);
          Point2D.Double p4 = wcs.pix2wcs(widthPix + ORGIN_X, ORGIN_Y);
          String footprint = String.format("[%s,%s],[%s,%s],[%s,%s],[%s,%s],[%s,%s]", p2.x, p2.y, p3.x, p3.y, p4.x, p4.y, p1.x, p1.y, p2.x, p2.y);
          row.put(COORDINATES, footprint);
          row.put(COORDINATES_TYPE, "polygon");
          points = Arrays.asList(p2, p3, p4, p1);
        }
      } catch (IllegalArgumentException ex) {
        LOG.log(Level.SEVERE, null, ex);
      } catch (RuntimeException ex) {
        LOG.log(Level.SEVERE, null, ex);
      }
    }
    return points;
  }

  /**
   * Computes and stores Healpix.
   *
   * @param points points describing the polygon   
   */
  private void computeHealpix(final List<Point2D.Double> points) {
      computeHealpixEquatorial(points);
      //computeHealpixGalactic(points);
  }
  
  private void computeHealpixEquatorial(final List<Point2D.Double> points) {
    try {
      List<Point> skyPoints = new ArrayList<Point>(points.size());
      for (Point2D.Double point : points) {
        skyPoints.add(new Point(point.x, point.y, CoordSystem.EQUATORIAL));
      }
      if (skyPoints.size() == 1) {
        Index index = GeometryIndex.createIndex(skyPoints.get(0), fr.cnes.sitools.SearchGeometryEngine.Scheme.valueOf(Scheme.RING.name()));
        for (int order = minOrder; order <= maxOrder; order++) {
          ((RingIndex) index).setOrder(order);
          String pixelNumber = String.valueOf(index.getIndex());
          switch (scheme) {
            case RING:
              break;
            case NESTED:
              //((NestedIndex) index).setOrderMax(order);
              int nside = (int) Math.pow(2.0, order);
              HealpixIndex healpixIndex = new HealpixIndex(nside, Scheme.RING);
              pixelNumber = String.valueOf(healpixIndex.ring2nest(Long.valueOf(pixelNumber)));
              break;
            default:
              throw new RuntimeException("Unknown Scheme");
          }
          row.put("order" + order, pixelNumber);
        }
      } else {
        Shape polygon = new Polygon(skyPoints);
        Index index = GeometryIndex.createIndex(polygon, fr.cnes.sitools.SearchGeometryEngine.Scheme.valueOf(Scheme.RING.name()));
        for (int order = minOrder; order <= maxOrder; order++) {
          row.put("order" + order, computeAtOrder(order, index));
        }
      }
    } catch (Exception ex) {
      throw new DataImportHandlerException(DataImportHandlerException.SKIP_ROW, row.toString(), ex);
    }
      
  }
  
  private void computeHealpixGalactic(final List<Point2D.Double> points) {
    try {
      List<Point> skyPoints = new ArrayList<Point>(points.size());
      AstroCoordinate astro = new AstroCoordinate();
      for (Point2D.Double point : points) {
        astro.setRaAsDecimal(point.x);
        astro.setDecAsDecimal(point.y);
        astro.setCoordinateSystem(AstroCoordinate.CoordinateSystem.EQUATORIAL);
        astro.processTo(AstroCoordinate.CoordinateSystem.GALACTIC);
        skyPoints.add(new Point(astro.getRaAsDecimal(),astro.getDecAsDecimal(), CoordSystem.GALACTIC));
      }
      if (skyPoints.size() == 1) {
        Index index = GeometryIndex.createIndex(skyPoints.get(0), fr.cnes.sitools.SearchGeometryEngine.Scheme.valueOf(Scheme.RING.name()));
        for (int order = minOrder; order <= maxOrder; order++) {
          ((RingIndex) index).setOrder(order);
          String pixelNumber = String.valueOf(index.getIndex());
          switch (scheme) {
            case RING:
              break;
            case NESTED:
              //((NestedIndex) index).setOrderMax(order);
              int nside = (int) Math.pow(2.0, order);
              HealpixIndex healpixIndex = new HealpixIndex(nside, Scheme.RING);
              pixelNumber = String.valueOf(healpixIndex.ring2nest(Long.valueOf(pixelNumber)));
              break;
            default:
              throw new RuntimeException("Unknown Scheme");
          }
          row.put("order" + order, pixelNumber);
        }
      } else {
        Shape polygon = new Polygon(skyPoints);
        Index index = GeometryIndex.createIndex(polygon, fr.cnes.sitools.SearchGeometryEngine.Scheme.valueOf(Scheme.RING.name()));
        for (int order = minOrder; order <= maxOrder; order++) {
          row.put("order" + order, computeAtOrder(order, index));
        }
      }
    } catch (Exception ex) {
      throw new DataImportHandlerException(DataImportHandlerException.SKIP_ROW, row.toString(), ex);
    }
      
  }

  /**
   * Returns the conversion from an array of long into an array of Long.
   *
   * @param array long array
   * @return the converted array as Long datatype
   */
  private List<Long> convertArray(final long[] array) {
    List<Long> result = new ArrayList<Long>(array.length);
    for (long item : array) {
      result.add(item);
    }
    return result;
  }

  /**
   * Returns the Healpix index in RING scheme at a specific order.
   *
   * @param order Healpix resolution
   * @param index index
   * @return the Healpix index at a given order
   * @throws Exception scheme is unknown
   */
  private List<Long> computeAtOrder(final int order, final Index index) throws Exception {
    ((RingIndex) index).setOrder(order);
    Object obj = index.getIndex();
    long[] result = GeometryIndex.decodeRangeSet((RangeSet) obj);
    switch (scheme) {
      case RING:
        break;
      case NESTED:
        int nside = (int) Math.pow(2.0, order);
        HealpixIndex healpixIndex = new HealpixIndex(nside, Scheme.RING);
        for (int i = 0; i < result.length; i++) {
          long pixelNumber = result[i];
          result[i] = healpixIndex.ring2nest(pixelNumber);
        }
        break;
      default:
        throw new RuntimeException("Unknown Scheme");
    }

    return convertArray(result);
  }

  @Override
  public final boolean findKey(final String key) {
    boolean isFound = false;
    for (Map<String, String> field : fields) {
      if (containWcsKey(field, key)) {
        isFound = true;
        break;
      }
    }
    return isFound;
  }

  @Override
  public final String getStringValue(final String key) {
    return getStringValue(key, null);
  }

  @Override
  public final String getStringValue(final String key, final String defaultValue) {
    String value = defaultValue;
    for (Map<String, String> field : fields) {
      if (containWcsKey(field, key)) {
        String fieldName = field.get(DataImporter.COLUMN);
        value = String.valueOf(this.row.get(fieldName));
        break;
      }
    }
    return value;
  }

  @Override
  public final double getDoubleValue(final String key) {
    return getDoubleValue(key, 0);
  }

  @Override
  public final double getDoubleValue(final String key, final double defaultValue) {
    double value = defaultValue;
    for (Map<String, String> field : fields) {
      if (containWcsKey(field, key)) {
        String fieldName = field.get(DataImporter.COLUMN);
        value = Double.valueOf(String.valueOf(this.row.get(fieldName)));
        break;
      }
    }
    return value;
  }

  @Override
  public final float getFloatValue(final String key) {
    return getFloatValue(key, 0);
  }

  @Override
  public final float getFloatValue(final String key, final float defaultValue) {
    float value = defaultValue;
    for (Map<String, String> field : fields) {
      if (containWcsKey(field, key)) {
        String fieldName = field.get(DataImporter.COLUMN);
        value = Float.valueOf(String.valueOf(this.row.get(fieldName)));
        break;
      }
    }
    return value;
  }

  @Override
  public final int getIntValue(final String key) {
    return getIntValue(key, 0);
  }

  @Override
  public final int getIntValue(final String key, final int defaultValue) {
    int value = defaultValue;
    for (Map<String, String> field : fields) {
      if (containWcsKey(field, key)) {
        String fieldName = field.get(DataImporter.COLUMN);
        value = Integer.valueOf(String.valueOf(this.row.get(fieldName)));
        break;
      }
    }
    return value;
  }
}