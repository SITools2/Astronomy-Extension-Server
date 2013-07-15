package edu.jhu.pha.sdss.fits;

import java.util.Arrays;


/**
   <PRE>
   Current Version
   ===============
   ID:            $Id: ScaleUtils.java,v 1.14 2004/07/23 18:52:35 carliles Exp $
   Revision:      $Revision: 1.14 $
   Date/time:     $Date: 2004/07/23 18:52:35 $
   </PRE>

   Array operations that should be implemented in one method, but which
   have to be reimplemented for each primitive data type just to have
   the right type in the damn parameter list.
*/
public class ScaleUtils
{
  public static final String[] SCALE_NAMES = new String[]
    {
      "Linear", "Log", "Square Root", "Square", "Hist EQ", "Arcsinh"
    };
  public static final int LINEAR = 0;
  public static final int LOG = 1;
  public static final int SQUARE_ROOT = 2;
  public static final int SQUARE = 3;
  public static final int HIST_EQ = 4;
  public static final int ASINH = 5;

  public static String[] getScaleNames()
  {
    return SCALE_NAMES;
  }

  public static double arcsinh(double val)
  {
    return Math.log(val + Math.sqrt(1.0 + val * val));
  }

  /**
     @param data The image intensity values indexed by y, then x.
     @param width The width of the image.
     @param height The height of the image.
     @param bZero The value of the BZERO field of the FITS header.
     @param bScale The value of the BSCALE field of the FITS header.
     @param min The minimum input data value over which to scale.
     @param max The maximum input data value over which to scale.
     @return An array of short arrays (suitable for use in a
     PixelInterleavedSampleModel with 3 bands) corresponding to linear, log,
     square root, and square scaling of intensities, in that order.
  */
  public static short[][] scaleToUShort(byte[][] data, Histogram hist,
                                        int width, int height,
                                        double bZero, double bScale,
                                        double min, double max, double sigma)
  {
    int arraySize = width * height * 3;
    short[] linearResult = new short[arraySize];
    short[] logResult = new short[arraySize];
    short[] sqrtResult = new short[arraySize];
    short[] squareResult = new short[arraySize];
    short[] histEqResult = new short[arraySize];
    short[] asinhResult = new short[arraySize];

    double nBins = Math.pow(2.0, 16.0) - 1.0;
    double offset = bZero - min;
    double log10 = Math.log(10.0);

    double linearScaleFactor = nBins / (max - min);
    double sqrtScaleFactor = nBins / Math.sqrt(max - min);
    double squareScaleFactor = nBins / Math.pow(max - min, 2.0);
    double logScaleFactor = nBins / (Math.log(nBins) / log10);
    double logLinearScaleFactor = Math.log(linearScaleFactor);
    double asinhScaleFactor = nBins / arcsinh((max - min) / sigma);

    int minIndex = (int)Math.rint((min - bZero) / bScale);
    int maxIndex = (int)Math.rint((max - bZero) / bScale);
    double rootBscale = Math.sqrt(bScale);
    double logBscale = Math.log(bScale);
    double rootMax = Math.sqrt(max - min);
    double logMax = Math.log(max - min);

    for(int y = 0; y < height; ++y)
    {
      for(int x = 0; x < width; ++x)
      {
        int bandedIndex = (y * width + x) * 3;
        int shifted = data[y][x];
        if(shifted < 0)
        {
          shifted += 256;
        }
        double val =
          Math.max(0.0, Math.min(offset + bScale * (double)shifted,
                                 max - min));
        int mapIndex =
          Math.max(minIndex, Math.min(maxIndex, shifted)) - minIndex;
        double sqrtVal =
          Math.max(0.0, Math.min(rootMax,
                                 rootBscale * BYTE_SQRT_MAP[mapIndex]));
        double logVal =
          Math.max(0.0, Math.min(logMax, logBscale + BYTE_LOG_MAP[mapIndex]));

        linearResult[bandedIndex] =
          linearResult[bandedIndex + 1] =
          linearResult[bandedIndex + 2] =
          (short)(val * linearScaleFactor);

        squareResult[bandedIndex] =
          squareResult[bandedIndex + 1] =
          squareResult[bandedIndex + 2] =
          (short)(val * val * squareScaleFactor);

        //        shifted = (int)Math.max(min, Math.min(max, shifted));

        sqrtResult[bandedIndex] =
          sqrtResult[bandedIndex + 1] =
          sqrtResult[bandedIndex + 2] =
          (short)(sqrtVal * sqrtScaleFactor);

        logResult[bandedIndex] =
          logResult[bandedIndex + 1] =
          logResult[bandedIndex + 2] =
          val <= 0.0 ?
          (short)0 :
          (short)(((logVal + logLinearScaleFactor) / log10) * logScaleFactor);

        histEqResult[bandedIndex] =
          histEqResult[bandedIndex + 1] =
          histEqResult[bandedIndex + 2] =
          (short)hist.
          getEqualizedValue((int)(val * linearScaleFactor));

        asinhResult[bandedIndex] =
          asinhResult[bandedIndex + 1] =
          asinhResult[bandedIndex + 2] =
          (short)(arcsinh(val / sigma) * asinhScaleFactor);
      }
    }

    return new short[][]
      {
        linearResult, logResult, sqrtResult,
        squareResult, histEqResult, asinhResult
      };
  }

  /**
     @param data The image intensity values indexed by y, then x.
     @param width The width of the image.
     @param height The height of the image.
     @param bZero The value of the BZERO field of the FITS header.
     @param bScale The value of the BSCALE field of the FITS header.
     @param min The minimum input data value over which to scale.
     @param max The maximum input data value over which to scale.
     @return An array of short arrays (suitable for use in a
     PixelInterleavedSampleModel with 3 bands) corresponding to linear, log,
     square root, and square scaling of intensities, in that order.
  */
  public static short[][] scaleToUShort(short[][] data, Histogram hist,
                                        int width, int height,
                                        double bZero, double bScale,
                                        double min, double max, double sigma)
  {
    //    long startTime = System.currentTimeMillis();

    int arraySize = width * height * 3;
    short[] linearResult = new short[arraySize];
    short[] logResult = new short[arraySize];
    short[] sqrtResult = new short[arraySize];
    short[] squareResult = new short[arraySize];
    short[] histEqResult = new short[arraySize];
    short[] asinhResult = new short[arraySize];

    double nBins = Math.pow(2.0, 16.0) - 1.0;
    double offset = bZero - min;
    double log10 = Math.log(10.0);

    double linearScaleFactor = nBins / (max - min);
    double sqrtScaleFactor = nBins / Math.sqrt(max - min);
    double squareScaleFactor = nBins / Math.pow(max - min, 2.0);
    double logScaleFactor = nBins / (Math.log(nBins) / log10);
    double logLinearScaleFactor = Math.log(linearScaleFactor);
    double asinhScaleFactor = nBins / arcsinh((max - min) / sigma);

    int minIndex = (int)Math.rint((min - bZero) / bScale);
    int maxIndex = (int)Math.rint((max - bZero) / bScale);
    double rootBscale = Math.sqrt(bScale);
    double logBscale = Math.log(bScale);
    double rootMax = Math.sqrt(max - min);
    double logMax = Math.log(max - min);

    for(int y = 0; y < height; ++y)
    {
      for(int x = 0; x < width; ++x)
      {
        int bandedIndex = (y * width + x) * 3;
        double val =
          Math.max(0.0, Math.min(offset + bScale * (double)data[y][x],
                                 max - min));
        int mapIndex =
          Math.max(minIndex, Math.min(maxIndex, data[y][x])) - minIndex;
        double sqrtVal =
          Math.max(0.0, Math.min(rootMax,
                                 rootBscale * SHORT_SQRT_MAP[mapIndex]));
        double logVal =
          Math.max(0.0, Math.min(logMax, logBscale + SHORT_LOG_MAP[mapIndex]));

        linearResult[bandedIndex] =
          linearResult[bandedIndex + 1] =
          linearResult[bandedIndex + 2] =
          (short)(val * linearScaleFactor);

        squareResult[bandedIndex] =
          squareResult[bandedIndex + 1] =
          squareResult[bandedIndex + 2] =
          (short)(val * val * squareScaleFactor);

        sqrtResult[bandedIndex] =
          sqrtResult[bandedIndex + 1] =
          sqrtResult[bandedIndex + 2] =
          (short)(sqrtVal * sqrtScaleFactor);

        logResult[bandedIndex] =
          logResult[bandedIndex + 1] =
          logResult[bandedIndex + 2] =
          val <= 0.0 ?
          (short)0 :
          (short)(((logVal + logLinearScaleFactor) / log10) * logScaleFactor);

        histEqResult[bandedIndex] =
          histEqResult[bandedIndex + 1] =
          histEqResult[bandedIndex + 2] =
          (short)hist.
          getEqualizedValue((int)(val * linearScaleFactor));

        asinhResult[bandedIndex] =
          asinhResult[bandedIndex + 1] =
          asinhResult[bandedIndex + 2] =
          (short)(arcsinh(val / sigma) * asinhScaleFactor);
      }
    }

    //    long endTime = System.currentTimeMillis();
    //    System.err.println("scale took " + (endTime - startTime) + " ms");

    return new short[][]
      {
        linearResult, logResult, sqrtResult,
        squareResult, histEqResult, asinhResult
      };
  }

  /**
     @param data The image intensity values indexed by y, then x.
     @param width The width of the image.
     @param height The height of the image.
     @param bZero The value of the BZERO field of the FITS header.
     @param bScale The value of the BSCALE field of the FITS header.
     @param min The minimum input data value over which to scale.
     @param max The maximum input data value over which to scale.
     @return An array of short arrays (suitable for use in a
     PixelInterleavedSampleModel with 3 bands) corresponding to linear, log,
     square root, and square scaling of intensities, in that order.
  */
  public static short[][] scaleToUShort(int[][] data, Histogram hist,
                                        int width, int height,
                                        double bZero, double bScale,
                                        double min, double max, double sigma)
  {
    //    long startTime = System.currentTimeMillis();

    int arraySize = width * height * 3;
    short[] linearResult = new short[arraySize];
    short[] logResult = new short[arraySize];
    short[] sqrtResult = new short[arraySize];
    short[] squareResult = new short[arraySize];
    short[] histEqResult = new short[arraySize];
    short[] asinhResult = new short[arraySize];

    double nBins = Math.pow(2.0, 16.0) - 1.0;
    double offset = bZero - min;
    double log10 = Math.log(10.0);

    double linearScaleFactor = nBins / (max - min);
    double sqrtScaleFactor = nBins / Math.sqrt(max - min);
    double squareScaleFactor = nBins / Math.pow(max - min, 2.0);
    double logScaleFactor = nBins / (Math.log(nBins) / log10);
    double asinhScaleFactor = nBins / arcsinh((max - min) / sigma);

    for(int y = 0; y < height; ++y)
    {
      for(int x = 0; x < width; ++x)
      {
        int bandedIndex = (y * width + x) * 3;
        double val = offset + bScale * (double)data[y][x];
        val = Math.max(0.0, Math.min(val, max - min));

        linearResult[bandedIndex] =
          linearResult[bandedIndex + 1] =
          linearResult[bandedIndex + 2] =
          (short)(val * linearScaleFactor);

        squareResult[bandedIndex] =
          squareResult[bandedIndex + 1] =
          squareResult[bandedIndex + 2] =
          (short)(val * val * squareScaleFactor);

        sqrtResult[bandedIndex] =
          sqrtResult[bandedIndex + 1] =
          sqrtResult[bandedIndex + 2] =
          (short)(Math.sqrt(val) * sqrtScaleFactor);

        logResult[bandedIndex] =
          logResult[bandedIndex + 1] =
          logResult[bandedIndex + 2] =
          val <= 0.0 ?
          (short)0 :
          (short)(((Math.log(val * linearScaleFactor)) /
                   log10) * logScaleFactor);

        histEqResult[bandedIndex] =
          histEqResult[bandedIndex + 1] =
          histEqResult[bandedIndex + 2] =
          (short)hist.
          getEqualizedValue((int)(val * linearScaleFactor));

        asinhResult[bandedIndex] =
          asinhResult[bandedIndex + 1] =
          asinhResult[bandedIndex + 2] =
          (short)(arcsinh(val / sigma) * asinhScaleFactor);
      }
    }

    //    long endTime = System.currentTimeMillis();
    //    System.err.println("scale took " + (endTime - startTime) + " ms");

    return new short[][]
      {
        linearResult, logResult, sqrtResult,
        squareResult, histEqResult, asinhResult
      };
  }

  /**
     @param data The image intensity values indexed by y, then x.
     @param width The width of the image.
     @param height The height of the image.
     @param bZero The value of the BZERO field of the FITS header.
     @param bScale The value of the BSCALE field of the FITS header.
     @param min The minimum input data value over which to scale.
     @param max The maximum input data value over which to scale.
     @return An array of short arrays (suitable for use in a
     PixelInterleavedSampleModel with 3 bands) corresponding to linear, log,
     square root, and square scaling of intensities, in that order.
  */
  public static short[][] scaleToUShort(float[][] data, Histogram hist,
                                        int width, int height,
                                        double bZero, double bScale,
                                        double min, double max, double sigma)
  {
    int arraySize = width * height * 3;
    short[] linearResult = new short[arraySize];
    short[] logResult = new short[arraySize];
    short[] sqrtResult = new short[arraySize];
    short[] squareResult = new short[arraySize];
    short[] histEqResult = new short[arraySize];
    short[] asinhResult = new short[arraySize];

    double nBins = Math.pow(2.0, 16.0) - 1.0;
    double offset = bZero - min;
    double log10 = Math.log(10.0);

    double linearScaleFactor = nBins / (max - min);
    double sqrtScaleFactor = nBins / Math.sqrt(max - min);
    double squareScaleFactor = nBins / Math.pow(max - min, 2.0);
    double logScaleFactor = nBins / (Math.log(nBins) / log10);
    double asinhScaleFactor = nBins / arcsinh((max - min) / sigma);

    for(int y = 0; y < height; ++y)
    {
      for(int x = 0; x < width; ++x)
      {
        int bandedIndex = (y * width + x) * 3;
        double val = offset + bScale * (double)data[y][x];
        val = Math.max(0.0, Math.min(val, max - min));

        linearResult[bandedIndex] =
          linearResult[bandedIndex + 1] =
          linearResult[bandedIndex + 2] =
          (short)(val * linearScaleFactor);

        squareResult[bandedIndex] =
          squareResult[bandedIndex + 1] =
          squareResult[bandedIndex + 2] =
          (short)(val * val * squareScaleFactor);

        sqrtResult[bandedIndex] =
          sqrtResult[bandedIndex + 1] =
          sqrtResult[bandedIndex + 2] =
          (short)(Math.sqrt(val) * sqrtScaleFactor);

        logResult[bandedIndex] =
          logResult[bandedIndex + 1] =
          logResult[bandedIndex + 2] =
          val <= 0.0 ?
          (short)0 :
          (short)(((Math.log(val * linearScaleFactor)) /
                   log10) * logScaleFactor);

        histEqResult[bandedIndex] =
          histEqResult[bandedIndex + 1] =
          histEqResult[bandedIndex + 2] =
          (short)hist.getEqualizedValue((int)(val * linearScaleFactor));

        asinhResult[bandedIndex] =
          asinhResult[bandedIndex + 1] =
          asinhResult[bandedIndex + 2] =
          (short)(arcsinh(val / sigma) * asinhScaleFactor);
      }
    }

    return new short[][]
      {
        linearResult, logResult, sqrtResult,
        squareResult, histEqResult, asinhResult
      };
  }

  /**
     @param data The image intensity values indexed by y, then x.
     @param width The width of the image.
     @param height The height of the image.
     @param bZero The value of the BZERO field of the FITS header.
     @param bScale The value of the BSCALE field of the FITS header.
     @param min The minimum input data value over which to scale.
     @param max The maximum input data value over which to scale.
     @return An array of short arrays (suitable for use in a
     PixelInterleavedSampleModel with 3 bands) corresponding to linear, log,
     square root, and square scaling of intensities, in that order.
  */
  public static short[][] scaleToUShort(double[][] data, Histogram hist,
                                        int width, int height,
                                        double bZero, double bScale,
                                        double min, double max, double sigma)
  {
    int arraySize = width * height * 3;
    short[] linearResult = new short[arraySize];
    short[] logResult = new short[arraySize];
    short[] sqrtResult = new short[arraySize];
    short[] squareResult = new short[arraySize];
    short[] histEqResult = new short[arraySize];
    short[] asinhResult = new short[arraySize];    
    double nBinsForGrahic = 256;//Bug fixes by JCM - number of values of PNG, JPEG.
    double offset = bZero - min; 
    double log10 = Math.log(10.0);

    double linearScaleFactor = nBinsForGrahic / (max - min);
    double sqrtScaleFactor = nBinsForGrahic / Math.sqrt(max - min);
    double squareScaleFactor = nBinsForGrahic / Math.pow(max - min, 2.0);
    double logScaleFactor = nBinsForGrahic / (Math.log(nBinsForGrahic) / log10);
    double asinhScaleFactor = nBinsForGrahic / arcsinh((max - min) / sigma);
    
    for(int y = 0; y < height; ++y)
    {
      for(int x = 0; x < width; ++x)
      {                                       
        int bandedIndex = (y * width + x) * 3;
        double val = offset + bScale * (double)data[y][x];
        if (val > max)
            val = max;
        if (val < min)
            val = min;
        val = Math.max(0.0, Math.min(val, max - min));

        linearResult[bandedIndex] =
          linearResult[bandedIndex + 1] =
          linearResult[bandedIndex + 2] =
          (short)(val * linearScaleFactor);

        squareResult[bandedIndex] =
          squareResult[bandedIndex + 1] =
          squareResult[bandedIndex + 2] =
          (short)(val * val * squareScaleFactor);

        sqrtResult[bandedIndex] =
          sqrtResult[bandedIndex + 1] =
          sqrtResult[bandedIndex + 2] =
          (short)(Math.sqrt(val) * sqrtScaleFactor);

        logResult[bandedIndex] =
          logResult[bandedIndex + 1] =
          logResult[bandedIndex + 2] =
          val <= 0.0 ?
          (short)0 :
          (short)(((Math.log(val * linearScaleFactor)) /
                   log10) * logScaleFactor);

        histEqResult[bandedIndex] =
          histEqResult[bandedIndex + 1] =
          histEqResult[bandedIndex + 2] =
          (short)hist.
          getEqualizedValue((int)(val * linearScaleFactor));

        asinhResult[bandedIndex] =
          asinhResult[bandedIndex + 1] =
          asinhResult[bandedIndex + 2] =
          (short)(arcsinh(val / sigma) * asinhScaleFactor);
      }
    }

    return new short[][]
      {
        linearResult, logResult, sqrtResult,
        squareResult, histEqResult, asinhResult
      };
  }

  /**
     Finds the minimum and maximum data values in <CODE>data</CODE>.
   */
  public static Histogram computeHistogram(byte[][] data,
                                           double bZero, double bScale)
  {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    double nBins = Math.pow(2.0, 16.0) - 1.0;
    int totalCount = 0;
    int[] counts = new int[(int)(nBins + 1)];
    Arrays.fill(counts, 0);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        int dataVal = data[i][j];
        if(dataVal < 0)
        {
          dataVal += 256;
        }
        double val = bZero + bScale * dataVal;

        if(val > max)
        {
          max = val;
        }
        if(val < min)
        {
          min = val;
        }
      }
    }

    double offset = bZero - min;
    double linearScaleFactor = nBins / (max - min);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        int dataVal = data[i][j];
        if(dataVal < 0)
        {
          dataVal += 256;
        }

        ++totalCount;
        counts[(int)((offset + bScale * dataVal) * linearScaleFactor)] += 1;
      }
    }

    return new Histogram(min, max, totalCount, counts);
  }

  /**
     Finds the minimum and maximum data values in <CODE>data</CODE>.
   */
  public static Histogram computeHistogram(short[][] data,
                                           double bZero, double bScale)
  {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    double nBins = Math.pow(2.0, 16.0) - 1.0;
    int totalCount = 0;
    int[] counts = new int[(int)(nBins + 1)];
    Arrays.fill(counts, 0);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        double val = bZero + bScale * (double)data[i][j];

        if(val > max)
        {
          max = val;
        }
        if(val < min)
        {
          min = val;
        }
      }
    }

    double offset = bZero - min;
    double linearScaleFactor = nBins / (max - min);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        double val = offset + bScale * (double)data[i][j];
        val = Math.max(0.0, Math.min(val, max - min));

        ++totalCount;
        counts[(int)(val * linearScaleFactor)] += 1;
      }
    }

    return new Histogram(min, max, totalCount, counts);
  }

  /**
     Finds the minimum and maximum data values in <CODE>data</CODE>.
   */
  public static Histogram computeHistogram(int[][] data,
                                           double bZero, double bScale)
  {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    double nBins = Math.pow(2.0, 16.0) - 1.0;
    int totalCount = 0;
    int[] counts = new int[(int)(nBins + 1)];
    Arrays.fill(counts, 0);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        double val = bZero + bScale * (double)data[i][j];

        if(val > max)
        {
          max = val;
        }
        if(val < min)
        {
          min = val;
        }
      }
    }

    double offset = bZero - min;
    double linearScaleFactor = nBins / (max - min);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        double val = offset + bScale * (double)data[i][j];
        val = Math.max(0.0, Math.min(val, max - min));

        ++totalCount;
        counts[(int)(val * linearScaleFactor)] += 1;
      }
    }

    return new Histogram(min, max, totalCount, counts);
  }

  /**
     Finds the minimum and maximum data values in <CODE>data</CODE>.
   */
  public static Histogram computeHistogram(float[][] data,
                                           double bZero, double bScale)
  {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    double nBins = Math.pow(2.0, 16.0) - 1.0;
    int totalCount = 0;
    int[] counts = new int[(int)(nBins + 1)];
    Arrays.fill(counts, 0);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        double val = bZero + bScale * (double)data[i][j];

        if(val > max)
        {
          max = val;
        }
        if(val < min)
        {
          min = val;
        }
      }
    }

    double offset = bZero - min;
    double linearScaleFactor = nBins / (max - min);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        double val = offset + bScale * (double)data[i][j];
        val = Math.max(0.0, Math.min(val, max - min));

        ++totalCount;
        counts[(int)(val * linearScaleFactor)] += 1;
      }
    }

    return new Histogram(min, max, totalCount, counts);
  }

  /**
     Finds the minimum and maximum data values in <CODE>data</CODE>.
   */
  public static Histogram computeHistogram(double[][] data,
                                           double bZero, double bScale)
  {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;
    double nBins = Math.pow(2.0, 16.0) - 1.0;
    int totalCount = 0;
    int[] counts = new int[(int)(nBins + 1)];
    Arrays.fill(counts, 0);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        double val = bZero + bScale * data[i][j];
        
        if(val > max)
        {
          max = val;
        }
        if(val < min)
        {
          min = val;
        }
      }
    }

    double offset = bZero - min;
    double linearScaleFactor = nBins / (max - min);

    for(int i = 0; i < data.length; ++i)
    {
      for(int j = 0; j < data[i].length; ++j)
      {
        double val = offset + bScale * (double)data[i][j];
        val = Math.max(0.0, Math.min(val, max - min));

        ++totalCount;
        counts[(int)(val * linearScaleFactor)] += 1;
      }
    }

    return new Histogram(min, max, totalCount, counts);
  }

  /**
     @return CVS Revision number.
  */
  public static String revision()
  {
    return "$Revision: 1.14 $";
  }

  protected static double[] createSqrtMap(int size)
  {
    double[] result = new double[size];

    for(int i = 0; i < size; ++i)
    {
      result[i] = Math.sqrt(i);
    }

    return result;
  }

  protected static double[] createLogMap(int size)
  {
    double[] result = new double[size];

    for(int i = 0; i < result.length; ++i)
    {
      result[i] = i == 0 ? 0.0 : Math.log(i);
    }

    return result;
  }

  protected static final double[] BYTE_SQRT_MAP =
    createSqrtMap((int)Math.pow(2.0, 8.0));
  protected static final double[] BYTE_LOG_MAP =
    createLogMap((int)Math.pow(2.0, 8.0));
  protected static final double[] SHORT_SQRT_MAP =
    createSqrtMap((int)Math.pow(2.0, 16.0));
  protected static final double[] SHORT_LOG_MAP =
    createLogMap((int)Math.pow(2.0, 16.0));
}

/**
   Revision History
   ================

   $Log: ScaleUtils.java,v $
   Revision 1.14  2004/07/23 18:52:35  carliles
   SlowFITSImage is done.

   Revision 1.13  2004/07/22 22:29:08  carliles
   Added "low" memory consumption SlowFITSImage.

   Revision 1.12  2004/07/21 18:03:56  carliles
   Added asinh with sigma estimation.

   Revision 1.11  2004/07/16 02:48:53  carliles
   Hist EQ doesn't look quite right, but there's nothing to compare it to, and the
   math looks right.

   Revision 1.10  2004/07/14 02:40:49  carliles
   Scaling should be done once and for all, with all possible accelerations.  Now
   just have to add hist eq and asinh.

   Revision 1.9  2004/07/09 02:22:31  carliles
   Added log/sqrt maps, fixed wrong output for byte images (again).

   Revision 1.8  2004/06/21 05:38:39  carliles
   Got rescale lookup acceleration working for short images.  Also in theory for
   int images, though I can't test because of dynamic range of my int image.

   Revision 1.7  2004/06/17 01:05:05  carliles
   Fixed some image orientation shit.  Added getOriginalValue method to FITSImage.

   Revision 1.6  2004/06/07 21:14:06  carliles
   Rescale works nicely for all types now.

   Revision 1.5  2004/06/04 01:01:36  carliles
   Got rid of some overmodelling.

   Revision 1.4  2004/06/02 22:17:37  carliles
   Got the hang of cut levels.  Need to implement widely and as efficiently as
   possible.

   Revision 1.3  2004/06/02 19:39:36  carliles
   Adding histogram crap.

   Revision 1.2  2004/05/27 18:45:43  carliles
   Fixed scaling from byte to short (had to offset by 128 because of signed bytes
   in Java).

   Revision 1.1  2004/05/26 16:56:11  carliles
   Initial checkin of separate FITS package.

   Revision 1.12  2003/08/19 19:12:30  carliles
*/
