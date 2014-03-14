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
package fr.cnes.sitools.astro.cutout;

import healpix.core.HealpixIndex;
import healpix.tools.SpatialVector;

import java.awt.geom.Point2D;

import jsky.coords.WCSTransform;
import fr.cnes.sitools.extensions.common.AstroCoordinate;

/**
 * Create WCS.
 */
public class WcsComputation {

    /**
     * Converts one degree to arcsec.
     */
    private static final double DEG_TO_ARCSEC = 3600;
    /**
     * Converts one arsec to degree.
     */
    private static final double ARCSEC_TO_DEG = 1.0 / DEG_TO_ARCSEC;
    /**
     * Epoch = 2000.
     */
    private static final double EPOCH = 2000;
    /**
     * Equinox = 2000.
     */
    private static final int EQUINOX = 2000;
    /**
     * Number of pixels along X axis.
     */
    private transient int naxis1;
    /**
     * Number of pixels along Y axis.
     */
    private transient int naxis2;
    /**
     * X coordinate of the center in the sky.
     */
    private transient double crval1;
    /**
     * Y coordinate of the center in the sky.
     */
    private transient double crval2;
    /**
     * X coordinate of the center in the detector.
     */
    private transient double crpix1;
    /**
     * Y coordinate of the center in the detector.
     */
    private transient double crpix2;
    /**
     * projection along X axis.
     */
    private transient String ctype1;
    /**
     * projection along Y axis.
     */
    private transient String ctype2;
    /**
     * Arcsec per pixel along X axis.
     */
    private transient double cdelt1;
    /**
     * Arcsec per pixel along X axis.
     */
    private transient double cdelt2;
    /**
     * Rotation of the detector according to the north.
     */
    private transient double crota;
    /**
     * CD matrix.
     */
    private transient double[] cd = new double[4];
    /**
     * WCS.
     */
    private transient WCSTransform wcs;

    /**
     * Constructs a WCS object based on ICRS.
     *
     * @param fov coordinates of 4 points
     * @param cdelt1 arcsec per pixel along X axis
     * @param cdelt2 arcsec per pixel along Y axis
     * @param rotation rotation
     * @param coordinateSystem coordinate system
     * @throws Exception
     */
    public WcsComputation(final double[] fov, final double cdelt1, final double cdelt2, double rotation, final AstroCoordinate.CoordinateSystem coordinateSystem) throws Exception {
        if (fov.length != 8) {
            throw new IllegalArgumentException("Too much points in the shape definition");
        }
        computeCoeffWcs(fov, cdelt1, cdelt2, rotation, coordinateSystem);
        setWcs(new WCSTransform(getCrval1(), getCrval2(),
                getCdelt1() * DEG_TO_ARCSEC, getCdelt2() * DEG_TO_ARCSEC,
                getCrpix1(), getCrpix2(),
                getNaxis1(), getNaxis2(),
                getCrota(), EQUINOX, EPOCH, "-TAN"));
    }

    /**
     * Computes WCS coeff.
     *
     * @param fov coordinates of the field of view in the coordinate system
     * @param cdelt1 arcsec per pixel along X for the ouput
     * @param cdelt2 arcsec per pixel along Y for the ouput
     * @param rotation rotation (positive from North to East)
     * @param coordinateSystem coordinate system
     * @throws Exception
     */
    private void computeCoeffWcs(final double[] fov, final double cdelt1, final double cdelt2, final double rotation, final AstroCoordinate.CoordinateSystem coordinateSystem) throws Exception {
        final int numberPoints = (int) (fov.length / 2.0);
        final SpatialVector[] vectors = new SpatialVector[numberPoints];
        for (int i = 0; i < numberPoints; i++) {
            vectors[i] = new SpatialVector(fov[i * 2], fov[(i * 2 + 1)]);
        }
        final SpatialVector centerShape = computeCenterShape(vectors);
        final double distAngX = HealpixIndex.angDist(vectors[0], vectors[1]);
        final double distAngY = HealpixIndex.angDist(vectors[0], vectors[vectors.length - 1]);
        final int nbPixelsAlongX = (int) Math.round(Math.toDegrees(distAngX) / (cdelt1 * ARCSEC_TO_DEG));
        final int nbPixelsAlongY = (int) Math.round(Math.toDegrees(distAngY) / (cdelt2 * ARCSEC_TO_DEG));
        setCdelt1(cdelt1 * ARCSEC_TO_DEG * -1);
        setCdelt2(cdelt2 * ARCSEC_TO_DEG);
        setNaxis1(nbPixelsAlongX);
        setNaxis2(nbPixelsAlongY);
        setCrval1(centerShape.ra());
        setCrval2(centerShape.dec());
        setCrpix1(nbPixelsAlongX / 2.0);
        setCrpix2(nbPixelsAlongY / 2.0);
        setCrota(rotation);
        setCd(new double[]{getCdelt1() * Math.cos(Math.toRadians(getCrota())),
            -1 * getCdelt2() * Math.sin(Math.toRadians(getCrota())),
            getCdelt1() * Math.sin(Math.toRadians(getCrota())),
            getCdelt2() * Math.cos(Math.toRadians(getCrota()))});
        switch (coordinateSystem) {
            case GALACTIC:
                setCtype1("GLON-TAN");
                setCtype2("GLAT-TAN");
                break;
            case EQUATORIAL:
                setCtype1("RA---TAN");
                setCtype2("DEC--TAN");
                break;
            default:
                throw new IllegalAccessException("The coordinate system " + coordinateSystem + " is not supported.");
        }
    }

    /**
     * Computes the center of the shape.
     *
     * @param vectors vectors that provides the shape.
     * @return Returns the center of the shape.
     */
    private SpatialVector computeCenterShape(final SpatialVector[] vectors) {
        SpatialVector centerShape = new SpatialVector(vectors[0]);
        for (int iPoint = 1; iPoint < vectors.length; iPoint++) {
            centerShape = centerShape.add(vectors[iPoint]);
        }
        return centerShape;
    }

    /**
     * Transforms pixels to sky and returns it.
     *
     * @param x pixel number along X
     * @param y pixel number along Y
     * @return the sky coordinates
     */
    protected final Point2D.Double pix2wcs(final double x, final double y) {
        return getWcs().pix2wcs(x, y);
    }

    /**
     * Returns the number of pixels along RA.
     *
     * @return the naxis1
     */
    protected final int getNaxis1() {
        return naxis1;
    }

    /**
     * Sets the number of pixels along Ra axis.
     *
     * @param naxis1Val the naxis1 to set
     */
    protected final void setNaxis1(final int naxis1Val) {
        this.naxis1 = naxis1Val;
    }

    /**
     * Returns the number of pixels along Declination axis.
     *
     * @return the naxis2
     */
    protected final int getNaxis2() {
        return naxis2;
    }

    /**
     * Sets the number of pixels along declination axis.
     *
     * @param naxis2Val the naxis2 to set
     */
    protected final void setNaxis2(final int naxis2Val) {
        this.naxis2 = naxis2Val;
    }

    /**
     * Returns the CRVAL1.
     *
     * @return the crval1
     */
    protected final double getCrval1() {
        return crval1;
    }

    /**
     * Sets the CRVAL1.
     *
     * @param crval1Val the crval1 to set
     */
    protected final void setCrval1(final double crval1Val) {
        this.crval1 = crval1Val;
    }

    /**
     * Returns the CRVAL2.
     *
     * @return the crval2
     */
    protected final double getCrval2() {
        return crval2;
    }

    /**
     * Sets the CRVAL2.
     *
     * @param crval2Val the crval2 to set
     */
    protected final void setCrval2(final double crval2Val) {
        this.crval2 = crval2Val;
    }

    /**
     * Returns the CRPIX1.
     *
     * @return the crpix1
     */
    protected final double getCrpix1() {
        return crpix1;
    }

    /**
     * Sets the CRPIX1.
     *
     * @param crpix1Val the crpix1 to set
     */
    protected final void setCrpix1(final double crpix1Val) {
        this.crpix1 = crpix1Val;
    }

    /**
     * Returns the CRPIX2.
     *
     * @return the crpix2
     */
    protected final double getCrpix2() {
        return crpix2;
    }

    /**
     * Sets the CRPIX2.
     *
     * @param crpix2Val the crpix2 to set
     */
    protected final void setCrpix2(final double crpix2Val) {
        this.crpix2 = crpix2Val;
    }

    /**
     * Returns CTYPE1.
     *
     * @return the ctype1
     */
    protected final String getCtype1() {
        return ctype1;
    }

    /**
     * Sets the CTYPE1.
     *
     * @param ctype1Val the ctype1 to set
     */
    protected final void setCtype1(final String ctype1Val) {
        this.ctype1 = ctype1Val;
    }

    /**
     * Returns the CTYPE2.
     *
     * @return the ctype2
     */
    protected final String getCtype2() {
        return ctype2;
    }

    /**
     * Sets the ctype2.
     *
     * @param ctype2Val the ctype2 to set
     */
    protected final void setCtype2(final String ctype2Val) {
        this.ctype2 = ctype2Val;
    }

    /**
     * Returns cdelt1.
     *
     * @return the cdelt1
     */
    protected final double getCdelt1() {
        return cdelt1;
    }

    /**
     * Sets the cdelt1.
     *
     * @param cdelt1Val the cdelt1
     */
    protected final void setCdelt1(final double cdelt1Val) {
        this.cdelt1 = cdelt1Val;
    }

    /**
     * Returns the Cdelt2.
     *
     * @return the cdelt2
     */
    protected final double getCdelt2() {
        return cdelt2;
    }

    /**
     * Sets the cdelt2.
     *
     * @param cdelt2Val the cdelt2 to set
     */
    protected final void setCdelt2(final double cdelt2Val) {
        this.cdelt2 = cdelt2Val;
    }

    /**
     * Returns crota.
     *
     * @return the crota
     */
    protected final double getCrota() {
        return crota;
    }

    /**
     * Sets the crota.
     *
     * @param crotaVal the crota to set
     */
    protected final void setCrota(final double crotaVal) {
        this.crota = crotaVal;
    }

    /**
     * Returns the CD matric.
     *
     * @return the cd
     */
    protected final double[] getCd() {
        return cd;
    }

    /**
     * Sets the CD matrix.
     *
     * @param cdVal the cd to set
     */
    protected final void setCd(final double[] cdVal) {
        this.cd = cdVal;
    }

    /**
     * Returns the WCS object.
     *
     * @return the wcs
     */
    protected final WCSTransform getWcs() {
        return wcs;
    }

    /**
     * Sets the WCS object.
     *
     * @param wcsVal the wcs to set
     */
    protected final void setWcs(final WCSTransform wcsVal) {
        this.wcs = wcsVal;
    }
}
