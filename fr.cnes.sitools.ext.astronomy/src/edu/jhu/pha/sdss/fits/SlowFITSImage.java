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



/**
   <PRE>
   Current Version
   ===============
   ID:            $Id: SlowFITSImage.java,v 1.2 2004/07/23 18:52:35 carliles Exp $
   Revision:      $Revision: 1.2 $
   Date/time:     $Date: 2004/07/23 18:52:35 $
   </PRE>
*/
public class SlowFITSImage extends FITSImage
{
  public SlowFITSImage(Fits fits)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    this(fits, SCALE_LINEAR);
  }

  public SlowFITSImage(Fits fits, int scaleMethod)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    this(fits, createScaledImage(fits, scaleMethod), scaleMethod);
  }

  public SlowFITSImage(Fits fits, BufferedImage delegate, int scaleMethod)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    super(delegate.getColorModel(),
          delegate.getRaster().createCompatibleWritableRaster(),
          true, null);

    setFits(fits);
    setHistogram((Histogram)delegate.getProperty("histogram"));
    setDelegate(delegate);
    _scaleMethod = scaleMethod;
    _scaledData = (short[])delegate.getProperty("scaledData");

    ImageHDU imageHDU = (ImageHDU)delegate.getProperty("imageHDU");
    if(imageHDU == null || !(imageHDU instanceof ImageHDU))
    {
      imageHDU = findFirstImageHDU(fits);
    }
    setImageHDU(imageHDU);

    _min = getHistogram().getMin();
    _max = getHistogram().getMax();
    _sigma = getHistogram().estimateSigma();
  }

  public SlowFITSImage(File file)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    this(new Fits(file));
  }

  public SlowFITSImage(File file, int scaleMethod)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    this(new Fits(file), scaleMethod);
  }

  public SlowFITSImage(String filename)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    this(new Fits(filename));
  }

  public SlowFITSImage(String filename, int scaleMethod)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    this(new Fits(filename), scaleMethod);
  }

  public SlowFITSImage(URL url)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    this(new Fits(url));
  }

  public SlowFITSImage(URL url, int scaleMethod)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    this(new Fits(url), scaleMethod);
  }

  public static String[] getScaleNames()
  {
    return ScaleUtils.getScaleNames();
  }

  public Fits getFits()
  {
    return _fits;
  }

  public ImageHDU getImageHDU()
  {
    return _imageHDU;
  }

  protected void setHistogram(Histogram histogram)
  {
    _histogram = histogram;
  }

  public Histogram getHistogram()
  {
    return _histogram;
  }

  public double getOriginalValue(int x, int y) throws FitsException
  {
    double result = Double.NaN;
    double bZero = getImageHDU().getBZero();
    double bScale = getImageHDU().getBScale();
    Object data = getImageHDU().getData().getData();

    switch(getImageHDU().getBitPix())
    {
    case 8:
      int dataVal = ((byte[][])data)[y][x];
      if(dataVal < 0)
      {
        dataVal += 256;
      }
      result = bZero + bScale * dataVal;
      break;
    case 16:
      result = bZero + bScale * ((double)((short[][])data)[y][x]);
      break;
    case 32:
      result = bZero + bScale * ((double)((int[][])data)[y][x]);
      break;
    case -32:
      result = bZero + bScale * ((double)((float[][])data)[y][x]);
      break;
    case -64:
      result = bZero + bScale * ((double[][])data)[y][x];
      break;
    default:
      break;
    }

    return result;
  }

  public int getScaleMethod()
  {
    return _scaleMethod;
  }

  public void setScaleMethod(int scaleMethod)
    //    throws FitsException, DataTypeNotSupportedException
  {
    if(scaleMethod != _scaleMethod)
    {
      try
      {
        setDelegate(createScaledImage(getImageHDU(), _scaledData,
                                      getHistogram(), _min, _max,
                                      _sigma, scaleMethod));
        _scaleMethod = scaleMethod;
      }
      catch(Exception e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  public void rescale(double min, double max, double sigma)
  {
    if(min != _min || max != _max || sigma != _sigma)
    {
      try
      {
        _min = min;
        _max = max;
        _sigma = sigma;
        setDelegate(createScaledImage(getImageHDU(), _scaledData,
                                      getHistogram(), min, max,
                                      sigma, _scaleMethod));
      }
      catch(Exception e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  protected BufferedImage getDelegate()
  {
    return _delegate;
  }

  protected void setDelegate(BufferedImage delegate)
  {
    _delegate = delegate;
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

  public WritableRaster copyData(WritableRaster outRaster)
  {
    return getDelegate().copyData(outRaster);
  }

  public Graphics2D createGraphics()
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public void flush()
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public WritableRaster getAlphaRaster()
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public ColorModel getColorModel()
  {
    return getDelegate().getColorModel();
  }

  public Raster getData()
  {
    return getDelegate().getData();
  }

  public Raster getData(Rectangle rect)
  {
    return getDelegate().getData(rect);
  }

  public Graphics getGraphics()
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public int getHeight()
  {
    return getDelegate().getHeight();
  }

  public int getHeight(ImageObserver observer)
  {
    return getDelegate().getHeight(observer);
  }

  public int getMinTileX()
  {
    return getDelegate().getMinTileX();
  }

  public int getMinTileY()
  {
    return getDelegate().getMinTileY();
  }

  public int getMinX()
  {
    return getDelegate().getMinX();
  }

  public int getMinY()
  {
    return getDelegate().getMinY();
  }

  public int getNumXTiles()
  {
    return getDelegate().getNumXTiles();
  }

  public int getNumYTiles()
  {
    return getDelegate().getNumYTiles();
  }

  public Object getProperty(String name)
  {
    return getDelegate().getProperty(name);
  }

  public Object getProperty(String name, ImageObserver observer)
  {
    return getDelegate().getProperty(name, observer);
  }

  public String[] getPropertyNames()
  {
    return getDelegate().getPropertyNames();
  }

  public WritableRaster getRaster()
  {
    return getDelegate().getRaster();
  }

  public int getRGB(int x, int y)
  {
    return getDelegate().getRGB(x, y);
  }

  public int[] getRGB(int startX, int startY, int w, int h,
                      int[] rgbArray, int offset, int scansize)
  {
    return getDelegate().getRGB(startX, startY, w, h,
                                rgbArray, offset, scansize);
  }

  public SampleModel getSampleModel()
  {
    return getDelegate().getSampleModel();
  }

  public ImageProducer getSource()
  {
    return getDelegate().getSource();
  }

  public Vector getSources()
  {
    return getDelegate().getSources();
  }

  public BufferedImage getSubimage(int x, int y, int w, int h)
  {
    return getDelegate().getSubimage(x, y, w, h);
  }

  public Raster getTile(int tileX, int tileY)
  {
    return getDelegate().getTile(tileX, tileY);
  }

  public int getTileGridXOffset()
  {
    return getDelegate().getTileGridXOffset();
  }

  public int getTileGridYOffset()
  {
    return getDelegate().getTileGridYOffset();
  }

  public int getTileHeight()
  {
    return getDelegate().getTileHeight();
  }

  public int getTileWidth()
  {
    return getDelegate().getTileWidth();
  }

  public int getType()
  {
    return getDelegate().getType();
  }

  public int getWidth()
  {
    return getDelegate().getWidth();
  }

  public int getWidth(ImageObserver observer)
  {
    return getDelegate().getWidth(observer);
  }

  public WritableRaster getWritableTile(int tileX, int tileY)
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public Point[] getWritableTileIndices()
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public boolean hasTileWriters()
  {
    return false;
  }

  public boolean isAlphaPremultiplied()
  {
    return true;
  }

  public boolean isTileWritable(int tileX, int tileY)
  {
    return false;
  }

  public void releaseWritableTile(int tileX, int tileY)
  {
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

  public void setData(Raster r)
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public void setRGB(int x, int y, int rgb)
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public void setRGB(int startX, int startY, int w, int h,
                     int[] rgbArray, int offset, int scansize)
  {
    throw new RuntimeException(new Exception().getStackTrace()[0].
                               getMethodName() + " not supported");
  }

  public String toString()
  {
    return getDelegate().toString();
  }

  // END BufferedImage METHODS

  protected void setFits(Fits fits)
  {
    _fits = fits;
  }

  protected void setImageHDU(ImageHDU imageHDU)
  {
    _imageHDU = imageHDU;
  }

  /*
  protected void setScaledImages(BufferedImage[] scaledImages)
  {
    _scaledImages = scaledImages;
  }

  protected BufferedImage[] getScaledImages()
  {
    return _scaledImages;
  }
  */

  protected static ImageHDU findFirstImageHDU(Fits fits)
    throws FitsException, IOException
  {
    ImageHDU result = null;    
    BasicHDU basicHDU = fits.readHDU();    
    while (basicHDU != null) {
        if (basicHDU instanceof ImageHDU && basicHDU.getData().getSize() != 0) {
            result = (ImageHDU) basicHDU;
            break;
        } else {
            basicHDU = fits.readHDU();
        }
    }    

    return result;
  }

  protected static BufferedImage createScaledImage(Fits fits, int scaleMethod)
    throws FitsException, DataTypeNotSupportedException,
           NoImageDataFoundException, IOException
  {
    BufferedImage result = null;
    ImageHDU imageHDU = findFirstImageHDU(fits);

    if(imageHDU != null)
    {
      result = createScaledImage(imageHDU, scaleMethod);
    }
    else
    {
      throw new NoImageDataFoundException();
    }

    return result;
  }

  /**
     @return An array of BufferedImages from hdu with intensity values scaled
     to short range using linear, log, square root, and square scales,
     in that order.
   */
  public static BufferedImage createScaledImage(ImageHDU hdu, int scaleMethod)
    throws FitsException, DataTypeNotSupportedException
  {
    int bitpix = hdu.getBitPix();
    int width = hdu.getAxes()[1]; // yes, the axes are in the wrong order
    int height = hdu.getAxes()[0];
    double bZero = hdu.getBZero();
    double bScale = hdu.getBScale();
    Object data = hdu.getData().getData();
    Histogram hist = null;

    switch(bitpix)
    {
    case 8:
      hist = ScaleUtils.computeHistogram((byte[][])data, bZero, bScale);
      break;
    case 16:
      hist = ScaleUtils.computeHistogram((short[][])data, bZero, bScale);
      break;
    case 32:
      hist = ScaleUtils.computeHistogram((int[][])data, bZero, bScale);
      break;
    case -32:
      hist = ScaleUtils.computeHistogram((float[][])data, bZero, bScale);
      break;
    case -64:
      hist = ScaleUtils.computeHistogram((double[][])data, bZero, bScale);
      break;
    default:
      throw new DataTypeNotSupportedException(bitpix);
    }

    return createScaledImage(hdu, null, hist, hist.getMin(), hist.getMax(),
                             hist.estimateSigma(), scaleMethod);
  }

  public static BufferedImage createScaledImage(ImageHDU hdu,
                                                short[] result,
                                                Histogram hist,
                                                double min, double max,
                                                double sigma,
                                                int scaleMethod)
    throws FitsException, DataTypeNotSupportedException
  {
    int bitpix = hdu.getBitPix();
    Object data = hdu.getData().getData();
    int width = hdu.getAxes()[1]; // yes, the axes are in the wrong order
    int height = hdu.getAxes()[0];
    double bZero = hdu.getBZero();
    double bScale = hdu.getBScale();
    short[] scaledData = SlowScaleUtils.scale(data, result, width, height,
                                              bZero, bScale, min, max,
                                              sigma, hist, scaleMethod);

    ColorModel cm =
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                              false, false, Transparency.OPAQUE,
                              DataBuffer.TYPE_USHORT);
    SampleModel sm = cm.createCompatibleSampleModel(width, height);

    Hashtable properties = new Hashtable();
    properties.put("histogram", hist);
    properties.put("imageHDU", hdu);
    properties.put("scaledData", scaledData);

    DataBuffer db = new DataBufferUShort(scaledData, height);
    WritableRaster r = Raster.createWritableRaster(sm, db, null);

    return new BufferedImage(cm, r, false, properties);
  }

  public static class DataTypeNotSupportedException extends Exception
  {
    public DataTypeNotSupportedException(int bitpix)
    {
      super(bitpix + " is not a valid FITS data type.");
    }
  }

  public static class NoImageDataFoundException extends Exception
  {
    public NoImageDataFoundException()
    {
      super("No image data found in FITS file.");
    }
  }

  /**
     @return CVS Revision number.
  */
  public static String revision()
  {
    return "$Revision: 1.2 $";
  }

  protected Fits _fits;
  protected ImageHDU _imageHDU;
  protected BasicHDU[] _hdus;

  protected int _scaleMethod;
  protected short[] _scaledData = null;
  protected double _sigma = Double.NaN;
  protected double _min = Double.NaN;
  protected double _max = Double.NaN;
  protected Histogram _histogram;
  protected BufferedImage _delegate;
}

/**
   Revision History
   ================

   $Log: SlowFITSImage.java,v $
   Revision 1.2  2004/07/23 18:52:35  carliles
   SlowFITSImage is done.

   Revision 1.1  2004/07/22 22:29:08  carliles
   Added "low" memory consumption SlowFITSImage.

   Revision 1.19  2004/07/21 22:24:57  carliles
   Removed some commented crap.

   Revision 1.18  2004/07/21 18:03:55  carliles
   Added asinh with sigma estimation.

   Revision 1.17  2004/07/16 02:48:53  carliles
   Hist EQ doesn't look quite right, but there's nothing to compare it to, and the
   math looks right.

   Revision 1.16  2004/07/14 02:40:49  carliles
   Scaling should be done once and for all, with all possible accelerations.  Now
   just have to add hist eq and asinh.

   Revision 1.15  2004/07/09 02:22:31  carliles
   Added log/sqrt maps, fixed wrong output for byte images (again).

   Revision 1.14  2004/06/21 05:38:39  carliles
   Got rescale lookup acceleration working for short images.  Also in theory for
   int images, though I can't test because of dynamic range of my int image.

   Revision 1.13  2004/06/19 01:11:49  carliles
   Converted FITSImage to extend BufferedImage.

   Revision 1.12  2004/06/17 01:05:05  carliles
   Fixed some image orientation shit.  Added getOriginalValue method to FITSImage.

   Revision 1.11  2004/06/16 22:27:20  carliles
   Fixed bug with ImageHDU crap in FITSImage.

   Revision 1.10  2004/06/16 22:21:02  carliles
   Added method to fetch ImageHDU from FITSImage.

   Revision 1.9  2004/06/07 21:14:06  carliles
   Rescale works nicely for all types now.

   Revision 1.8  2004/06/07 20:05:19  carliles
   Added rescale to FITSImage.

   Revision 1.7  2004/06/04 23:11:52  carliles
   Cleaned up histogram crap a bit.

   Revision 1.6  2004/06/04 01:01:36  carliles
   Got rid of some overmodelling.

   Revision 1.5  2004/06/02 22:17:37  carliles
   Got the hang of cut levels.  Need to implement widely and as efficiently as
   possible.

   Revision 1.4  2004/06/02 19:39:36  carliles
   Adding histogram crap.

   Revision 1.3  2004/05/27 17:01:03  carliles
   ImageIO FITS reading "works".  Some cleanup would be good.

   Revision 1.2  2004/05/26 23:00:15  carliles
   Fucking Sun and their fucking BufferedImages everywhere.

   Revision 1.1  2004/05/26 16:56:11  carliles
   Initial checkin of separate FITS package.

   Revision 1.12  2003/08/19 19:12:30  carliles
*/
