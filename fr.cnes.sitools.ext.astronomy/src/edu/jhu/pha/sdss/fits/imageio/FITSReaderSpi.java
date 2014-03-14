package edu.jhu.pha.sdss.fits.imageio;

import java.io.InputStream;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
   Written by Samuel Carliles 2004.

   <PRE>
   Current Version
   ===============
   ID:            $Id: FITSReaderSpi.java,v 1.4 2006/08/24 20:55:28 carliles Exp $
   Revision:      $Revision: 1.4 $
   Date/time:     $Date: 2006/08/24 20:55:28 $
   </PRE>
*/

public class FITSReaderSpi extends ImageReaderSpi
{
  public FITSReaderSpi()
  {
    super("JHU/NVO", // vendorName
          "$Revision: 1.4 $", // version
          new String[]
      {
        "FITS", "fits"
      }, // names
          new String[]
      {
        "fits", "fits.gz"
      }, // suffixes
          new String[]
      {
        "image/x-fits", "image/x-gfits"
      }, // MIMETypes
          FITSReader.class.getName(), // readerClassName
          new Class[]
      {
        ImageInputStream.class
      }, // inputTypes
          null, // writerSpiNames
          false, // supportsStandardStreamMetadataFormat
          null, // nativeStreamMetadataFormatName
          null, // nativeStreamMetadataFormatClassName
          null, // extraStreamMetadataFormatNames
          null, // extraStreamMetadataFormatClassNames
          false, // supportsStandardImageMetadataFormat
          null, // nativeImageMetadataFormatName
          null, // nativeImageMetadataFormatClassName
          null, // extraImageMetadataFormatNames
          null // extraImageMetadataFormatClassNames
          );
  }

  public boolean canDecodeInput(Object source)
  {
    boolean result = source instanceof ImageInputStream;

    // Returns true if the supplied source object appears to be of the
    // format supported by this reader.
    if(result)
    {
      try
      {
        InputStream in =
          new ImageInputStreamInputStream((ImageInputStream)source);

        // check for FITS
        byte[] buf = new byte[80];
        in.read(buf, 0, 80);
        ((ImageInputStreamInputStream)in).unread(buf);

        result = new String(buf).replaceAll("[ ]+", " ").
          startsWith("SIMPLE = T");
      }
      catch(Exception e)
      {
        e.printStackTrace();
        result = false;
      }
    }

    return result;
  }

  public ImageReader createReaderInstance()
  {
    return createReaderInstance(null);
  }

  public ImageReader createReaderInstance(Object extension)
  {
    return new FITSReader(this, null);
  }

  public String[] getImageWriterSpiNames()
  {
    return null;
  }

  public Class[] getInputTypes()
  {
    return inputTypes;
  }

  public boolean isOwnReader(ImageReader reader)
  {
    return (reader instanceof FITSReader);
  }

  public String getDescription(Locale locale)
  {
    return "It reads FITS images, including gzipped FITS images.";
  }

  /*
  public void onDeregistration(ServiceRegistry registry, Class category)
  {
    System.err.println("Deregistered");
    System.err.flush();
  }

  public void onRegistration(ServiceRegistry registry, Class category)
  {
    System.err.println("Registered");
    System.err.flush();
  }
  */

  /**
     @return CVS Revision number.
  */
  public static String revision()
  {
    return "$Revision: 1.4 $";
  }
}

/**
   Revision History
   ================

   $Log: FITSReaderSpi.java,v $
   Revision 1.4  2006/08/24 20:55:28  carliles
   Updated to work with change in jdk implementation.

   Revision 1.3  2004/05/27 17:01:03  carliles
   ImageIO FITS reading "works".  Some cleanup would be good.

   Revision 1.2  2004/05/26 21:28:59  carliles
   FITSReaderSpi looks pretty done.

   Revision 1.1  2004/05/26 17:08:03  carliles
   Created imageio package.

   Revision 1.2  2004/05/26 16:59:44  carliles
   Fixed some package crap.

   Revision 1.1  2004/05/26 16:56:11  carliles
   Initial checkin of separate FITS package.

   Revision 1.6  2003/07/25 00:55:24  carliles
*/
