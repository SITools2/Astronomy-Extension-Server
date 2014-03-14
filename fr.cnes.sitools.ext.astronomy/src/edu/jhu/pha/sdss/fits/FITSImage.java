package edu.jhu.pha.sdss.fits;


import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import fr.cnes.sitools.astro.image.ZScale;

/**
 * <PRE>
 * Current Version
 * ===============
 * ID:            $Id: FITSImage.java,v 1.21 2004/07/23 21:54:11 carliles Exp $
 * Revision:      $Revision: 1.21 $
 * Date/time:     $Date: 2004/07/23 21:54:11 $
 * </PRE>
 *
 * The scaling algorithms offered are linear, log, square root, square,
 * histogram equalization, and inverse hyperbolic sine. Note that the histogram
 * equalization algorithm is just that; it works to fit the values to a uniform
 * distribution curve. The inverse hyperbolic sine scaling has linear behavior
 * below the sigma parameter and logarithmic behavior above the sigma parameter.
 */
public class FITSImage extends BufferedImage {

    public static final int SCALE_LINEAR = ScaleUtils.LINEAR;
    public static final int SCALE_LOG = ScaleUtils.LOG;
    public static final int SCALE_SQUARE_ROOT = ScaleUtils.SQUARE_ROOT;
    public static final int SCALE_SQUARE = ScaleUtils.SQUARE;
    public static final int SCALE_HISTOGRAM_EQUALIZATION = ScaleUtils.HIST_EQ;
    public static final int SCALE_ASINH = ScaleUtils.ASINH;

    public FITSImage(Fits fits)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(fits, SCALE_LINEAR);
    }

    public FITSImage(Fits fits, int scaleMethod)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(fits, createScaledImages(fits, 1, 0), scaleMethod);
    }

    public FITSImage(Fits fits, int hduImageNumber, int deepLevel, int scaleMethod)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(fits, createScaledImages(fits, hduImageNumber, deepLevel), scaleMethod);
    }
    
    public FITSImage(Fits fits, BufferedImage[] scaledImages, int scaleMethod)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        super(scaledImages[0].getColorModel(),
                scaledImages[0].getRaster().createCompatibleWritableRaster(),
                true, null);

        setFits(fits);
        setScaledImages(scaledImages);
        setScaleMethod(scaleMethod);

        ImageHDU imageHDU = (ImageHDU) scaledImages[0].getProperty("imageHDU");
        if (imageHDU == null || !(imageHDU instanceof ImageHDU)) {
            imageHDU = findImageHDU(fits, 1);
        }
        setImageHDU(imageHDU);

        _min = getHistogram().getMin();
        _max = getHistogram().getMax();
    }

    public FITSImage(File file)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(new Fits(file));
    }

    public FITSImage(File file, int scaleMethod)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(new Fits(file), scaleMethod);
    }

    public FITSImage(String filename)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(new Fits(filename));
    }

    public FITSImage(String filename, int scaleMethod)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(new Fits(filename), scaleMethod);
    }

    public FITSImage(URL url)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(new Fits(url));
    }

    public FITSImage(URL url, int scaleMethod)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        this(new Fits(url), scaleMethod);
    }

    /**
     * @return Printable names of the different scaling algorithms, indexed as
     * <CODE>SCALE_LINEAR</CODE>, <CODE>SCALE_LOG</CODE>, etc.
     */
    public static String[] getScaleNames() {
        return ScaleUtils.getScaleNames();
    }

    public Fits getFits() {
        return _fits;
    }

    public ImageHDU getImageHDU() {
        return _imageHDU;
    }

    public Histogram getHistogram() {
        return (Histogram) getScaledImages()[SCALE_LINEAR].getProperty("histogram");
    }

    /**
     * @return The actual data value at postion (x, y), with bZero and bScale
     * applied.
     */
    public double getOriginalValue(int x, int y) throws FitsException {
        double result = Double.NaN;
        double bZero = getImageHDU().getBZero();
        double bScale = getImageHDU().getBScale();
        Object data = getImageHDU().getData().getData();

        switch (getImageHDU().getBitPix()) {
            case 8:
                int dataVal = ((byte[][]) data)[y][x];
                if (dataVal < 0) {
                    dataVal += 256;
                }
                result = bZero + bScale * dataVal;
                break;
            case 16:
                result = bZero + bScale * ((double) ((short[][]) data)[y][x]);
                break;
            case 32:
                result = bZero + bScale * ((double) ((int[][]) data)[y][x]);
                break;
            case -32:
                result = bZero + bScale * ((double) ((float[][]) data)[y][x]);
                break;
            case -64:
                result = bZero + bScale * ((double[][]) data)[y][x];
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * @return One of <CODE>SCALE_LINEAR</CODE>, <CODE>SCALE_LOG</CODE>, etc.
     */
    public int getScaleMethod() {
        return _scaleMethod;
    }

    /**
     * <CODE>scaleMethod</CODE> must be one of
     * <CODE>SCALE_LINEAR</CODE>,
     * <CODE>SCALE_LOG</CODE>, etc. The image must be redrawn after this method
     * is called for the change to be visible.
     */
    public void setScaleMethod(int scaleMethod) {
        _scaleMethod = scaleMethod;
    }

    /**
     * Rescales the image with the given min and max range values.
     * <CODE>sigma</CODE> is used for the inverse hyperbolic sine scaling as the
     * value (in the range of the data values with bZero and bScale applied) at
     * which the behavior becomes more logarithmic and less linear. The image
     * must be redrawn after this method is called for the change to be visible.
     */
    public void rescale(double min, double max, double sigma)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        if (min != _min || max != _max || sigma != _sigma) {
            _min = min;
            _max = max;
            _sigma = sigma;
            setScaledImages(createScaledImages(getImageHDU(), getImageHDU().getData(), getHistogram(),
                    min, max, sigma));
            setScaleMethod(getScaleMethod());
        }
    }

    protected BufferedImage getDelegate() {
        return getScaledImages()[getScaleMethod()];
    }

    // BEGIN BufferedImage METHODS
  /*
     public void addTileObserver(TileObserver to)
     {
     //    throw new RuntimeException(new Exception().getStackTrace()[0].
     //                               getMethodName() + " not supported");
     getDelegate().addTileObserver(to);
     }

     public void coerceData(boolean isAlphaPremultiplied)
     {
     //    throw new RuntimeException(new Exception().getStackTrace()[0].
     //                               getMethodName() + " not supported");
     getDelegate().coerceData(isAlphaPremultiplied);
     }
     */
    public WritableRaster copyData(WritableRaster outRaster) {
        return getDelegate().copyData(outRaster);
    }

    public Graphics2D createGraphics() {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public void flush() {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public WritableRaster getAlphaRaster() {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public ColorModel getColorModel() {
        return getDelegate().getColorModel();
    }

    public Raster getData() {
        return getDelegate().getData();
    }

    public Raster getData(Rectangle rect) {
        return getDelegate().getData(rect);
    }

    public Graphics getGraphics() {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public int getHeight() {
        return getDelegate().getHeight();
    }

    public int getHeight(ImageObserver observer) {
        return getDelegate().getHeight(observer);
    }

    public int getMinTileX() {
        return getDelegate().getMinTileX();
    }

    public int getMinTileY() {
        return getDelegate().getMinTileY();
    }

    public int getMinX() {
        return getDelegate().getMinX();
    }

    public int getMinY() {
        return getDelegate().getMinY();
    }

    public int getNumXTiles() {
        return getDelegate().getNumXTiles();
    }

    public int getNumYTiles() {
        return getDelegate().getNumYTiles();
    }

    public Object getProperty(String name) {
        return getDelegate().getProperty(name);
    }

    public Object getProperty(String name, ImageObserver observer) {
        return getDelegate().getProperty(name, observer);
    }

    public String[] getPropertyNames() {
        return getDelegate().getPropertyNames();
    }

    public WritableRaster getRaster() {
        return getDelegate().getRaster();
    }

    public int getRGB(int x, int y) {
        return getDelegate().getRGB(x, y);
    }

    public int[] getRGB(int startX, int startY, int w, int h,
            int[] rgbArray, int offset, int scansize) {
        return getDelegate().getRGB(startX, startY, w, h,
                rgbArray, offset, scansize);
    }

    public SampleModel getSampleModel() {
        return getDelegate().getSampleModel();
    }

    public ImageProducer getSource() {
        return getDelegate().getSource();
    }

    public Vector getSources() {
        return getDelegate().getSources();
    }

    public BufferedImage getSubimage(int x, int y, int w, int h) {
        return getDelegate().getSubimage(x, y, w, h);
    }

    public Raster getTile(int tileX, int tileY) {
        return getDelegate().getTile(tileX, tileY);
    }

    public int getTileGridXOffset() {
        return getDelegate().getTileGridXOffset();
    }

    public int getTileGridYOffset() {
        return getDelegate().getTileGridYOffset();
    }

    public int getTileHeight() {
        return getDelegate().getTileHeight();
    }

    public int getTileWidth() {
        return getDelegate().getTileWidth();
    }

    public int getType() {
        return getDelegate().getType();
    }

    public int getWidth() {
        return getDelegate().getWidth();
    }

    public int getWidth(ImageObserver observer) {
        return getDelegate().getWidth(observer);
    }

    public WritableRaster getWritableTile(int tileX, int tileY) {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public Point[] getWritableTileIndices() {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public boolean hasTileWriters() {
        return false;
    }

    public boolean isAlphaPremultiplied() {
        return true;
    }

    public boolean isTileWritable(int tileX, int tileY) {
        return false;
    }

    public void releaseWritableTile(int tileX, int tileY) {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    /*
     public void removeTileObserver(TileObserver to)
     {
     //    throw new RuntimeException(new Exception().getStackTrace()[0].
     //                               getMethodName() + " not supported");
     getDelegate().removeTileObserver(to);
     }
     */
    public void setData(Raster r) {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public void setRGB(int x, int y, int rgb) {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public void setRGB(int startX, int startY, int w, int h,
            int[] rgbArray, int offset, int scansize) {
        throw new RuntimeException(new Exception().getStackTrace()[0].
                getMethodName() + " not supported");
    }

    public String toString() {
        return getDelegate().toString();
    }

    // END BufferedImage METHODS
    protected FITSImage(ColorModel cm, WritableRaster r,
            boolean isRasterPremultiplied, Hashtable properties) {
        super(cm, r, isRasterPremultiplied, properties);
    }

    protected void setFits(Fits fits) {
        _fits = fits;
    }

    protected void setImageHDU(ImageHDU imageHDU) {
        _imageHDU = imageHDU;
    }

    protected void setScaledImages(BufferedImage[] scaledImages) {
        _scaledImages = scaledImages;
    }

    protected BufferedImage[] getScaledImages() {
        return _scaledImages;
    }

    protected static ImageHDU findImageHDU(Fits fits, int hduImageNumber)
            throws FitsException, IOException {
        ImageHDU result = null;
        BasicHDU basicHDU = fits.readHDU();
        int hduImageFound = 0;
        while (basicHDU != null) {
            if (basicHDU instanceof ImageHDU && basicHDU.getData().getSize() != 0) {
                hduImageFound++;
                if (hduImageFound == hduImageNumber) {
                    result = (ImageHDU) basicHDU;
                    break;
                }
            } 
            basicHDU = fits.readHDU();            
        }

        return result;
    }

    protected static BufferedImage[] createScaledImages(Fits fits, int hduImageNumber, int deepLevel)
            throws FitsException, DataTypeNotSupportedException,
            NoImageDataFoundException, IOException {
        BufferedImage[] result = null;
        ImageHDU imageHDU = findImageHDU(fits, hduImageNumber);

        if (imageHDU != null) {
            result = createScaledImagesPatch(imageHDU, deepLevel);
        } else {
            throw new NoImageDataFoundException();
        }

        return result;
    }
    
    /**
     * @return An array of BufferedImages from hdu with intensity values scaled
     * to short range using linear, log, square root, and square scales, in that
     * order.
     */
    public static BufferedImage[] createScaledImagesPatch(ImageHDU hdu, int deepLevel)
            throws FitsException, DataTypeNotSupportedException {
        int bitpix = hdu.getBitPix();
        double bZero = hdu.getBZero();
        double bScale = hdu.getBScale();
        Object data;
        Histogram hist = null;

        switch (bitpix) {
            case 8:
                data = (hdu.getAxes().length == 3) ? ((byte[][][])hdu.getData().getData())[deepLevel] : (byte[][]) hdu.getData().getData();
                hist = ScaleUtils.computeHistogram((byte[][]) data, bZero, bScale);
                break;
            case 16:
                data = (hdu.getAxes().length == 3) ? ((short[][][])hdu.getData().getData())[deepLevel] : (short[][]) hdu.getData().getData();
                hist = ScaleUtils.computeHistogram((short[][]) data, bZero, bScale);
                break;
            case 32:
                data = (hdu.getAxes().length == 3) ? ((int[][][])hdu.getData().getData())[deepLevel] : (int[][]) hdu.getData().getData();
                hist = ScaleUtils.computeHistogram((int[][]) data, bZero, bScale);
                break;
            case -32:
                data = (hdu.getAxes().length == 3) ? ((float[][][])hdu.getData().getData())[deepLevel] : (float[][]) hdu.getData().getData();
                hist = ScaleUtils.computeHistogram((float[][]) data, bZero, bScale);
                break;
            case -64:
                data = (hdu.getAxes().length == 3) ? ((double[][][])hdu.getData().getData())[deepLevel] : (double[][]) hdu.getData().getData();
                hist = ScaleUtils.computeHistogram((double[][]) data, bZero, bScale);
                break;
            default:
                throw new DataTypeNotSupportedException(bitpix);
        }
        double contrast = 0.25;
        int opt_size = 600;    /* desired number of pixels in sample   */

        int len_stdline = 120;  /* optimal number of pixels per line    */

        ZScale zscale = new ZScale(hdu, contrast, opt_size, len_stdline);
        ZScale.ZscaleResult retval = zscale.compute();
        return createScaledImages(hdu, data, hist,
                retval.getZ1(), retval.getZ2(),
                hist.estimateSigma());
    }    

    /**
     * @return An array of BufferedImages from hdu with intensity values scaled
     * to short range using linear, log, square root, and square scales, in that
     * order.
     */
    public static BufferedImage[] createScaledImages(ImageHDU hdu)
            throws FitsException, DataTypeNotSupportedException {
        int bitpix = hdu.getBitPix();
        double bZero = hdu.getBZero();
        double bScale = hdu.getBScale();
        Object data = hdu.getData().getData();
        Histogram hist = null;

        switch (bitpix) {
            case 8:
                hist = ScaleUtils.computeHistogram((byte[][]) data, bZero, bScale);
                break;
            case 16:
                hist = ScaleUtils.computeHistogram((short[][]) data, bZero, bScale);
                break;
            case 32:
                hist = ScaleUtils.computeHistogram((int[][]) data, bZero, bScale);
                break;
            case -32:
                hist = ScaleUtils.computeHistogram((float[][]) data, bZero, bScale);
                break;
            case -64:
                hist = ScaleUtils.computeHistogram((double[][]) data, bZero, bScale);
                break;
            default:
                throw new DataTypeNotSupportedException(bitpix);
        }
    return createScaledImages(hdu, hdu.getData(), hist,
                              hist.getMin(), hist.getMax(),
                              hist.estimateSigma());
    }    

    public static BufferedImage[] createScaledImages(ImageHDU hdu, Object data, Histogram hist,
            double min, double max,
            double sigma)
            throws FitsException, DataTypeNotSupportedException {
        int bitpix = hdu.getBitPix();
        int nbAxes = hdu.getAxes().length;
        int width = hdu.getAxes()[nbAxes-1]; // yes, the axes are in the wrong order
        int height = hdu.getAxes()[nbAxes-2];
        double bZero = hdu.getBZero();
        double bScale = hdu.getBScale();
        short[][] scaledData = null;

        switch (bitpix) {
            case 8:
                scaledData = ScaleUtils.scaleToUShort((byte[][]) data, hist,
                        width, height, bZero, bScale,
                        min, max, sigma);
                break;
            case 16:
                scaledData = ScaleUtils.scaleToUShort((short[][]) data, hist,
                        width, height, bZero, bScale,
                        min, max, sigma);
                break;
            case 32:
                scaledData = ScaleUtils.scaleToUShort((int[][]) data, hist,
                        width, height, bZero, bScale,
                        min, max, sigma);
                break;
            case -32:
                scaledData = ScaleUtils.scaleToUShort((float[][]) data, hist,
                        width, height, bZero, bScale,
                        min, max, sigma);
                break;
            case -64:
                scaledData = ScaleUtils.scaleToUShort((double[][]) data, hist,
                        width, height, bZero, bScale,
                        min, max, sigma);
                break;
            default:
                throw new DataTypeNotSupportedException(bitpix);
        }

        ColorModel cm
                = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                false, false, Transparency.OPAQUE,
                DataBuffer.TYPE_USHORT);
        SampleModel sm = cm.createCompatibleSampleModel(width, height);

        Hashtable properties = new Hashtable();
        properties.put("histogram", hist);
        properties.put("imageHDU", hdu);

        BufferedImage[] result = new BufferedImage[scaledData.length];

        for (int i = 0; i < result.length; ++i) {
            DataBuffer db = new DataBufferUShort(scaledData[i], height);
            WritableRaster r = Raster.createWritableRaster(sm, db, null);

            result[i] = new BufferedImage(cm, r, false, properties);
        }

        return result;
    }

    public static class DataTypeNotSupportedException extends Exception {

        public DataTypeNotSupportedException(int bitpix) {
            super(bitpix + " is not a valid FITS data type.");
        }
    }

    public static class NoImageDataFoundException extends Exception {

        public NoImageDataFoundException() {
            super("No image data found in FITS file.");
        }
    }

    /**
     * @return CVS Revision number.
     */
    public static String revision() {
        return "$Revision: 1.21 $";
    }

    protected Fits _fits;
    protected ImageHDU _imageHDU;
    protected BasicHDU[] _hdus;

    protected int _scaleMethod;
    protected double _sigma = Double.NaN;
    protected double _min = Double.NaN;
    protected double _max = Double.NaN;
    protected BufferedImage[] _scaledImages;
}

/**
 * Revision History ================
 *
 * $Log: FITSImage.java,v $ Revision 1.21 2004/07/23 21:54:11 carliles Added
 * javadocs.
 *
 * Revision 1.20 2004/07/22 22:29:08 carliles Added "low" memory consumption
 * SlowFITSImage.
 *
 * Revision 1.19 2004/07/21 22:24:57 carliles Removed some commented crap.
 *
 * Revision 1.18 2004/07/21 18:03:55 carliles Added asinh with sigma estimation.
 *
 * Revision 1.17 2004/07/16 02:48:53 carliles Hist EQ doesn't look quite right,
 * but there's nothing to compare it to, and the math looks right.
 *
 * Revision 1.16 2004/07/14 02:40:49 carliles Scaling should be done once and
 * for all, with all possible accelerations. Now just have to add hist eq and
 * asinh.
 *
 * Revision 1.15 2004/07/09 02:22:31 carliles Added log/sqrt maps, fixed wrong
 * output for byte images (again).
 *
 * Revision 1.14 2004/06/21 05:38:39 carliles Got rescale lookup acceleration
 * working for short images. Also in theory for int images, though I can't test
 * because of dynamic range of my int image.
 *
 * Revision 1.13 2004/06/19 01:11:49 carliles Converted FITSImage to extend
 * BufferedImage.
 *
 * Revision 1.12 2004/06/17 01:05:05 carliles Fixed some image orientation shit.
 * Added getOriginalValue method to FITSImage.
 *
 * Revision 1.11 2004/06/16 22:27:20 carliles Fixed bug with ImageHDU crap in
 * FITSImage.
 *
 * Revision 1.10 2004/06/16 22:21:02 carliles Added method to fetch ImageHDU
 * from FITSImage.
 *
 * Revision 1.9 2004/06/07 21:14:06 carliles Rescale works nicely for all types
 * now.
 *
 * Revision 1.8 2004/06/07 20:05:19 carliles Added rescale to FITSImage.
 *
 * Revision 1.7 2004/06/04 23:11:52 carliles Cleaned up histogram crap a bit.
 *
 * Revision 1.6 2004/06/04 01:01:36 carliles Got rid of some overmodelling.
 *
 * Revision 1.5 2004/06/02 22:17:37 carliles Got the hang of cut levels. Need to
 * implement widely and as efficiently as possible.
 *
 * Revision 1.4 2004/06/02 19:39:36 carliles Adding histogram crap.
 *
 * Revision 1.3 2004/05/27 17:01:03 carliles ImageIO FITS reading "works". Some
 * cleanup would be good.
 *
 * Revision 1.2 2004/05/26 23:00:15 carliles Fucking Sun and their fucking
 * BufferedImages everywhere.
 *
 * Revision 1.1 2004/05/26 16:56:11 carliles Initial checkin of separate FITS
 * package.
 *
 * Revision 1.12 2003/08/19 19:12:30 carliles
 */
