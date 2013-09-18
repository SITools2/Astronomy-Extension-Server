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
package fr.cnes.sitools.astro.cutout;


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
  Class for handling cutOutProcessing feature on FITS file.
 * @author malapert
 */
public class CutOutSITools2 implements CutOutInterface {
    
    static {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }
    /**
     * Fits image.
     */
    private FITSImage fits;
    /**
     * Right ascension coordinate (in deg) for the center of the cutOut.
     */
    private double ra;
    /**
     * Declination coordinate (in deg) for the center of the cutOut.
     */
    private double dec;
    /**
     * Width (in deg) of the cutOut.
     */
    private double widthDeg;
    /**
     * Height (in deg) of the cutOut.
     */
    private double heightDeg;    
    /**
     * Scale in deg/pixel along NAXIS1.
     */
    private transient double scaleDegPerPixelWidth;
    /**
     * Scale in deg/pixel along NAXIS2.
     */    
    private transient double scaleDegPerPixelHeight;
    /**
     * Transformation (ra,dec) in pixels reference.
     */
    private transient Point2D.Double xyCoord;
    /**
     * CRPIX1 shift when the corner is out of the image.
     */
    private transient int shiftCrpix1 = 0;
    /**
     * CRPIX1 shift when the corner is out of the image.
     */    
    private transient int shiftCrpix2 = 0;
    /**
     * lengths of the image following the cutOut.
     */
    private final transient int[] lengths;
    /**
     * corners of the origin point of cutOut.
     * 
     * ---------
     * |        |
     * |      -----
     * |      |   |
     * |______|___|
     *    (corners)  
     */    
    private final transient int[] corners;
    /**
     * NAXIS2 index in the corners and length arrays.
     */
    private int height = 0;
    /**
     * NAXIS1 index in the corners and length arrays.
     */    
    private int width = 1; 
    /**
     * DEEP for Cube FITS (start at 0).
     */    
    public static final int DEEP = 0;
    /**
     * DeepLevel in the FITS cube.
     */
    private int deepLevel;
    /**
     * TODO : should provide a choice between PNG and JPEG.
     */
    private SupportedFileFormat supportedFormatOutput;

    /**
     * Constructs a cut out based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param widthDeg width in deg
     * @param heightDeg height in deg
     * @param hduImageNumber number of the image HDU to read (start=1)
     * @param deepLevel level of the FITS cube (start=0)
     * @throws CutOutException When a problem happens
     */
    public CutOutSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double widthDeg, final double heightDeg, final int hduImageNumber, final int deepLevel) throws CutOutException  {
        try {
            setFits(new FITSImage(fitsObj, hduImageNumber, deepLevel, FITSImage.SCALE_LINEAR));
            setRa(rightAscension);
            setDec(declination);
            setWidthDeg(widthDeg);
            setHeightDeg(heightDeg);
            final int nbAxes = getFits().getImageHDU().getAxes().length;
            this.corners = new int[nbAxes];
            this.lengths = new int[nbAxes];
            setDeepLevel(deepLevel);
            init();
        } catch (FitsException ex) {
            throw new CutOutException(ex);
        } catch (FITSImage.DataTypeNotSupportedException ex) {
            throw new CutOutException(ex);
        } catch (FITSImage.NoImageDataFoundException ex) {
            throw new CutOutException(ex);
        } catch (IOException ex) {
            throw new CutOutException(ex);
        } catch (RuntimeException err) {
            throw new CutOutException(err);
        }
    }

    /**
     * Constructs a cut out based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param radius radius in deg
     * @param hduImageNumber number of the image HDU to read (start=1)
     * @param deepLevel level of the FITS cube (start=0)
     * @throws CutOutException When a problem happens
     */
    public CutOutSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double radius, final int hduImageNumber, final int deepLevel) throws CutOutException  {
        this(fitsObj, rightAscension, declination, radius * 2d, radius * 2d, hduImageNumber, deepLevel);
    }

    /**
     * Constructs a cut out based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param widthDeg width in deg
     * @param heightDeg height in deg
     * @param hduImageNumber number of the image HDU to read
     * @throws CutOutException When a problem happens
     */
    public CutOutSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double widthDeg, final double heightDeg, final int hduImageNumber) throws CutOutException {
        this(fitsObj, rightAscension, declination, widthDeg, heightDeg, hduImageNumber, 0);
    }
    /**
     * Constructs a cut out based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param radius radius in deg
     * @param hduImageNumber number of the image HDU to read
     * @throws CutOutException When a problem happens
     */
    public CutOutSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double radius, final int hduImageNumber) throws CutOutException  {
        this(fitsObj, rightAscension, declination, radius, hduImageNumber, 0);
    }
    /**
     * Constructs a cut out based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param radius radius in deg
     * @throws CutOutException When a problem happens
     */
    public CutOutSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double radius) throws CutOutException  {
        this(fitsObj, rightAscension, declination, radius, 1, 0);
    }
    /**
     * Constructs a cut out based on the image, the center of the zone to cut and its radius.
     * @param fitsObj Object to cut
     * @param rightAscension right ascension coordinates in deg
     * @param declination declination in deg
     * @param widthDeg width in deg
     * @param heightDeg height in deg
     * @throws CutOutException When a problem happens
     */
    public CutOutSITools2(final Fits fitsObj, final double rightAscension, final double declination, final double widthDeg, final double heightDeg) throws CutOutException  {
        this(fitsObj, rightAscension, declination, widthDeg, heightDeg, 1, 0);
    }
    /**
     * Computes the ranges of pixels to cut and checks if cutOut is inside the image.
     *
     * Raises an IllegalArgumentException if the cutOut is outside the image
     */
    private void init() {
        final WCSTransform wcs = getWcs(this.getFits().getImageHDU());
        this.scaleDegPerPixelWidth = computeScaleDegPerPixelWidth(wcs);
        this.scaleDegPerPixelHeight = computeScaleDegPerPixelHeight(wcs);
        this.xyCoord = wcs.wcs2pix(this.getRa(), this.getDec());
        if (isDataCube()) {
            setWidth(getWidth() + 1);
            setHeight(getHeight() + 1);
            this.lengths[getWidth()] = (int) Math.round(computeOutputWidth());
            this.lengths[getHeight()] = (int) Math.round(computeOutputHeight());
            this.lengths[0] = 1;
            this.corners[getWidth()] = (int) Math.round(computeCornerWidth(xyCoord));
            this.corners[getHeight()] = (int) Math.round(computeCornerHeight(xyCoord));
            this.corners[DEEP] = getDeepLevel() - 1;
        } else {
            this.lengths[getWidth()] = (int) Math.round(computeOutputWidth());
            this.lengths[getHeight()] = (int) Math.round(computeOutputHeight());
            this.corners[getWidth()] = (int) Math.round(computeCornerWidth(xyCoord));
            this.corners[getHeight()] = (int) Math.round(computeCornerHeight(xyCoord));
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
            // Checks if the cutOut is completely outside the image
            if (this.lengths[getWidth()] <= Math.abs(this.corners[getWidth()])) {
                throw new IllegalArgumentException("Cut out of the image");
            } else {
                this.shiftCrpix1 = this.corners[getWidth()];
                this.lengths[getWidth()] = this.lengths[getWidth()] - Math.abs(this.corners[getWidth()]);
                if (this.lengths[getWidth()] > wcs.getWidth()) {
                    this.lengths[getWidth()] = (int) wcs.getWidth();
                }
                this.corners[getWidth()] = 0;
            }
        } else if (isCutWidthOutImage((int) wcs.getWidth())) {
           if (this.corners[getWidth()] >= wcs.getWidth()) {
                throw new IllegalArgumentException("Cut out of the image");
           } else {
                this.lengths[getWidth()] = this.lengths[getWidth()] - (this.corners[getWidth()] + this.lengths[getWidth()] - (int) wcs.getWidth());
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
            if (this.lengths[getHeight()] <= Math.abs(this.corners[getHeight()])) {
                throw new IllegalArgumentException("Cut out of the image");
            } else {
                this.shiftCrpix2 = this.corners[getHeight()];
                this.lengths[getHeight()] = this.lengths[getHeight()] - Math.abs(this.corners[getHeight()]);
                if (this.lengths[getHeight()] > wcs.getHeight()) {
                    this.lengths[getHeight()] = (int) wcs.getHeight();
                }
                this.corners[getHeight()] = 0;
            }
        } else if (isCutHeightOutImage((int) wcs.getHeight())) {
           if (this.corners[getHeight()] >= wcs.getHeight()) {
                throw new IllegalArgumentException("Cut out of the image");
           } else {
                this.lengths[getHeight()] = this.lengths[getHeight()] - (this.corners[getHeight()] + this.lengths[getHeight()] - (int) wcs.getHeight());
           }
        }
    }
    /**
     * Returns True when the corner of the cutOut is out of the image along the width.
     * @return True when the corner of the cutOut is out of the image otherwise False
     */
    private boolean isCornerCutWidthOutImage() {
        return (this.corners[getWidth()] < 0);
    }
    /**
     * Returns True when the corner of the cutOut is out of the image along the height.
     * @return True when the corner of the cutOut is out of the image otherwise False
     */
    private boolean isCornerCutHeightOutImage() {
        return (this.corners[getHeight()] < 0);
    }
    /**
     * Returns True when the length of the cutOut is out of the image along the width.
     * @param imageWidth length of the image along the width
     * @return True when the cutOut is out of the image otherwise False
     */
    private boolean isCutWidthOutImage(final int imageWidth) {
        return (this.corners[getWidth()] + this.lengths[getWidth()] >= imageWidth);
    }
    /**
     * Returns True when the length of the cutOut is out of the image along the height.
     * @param imageHeight length of the image along the height
     * @return True when the cutOut is out of the image otherwise False
     */
    private boolean isCutHeightOutImage(final int imageHeight) {
        return (this.corners[getHeight()] + this.lengths[getHeight()] >= imageHeight);
    }
    /**
     * Computes the shift to give to CRPIX1 when the corner is out of the cutOut.
     * @return the shift to apply for CRPIX1
     */
    private double computeShiftCrpixWidth() {
        return xyCoord.getX() - (computeOutputWidth() - 1) / 2 - this.shiftCrpix1;
    }
    /**
     * Computes the shift to give to CRPIX2 when the corner is out of the cutOut.
     * @return the shift to apply for CRPIX2
     */
    private double computeShiftCrpixHeight() {
        return xyCoord.getY() - (computeOutputHeight() - 1) / 2 - this.shiftCrpix2;
    }
    /**
     * Computes the coordinates of the corner along the width axis.
     * @param xyCoordVal central position of the cutOut
     * @return the coordinates of the corner along the width axis in pixels
     */
    private double computeCornerWidth(final Point2D.Double xyCoordVal) {
        return xyCoordVal.getX() - (computeOutputWidth() - 1) / 2.0d;
    }
    /**
     * Computes the coordinates of the corner along the height axis.
     * @param xyCoordVal central position of the cutOut
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
     * Computes the number of pixels along the width axis as the result of the cutOut.
     * @return the number of pixels along the width axis as the result of the cutOut
     */
    private double computeOutputWidth() {
        return this.getWidthDeg() / scaleDegPerPixelWidth;
    }
    /**
     * Computes the number of pixels along the height axis as the result of the cutOut.
     * @return the number of pixels along the height axis as the result of the cutOut
     */
    private double computeOutputHeight() {
        return this.getHeightDeg() / scaleDegPerPixelHeight;
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
     * cutOut processing for FITS output.
     * @return Returns an ImageHDU of the cut
     * @throws CutOutException When a problem happens
     */
    private ImageHDU cutOutFitsProcessing() throws CutOutException {
        try {
            final ImageHDU originHdu = getFits().getImageHDU();
            Object obj = this.getFits().getImageHDU().getTiler().getTile(this.corners, this.lengths);
            obj = Utility.array1DTo2D(obj, originHdu.getBitPix(), this.lengths[getWidth()], this.lengths[getHeight()]);
            final ImageData imgData = new ImageData(obj);
            final ImageHDU imageHDU = (ImageHDU) Fits.makeHDU(imgData);
            imageHDU.addValue("NAXIS1", this.lengths[getWidth()], null);
            imageHDU.addValue("NAXIS2", this.lengths[getHeight()], null);
            imageHDU.addValue("CRPIX1", originHdu.getHeader().getDoubleValue("CRPIX1") - computeShiftCrpixWidth() , null);
            imageHDU.addValue("CRPIX2", originHdu.getHeader().getDoubleValue("CRPIX2") - computeShiftCrpixHeight(), null);
            imageHDU.addValue("CREATOR", "SITools2", "http://sitools2.sourceforge.net");
            propagateKeywords(originHdu.getHeader(), imageHDU);
            imageHDU.getHeader().insertHistory("CUT FITS DATE : " + GregorianCalendar.getInstance().getTime().toString());
            imageHDU.getHeader().insertHistory(String.format("CUT FITS query (ra,dec,width, height) = (%s,%s,%s,%s)", this.getRa(), this.getDec(), this.getWidthDeg(), this.getHeightDeg()));
            return imageHDU;
        } catch (IOException ex) {
            throw new CutOutException(ex);
        } catch (HeaderCardException ex) {
            throw new CutOutException(ex);
        } catch (FitsException ex) {
            throw new CutOutException(ex);
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
    public final void createCutoutPreview(final OutputStream outputStream) throws CutOutException {
        createCutoutPreview(outputStream, Float.NaN);
    }

    /**
     * Creates cutout preview.
     * @param outputStream output stream
     * @param scaleFactor factor to scale images
     * @throws CutOutException when an error happend during the processing
     */
    public final void createCutoutPreview(final OutputStream outputStream, final float scaleFactor) throws CutOutException {
        this.setFormatOutput(SupportedFileFormat.JPEG);
        try {
            Raster raster;
            if (isDataCube()) {
                final double[][] data = ((double[][][]) getFits().getImageHDU().getData().getData())[this.getDeepLevel()];
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
                raster = getFits().getData();
            }
            final BufferedImage newimage = new BufferedImage(raster.getWidth(), raster.getHeight(), BufferedImage.TYPE_INT_RGB);
            newimage.setData(raster);
            final PlanarImage planarImageWrap = PlanarImage.wrapRenderedImage(newimage);
            final ParameterBlock parameterBlock = new ParameterBlock();
            parameterBlock.addSource(planarImageWrap);
            parameterBlock.add((float) this.corners[getWidth()]);
            parameterBlock.add((float) this.corners[getHeight()]);
            parameterBlock.add((float) this.lengths[getWidth()]);
            parameterBlock.add((float) this.lengths[getHeight()]);
            PlanarImage planarImage = JAI.create("crop", parameterBlock);
            planarImage = flip(planarImage);
            if (scaleFactor > 0) {
                planarImage = (PlanarImage) scaleParameter(planarImage, scaleFactor);
            }
            try {
                ImageIO.write(planarImage, "jpeg", outputStream);
            } catch (IOException ex) {
                throw new CutOutException(ex);
            }
      } catch (IOException ex) {
            Logger.getLogger(CutOutSITools2.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOutException(ex);
        } catch (FitsException ex) {
            Logger.getLogger(CutOutSITools2.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOutException(ex);
        } catch (FITSImage.DataTypeNotSupportedException ex) {
            Logger.getLogger(CutOutSITools2.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOutException(ex);
        } catch (FITSImage.NoImageDataFoundException ex) {
            Logger.getLogger(CutOutSITools2.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOutException(ex);
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
    public final boolean isDataCube() {
        try {
            return (this.getFits().getImageHDU().getAxes().length == 3);
        } catch (FitsException ex) {
            throw new RuntimeException("Error when loading the number of axis in the FITS header");
        }
    }

    @Override
    public final void createCutoutFits(final OutputStream outputStream) throws CutOutException {
        this.setFormatOutput(SupportedFileFormat.FITS);
        DataOutputStream dataOutputStream = null;
        try {
            final ImageHDU imageHdu = cutOutFitsProcessing();
            final Fits outputFits = new Fits();
            outputFits.addHDU(imageHdu);
            dataOutputStream = new DataOutputStream(outputStream);
            outputFits.write(dataOutputStream);
            try {
                dataOutputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(CutOutSITools2.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FitsException ex) {
            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException ex1) {
                    Logger.getLogger(CutOutSITools2.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            throw new CutOutException(ex);
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
//      throw new CutOutException("Problem when GIF creation");
//  }

    /**
     * Returns the FITS.
     * @return the fits
     */
    public final FITSImage getFits() {
        return fits;
    }

    /**
     * Sets the FITS.
     * @param fitsVal the fits to set
     */
    private void setFits(final FITSImage fitsVal) {
        this.fits = fitsVal;
    }

    /**
     * Returns the right ascension.
     * @return the ra
     */
    public final double getRa() {
        return ra;
    }

    /**
     * Sets the right ascension.
     * @param raVal the ra to set
     */
    private void setRa(final double raVal) {
        this.ra = raVal;
    }

    /**
     * Returns the declination.
     * @return the dec
     */
    public final double getDec() {
        return dec;
    }

    /**
     * Sets the declination.
     * @param decVal the dec to set
     */
    private void setDec(final double decVal) {
        this.dec = decVal;
    }

    /**
     * Returns the width in degree.
     * @return the widthDeg
     */
    public final double getWidthDeg() {
        return widthDeg;
    }

    /**
     * Sets the width in degree.
     * @param widthDegVal the widthDeg to set
     */
    private void setWidthDeg(final double widthDegVal) {
        this.widthDeg = widthDegVal;
    }

    /**
     * Returns the height in degree.
     * @return the heightDeg
     */
    public final double getHeightDeg() {
        return heightDeg;
    }

    /**
     * Sets the height in degree.
     * @param heightDegVal the heightDeg to set
     */
    private void setHeightDeg(final double heightDegVal) {
        this.heightDeg = heightDegVal;
    }

    /**
     * Returns the deep level.
     * @return the deepLevel
     */
    public final int getDeepLevel() {
        return deepLevel;
    }

    /**
     * Sets the deep level.
     * @param deepLevelVal the deepLevel to set
     */
    private void setDeepLevel(final int deepLevelVal) {
        this.deepLevel = deepLevelVal;
    }

    /**
     * Returns the height.
     * @return the height
     */
    private int getHeight() {
        return height;
    }

    /**
     * Sets the height index number.
     * @param heightVal the height to set
     */
    private void setHeight(final int heightVal) {
        this.height = heightVal;
    }

    /**
     * Returns the width index number.
     * @return the width
     */
    private int getWidth() {
        return width;
    }

    /**
     * Sets the width index number.
     * @param widthVal the width to set
     */
    private void setWidth(final int widthVal) {
        this.width = widthVal;
    }
}
