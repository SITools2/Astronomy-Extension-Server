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
 * <p>Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new HealpixGridDecorator(graph, Scheme.RING, 0);
 * ((HealpixGridDecorator)graph).setCoordinateTransformation(CoordinateTransformation.NATIVE);
 * ((HealpixGridDecorator)graph).setDebug(true);
 * graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
 * ((CircleDecorator)graph).setColor(Color.yellow); 
 * Utility.createJFrame(graph, 900);
 * </code>
 * </pre></p>
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class HealpixGridDecorator extends AbstractGraphDecorator {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(HealpixGridDecorator.class.getName());
  /**
   * Default value for transparency.
   */
  public static final float DEFAULT_TRANSPARENCY = 0.5f;  
  /**
   * Angle = 180.
   */
  private static final double ANGLE_HALF_CIRCLE = 180.;
  /**
   * Angle = 360.
   */
  private static final double ANGLE_CIRCLE = 360.;  
  /**
   * Max latitude in decimal degree.
   */
  private static final double MAX_LAT = 90.;
  /**
   * Initialize a half space.
   */
  private static final Vec3 HALF_SPACE = new Vec3(Math.cos(Graph.LONG_MAX * Math.PI / ANGLE_HALF_CIRCLE),
                                                  Math.sin(Graph.LONG_MAX * Math.PI / ANGLE_HALF_CIRCLE),
                                                  0);
  /**
   * Initialize a normal to the previous half space.
   */
  private static final Vec3 NORMAL_HALF_SPACE = new Vec3(Math.cos(Graph.LONG_MAX / 2 * Math.PI / ANGLE_HALF_CIRCLE),
                                                         Math.sin(Graph.LONG_MAX / 2 * Math.PI / ANGLE_HALF_CIRCLE),
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
  private transient PixelSide pixelSide = PixelSide.LEFT;
  /**
   * Initialize a coordinate transformation.
   */
  private CoordinateTransformation coordinateTransformation = CoordinateTransformation.NATIVE;
  /**
   * Debug mode.
   */
  private boolean debug = false;

  /**
   * Returns if the grid is in debug mode.
   * @return the debug
   */
  public final boolean isDebug() {
      return debug;
  }
    
  /**
   * Sets the debug flag.
   * @param isDebugVal debug
   */
  public final void setDebug(final boolean isDebugVal) {
    this.debug = isDebugVal;
  }    

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
   * @param graph graph to decorate
   * @param nside nside
   * @param scheme Healpix scheme
   * @param colorVal color of the grid to apply
   * @param alphaVal transparency
   * @throws GraphException Healpix Exception
   */
  public HealpixGridDecorator(final Graph graph, final int nside, final Scheme scheme, final Color colorVal, final float alphaVal) throws GraphException {
    setGraph(graph);
    setAlpha(alphaVal);
    setColor(colorVal);
      try {
          setHealpixBase(new HealpixIndex(nside, scheme));
      } catch (Exception ex) {
          LOG.log(Level.FINER, null, ex);
          throw new GraphException(ex);
      }
  }

  /**
   * Constructs a grid decorator.
   * @param graph graph to decorate
   * @param scheme Healpix Scheme
   * @param order Healpix order
   * @param colorVal color of the grid
   * @param alphaVal transparency
   * @throws GraphException Healpix Exception
   */
  public HealpixGridDecorator(final Graph graph, final Scheme scheme, final int order, final Color colorVal, final float alphaVal) throws GraphException {
    this(graph, (int) Math.pow(2, order), scheme, colorVal, alphaVal);
  }

  /**
   * Construct a grid decorator.
   * @param graph graph to decorate
   * @param scheme Healpix scheme
   * @param order Healpix order
   * @throws Exception Healpix Exception
   */
  public HealpixGridDecorator(final Graph graph, final Scheme scheme, final int order) throws Exception {
    this(graph, (int) Math.pow(2, order), scheme, Color.RED, DEFAULT_TRANSPARENCY);
  }

  @Override
  public void paint(final Graphics graphic) {
    getGraph().paint(graphic);
    final Graphics2D graphic2D = (Graphics2D) graphic;
    final Composite originalComposite = graphic2D.getComposite();
    graphic2D.setComposite(makeComposite(this.getAlpha()));
    drawPixels((Graphics2D) graphic, getColor());
    graphic2D.setComposite(originalComposite);
  }

  /**
   * Set pixel side of a current vector.
   *
   * @param vectors List of vectors from a pixel
   * @param index index of the current vector
   */
  private void setPixelSide(final Vec3[] vectors, final int index) {

    // detect when current vector is not in the region having (rightAscension,declination) = (180,0)
    if (HALF_SPACE.dot(vectors[index]) < 0) {
      for (int pointIndex = 0; pointIndex < vectors.length; pointIndex++) {
        final double scalarProduct = NORMAL_HALF_SPACE.dot(vectors[pointIndex]);
        final double val = (scalarProduct >= -EPSILON && scalarProduct <= EPSILON) ? 0 : scalarProduct;
        if (val < 0) {
          this.pixelSide = PixelSide.RIGHT;
          break;
        } else if (val > 0) {
          this.pixelSide = PixelSide.LEFT;
          break;
        }
      }
    } else {
      // Handling (rightAscension,declination)=(180,0) for a projection centered on (0,0)        
      double scalarProduct = NORMAL_HALF_SPACE.dot(vectors[index]);
      double val = (scalarProduct >= -EPSILON && scalarProduct <= EPSILON) ? 0 : scalarProduct;
      if (val == 0) {
        // need other vectors to know where the pixel should be located
        for (int pointIndex = 0; pointIndex < vectors.length; pointIndex++) {
          scalarProduct = NORMAL_HALF_SPACE.dot(vectors[pointIndex]);
          val = (scalarProduct >= -EPSILON && scalarProduct <= EPSILON) ? 0 : scalarProduct;
          if (val < 0) {
            this.pixelSide = PixelSide.RIGHT;
            break;
          } else if (val > 0) {
            this.pixelSide = PixelSide.LEFT;
            break;
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
   * Split Healpix pixel in two parts and return them.
   *
   * @param vectors List of vectors from a pixel
   * @return the two new shapes
   */
  protected final Coordinates[] splitHealpixPixelForDetectedBorder(final Vec3[] vectors) {
    final Coordinates[] healpixShape = new Coordinates[]{new Coordinates(), new Coordinates()};    
    this.pixelSide = PixelSide.LEFT;
    boolean collision = false;
    //int[] vectorIndexCollision = new int[2];

    int frontLeft = 0;
    int frontRight = 0;
    for (int i = 0; i < vectors.length; i++) {
      final Vec3 secondPoint = vectors[i];
      final Pointing ptg = new Pointing(secondPoint);
      double spra = ptg.phi * ANGLE_HALF_CIRCLE / Math.PI;
      final double spdec = MAX_LAT - (ptg.theta * ANGLE_HALF_CIRCLE / Math.PI);
      if (spra < 0) {
        spra += ANGLE_CIRCLE;
      }
      final PixelSide originalPixelSide = this.pixelSide;
      setPixelSide(vectors, i);
      if (!originalPixelSide.equals(pixelSide) && i != 0) {
        // we change the pixel Side
        //vectorIndexCollision[originalPixelSide.getCode()] = i;
        collision = true;
      }
      if (Math.abs(spra - ANGLE_HALF_CIRCLE) < EPSILON_GRAPH) {
        // border, we add an entry at each shape
        frontLeft++;
        frontRight++;
        healpixShape[PixelSide.LEFT.getCode()].addCoordinate(spra - EPSILON_GRAPH, spdec);
        healpixShape[PixelSide.RIGHT.getCode()].addCoordinate(spra + EPSILON_GRAPH, spdec);
        collision = true;
      } else {
        healpixShape[pixelSide.getCode()].addCoordinate(spra, spdec);
        if (collision && pixelSide.equals(PixelSide.RIGHT)) {
          healpixShape[PixelSide.LEFT.getCode()].addCoordinate(ANGLE_HALF_CIRCLE - EPSILON_GRAPH, spdec);
          frontLeft++;
        } else if (collision && pixelSide.equals(PixelSide.LEFT)) {
          healpixShape[PixelSide.RIGHT.getCode()].addCoordinate(ANGLE_HALF_CIRCLE + EPSILON_GRAPH, spdec);
          frontRight++;
        }
      }
    }

    // we remove the shape if there is only border rightAscension=180 on this shape
    if (healpixShape[PixelSide.LEFT.getCode()].getLength() == frontLeft) {
      healpixShape[PixelSide.LEFT.getCode()] = new Coordinates();
    }
    if (healpixShape[PixelSide.RIGHT.getCode()].getLength() == frontRight) {
      healpixShape[PixelSide.RIGHT.getCode()] = new Coordinates();
    }

    return healpixShape;
  }

  /**
   * Draws pixels.
   *
   * @param graphic2D graph to decorate
   * @param color color of pixels
   */
  protected void drawPixels(final Graphics2D graphic2D, final Color color) {
    graphic2D.setPaint(color);
    try {
      final HealpixIndex healpixRing = new HealpixIndex(getHealpixBase().getNside(), Scheme.RING);
      final RangeSet rangePixelsRing = healpixRing.queryStrip(Math.toRadians(MAX_LAT - Graph.LAT_MAX), Math.toRadians(MAX_LAT - Graph.LAT_MIN), true);
      final RangeSet.ValueIterator iter = rangePixelsRing.valueIterator();
      while (iter.hasNext()) {
        final long pixelRing = iter.next();
        final long pixelNested = healpixRing.ring2nest(pixelRing);
        drawHealpixPolygon(graphic2D, getHealpixBase(), pixelNested, getCoordinateTransformation());
      }
    } catch (Exception ex) {
      LOG.log(Level.FINER, null, ex);
    }
  }

  /**
   * Computes reference frame transformation.
   *
   * @param vectors vectors to transform
   * @param coordTransformation Type of transformation
   * @return Returns the vectors that have been converted
   * @throws GraphException Coordinates transformation exception
   */
  protected final Vec3[] computeReferenceFrameTransformation(final Vec3[] vectors, final CoordinateTransformation coordTransformation) throws GraphException {
    if (coordTransformation.equals(CoordinateTransformation.NATIVE)) {
      return vectors;
    }

    AngularPosition angularPosition = new AngularPosition();
    for (int i = 0; i < vectors.length; i++) {
        try {
            final Vec3 vector = vectors[i];
            final SpatialVector sp = new SpatialVector(vector.x, vector.y, vector.z);
            double rightAscension = sp.ra();
            double declination = sp.dec();
            angularPosition.setPhi(rightAscension);
            angularPosition.setTheta(declination);
            angularPosition = CoordTransform.transformInDeg(angularPosition, coordTransformation.getTransformationCode());
            rightAscension = angularPosition.phi;
            declination = angularPosition.theta;
            final Pointing point = new Pointing((MAX_LAT - declination) * Math.PI / ANGLE_HALF_CIRCLE, rightAscension * Math.PI / ANGLE_HALF_CIRCLE);
            vectors[i] = new Vec3(point);
        } catch (Exception ex) {
            LOG.log(Level.FINER, null, ex);
            throw new GraphException(ex);
        }
    }
    return vectors;
  }

  /**
   * Computes number of vectors are needed to have the better approximation of the shape.
   *
   * @param nside nside
   * @param pix pixel number
   * @return Return the number of vectors needed between two corners from HEALPIX pixel
   */
  protected final int computeNumberPointsForPixel(final int nside, final long pix) {
    //TO DO : modifier getPixRes();
    final double angularResolution = HealpixIndex.getPixRes(nside);
    final double angularResolutionImage = ANGLE_CIRCLE * DEG2ARCSEC / getPixelWidth();
    final int numberOfStep = (int) (angularResolution / angularResolutionImage);
    return (numberOfStep == 0) ? 1 : numberOfStep;
  }

  /**
   * Draws Healpix.
   * 
   * @param graphic2D graph to decorate
   * @param healpix healpix
   * @param pix pixel to draw
   * @param coordinateTransformationVal coordinate transformation
   */
  protected void drawHealpixPolygon(final Graphics2D graphic2D, final HealpixIndex healpix, final long pix, final CoordinateTransformation coordinateTransformationVal) {
    try {
      final int numberOfVectors = computeNumberPointsForPixel(getHealpixBase().getNside(), pix);
      final Vec3[] vectors = healpix.boundaries(pix, numberOfVectors);
      computeReferenceFrameTransformation(vectors, coordinateTransformationVal);
      final Coordinates[] shapes = splitHealpixPixelForDetectedBorder(vectors);

      final Polygon2D poly = new Polygon2D();
      for (int i = 0; i < shapes.length; i++) {
        final Coordinates shape = shapes[i];
        final List<Point2D.Double> pixels = shape.getPixelsFromProjection(this.getProjection(), getRange(), getPixelWidth(), getPixelHeight());
        poly.setPoints(pixels);
        graphic2D.draw(poly);

        // draw text
        if (!pixels.isEmpty() && isDebug()) {
          final Point2D.Double center = shape.getPixelCenterFromProjection(this.getProjection(), getRange(), getPixelWidth(), getPixelHeight());
          graphic2D.drawString(String.valueOf(pix), (float) center.x, (float) center.y);
        }
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * Returns the healpix index.
   *
   * @return the healpixIndex
   */
  protected final HealpixIndex getHealpixBase() {
    return healpixBase;
  }

  /**
   * Sets the healpix index.
   *
   * @param healpixBaseVal Healpix index
   */
  protected final void setHealpixBase(final HealpixIndex healpixBaseVal) {
    this.healpixBase = healpixBaseVal;
  }

  /**
   * Returns the transparency value.
   *
   * @return the alpha
   */
  public final float getAlpha() {
    return alpha;
  }

  /**
   * Sets the Opacity value.
   *
   * @param alphaVal the alpha to set
   */
  public final void setAlpha(final float alphaVal) {
    this.alpha = alphaVal;
  }

  /**
   * Returns the color.
   *
   * @return the color
   */
  public final Color getColor() {
    return color;
  }

  /**
   * Sets the color.
   *
   * @param colorVal the color to set
   */
  public final void setColor(final Color colorVal) {
    this.color = colorVal;
  }

  /**
   * Returns the coordinate transformation.
   *
   * @return the coordinateTransformation
   */
  public final CoordinateTransformation getCoordinateTransformation() {
    return coordinateTransformation;
  }

  /**
   * Sets the coordinate transformation.
   *
   * @param coordinateTransformationVal the coordinateTransformation to set
   */
  public final void setCoordinateTransformation(final CoordinateTransformation coordinateTransformationVal) {
    this.coordinateTransformation = coordinateTransformationVal;
  }
}
