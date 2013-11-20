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
package fr.cnes.sitools.searchgeometryengine;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This object contains methods to create a cone.
 *
 * <p>
 * A cone is delimited by both a disk radius on the sphere and a point that is
 * the center of this cone. The origin of the cone is the center of the Earth.
 * </p>
 *
 * <p>
 * As below , an example for creating a cone in the Geocentric frame:<br/>
 * <pre>
 * <code>
 * double longitude = -20.0;
 * double latitude = 10.0;
 * double radius = Math.PI/4;
 * Point coneCenter = new Point(longitude, latitude, CoordSystem.GEOCENTRIC);
 * Shape shape = new Cone(coneCenter,radius);
 * </code>
 * </pre>
 * <br/>
 * <br/>
 * Another example for creating a cone in the Equatorial frame:<br/>
 * <pre>
 * <code>
 * double longitude = 240.0;
 * double latitude = 10.0;
 * double radius = Math.PI/4;
 * Point coneCenter = new Point(longitude, latitude, CoordSystem.EQUATORIAL);
 * Shape shape = new Cone(coneCenter,radius);
 * </code>
 * </pre>
 * </p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class Cone implements Shape {
    /** Logger. */
    private static final Logger LOG = Logger.getLogger(Cone.class.getName());
    /** Center of the disk on the sphere. */
    private Point center;
    /** Radius of the disk in radians. */
    private double radius;

    /**
     * Constructs a cone with the center of the cone on the disk
     * and a radius in radians from this center.
     *
     * @param centerCone the disk center of the sphere
     * @param radiusCone the disk radius in radians
     */
    public Cone(final Point centerCone, final double radiusCone) {
        this();
        assert centerCone != null;
        setCenter(centerCone);
        setRadius(radiusCone);
        LOG.log(Level.FINEST, "Cone(center,radius) =  ({0},{1})", new Object[]{center, radius});
    }

    /** Empty constructor. */
    protected Cone() {
    }

    /**
     * Returns the disk center on the sphere.
     *
     * @return the center
     */
    public final Point getCenter() {
        return center;
    }

    /**
     * Get the disk radius in radians.
     *
     * @return the disk radius in radians
     */
    public final double getRadius() {
        return radius;
    }

    /**
     * Returns <code>true</code> because a cone is a surface delimited by its disk.
     *
     * @return true
     */
    @Override
    public final boolean isSurface() {
        return true;
    }

    /**
     * Sets the disk center.
     *
     * @param val the center
     */
    protected final void setCenter(final Point val) {
        assert val != null;
        this.center = val;
    }

    /**
     * Sets the disk radius in radians.
     *
     * @param val the disk radius in radians
     */
    protected final void setRadius(final double val) {
        this.radius = val;
    }

    /**
     * Returns the geometry type.
     * @return the geometry type
     */
    @Override
    public final Type getType() {
        return Shape.Type.CONE;
    }

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.center != null ? this.center.hashCode() : 0);
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.radius) ^ (Double.doubleToLongBits(this.radius) >>> 32));
        return hash;
    }

    @Override
    public final boolean equals(final Object obj) {
        boolean isEqual;
        //check for self-comparison
        if (this == obj) {
            isEqual = true;
        } else if (!(obj instanceof Cone)) {
            isEqual = false;
        } else {
            //cast to native object is now safe
            final Cone that = (Cone) obj;

            //now a proper field-by-field evaluation can be made
            isEqual =  that.getCenter().equals(this.center)
                    && that.getRadius() == this.radius
                    && that.getType() == Shape.Type.CONE;
        }
        return isEqual;
    }
}
