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
package fr.cnes.sitools.astro.graph;

import healpix.core.AngularPosition;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.RangeSet;
import healpix.essentials.Scheme;
import healpix.essentials.Vec3;
import healpix.tools.CoordTransform;
import healpix.tools.SpatialVector;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This object provides methods for decorate a graph by a grid.
 *
 * <p>
 * Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new HealpixGridDecorator(graph, Scheme.RING, 0);
 * ((HealpixGridDecorator)graph).setCoordinateTransformation(CoordinateTransformation.NATIVE);
 * ((HealpixGridDecorator)graph).setDebug(true);
 * graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
 * ((CircleDecorator)graph).setColor(Color.yellow); 
 * Utility.createJFrame(graph, 900, 500);
 * </code>
 * </pre>
 * </p>
 * @author Jean-Christophe Malapert
 */
public class HealpixGridDecorator extends AbstractGraphDecorator {

  /**
   * Initialize a half space.
   */
  private static final Vec3 HALF_SPACE = new Vec3(Math.cos(Graph.LONG_MAX * Math.PI / 180),
          Math.sin(Graph.LONG_MAX * Math.PI / 180),
          0);
  /**
   * Initialize a normal to the previous half space.
   */
  private static final Vec3 NORMAL_HALF_SPACE = new Vec3(Math.cos(Graph.LONG_MAX / 2 * Math.PI / 180),
          Math.sin(Graph.LONG_MAX / 2 * Math.PI / 180),
          0);
  /**
   * Define an error on the equality of a double.
   */
  private static final double EPSILON = 1E-10;
  /**
   * Define an error on the equality of a double for the graph.
   */  
  private static final double EPSILON_GRAPH = 1E-10;
  /**
   * Converts degree to arcsec.
   */
  protected static final double DEG2ARCSEC = 3600;
  /**
   * Healpix index.
   */
  private HealpixIndex healpixBase;
  /**
   * Transparency.
   */
  private float alpha;
  /**
   * Color of this decorator.
   */
  private Color color;
  /**
   * Initialize a pixel side.
   */
  private PixelSide pixelSide = PixelSide.LEFT;
  /**
   * Initialize a coordinate transformation.
   */
  private CoordinateTransformation coordinateTransformation = CoordinateTransformation.NATIVE;
  /**
   * Debug mode.
   */
  protected boolean isDebug = false;

  /**
   * PixelSide.
   */
  public enum PixelSide {

    /**
     *
     */
    LEFT(0),
    /**
     *
     */
    RIGHT(1);
    /**
     * Code.
     */
    private int code;

    /**
     * Constructor.
     * @param codeVal code 
     */
    PixelSide(final int codeVal) {
      this.code = codeVal;
    }

    /**
     * Returns the code.
     * @return the code
     */
    public int getCode() {
      return this.code;
    }
  }

  /**
   * Supported coordinates transformation.
   */
  public enum CoordinateTransformation {

    /**
     * No transformation.
     */
    NATIVE(-1),
    /**
     * Equatorial to Galactic.
     */
    EQ2GAL(0),
    /**
     * Galactic to Equatorial.
     */
    GAL2EQ(1),
    /**
     * Equatorial to Ecliptic.
     */
    EQ2ECL(2),
    /**
     * Ecliptic to Equatorial.
     */
    ECL2EQ(3),
    /**
     * Ecliptic to Galactic.
     */
    ECL2GAL(4),
    /**
     * Galactic to Equatorial.
     */
    GAL2ECL(5);
    /**
     * transformation code.
     */
    private int transformationCode;

    /**
     * Constructs a coordinate tansformation.
     * @param transformationCodeVal the coordinate transformation code 
     */
    CoordinateTransformation(final int transformationCodeVal) {
      this.transformationCode = transformationCodeVal;
    }

    /**
     * Returns the transformation code.
     * @return the transformation code
     */
    public int getTransformationCode() {
      return this.transformationCode;
    }
  }

  /**
   * Constructs a grid decorator.
   *
   * @param g graph to decorate
   * @param nside nside
   * @param scheme Healpix scheme
   * @param colorVal color of the grid to apply
   * @param alphaVal transparency
   * @throws Exception Healpix Exception
   */
  public HealpixGridDecorator(final Graph g, final int nside, final Scheme scheme, final Color colorVal, final float alphaVal) throws Exception {
    setGraph(g);
    setAlpha(alphaVal);
    setColor(colorVal);
    setHealpixBase(new HealpixIndex(nside, scheme));
  }

  /**
   * Constructs a grid decorator.
   * @param g graph to decorate
   * @param scheme Healpix Scheme
   * @param order Healpix order
   * @param colorVal color of the grid
   * @param alphaVal transparency
   * @throws Exception Healpix Exception
   */
  public HealpixGridDecorator(final Graph g, final Scheme scheme, final int order, final Color colorVal, final float alphaVal) throws Exception {
    this(g, (int) Math.pow(2, order), scheme, colorVal, alphaVal);
  }

  /**
   * Construct a grid decorator.
   * @param g graph to decorate
   * @param scheme Healpix scheme
   * @param order Healpix order
   * @throws Exception Healpix Exception
   */
  public HealpixGridDecorator(final Graph g, final Scheme scheme, final int order) throws Exception {
    this(g, (int) Math.pow(2, order), scheme, Color.RED, 0.5f);
  }

  @Override
  public void paint(Graphics g) {
    getGraph().paint(g);
    Graphics2D g2 = (Graphics2D) g;
    Composite originalComposite = g2.getComposite();
    g2.setComposite(makeComposite(this.getAlpha()));
    drawPixels((Graphics2D) g, getColor());
    g2.setComposite(originalComposite);
  }

  /**
   * Set pixel side of a current vector.
   *
   * @param vectors List of vectors from a pixel
   * @param i index of the current vector
   */
  private void setPixelSide(final Vec3[] vectors, final int i) {

    // detect when current vector is not in the region having (ra,dec) = (180,0)
    if (HALF_SPACE.dot(vectors[i]) < 0) {
      for (int pointIndex = 0; pointIndex < vectors.length; pointIndex++) {
        double val = NORMAL_HALF_SPACE.dot(vectors[pointIndex]);
        val = (val >= -EPSILON && val <= EPSILON) ? 0 : val;
        if (val < 0) {
          this.pixelSide = PixelSide.RIGHT;
          break;
        } else if (val > 0) {
          this.pixelSide = PixelSide.LEFT;
          break;
        } else {
          // need other vectors
        }
      }
    } else {
      // Handling (ra,dec)=(180,0) for a projection centered on (0,0)        
      double val = NORMAL_HALF_SPACE.dot(vectors[i]);
      val = (val >= -EPSILON && val <= EPSILON) ? 0 : val;
      if (val == 0) {
        // need other vectors to know where the pixel should be located
        for (int pointIndex = 0; pointIndex < vectors.length; pointIndex++) {
          val = NORMAL_HALF_SPACE.dot(vectors[pointIndex]);
          val = (val >= -EPSILON && val <= EPSILON) ? 0 : val;
          if (val < 0) {
            this.pixelSide = PixelSide.RIGHT;
            break;
          } else if (val > 0) {
            this.pixelSide = PixelSide.LEFT;
            break;
          } else {
            // Do not know, need the following vector
          }
        }
      } else if (val < 0) {
        this.pixelSide = PixelSide.RIGHT;
      } else {
        this.pixelSide = PixelSide.LEFT;
      }
    }
  }

  /**
   * Split Healpix pixel in two parts
   *
   * @param vectors List of vectors from a pixel
   * @return the two new shapes
   */
  protected Coordinates[] splitHealpixPixelForDetectedBorder(final Vec3[] vectors) {
    Coordinates[] healpixShape = new Coordinates[2];
    healpixShape[0] = new Coordinates();
    healpixShape[1] = new Coordinates();
   
    this.pixelSide = PixelSide.LEFT;
    boolean collision = false;
    int[] vectorIndexCollision = new int[2];

    int frontLeft = 0;
    int frontRight = 0;
    for (int i = 0; i < vectors.length; i++) {
      Vec3 secondPoint = vectors[i];
      Pointing ptg = new Pointing(secondPoint);
      double spra = ptg.phi * 180. / Math.PI,
              spdec = 90. - (ptg.theta * 180. / Math.PI);
      if (spra < 0) {
        spra += 360.;
      }
      PixelSide originalPixelSide = this.pixelSide;
      setPixelSide(vectors, i);
      if (!originalPixelSide.equals(pixelSide) && i != 0) {
        // we change the pixel Side
        vectorIndexCollision[originalPixelSide.getCode()] = i;
        collision = true;
      }
      if (Math.abs(spra - 180.0) < EPSILON_GRAPH) {
        // border, we add an entry at each shape
        frontLeft++;
        frontRight++;
        healpixShape[PixelSide.LEFT.getCode()].addCoordinate(spra - EPSILON_GRAPH, spdec);
        healpixShape[PixelSide.RIGHT.getCode()].addCoordinate(spra + EPSILON_GRAPH, spdec);
        collision = true;
      } else {
        healpixShape[pixelSide.getCode()].addCoordinate(spra, spdec);
        if (collision && pixelSide.equals(PixelSide.RIGHT)) {
          healpixShape[PixelSide.LEFT.getCode()].addCoordinate(180.0 - EPSILON_GRAPH, spdec);
          frontLeft++;
        } else if (collision && pixelSide.equals(PixelSide.LEFT)) {
          healpixShape[PixelSide.RIGHT.getCode()].addCoordinate(180.0 + EPSILON_GRAPH, spdec);
          frontRight++;
        }
      }
    }

    // we remove the shape if there is only border ra=180 on this shape
    if (healpixShape[PixelSide.LEFT.getCode()].getLength() == frontLeft) {
      healpixShape[PixelSide.LEFT.getCode()] = new Coordinates();
    }
    if (healpixShape[PixelSide.RIGHT.getCode()].getLength() == frontRight) {
      healpixShape[PixelSide.RIGHT.getCode()] = new Coordinates();
    }

    return healpixShape;
  }

  /**
   *
   * @param isDebug
   */
  public void setDebug(boolean isDebug) {
    this.isDebug = isDebug;
  }

  /**
   * Draw pixels
   *
   * @param g2 graph
   * @param color color of pixels
   */
  protected void drawPixels(Graphics2D g2, final Color color) {
    g2.setPaint(color);
    try {
      HealpixIndex healpixRing = new HealpixIndex(getHealpixBase().getNside(), Scheme.RING);
      RangeSet rangePixelsRing = healpixRing.queryStrip(Math.toRadians(90 - Graph.LAT_MAX), Math.toRadians(90 - Graph.LAT_MIN), true);
      RangeSet.ValueIterator iter = rangePixelsRing.valueIterator();
      while (iter.hasNext()) {
        long pixelRing = iter.next();
        long pixelNested = healpixRing.ring2nest(pixelRing);
        drawHealpixPolygon(g2, getHealpixBase(), pixelNested, getCoordinateTransformation());
      }
    } catch (Exception ex) {
      Logger.getLogger(HealpixGridDecorator.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Compute reference frame transformation
   *
   * @param vectors vectors to transform
   * @param coordTransformation Type of transformation
   * @return Returns the vectors that have been converted
   * @throws Exception
   */
  protected Vec3[] computeReferenceFrameTransformation(Vec3[] vectors, final CoordinateTransformation coordTransformation) throws Exception {
    if (coordTransformation.equals(CoordinateTransformation.NATIVE)) {
      return vectors;
    }

    AngularPosition angularPosition = new AngularPosition();
    for (int i = 0; i < vectors.length; i++) {
      Vec3 vector = vectors[i];
      SpatialVector sp = new SpatialVector(vector.x, vector.y, vector.z);
      double ra = sp.ra();
      double dec = sp.dec();
      angularPosition.setPhi(ra);
      angularPosition.setTheta(dec);
      angularPosition = CoordTransform.transformInDeg(angularPosition, coordTransformation.getTransformationCode());
      ra = angularPosition.phi;
      dec = angularPosition.theta;
      Pointing point = new Pointing((90.0 - dec) * Math.PI / 180.0, ra * Math.PI / 180.0);
      vectors[i] = new Vec3(point);
    }
    return vectors;
  }

  /**
   * Compute number of vectors are needed to have the better approximation of the shape.
   *
   * @param nside nside
   * @param pix pixel number
   * @return Return the number of vectors needed between two corners from HEALPIX pixel
   */
  protected int computeNumberPointsForPixel(int nside, long pix) {
    //TO DO : modifier getPixRes();
    double angularResolution = HealpixIndex.getPixRes(nside);
    double angularResolutionImage = 360 * DEG2ARCSEC / getPixelWidth();
    int numberOfStep = (int) (angularResolution / angularResolutionImage);
    return (numberOfStep == 0) ? 1 : numberOfStep;
  }

  /**
   *
   * @param g2
   * @param healpix
   * @param pix
   * @param coordinateTransformation
   */
  protected void drawHealpixPolygon(Graphics2D g2, final HealpixIndex healpix, long pix, final CoordinateTransformation coordinateTransformation) {
    try {
      int numberOfVectors = computeNumberPointsForPixel(getHealpixBase().getNside(), pix);
      Vec3[] vectors = healpix.boundaries(pix, numberOfVectors);
      computeReferenceFrameTransformation(vectors, coordinateTransformation);
      Coordinates[] shapes = splitHealpixPixelForDetectedBorder(vectors);

      for (int i = 0; i < shapes.length; i++) {
        Coordinates shape = shapes[i];
        List<Point2D.Double> pixels = shape.getPixelsFromProjection(this.getProjection(), getRange(), getPixelWidth(), getPixelHeight());
        g2.draw(new Polygon2D(pixels));

        // draw text
        if (!pixels.isEmpty() && isDebug) {
          Point2D.Double center = shape.getPixelCenterFromProjection(this.getProjection(), getRange(), getPixelWidth(), getPixelHeight());
          g2.drawString(String.valueOf(pix), (float) center.x, (float) center.y);//
        }
      }



    } catch (Exception ex) {
      Logger.getLogger(HealpixGridDecorator.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Get healpix index
   *
   * @return the healpixIndex
   */
  protected HealpixIndex getHealpixBase() {
    return healpixBase;
  }

  /**
   * Set healpix index
   *
   * @param healpixBase
   */
  protected final void setHealpixBase(HealpixIndex healpixBase) {
    this.healpixBase = healpixBase;
  }

  /**
   * Get transparency value
   *
   * @return the alpha
   */
  public float getAlpha() {
    return alpha;
  }

  /**
   * Set the Opacity value
   *
   * @param alpha the alpha to set
   */
  public final void setAlpha(float alpha) {
    this.alpha = alpha;
  }

  /**
   * Get the color
   *
   * @return the color
   */
  public Color getColor() {
    return color;
  }

  /**
   * Set the color
   *
   * @param color the color to set
   */
  public final void setColor(final Color color) {
    this.color = color;
  }

  /**
   * Get the coordinate transformation
   *
   * @return the coordinateTransformation
   */
  public CoordinateTransformation getCoordinateTransformation() {
    return coordinateTransformation;
  }

  /**
   * Set the coordinate transformation
   *
   * @param coordinateTransformation the coordinateTransformation to set
   */
  public void setCoordinateTransformation(final CoordinateTransformation coordinateTransformation) {
    this.coordinateTransformation = coordinateTransformation;
  }
  private static final Logger LOG = Logger.getLogger(HealpixGridDecorator.class.getName());
}
