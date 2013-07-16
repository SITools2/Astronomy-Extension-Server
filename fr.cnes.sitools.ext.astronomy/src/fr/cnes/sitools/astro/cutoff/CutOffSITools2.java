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
import fr.cnes.sitools.extensions.common.Utility;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.TransposeDescriptor;
import jsky.coords.WCSKeywordProvider;
import jsky.coords.WCSTransform;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.util.Cursor;

/**
  Class for handling cutoffProcessing feature on FITS file.
 * @author malapert
 */
public class CutOffSITools2 implements CutOffInterface {
    
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
    private final int[] lengths;
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
    private final int[] corners;
    /**
     * NAXIS2 index in the corners and length arrays.
     */
    public int HEIGHT = 0;
    /**
     * NAXIS1 index in the corners and length arrays.
     */    
    public int WIDTH = 1; 
    /**
     * DEEP for Cube FITS (start at 0).
     */    
    public static final int DEEP = 0;    
    /**
     * DeepLevel in the FITS cube.
     */
    private int deepLevel = 0;
    
    private SupportedFileFormat supportedFormatOutput;
    
    /**
     * Constructs a cut off based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param radius radius in deg
     * @param hduImageNumber number of the image HDU to read (start=1)
     * @param deepLevel level of the FITS cube (start=0)
     * @throws CutOffException When a problem happens
     */
    public CutOffSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double radius, final int hduImageNumber, final int deepLevel) throws CutOffException  {
        try {
            this.fits = new FITSImage(fitsObj, hduImageNumber, deepLevel, FITSImage.SCALE_LINEAR);
            this.ra = rightAscension;
            this.dec = declination;
            this.sr = radius;
            int nbAxes = this.fits.getImageHDU().getAxes().length;
            this.corners = new int[nbAxes];
            this.lengths = new int[nbAxes];
            this.deepLevel = deepLevel;
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
     * Constructs a cut off based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param radius radius in deg
     * @param hduImageNumber number of the image HDU to read
     * @throws CutOffException When a problem happens
     */
    public CutOffSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double radius, final int hduImageNumber) throws CutOffException  {
        this(fitsObj, rightAscension, declination, radius, hduImageNumber, 0);
    }
    
    /**
     * Constructs a cut off based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param radius radius in deg
     * @throws CutOffException When a problem happens
     */
    public CutOffSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double radius) throws CutOffException  {
        this(fitsObj, rightAscension, declination, radius, 1, 0);
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
        this.xyCoord = wcs.wcs2pix(this.ra, this.dec);
        if (getIsDataCube()) {
            WIDTH++;
            HEIGHT++;
            this.lengths[WIDTH] = (int) Math.round(computeOutputWidth());
            this.lengths[HEIGHT] = (int) Math.round(computeOutputHeight());
            this.lengths[0] = 1;
            this.corners[WIDTH] = (int) Math.round(computeCornerWidth(xyCoord));
            this.corners[HEIGHT] = (int) Math.round(computeCornerHeight(xyCoord));
            this.corners[DEEP] = deepLevel - 1;
        } else {
            this.lengths[WIDTH] = (int) Math.round(computeOutputWidth());
            this.lengths[HEIGHT] = (int) Math.round(computeOutputHeight());            
            this.corners[WIDTH] = (int) Math.round(computeCornerWidth(xyCoord));
            this.corners[HEIGHT] = (int) Math.round(computeCornerHeight(xyCoord));            
        }

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
            final ImageHDU originHdu = fits.getImageHDU();
            Object obj = this.fits.getImageHDU().getTiler().getTile(this.corners, this.lengths);
            obj = Utility.array1DTo2D(obj, originHdu.getBitPix(), this.lengths[WIDTH], this.lengths[HEIGHT]);
            final ImageData imgData = new ImageData(obj);
            final ImageHDU imageHDU = (ImageHDU) Fits.makeHDU(imgData);
            imageHDU.addValue("NAXIS1", this.lengths[WIDTH], null);
            imageHDU.addValue("NAXIS2", this.lengths[HEIGHT], null);
            imageHDU.addValue("CRPIX1", originHdu.getHeader().getDoubleValue("CRPIX1") - computeShiftCrpixWidth() , null);
            imageHDU.addValue("CRPIX2", originHdu.getHeader().getDoubleValue("CRPIX2") - computeShiftCrpixHeight(), null);
            imageHDU.addValue("CREATOR", "SITools2", "http://sitools2.sourceforge.net");
            propagateKeywords(originHdu.getHeader(), imageHDU);
            imageHDU.getHeader().insertHistory("CUT FITS DATE : " + GregorianCalendar.getInstance().getTime().toString());
            imageHDU.getHeader().insertHistory(String.format("CUT FITS query (ra,dec,sr) = (%s,%s,%s)", this.ra, this.dec, this.sr));
            return imageHDU;
        } catch (IOException ex) {
            throw new CutOffException(ex);
        } catch (HeaderCardException ex) {
            throw new CutOffException(ex);
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        }
    }
    /**
     * Propagates keywords.
     * @param headerSrc header that contains the keywords to propagate
     * @param hdu header destination
     * @throws HeaderCardException When an error happens
     */
    private void propagateKeywords(final Header headerSrc, final ImageHDU hdu) throws HeaderCardException {
        final List keysToNotOverride = Arrays.asList("SIMPLE", "NAXIS", "NAXIS1", "NAXIS2", "NAXIS3", "EXTEND", "END", "EXTEND", "BITPIX", "CRPIX1", "CRPIX2");
        final Cursor iter = headerSrc.iterator();
        while (iter.hasNext()) {
            final HeaderCard card = (HeaderCard) iter.next();
            final String key = card.getKey();
            if (!keysToNotOverride.contains(key)) {
                final String value = (card.isKeyValuePair()) ? card.getValue() : null;
                if (value == null || value.isEmpty()) {
                    continue;
                }
                if (card.isStringValue()) {
                    hdu.addValue(key, value, card.getComment());
                } else {
                    try {
                        final int numericalValue = Integer.parseInt(value);
                        hdu.addValue(key, numericalValue, card.getComment());
                    } catch (NumberFormatException ex) {
                        try {
                            final double numericalValue = Double.parseDouble(value);
                            hdu.addValue(key, numericalValue, card.getComment());
                        } catch (NumberFormatException ex1) {
                            hdu.addValue(key, value, card.getComment());
                        }
                    }
                }
            }
        }
    }

    @Override
    public final SupportedFileFormat getFormatOutput() {
        return this.supportedFormatOutput;
    }
    /**
     * Scales the image.
     * @param source image
     * @param scaleFactor scale factor
     * @return the image scaled
     */
    private Object scaleParameter(final Object source, final float scaleFactor) {
        final ParameterBlock pbScale = new ParameterBlock();
        final Interpolation interp = Interpolation.getInstance(Interpolation.INTERP_BICUBIC);
        pbScale.addSource(source);
        pbScale.add(scaleFactor);
        pbScale.add(scaleFactor);
        pbScale.add(0f);
        pbScale.add(0f);
        pbScale.add(interp);
        return JAI.create("scale", pbScale);
    }

    @Override
    public void createCutoutPreview(OutputStream os) throws CutOffException {
        createCutoutPreview(os, Float.NaN);
    }

    public final void createCutoutPreview(final OutputStream os, final float scaleFactor) throws CutOffException {
        this.setFormatOutput(SupportedFileFormat.JPEG);
        try {
            Raster raster;
            if (getIsDataCube()) {
                final double[][] data = ((double[][][]) fits.getImageHDU().getData().getData())[this.deepLevel];
                final ImageData imgData = new ImageData(data);
                final BasicHDU bHdu = Fits.makeHDU(data);
                final Header hdr = bHdu.getHeader();
                hdr.setNaxes(2);
                hdr.setNaxis(1, lengths[lengths.length - 1]);
                hdr.setNaxis(2, lengths[lengths.length - 2]);
                final ImageHDU imageHDU = new ImageHDU(hdr, imgData);
                final BufferedImage[] bufferedImage = FITSBufferedImage.createScaledImages(imageHDU);
                final FITSImage fitsImage = new FITSBufferedImage(bufferedImage, FITSBufferedImage.SCALE_LINEAR);
                raster = fitsImage.getData();
            } else {
                raster = fits.getData();
            }
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
            if (scaleFactor > 0) {
                pI = (PlanarImage) scaleParameter(pI, scaleFactor);
            }
            
            try {
                ImageIO.write(pI, "jpeg", os);
            } catch (IOException ex) {
                throw new CutOffException(ex);
            }
      } catch (IOException ex) {
            Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOffException(ex);
        } catch (FitsException ex) {
            Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOffException(ex);
        } catch (FITSImage.DataTypeNotSupportedException ex) {
            Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOffException(ex);
        } catch (FITSImage.NoImageDataFoundException ex) {
            Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOffException(ex);
        }
    }
    /**
    * Performs the 'flip' or 'flipy' operation:
    * Flip an image across an imaginary vertical line that runs through the center of the image.
    * @param inImg the image to transform
    * @return the transformed image
    */
    protected static PlanarImage flip(final PlanarImage inImg) {
        final ParameterBlock params = new ParameterBlock();
        params.addSource(inImg);
        params.add(TransposeDescriptor.FLIP_VERTICAL); // flip over Y
        return JAI.create("transpose", params);
    }

    @Override
    public final boolean isGraphicAvailable() {
        return true;
    }

    @Override
    public final boolean isFitsAvailable() {
        return true;
    }

    @Override
    public final boolean getIsDataCube() {
        try {
            return (this.fits.getImageHDU().getAxes().length == 3) ? true : false;
        } catch (FitsException ex) {
            throw new RuntimeException("Error when loading the number of axis in the FITS header");
        }
    }

    @Override
    public final void createCutoutFits(final OutputStream os) throws CutOffException {
        this.setFormatOutput(SupportedFileFormat.FITS);
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
                Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FitsException ex) {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException ex1) {
                    Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            throw new CutOffException(ex);
        }
    }
    /**
     * Sets the supportedOutputFormat.
     * @param supportedFileFormat supported the outputformat
     */
    private void setFormatOutput(final SupportedFileFormat supportedFileFormat) {
        this.supportedFormatOutput = supportedFileFormat;
    }

//  this.setFormatOutput(SupportedFileFormat.GIF_ANIMATED);
//  int naxis3 = axes[0];
//  AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
//  gifEncoder.setDelay(1000);
//  gifEncoder.setRepeat(0);
//  gifEncoder.start(os);
//  for (int j = 0; j < naxis3; j++) {
//      Object imageData = extractImageFromCube(basicHdu, j);
//      imageData = cutoutAndFlipImage(imageData, hdr.getIntValue("NAXIS1"), hdr.getIntValue("NAXIS2"));
//      FITSImage fitsImage = createFitsBufferedImage(imageData);
//      gifEncoder.addFrame(fitsImage);
//  }
//  if (!gifEncoder.finish()) {
//      throw new CutOffException("Problem when GIF creation");
//  }

}
