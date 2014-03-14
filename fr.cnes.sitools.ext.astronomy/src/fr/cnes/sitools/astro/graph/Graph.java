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
package fr.cnes.sitools.astro.graph;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JApplet;

import com.jhlabs.map.proj.Projection;

/**
 * Graph component of a decorator pattern.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public abstract class Graph extends JApplet {

  /**
   * Latitude (max) = 90°.
   */
  public static final int LAT_MAX = 90;
  /**
   * Latitude (min) = -90°.
   */
  public static final int LAT_MIN = -90;
  /**
   * Longitude (min) = -180°.
   */
  public static final int LONG_MIN = -180;
  /**
   * Longitude (max) = 180°.
   */
  public static final int LONG_MAX = 180;
  /**
   * Default graph width.
   */
  public static final int DEFAULT_GRAPH_WIDTH = 800;
  /**
   * Default graph height.
   */
  public static final int DEFAULT_GRAPH_HEIGHT = 400;
  /**
   * Angular distance on phi.
   */
  public static final double ANGULAR_DIST_PHI = 360.0;
  /**
   * Angular distance on phi*1.5.
   */
  public static final double ANGULAR_DIST_PHI_1_5 = 540.0;
  /**
   * Angular distance on phi/2.
   */
  public static final double ANGULAR_DIST_PHI_DIVIDE_2 = 180.0;
  /**
   * Angular distance on theta.
   */
  public static final double ANGULAR_DIST_THETA = 180.0;
  /**
   * Initialize longitude range.
   */
  private double widthLongitude = ANGULAR_DIST_PHI;
  /**
   * Initialize latitude range.
   */
  private double widthLatitude = ANGULAR_DIST_THETA;
  /**
   * Initialize pixel width.
   */
  private int pixelWidth = DEFAULT_GRAPH_WIDTH;
  /**
   * Initialize pixel height.
   */
  private int pixelHeight = DEFAULT_GRAPH_HEIGHT;
  /**
   * Range.
   */
  private double[] range;
  /**
   * Projection.
   */
  private Projection projection;
  /**
   * Number of values in RANGE: [xmin, xmax, ymin, ymax].
   */
  public static final int NUMBER_VALUES_RANGE = 4;
  /**
   * Mininum value of X coordinate in RANGE.
   */
  public static final int X_MIN = 0;
  /**
   * Maxinum value of X coordinate in RANGE.
   */
  public static final int X_MAX = 1;
  /**
   * Mininum value of Y coordinate in RANGE.
   */
  public static final int Y_MIN = 2;
  /**
   * Maxinum value of Y coordinate in RANGE.
   */
  public static final int Y_MAX = 3;
  /**
   * Maximum value of the right ascension.
   */
  public static final double RA_MAX = 360.;
  /**
   * Minimum value of the right ascension.
   */
  public static final double RA_MIN = 0.;
  /**
   * Maximum value of the declination.
   */
  public static final double DEC_MAX = 90.;
  /**
   * Minimum value of the declination.
   */
  public static final double DEC_MIN = -90.;  
  /**
   * Maximum value of the right ascension / 2.
   */
  public static final double RA_PI = 180.; 

  /**
   * Supported projections.
   */
  public enum ProjectionType {

    //ALBERS("aea", "Albers"), //Not perfect for Healpix projection
    //AEQD("aeqd", "Equidistant Azimuthal"), //Not perfect for Healpix projection
    //AIRY("airy", "Airy"),//Not perfect for Healpix projection
    /**
     * Aitoff projection.
     */
    AITOFF("aitoff", "Aitoff"),
    /**
     * Apian Globular I projection.
     */
    APIAN1("apian1", "Apian Globular I"),
    /**
     * Apian Globular II projection.
     */
    APIAN2("apian2", "Apian Globular II"),
    /**
     * August Epicycloidal projection.
     */
    AUGUST("august", "August Epicycloidal"),
    /**
     * Bacon Globular projection.
     */
    BACON("bacon", "Bacon Globular"),
    //BIPC("bipc", "Bipolar"), 
    /**
     * Boggs Eumorphic projection.
     */
    BOGGS("boggs", "Boggs Eumorphic"),
    /**
     * Bonne (Werner lat_1=90) projection.
     */
    BONNE("bonne", "Bonne (Werner lat_1=90)"),
    //CASS("cass", "Cassini"), // Big pb in Healpix projection
    //CC("cc", "Central Cylindrical"),       
    /**
     * Equal Area Cylindrical projection.
     */
    CEA("cea", "Equal Area Cylindrical"),
    /**
     * Collignon projection.
     */
    COLLG("collg", "Collignon"),
    /**
     * Craster Parabolic (Putnins P4) projection.
     */
    CRAST("crast", "Craster Parabolic (Putnins P4)"),
    /**
     * Denoyer Semi-Elliptical projection.
     */
    DENOY("denoy", "Denoyer Semi-Elliptical"),
    /**
     * Eckert I projection.
     */
    ECK1("eck1", "Eckert I"),
    /**
     * Eckert II projection.
     */
    ECK2("eck2", "Eckert II"),
    /**
     * Eckert III projection.
     */
    ECK3("eck3", "Eckert III"),
    /**
     * Eckert IV projection.
     */
    ECK4("eck4", "Eckert IV"),
    /**
     * Eckert V projection.
     */
    ECK5("eck5", "Eckert V"),
    /**
     * Eckert VI projection.
     */
    ECK6("eck6", "Eckert VI"),
    /**
     * Equidistant Cylindrical (Plate Carree) projection.
     */
    ECQ("eqc", "Equidistant Cylindrical (Plate Carree)"),
    //EQDC("eqdc", "Equidistant Conic"), // Big pb in Healpix projection
    //EULER("euler", "Euler"),// Big pb in Healpix projection
    /**
     * Fahey projection.
     */
    FAHEY("fahey", "Fahey"),
    /**
     * Foucaut projection.
     */
    FOUC("fouc", "Foucaut"),
    /**
     * Foucaut Sinusoidal projection.
     */
    FOUC_S("fouc_s", "Foucaut Sinusoidal"),
    /**
     * Fournier2 projection.
     */
    FOUR2("four2", "Fournier2"),
    /**
     * Gall (Gall Stereographic) projection.
     */
    GALL("gall", "Gall (Gall Stereographic)"),
    /**
     * Ginzburg8Projection projection.
     */
    GINS8("gins8", "Ginzburg8Projection"),
    //GNOM("gnom", "Gnomonic Azimuthal"),
    /**
     * Goode Homolosine projection.
     */
    GOODE("goode", "Goode Homolosine"),
    /**
     * Hammer & Eckert-Greifendorff projection.
     */
    HAMMER("hammer", "Hammer & Eckert-Greifendorff"),
    /**
     * Hatano Asymmetrical Equal Area projection.
     */
    HATANO("hatano", "Hatano Asymmetrical Equal Area"),
    /**
     * Holzel projection.
     */
    HOLZEL("holzel", "Holzel"),
    /**
     * Kavraisky V projection.
     */
    KAV5("kav5", "Kavraisky V"),
    /**
     * Kavraisky VII projection.
     */
    KAV7("kav7", "Kavraisky VII"),
    //LAGRNG("lagrng", "Lagrange"), //Strange
    /**
     * Larrivee projection.
     */
    LARR("larr", "Larrivee"),
    /**
     * Laskowski projection.
     */
    LASK("lask", "Laskowski"),
    //LCC("lcc", "Lambert Conformal Conic"),//Strange
    //LEAC("leac", "Lambert Equal Area Conic"), // Big pb in Healpix projection
    /**
     * Loximuthal projection.
     */
    LOXIM("loxim", "Loximuthal"),
    //LSAT("lsat", "Landsat"), //infinity problem
    /**
     * McBrydeThomas Sine1 projection.
     */
    MBT_S("mbt_s", "McBrydeThomas Sine1"),
    /**
     * McBryde-Thomas Flat-Pole Sine (No. 2) projection.
     */
    MBT_FBS("mbt_fps", "McBryde-Thomas Flat-Pole Sine (No. 2)"),
    /**
     * McBride-Thomas Flat-Polar Parabolic projection.
     */
    MBTFPP("mbtfpp", "McBride-Thomas Flat-Polar Parabolic"),
    /**
     * McBryde-Thomas Flat-Polar Quartic projection.
     */
    MBTFPQ("mbtfpq", "McBryde-Thomas Flat-Polar Quartic"),
    //MERC("merc", "Mercator"), //Infinity problem
    /**
     * Miller Cylindrical projection.
     */
    MILL("mill", "Miller Cylindrical"),
    /**
     * Mollweide projection.
     */
    MOLL("moll", "Mollweide"),
    //MURD1("murd1", "Murdoch1"), // Big pb in Healpix projection
    //MURD2("murd2", "Murdoch2"), //strange
    /**
     * Murdoch3 projection .
     */
    MURD3("murd3", "Murdoch3"),
    /**
     * Nell projection.
     */
    NELL("nell", "Nell"),
    /**
     * Nell Hammer projection.
     */
    NELLH("nell_h", "Nell Hammer"),
    /**
     * Nicolosi Globular projection.
     */
    NICOL("nicol", "Nicolosi Globular"), //File pb
    //NSPER("nsper", "Perspective"), //File pb
    //OMERC("omerc", "Oblique Mercator"),
    /**
     * Ortelius projection.
     */
    ORTEL("ortel", "Ortelius"),
    //ORTHO("ortho", "Orthographic Azimuthal"), // Big pb in Healpix projection
    //PCONIC("pconic", "Perspective Conic"), //File pb
    //POLY("poly", "Polyconic (American)"), //File pb
    /**
     * Putnins P1 projection.
     */
    PUTP1("putp1", "Putnins P1"),
    /**
     * Putnins P2 projection.
     */
    PUTP2("putp2", "Putnins P2"),
    /**
     * Putnins P4 projection.
     */
    PUTP4("putp4p", "Putnins P4"),
    /**
     * Putnins P5 projection.
     */
    PUTP5("putp5", "Putnins P5"),
    /**
     * Putnins P5 projection.
     */
    PUTP5P("putp5p", "Putnins P5"),
    /**
     * Quartic Authalic projection.
     */
    QUA_AUT("qua_aut", "Quartic Authalic"),
    /**
     * Rectangular Polyconic projection.
     */
    RPOLY("rpoly", "Rectangular Polyconic"),
    /**
     * Sinusoidal (Sanson-Flamsteed) projection.
     */
    SINU("sinu", "Sinusoidal (Sanson-Flamsteed)"),
    //STERE("stere", "Stereographic Azimuthal"), 
    //TCC("tcc", "TCC"),
    //TCEA("tcea", "TCEA"), // Big pb in Healpix projection
    //TISSOT("tissot", "Tissot"),// Big pb in Healpix projection
    //TMERC("tmerc", "Transverse Mercator"),
    /**
     * Urmaev Flat-Polar Sinusoidal projection.
     */
    URMFPS("urmfps", "Urmaev Flat-Polar Sinusoidal"),
    //UTM("utm", "Transverse Mercator"),
    /**
     * van der Grinten (I) projection.
     */
    VANDG("vandg", "van der Grinten (I)"),
    //VITK1("vitk1", "Vitkov sky"),// Big pb in Healpix projection
    /**
     * Wagner I (Kavraisky VI) projection.
     */
    WAG1("wag1", "Wagner I (Kavraisky VI)"),
    /**
     * Wagner II projection.
     */
    WAG2("wag2", "Wagner II"),
    /**
     * Wagner III projection.
     */
    WAG3("wag3", "Wagner III"),
    /**
     * Wagner IV projection.
     */
    WAG4("wag4", "Wagner IV"),
    /**
     * Wagner V projection.
     */
    WAG5("wag5", "Wagner V"),
    /**
     * Wagner VI projection.
     */
    WAG6("wag6", "Wagner VI"),
    /**
     * Wagner VII projection.
     */
    WAG7("wag7", "Wagner VII"),
    /**
     * Werenskiold I projection.
     */
    WEREN("weren", "Werenskiold I"),
    /**
     * Winkel1 projection.
     */
    WINK1("wink1", "Winkel1"),
    /**
     * Winkel2 projection.
     */
    WINK2("wink2", "Winkel2"),
    /**
     * Winkel Tripel projection.
     */
    WINTRI("wintri", "Winkel Tripel");
    /**
     * Projection code.
     */
    private final String projectionCode;
    /**
     * Projection name.
     */
    private final String projectionName;

    /**
     * Constructor.
     *
     * @param projectionCodeVal projection code
     * @param projectionNameVal projeciton name
     */
    ProjectionType(final String projectionCodeVal, final String projectionNameVal) {
      this.projectionCode = projectionCodeVal;
      this.projectionName = projectionNameVal;
    }

    /**
     * Returns the projection code.
     *
     * @return the projection code
     */
    public String getProjectionCode() {
      return this.projectionCode;
    }

    /**
     * Returns the projection name.
     *
     * @return the projection name
     */
    public String getProjectionName() {
      return this.projectionName;
    }
  }

  /**
   * Returns the projection.
   *
   * @return the projection
   */
  public Projection getProjection() {
    return this.projection;
  }

  /**
   * Sets the projection.
   *
   * @param projectionVal Set the projection's code
   */
  protected final void setProjection(final Projection projectionVal) {
    this.projection = projectionVal;
  }

  /**
   * Get the angular difference along longitude axis.
   *
   * @return Returns the angular difference in decimal degree
   */
  protected final double getWidthLongitude() {
    return this.widthLongitude;
  }

  /**
   * Set the angular difference along longitude axis.
   *
   * @param widthLongitudeVal the difference in decimal degree
   */
  protected final void setWidthLongitude(final double widthLongitudeVal) {
    this.widthLongitude = widthLongitudeVal;
  }

  /**
   * Get the angular difference along latitude axis.
   *
   * @return Returns the angular difference in decimal degree
   */
  protected final double getWidthLatitude() {
    return this.widthLatitude;
  }

  /**
   * Set the angular difference along latitude axis.
   *
   * @param widthLatitudeVal latitude width
   */
  protected final void setWidthLatitude(final double widthLatitudeVal) {
    this.widthLatitude = widthLatitudeVal;
  }

  /**
   * get the range of the plot's view.
   *
   * @return [xmin, xmax, ymin, ymax]
   */
  protected double[] getRange() {
    return this.range;
  }

  /**
   * Set the range of the plot's view.
   *
   * @param rangeVal [xmin, xmax, ymin, ymax]
   */
  protected final void setRange(final double[] rangeVal) {
    this.range = rangeVal;
  }
  
  /**
   * Setup the width of the image based on the height and the ratio.
   */
  public final void setupRatioImageSize() {
    final double deltaX = getRange()[X_MAX] - getRange()[X_MIN];
    final double deltaY = getRange()[Y_MAX] - getRange()[Y_MIN];
    final double ratio = Math.abs(deltaX / deltaY);
    final int height = getPixelHeight();
    setPixelWidth((int) Math.ceil(height * ratio));  
  }

  /**
   * Scale projected pixel along X axis.
   *
   * @param xPixel projected pixel
   * @return scaled pixel
   */
  protected final double scaleX(final double xPixel) {
    return (xPixel * getPixelWidth() / (getRange()[X_MAX] - getRange()[X_MIN]) + getPixelWidth() / 2.0D);
  }

  /**
   * Scale projected yPixel along Y axis.
   *
   * @param yPixel projected yPixel
   * @return the scaled yPixel
   */
  protected final double scaleY(final double yPixel) {
    return ((getRange()[Y_MAX] - yPixel) * this.getPixelHeight() / (getRange()[Y_MAX] - getRange()[Y_MIN]));
  }

  /**
   * Make an alpha composite.
   *
   * @param alpha alpha value (from 0.0 to 1.0)
   * @return the alpha composite
   */
  protected final AlphaComposite makeComposite(final float alpha) {
    final int type = AlphaComposite.SRC_OVER;
    return AlphaComposite.getInstance(type, alpha);
  }

  /**
   * Get the number of pixels along X axis.
   *
   * @return Returns the number of pixels along X axis
   */
  public final int getPixelWidth() {
    return this.pixelWidth;
  }

  /**
   * Get the number of pixels along Y axis.
   *
   * @return Returns the number of pixels along Y axis
   */
  public final int getPixelHeight() {
    return this.pixelHeight;
  }

  /**
   * Set the number of pixels along X axis.
   *
   * @param pixelWidthVal number of pixels along X axis
   */
  public final void setPixelWidth(final int pixelWidthVal) {
    this.pixelWidth = pixelWidthVal;
  }

  /**
   * Set the number of pixels along Y axis.
   *
   * @param pixelHeightVal number of pixels along Y axis
   */
  public final void setPixelHeight(final int pixelHeightVal) {
    this.pixelHeight = pixelHeightVal;   
  }

  /**
   * Convert Earth longitude to Ra.
   *
   * @param longEarth Earth longitude in decimal degree
   * @return Returns Ra in decimal degree
   */
  protected final double convertLongitudeFromEarthObsToAstro(final double longEarth) {
    return (longEarth + ANGULAR_DIST_PHI) % ANGULAR_DIST_PHI;
  }

  /**
   * Convert Ra to Earth longitude.
   *
   * @param longAstro Ra in decimal degree
   * @return Returns the Earth longitude in decimal degree
   */
  protected final double convertLongitudeFromAstroToEarth(final double longAstro) {
    return (longAstro + ANGULAR_DIST_PHI_1_5) % ANGULAR_DIST_PHI - ANGULAR_DIST_PHI_DIVIDE_2;
  }

  @Override
  public void paint(final Graphics graphic) {    
    final Graphics2D graphic2D = (Graphics2D) graphic;
    final Map map = new HashMap();
    map.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    map.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    map.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    map.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    graphic2D.setRenderingHints(map);
  }
}
