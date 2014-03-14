package edu.jhu.pha.sdss.fits.imageio;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import nom.tam.fits.Fits;
import edu.jhu.pha.sdss.fits.FITSImage;
import edu.jhu.pha.sdss.fits.SlowFITSImage;

/**
   Written by Samuel Carliles 2004.

   <PRE>
   Current Version
   ===============
   ID:            $Id: FITSReader.java,v 1.8 2006/08/24 20:55:28 carliles Exp $
   Revision:      $Revision: 1.8 $
   Date/time:     $Date: 2006/08/24 20:55:28 $
   </PRE>
*/
public class FITSReader extends ImageReader
{
  public FITSReader(ImageReaderSpi originatingProvider,
                    Object extensionObject)
  {
    super(originatingProvider);
    // what the hell is an extension object?  I don't think we need it.
  }

  public IIOMetadata getImageMetadata(int imageIndex)
  {
    return null;
  }

  public IIOMetadata getStreamMetadata()
  {
    return null;
  }

  public Iterator getImageTypes(int imageIndex)
  {
    // Returns an Iterator containing possible image types to which the
    // given image may be decoded, in the form of ImageTypeSpecifiers.
    read(imageIndex, null);

    return _imageTypeSpecifiers.iterator();
  }

  public int getNumImages(boolean allowSearch)
  {
    return read(0, null) == null ? 0 : 1;
  }

  public int getHeight(int imageIndex)
  {
    int result = 0;

    BufferedImage image = read(imageIndex, null);
    if(image != null)
    {
      result = image.getHeight();
    }

    return result;
  }

  public int getWidth(int imageIndex)
  {
    int result = 0;

    BufferedImage image = read(imageIndex, null);
    if(image != null)
    {
      result = image.getWidth();
    }

    return result;
  }

  /**
     We ignore both parameters, because we expect only one image per stream,
     and we only read it one way.
   */
  public BufferedImage read(int imageIndex, ImageReadParam param)
  {
    FITSImage result = getImage();

    if(result == null && !doneReading())
    {
      try
      {
        InputStream in =
          new ImageInputStreamInputStream((ImageInputStream)getInput());

        result = new SlowFITSImage(new Fits(in));
      }
      catch(Exception e)
      {
        e.printStackTrace();
        result = null;
      }

      setImage(result);
      setDoneReading(true);
      _imageTypeSpecifiers.add(new ImageTypeSpecifier(getImage()));
    }

    return result;
  }

  /**
     @return CVS Revision number.
  */
  public static String revision()
  {
    return "$Revision: 1.8 $";
  }

  protected FITSImage getImage()
  {
    return _image;
  }

  protected void setImage(FITSImage image)
  {
    _image = image;
  }

  protected boolean doneReading()
  {
    return _doneReading;
  }

  protected void setDoneReading(boolean done)
  {
    _doneReading = done;
  }

  protected FITSImage _image;
  protected boolean _doneReading = false;

  protected final List _imageTypeSpecifiers = new ArrayList();
}

/**
   Revision History
   ================

   $Log: FITSReader.java,v $
   Revision 1.8  2006/08/24 20:55:28  carliles
   Updated to work with change in jdk implementation.

   Revision 1.7  2004/07/22 22:29:09  carliles
   Added "low" memory consumption SlowFITSImage.

   Revision 1.6  2004/06/19 01:11:49  carliles
   Converted FITSImage to extend BufferedImage.

   Revision 1.5  2004/05/27 18:13:50  carliles
   Added more implementation to FITSReader, though none of it appears to be used,
   and none has been tested.

   Revision 1.4  2004/05/27 17:01:03  carliles
   ImageIO FITS reading "works".  Some cleanup would be good.

   Revision 1.3  2004/05/26 21:28:59  carliles
   FITSReaderSpi looks pretty done.

   Revision 1.2  2004/05/26 17:10:00  carliles
   Getting CVS crap in files in place.

   Revision 1.1  2004/05/26 16:56:11  carliles
   Initial checkin of separate FITS package.

   Revision 1.12  2003/08/19 19:12:30  carliles
*/
