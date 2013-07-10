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
package fr.cnes.sitools.astro.cutoff;

import edu.jhu.pha.sdss.fits.FITSImage;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.TransposeDescriptor;
import jsky.coords.WCSKeywordProvider;
import jsky.coords.WCSTransform;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;

/**
  Class for handling cutoffProcessing feature on FITS file.
 * @author malapert
 */
public class CutOffSITools2Bis implements CutOffInterface {
    
    static {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }
    /**
     * Fits image.
     */
    private final FITSImage fits;
    /**
     * Right ascension coordinate (in deg) for the center of the cutoff.
     */
    private final double ra;
    /**
     * Declination coordinate (in deg) for the center of the cutoff.
     */
    private final double dec;
    /**
     * Radius (in deg) of the cutoff.
     */
    private final double sr;
    /**
     * Scale in deg/pixel along NAXIS1.
     */
    private double scaleDegPerPixelWidth;
    /**
     * Scale in deg/pixel along NAXIS2.
     */    
    private double scaleDegPerPixelHeight;
    /**
     * Transformation (ra,dec) in pixels reference.
     */
    private Point2D.Double xyCoord;
    /**
     * CRPIX1 shift when the corner is out of the image.
     */
    private int shiftCrpix1 = 0;
    /**
     * CRPIX1 shift when the corner is out of the image.
     */    
    private int shiftCrpix2 = 0;
    /**
     * lengths of the image following the cutoff.
     */
    private final int[] lengths = new int[2];
    /**
     * corners of the origin point of cutoff.
     * 
     * ---------
     * |        |
     * |      -----
     * |      |   |
     * |______|___|
     *    (corners)  
     */    
    private final int[] corners = new int[2];
    /**
     * NAXIS2 index in the corners and length arrays.
     */
    public static final int HEIGHT = 0;
    /**
     * NAXIS1 index in the corners and length arrays.
     */    
    public static final int WIDTH = 1;    
    
    /**
     * Constructs a cut off based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param radius radius in deg
     * @throws CutOffException When a problem happens
     */
    public CutOffSITools2Bis(final Fits fitsObj, final double rightAscension, final double declination, final double radius) throws CutOffException  {
        try {
            this.fits = new FITSImage(fitsObj, FITSImage.SCALE_SQUARE);
            this.ra = rightAscension;
            this.dec = declination;
            this.sr = radius;
            init();
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (FITSImage.DataTypeNotSupportedException ex) {
            throw new CutOffException(ex);
        } catch (FITSImage.NoImageDataFoundException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }
    
    /**
     * Computes the ranges of pixels to cut and checks if cutoff is inside the image.
     * 
     * Raises an IllegalArgumentException if the cutOff is outside the image
     */
    private void init() {
        final WCSTransform wcs = getWcs(this.fits.getImageHDU());
        this.scaleDegPerPixelWidth = computeScaleDegPerPixelWidth(wcs);
        this.scaleDegPerPixelHeight = computeScaleDegPerPixelHeight(wcs);
        this.lengths[WIDTH] = (int) Math.round(computeOutputWidth());
        this.lengths[HEIGHT] = (int) Math.round(computeOutputHeight());
        this.xyCoord = wcs.wcs2pix(this.ra, this.dec);
        this.corners[WIDTH] = (int) Math.round(computeCornerWidth(xyCoord));
        this.corners[HEIGHT] = (int) Math.round(computeCornerHeight(xyCoord));
        /**
         * Checks this case
         *        ------------
         *        |          |
         *      -----      -----
         *      |___|      |___|
         *        |__________|
         */
        if (isCornerCutWidthOutImage()) {
            // Checks if the cutoff is completely outside the image
            if (this.lengths[WIDTH] <= Math.abs(this.corners[WIDTH])) {
                throw new IllegalArgumentException("Cut out of the image");
            } else {
                this.shiftCrpix1 = this.corners[WIDTH];
                this.lengths[WIDTH] = this.lengths[WIDTH] - Math.abs(this.corners[WIDTH]);
                if (this.lengths[WIDTH] > wcs.getWidth()) {
                    this.lengths[WIDTH] = (int) wcs.getWidth();
                }
                this.corners[WIDTH] = 0;
            }
        } else if (isCutWidthOutImage((int) wcs.getWidth())) {
           if (this.corners[WIDTH] >= wcs.getWidth()) {
                throw new IllegalArgumentException("Cut out of the image");
           } else {
                this.lengths[WIDTH] = this.lengths[WIDTH] - (this.corners[WIDTH] + this.lengths[WIDTH] - (int) wcs.getWidth());
           }
        }
        /**
         * Checks this case
         *               ---
         *        -------| |--
         *        |      --- |
         *        |          |
         *        |          |
         *        |______---_|
         *               |_|
         */
        if (isCornerCutHeightOutImage()) {
            if (this.lengths[HEIGHT] <= Math.abs(this.corners[HEIGHT])) {
                throw new IllegalArgumentException("Cut out of the image");
            } else {
                this.shiftCrpix2 = this.corners[HEIGHT];
                this.lengths[HEIGHT] = this.lengths[HEIGHT] - Math.abs(this.corners[HEIGHT]);
                if (this.lengths[HEIGHT] > wcs.getHeight()) {
                    this.lengths[HEIGHT] = (int) wcs.getHeight();
                }                
                this.corners[HEIGHT] = 0;
            }
        } else if (isCutHeightOutImage((int) wcs.getHeight())) {
           if (this.corners[HEIGHT] >= wcs.getHeight()) {
                throw new IllegalArgumentException("Cut out of the image");
           } else {
                this.lengths[HEIGHT] = this.lengths[HEIGHT] - (this.corners[HEIGHT] + this.lengths[HEIGHT] - (int) wcs.getHeight());
           }
        }
    }
    /**
     * Returns True when the corner of the cutoff is out of the image along the width.
     * @return True when the corner of the cutoff is out of the image otherwise False
     */
    private boolean isCornerCutWidthOutImage() {
        return (this.corners[WIDTH] < 0);
    }
    /**
     * Returns True when the corner of the cutoff is out of the image along the height.
     * @return True when the corner of the cutoff is out of the image otherwise False
     */
    private boolean isCornerCutHeightOutImage() {
        return (this.corners[HEIGHT] < 0);
    }
    /**
     * Returns True when the length of the cutoff is out of the image along the width.
     * @param imageWidth length of the image along the width
     * @return True when the cutoff is out of the image otherwise False
     */
    private boolean isCutWidthOutImage(final int imageWidth) {
        return (this.corners[WIDTH] + this.lengths[WIDTH] >= imageWidth);
    }
    /**
     * Returns True when the length of the cutoff is out of the image along the height.
     * @param imageHeight length of the image along the height
     * @return True when the cutoff is out of the image otherwise False
     */
    private boolean isCutHeightOutImage(final int imageHeight) {
        return (this.corners[HEIGHT] + this.lengths[HEIGHT] >= imageHeight);
    }
    /**
     * Computes the shift to give to CRPIX1 when the corner is out of the cutoff.
     * @return the shift to apply for CRPIX1
     */
    private double computeShiftCrpixWidth() {
        return xyCoord.getX() - (computeOutputWidth() - 1) / 2 - this.shiftCrpix1;
    }
    /**
     * Computes the shift to give to CRPIX2 when the corner is out of the cutoff.
     * @return the shift to apply for CRPIX2
     */
    private double computeShiftCrpixHeight() {
        return xyCoord.getY() - (computeOutputHeight() - 1) / 2 - this.shiftCrpix2;
    }
    /**
     * Computes the coordinates of the corner along the width axis.
     * @param xyCoordVal central position of the cutoff
     * @return the coordinates of the corner along the width axis in pixels
     */
    private double computeCornerWidth(final Point2D.Double xyCoordVal) {
        return xyCoordVal.getX() - (computeOutputWidth() - 1) / 2.0d;
    }
    /**
     * Computes the coordinates of the corner along the height axis.
     * @param xyCoordVal central position of the cutoff
     * @return the coordinates of the corner along the width axis in pixels
     */
    private double computeCornerHeight(final Point2D.Double xyCoordVal) {
        return xyCoordVal.getY() - (computeOutputHeight() - 1) / 2.0d;
    }
    /**
     * Computes the scale along the width axis in deg per pixels.
     * @param wcs wcs
     * @return the scale along the width axis in deg per pixels
     */
    private double computeScaleDegPerPixelWidth(final WCSTransform wcs) {
        return wcs.getWidthInDeg() / wcs.getWidth();
    }
    /**
     * Computes the scale along the height axis in deg per pixels.
     * @param wcs wcs
     * @return the scale along the height axis in deg per pixels
     */
    private double computeScaleDegPerPixelHeight(final WCSTransform wcs) {
        return wcs.getHeightInDeg() / wcs.getHeight();
    }
    /**
     * Computes the number of pixels along the width axis as the result of the cutoff.
     * @return the number of pixels along the width axis as the result of the cutoff
     */
    private double computeOutputWidth() {
        return this.sr / scaleDegPerPixelWidth;
    }
    /**
     * Computes the number of pixels along the height axis as the result of the cutoff.
     * @return the number of pixels along the height axis as the result of the cutoff
     */
    private double computeOutputHeight() {
        return this.sr / scaleDegPerPixelHeight;
    }
    /**
     * Returns the WCS after readinf an ImageHDU.
     * @param hdu FITS image
     * @return the WCS of the image
     */
    private WCSTransform getWcs(final ImageHDU hdu) {
        final WCSKeywordProvider wcsProvider = new FitsHeader(hdu.getHeader());
        return new WCSTransform(wcsProvider);
    }

    /**
     * CutOff processing for FITS output.
     * @return Returns an ImageHDU of the cut
     * @throws CutOffException When a problem happens
     */
    private ImageHDU cutOffFitsProcessing() throws CutOffException {
        try {
            final Object obj = this.fits.getImageHDU().getTiler().getTile(this.corners, this.lengths);
            final ImageData imgData = new ImageData(obj);
            final Header hdr = fits.getImageHDU().getHeader();
            hdr.addValue("NAXIS1", this.lengths[WIDTH], "");
            hdr.addValue("NAXIS2", this.lengths[HEIGHT], "");
            hdr.addValue("CRPIX1", hdr.getDoubleValue("CRPIX1") - computeShiftCrpixWidth() , "");
            hdr.addValue("CRPIX2", hdr.getDoubleValue("CRPIX2") - computeShiftCrpixHeight(), "");
            hdr.addValue("CREATOR", "SITools2", "http://sitools2.sourceforge.net");
            return new ImageHDU(hdr, imgData);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        } catch (HeaderCardException ex) {
            throw new CutOffException(ex);
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        }
    }

    @Override
    public SupportedFileFormat getFormatOutput() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createCutoutPreview(OutputStream os) throws CutOffException {
        final Raster raster = fits.getData();
        final BufferedImage newimage = new BufferedImage(raster.getWidth(), raster.getHeight(), BufferedImage.TYPE_INT_RGB);
        newimage.setData(raster);
        
        final PlanarImage p = PlanarImage.wrapRenderedImage(newimage);
        final ParameterBlock pb = new ParameterBlock();
        pb.addSource(p);
        pb.add((float) this.corners[WIDTH]);
        pb.add((float) this.corners[HEIGHT]);
        pb.add((float) this.lengths[WIDTH]);
        pb.add((float) this.lengths[HEIGHT]);
        PlanarImage pI = JAI.create("crop", pb);
        pI = flip(pI);       
        try {
            ImageIO.write(pI, "jpg", os);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }
    /**
    * Performs the 'flip' or 'flipy' operation:
    * Flip an image across an imaginary vertical line that runs through the center of the image.
    * @param inImg the image to transform
    * @return the transformed image
    */
    protected static PlanarImage flip(PlanarImage inImg) {
        ParameterBlock params = new ParameterBlock();
        params.addSource(inImg);
        params.add(TransposeDescriptor.FLIP_VERTICAL); // flip over Y
        PlanarImage outImg = JAI.create("transpose", params);
        return outImg;
    }

    @Override
    public boolean isGraphicAvailable() {
        return true;
    }

    @Override
    public boolean isFitsAvailable() {
        return true;
    }

    @Override
    public boolean getIsDataCube() {
        return false;
    }

    @Override
    public void createCutoutFits(OutputStream os) throws CutOffException {
        DataOutputStream dos = null;
        try {
            final ImageHDU imageHdu = cutOffFitsProcessing();
            final Fits outputFits = new Fits();
            outputFits.addHDU(imageHdu);
            dos = new DataOutputStream(os);
            outputFits.write(dos);
            try {
                dos.close();
            } catch (IOException ex) {
                Logger.getLogger(CutOffSITools2Bis.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FitsException ex) {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException ex1) {
                    Logger.getLogger(CutOffSITools2Bis.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            throw new CutOffException(ex);
        }
    }
}
