/*******************************************************************************
* Copyright 2012, 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.jhlabs.map.MapMath;
import com.jhlabs.map.proj.ProjectionException;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This object provides methods to decorate the Graph by coordinates
 *
 * <p>
 * This class allows to plot coordinate axes to the graph
 * </p>
 * 
 * <p>
 * Here is a code to illustrate how to use it:<br/>
 * <pre>
 * <code>
 * Graph graph = new GenericProjection(Graph.ProjectionType.ECQ); 
 * graph = new CoordinateDecorator(graph, Color.RED, 0.5f)
 * graph = new CircleDecorator(graph, 0.0, 0.0, 1, Scheme.RING, 10);
 * ((CircleDecorator)graph).setColor(Color.yellow); 
 * Utility.createJFrame(graph, 900, 500);
 * </code>
 * </pre>
 * </p>
 * @author Jean-Christophe Malapert
 */
public class CoordinateDecorator extends AbstractGraphDecorator {
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(CoordinateDecorator.class.getName());
    /**
     * Opacity from 0.0 to 1.0.
     */
    private float alpha;
    /**
     * Color for the coordinates axes.
     */
    private final Color color;
    /**
     * Default value for alpha sets to 0.5.
     */
    private static final float DEFAULT_ALPHA = 0.5f;
    /**
     * Incremental step for drawing grid (value = 10°).
     */
    private static final int INCREMENTAL_STEP_GRID = 10;
    /**
     * Incremental step for drawing grid (value = 20°).
     */
    private static final int TWO_INCREMENTAL_STEP_GRID = 20;    

    /**
     * Constructor.
     * @param g graph component
     * @param colorVal color of this concrete decorator
     * @param alphaVal transparency from 0 to 1
     */
    public CoordinateDecorator(final Graph g, final Color colorVal, final float alphaVal) {
        setGraph(g);
        this.color = colorVal;
        this.alpha = alphaVal;
    }

    /**
     * Constructs a new CoordinateDecorator.
     * @param g graph
     */
    public CoordinateDecorator(final Graph g) {
        this(g, Color.YELLOW, DEFAULT_ALPHA);
    }

    /**
     * Draw latitudes/declination.
     * @param g2 graph component
     * @param colorVal color
     */
    protected final void drawLatitudeLines(final Graphics2D g2, final Color colorVal) {
        g2.setPaint(colorVal);
        for (int lat = Graph.LAT_MIN; lat <= Graph.LAT_MAX; lat += INCREMENTAL_STEP_GRID) {
            Point2D.Double pos1 = null;
            for (int lon = Graph.LONG_MIN; lon <= Graph.LONG_MAX; lon += INCREMENTAL_STEP_GRID) {
                if (getGraph().getProjection().inside(MapMath.degToRad(lon), MapMath.degToRad(lat))) {
                    Point2D.Double pos2 = new Point2D.Double();
                    try {
                        getGraph().getProjection().project(MapMath.degToRad(lon), MapMath.degToRad(lat), pos2);
                        if (pos1 != null && pos2 != null) {
                            g2.draw(new Line2D.Double(getGraph().scaleX(pos1.x), getGraph().scaleY(pos1.y), getGraph().scaleX(pos2.x), getGraph().scaleY(pos2.y)));
                        }
                        pos1 = pos2;
                    } catch (ProjectionException ex) {
                        LOG.log(Level.WARNING, ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Draw longitudes/right ascension.
     * @param g2 graph
     * @param colorVal color
     */
    protected final void drawLongitudeLines(final Graphics2D g2, final Color colorVal) {
        g2.setPaint(colorVal);
        for (int lon = Graph.LONG_MIN; lon <= Graph.LONG_MAX; lon += TWO_INCREMENTAL_STEP_GRID) {
            Point2D.Double pos1 = null;
            for (int lat = Graph.LAT_MIN; lat <= Graph.LAT_MAX; lat += INCREMENTAL_STEP_GRID) {
                if (getGraph().getProjection().inside(MapMath.degToRad(lon), MapMath.degToRad(lat))) {
                    Point2D.Double pos2 = new Point2D.Double();
                    try {
                        getGraph().getProjection().project(MapMath.degToRad(lon), MapMath.degToRad(lat), pos2);
                        if (pos1 != null && pos2 != null) {
                            g2.draw(new Line2D.Double(getGraph().scaleX(pos1.x), getGraph().scaleY(pos1.y), getGraph().scaleX(pos2.x), getGraph().scaleY(pos2.y)));
                        }
                        pos1 = pos2;
                    } catch (ProjectionException ex) {
                        LOG.log(Level.FINEST, ex.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public final void paint(final Graphics g) {
        getGraph().paint(g);
        Graphics2D g2 = (Graphics2D) g;
        Composite originalComposite = g2.getComposite();
        g2.setComposite(makeComposite(this.alpha));
        drawLatitudeLines(g2, color);
        drawLongitudeLines(g2, color);
        g2.setComposite(originalComposite);
    }
}
