package edu.jhu.pha.sdss.fits;


/**
   <PRE>
   Current Version
   ===============
   ID:            $Id: SlowScaleUtils.java,v 1.1 2004/07/23 18:52:35 carliles Exp $
   Revision:      $Revision: 1.1 $
   Date/time:     $Date: 2004/07/23 18:52:35 $
   </PRE>

   Array operations that should be implemented in one method, but which
   have to be reimplemented for each primitive data type just to have
   the right type in the damn parameter list.
*/
public class SlowScaleUtils extends ScaleUtils
{
  public static final Scaler BYTE_SCALER = new ByteScaler();
  public static final Scaler SHORT_SCALER = new ShortScaler();
  public static final Scaler INT_SCALER = new IntScaler();
  public static final Scaler FLOAT_SCALER = new FloatScaler();
  public static final Scaler DOUBLE_SCALER = new DoubleScaler();

  public static short[] scale(Object data, short[] result,
                              int width, int height,
                              double bZero, double bScale,
                              double min, double max,
                              double sigma, Histogram hist,
                              int scaleMethod)
  {
    long startTime = System.currentTimeMillis();

    switch(scaleMethod)
    {
    case LOG:
      result = getScaler(data).logScaleToUShort(data, result,
                                                width, height,
                                                bZero, bScale,
                                                min, max);
      break;
    case SQUARE_ROOT:
      result = getScaler(data).sqrtScaleToUShort(data, result,
                                                 width, height,
                                                 bZero, bScale,
                                                 min, max);
      break;
    case SQUARE:
      result = getScaler(data).squareScaleToUShort(data, result,
                                                   width, height,
                                                   bZero, bScale,
                                                   min, max);
      break;
    case HIST_EQ:
      result = getScaler(data).histEQScaleToUShort(data, result,
                                                   width, height,
                                                   bZero, bScale,
                                                   min, max, hist);
      break;
    case ASINH:
      result = getScaler(data).asinhScaleToUShort(data, result,
                                                  width, height,
                                                  bZero, bScale,
                                                  min, max, sigma);
      break;
    case LINEAR:
    default:
      result = getScaler(data).linearScaleToUShort(data, result,
                                                   width, height,
                                                   bZero, bScale,
                                                   min, max);
      break;
    }

    long endTime = System.currentTimeMillis();
    if(System.getProperty("debug", "").equals("on"))
    {
      System.err.println("scale took " + (endTime - startTime) + " ms");
    }

    return result;
  }

  public static Scaler getScaler(Object data)
  {
    Scaler result = null;

    if(data instanceof byte[][])
    {
      result = BYTE_SCALER;
    }
    else if(data instanceof short[][])
    {
      result = SHORT_SCALER;
    }
    else if(data instanceof int[][])
    {
      result = INT_SCALER;
    }
    else if(data instanceof float[][])
    {
      result = FLOAT_SCALER;
    }
    else if(data instanceof double[][])
    {
      result = DOUBLE_SCALER;
    }

    return result;
  }

  public static interface Scaler
  {
    public short[] linearScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max);
    public short[] logScaleToUShort(Object data, short[] result,
                                    int width, int height,
                                    double bZero, double bScale,
                                    double min, double max);
    public short[] sqrtScaleToUShort(Object data, short[] result,
                                     int width, int height,
                                     double bZero, double bScale,
                                     double min, double max);
    public short[] squareScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max);
    public short[] histEQScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max,
                                       Histogram hist);
    public short[] asinhScaleToUShort(Object data, short[] result,
                                      int width, int height,
                                      double bZero, double bScale,
                                      double min, double max,
                                      double sigma);
  }

  public static class ByteScaler implements Scaler
  {
    public short[] linearScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      byte[][] vals = (byte[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          int shifted = vals[y][x];
          if(shifted < 0)
          {
            shifted += 256;
          }
          double val =
            Math.max(0.0, Math.min(offset + bScale * (double)shifted, uBound));

          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(val * linearScaleFactor);
        }
      }

      return result;
    }

    public short[] logScaleToUShort(Object data, short[] result,
                                    int width, int height,
                                    double bZero, double bScale,
                                    double min, double max)
    {
      int arraySize = width * height * 3;
      double log10 = Math.log(10.0);
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double logScaleFactor = nBins / (Math.log(nBins) / log10);
      double logLinearScaleFactor = Math.log(nBins / uBound);
      double offset = bZero - min;
      byte[][] vals = (byte[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      int minIndex = (int)Math.rint((min - bZero) / bScale);
      int maxIndex = (int)Math.rint((max - bZero) / bScale);
      double logBscale = Math.log(bScale);
      double logMax = Math.log(uBound);

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          int shifted = vals[y][x];
          if(shifted < 0)
          {
            shifted += 256;
          }
          double val =
            Math.max(0.0, Math.min(offset + bScale * (double)shifted, uBound));
          int mapIndex =
            Math.max(minIndex, Math.min(maxIndex, shifted)) - minIndex;
          double logVal =
            Math.max(0.0, Math.min(logMax, logBscale + BYTE_LOG_MAP[mapIndex]));

          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            val <= 0.0 ?
            (short)0 :
            (short)(((logVal + logLinearScaleFactor) / log10) * logScaleFactor);
        }
      }

      return result;
    }

    public short[] sqrtScaleToUShort(Object data, short[] result,
                                     int width, int height,
                                     double bZero, double bScale,
                                     double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double rootMax = Math.sqrt(uBound);
      double sqrtScaleFactor = nBins / rootMax;
      double offset = bZero - min;
      byte[][] vals = (byte[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      int minIndex = (int)Math.rint((min - bZero) / bScale);
      int maxIndex = (int)Math.rint((max - bZero) / bScale);
      double rootBscale = Math.sqrt(bScale);

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          int shifted = vals[y][x];
          if(shifted < 0)
          {
            shifted += 256;
          }
          double val =
            Math.max(0.0, Math.min(offset + bScale * (double)shifted, uBound));
          int mapIndex =
            Math.max(minIndex, Math.min(maxIndex, shifted)) - minIndex;
          double sqrtVal =
            Math.max(0.0, Math.min(rootMax,
                                   rootBscale * BYTE_SQRT_MAP[mapIndex]));
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(sqrtVal * sqrtScaleFactor);
        }
      }

      return result;
    }

    public short[] squareScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double squareScaleFactor = nBins / Math.pow(uBound, 2.0);
      double offset = bZero - min;
      byte[][] vals = (byte[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          int shifted = vals[y][x];
          if(shifted < 0)
          {
            shifted += 256;
          }
          double val =
            Math.max(0.0, Math.min(offset + bScale * (double)shifted, uBound));

          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(val * val * squareScaleFactor);
        }
      }

      return result;
    }

    public short[] histEQScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max,
                                       Histogram hist)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      byte[][] vals = (byte[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int shifted = vals[y][x];
          if(shifted < 0)
          {
            shifted += 256;
          }
          double val =
            Math.max(0.0, Math.min(offset + bScale * (double)shifted, uBound));

          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)hist.
            getEqualizedValue((int)(val * linearScaleFactor));
        }
      }

      return result;
    }

    public short[] asinhScaleToUShort(Object data, short[] result,
                                      int width, int height,
                                      double bZero, double bScale,
                                      double min, double max,
                                      double sigma)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor = nBins / uBound;
      double asinhScaleFactor = nBins / arcsinh(uBound / sigma);

      double offset = bZero - min;
      byte[][] vals = (byte[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int shifted = vals[y][x];
          if(shifted < 0)
          {
            shifted += 256;
          }
          double val =
            Math.max(0.0, Math.min(offset + bScale * (double)shifted, uBound));

          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(arcsinh(val / sigma) * asinhScaleFactor);
        }
      }

      return result;
    }
  }

  public static class ShortScaler implements Scaler
  {
    public short[] linearScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      short[][] vals = (short[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.max(0.0,
                             Math.min(offset + bScale * (double)vals[y][x],
                                      uBound)) *
                    linearScaleFactor);
        }
      }

      return result;
    }

    public short[] logScaleToUShort(Object data, short[] result,
                                    int width, int height,
                                    double bZero, double bScale,
                                    double min, double max)
    {
      int arraySize = width * height * 3;
      double log10 = Math.log(10.0);
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double logScaleFactor = nBins / (Math.log(nBins) / log10);
      double logLinearScaleFactor = Math.log(nBins / uBound);
      double offset = bZero - min;
      short[][] vals = (short[][])data;
      int minIndex = (int)Math.rint((min - bZero) / bScale);
      int maxIndex = (int)Math.rint((max - bZero) / bScale);
      double logBscale = Math.log(bScale);
      double logMax = Math.log(uBound);

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          double val =
            Math.max(0.0, Math.min(offset + bScale * (double)vals[y][x],
                                   uBound));
          int mapIndex =
            Math.max(minIndex, Math.min(maxIndex, vals[y][x])) - minIndex;
          double logVal =
            Math.max(0.0,
                     Math.min(logMax, logBscale + SHORT_LOG_MAP[mapIndex]));

          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            val <= 0.0 ?
            (short)0 :
            (short)(((logVal + logLinearScaleFactor) / log10) * logScaleFactor);
        }
      }

      return result;
    }

    public short[] sqrtScaleToUShort(Object data, short[] result,
                                     int width, int height,
                                     double bZero, double bScale,
                                     double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double sqrtScaleFactor = nBins / Math.sqrt(uBound);
      double offset = bZero - min;
      short[][] vals = (short[][])data;
      int minIndex = (int)Math.rint((min - bZero) / bScale);
      int maxIndex = (int)Math.rint((max - bZero) / bScale);
      double rootBscale = Math.sqrt(bScale);
      double rootMax = Math.sqrt(uBound);

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          double val =
            Math.max(0.0, Math.min(offset + bScale * (double)vals[y][x],
                                   uBound));
          int mapIndex =
            Math.max(minIndex, Math.min(maxIndex, vals[y][x])) - minIndex;
          double sqrtVal =
            Math.max(0.0, Math.min(rootMax,
                                   rootBscale * SHORT_SQRT_MAP[mapIndex]));
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(sqrtVal * sqrtScaleFactor);
        }
      }

      return result;
    }

    public short[] squareScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double squareScaleFactor = nBins / Math.pow(uBound, 2.0);
      double offset = bZero - min;
      short[][] vals = (short[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.
                    pow(Math.
                        max(0.0,
                            Math.min(offset + bScale * (double)vals[y][x],
                                     uBound)),
                        2.0) *
                    squareScaleFactor);
        }
      }

      return result;
    }

    public short[] histEQScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max,
                                       Histogram hist)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      short[][] vals = (short[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          double val = Math.max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound));
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)hist.
            getEqualizedValue((int)(val * linearScaleFactor));
        }
      }

      return result;
    }

    public short[] asinhScaleToUShort(Object data, short[] result,
                                      int width, int height,
                                      double bZero, double bScale,
                                      double min, double max,
                                      double sigma)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor = nBins / uBound;
      double asinhScaleFactor = nBins / arcsinh(uBound / sigma);

      double offset = bZero - min;
      short[][] vals = (short[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(arcsinh(Math.
                            max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound)) / sigma) * asinhScaleFactor);
        }
      }

      return result;
    }
  }

  public static class IntScaler implements Scaler
  {
    public short[] linearScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      int[][] vals = (int[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.max(0.0,
                             Math.min(offset + bScale * (double)vals[y][x],
                                      uBound)) *
                    linearScaleFactor);
        }
      }

      return result;
    }

    public short[] logScaleToUShort(Object data, short[] result,
                                    int width, int height,
                                    double bZero, double bScale,
                                    double min, double max)
    {
      int arraySize = width * height * 3;
      double log10 = Math.log(10.0);
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double logScaleFactor = nBins / (Math.log(nBins) / log10);
      double offset = bZero - min;
      int[][] vals = (int[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          double val = Math.max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound));
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            val <= 0.0 ?
            (short)0 :
            (short)(((Math.log(val * linearScaleFactor)) /
                     log10) * logScaleFactor);
        }
      }

      return result;
    }

    public short[] sqrtScaleToUShort(Object data, short[] result,
                                     int width, int height,
                                     double bZero, double bScale,
                                     double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double sqrtScaleFactor = nBins / Math.sqrt(uBound);
      double offset = bZero - min;
      int[][] vals = (int[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.
                    sqrt(Math.
                         max(0.0,
                             Math.min(offset + bScale * (double)vals[y][x],
                                      uBound))) *
                    sqrtScaleFactor);
        }
      }

      return result;
    }

    public short[] squareScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double squareScaleFactor = nBins / Math.pow(max - min, 2.0);
      double offset = bZero - min;
      int[][] vals = (int[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.
                    pow(Math.
                        max(0.0,
                            Math.min(offset + bScale * (double)vals[y][x],
                                     uBound)),
                        2.0) *
                    squareScaleFactor);
        }
      }

      return result;
    }

    public short[] histEQScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max,
                                       Histogram hist)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      int[][] vals = (int[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          double val = Math.max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound));
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)hist.
            getEqualizedValue((int)(val * linearScaleFactor));
        }
      }

      return result;
    }

    public short[] asinhScaleToUShort(Object data, short[] result,
                                      int width, int height,
                                      double bZero, double bScale,
                                      double min, double max,
                                      double sigma)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor = nBins / uBound;
      double asinhScaleFactor = nBins / arcsinh(uBound / sigma);

      double offset = bZero - min;
      int[][] vals = (int[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(arcsinh(Math.
                            max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound)) / sigma) * asinhScaleFactor);
        }
      }

      return result;
    }
  }

  public static class FloatScaler implements Scaler
  {
    public short[] linearScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      float[][] vals = (float[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.max(0.0,
                             Math.min(offset + bScale * (double)vals[y][x],
                                      uBound)) *
                    linearScaleFactor);
        }
      }

      return result;
    }

    public short[] logScaleToUShort(Object data, short[] result,
                                    int width, int height,
                                    double bZero, double bScale,
                                    double min, double max)
    {
      int arraySize = width * height * 3;
      double log10 = Math.log(10.0);
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double logScaleFactor = nBins / (Math.log(nBins) / log10);
      double offset = bZero - min;
      float[][] vals = (float[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          double val = Math.max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound));
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            val <= 0.0 ?
            (short)0 :
            (short)(((Math.log(val * linearScaleFactor)) /
                     log10) * logScaleFactor);
        }
      }

      return result;
    }

    public short[] sqrtScaleToUShort(Object data, short[] result,
                                     int width, int height,
                                     double bZero, double bScale,
                                     double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double sqrtScaleFactor = nBins / Math.sqrt(uBound);
      double offset = bZero - min;
      float[][] vals = (float[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.
                    sqrt(Math.
                         max(0.0,
                             Math.min(offset + bScale * (double)vals[y][x],
                                      uBound))) *
                    sqrtScaleFactor);
        }
      }

      return result;
    }

    public short[] squareScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double squareScaleFactor = nBins / Math.pow(max - min, 2.0);
      double offset = bZero - min;
      float[][] vals = (float[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.
                    pow(Math.
                        max(0.0,
                            Math.min(offset + bScale * (double)vals[y][x],
                                     uBound)),
                        2.0) *
                    squareScaleFactor);
        }
      }

      return result;
    }

    public short[] histEQScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max,
                                       Histogram hist)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      float[][] vals = (float[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          double val = Math.max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound));
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)hist.
            getEqualizedValue((int)(val * linearScaleFactor));
        }
      }

      return result;
    }

    public short[] asinhScaleToUShort(Object data, short[] result,
                                      int width, int height,
                                      double bZero, double bScale,
                                      double min, double max,
                                      double sigma)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor = nBins / uBound;
      double asinhScaleFactor = nBins / arcsinh(uBound / sigma);
      double offset = bZero - min;
      float[][] vals = (float[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(arcsinh(Math.
                            max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound)) / sigma) * asinhScaleFactor);
        }
      }

      return result;
    }
  }

  public static class DoubleScaler implements Scaler
  {
    public short[] linearScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      double[][] vals = (double[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.max(0.0,
                             Math.min(offset + bScale * (double)vals[y][x],
                                      uBound)) *
                    linearScaleFactor);
        }
      }

      return result;
    }

    public short[] logScaleToUShort(Object data, short[] result,
                                    int width, int height,
                                    double bZero, double bScale,
                                    double min, double max)
    {
      int arraySize = width * height * 3;
      double log10 = Math.log(10.0);
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double logScaleFactor = nBins / (Math.log(nBins) / log10);
      double offset = bZero - min;
      double[][] vals = (double[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          double val = Math.max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound));
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            val <= 0.0 ?
            (short)0 :
            (short)(((Math.log(val * linearScaleFactor)) /
                     log10) * logScaleFactor);
        }
      }

      return result;
    }

    public short[] sqrtScaleToUShort(Object data, short[] result,
                                     int width, int height,
                                     double bZero, double bScale,
                                     double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double sqrtScaleFactor = nBins / Math.sqrt(uBound);
      double offset = bZero - min;
      double[][] vals = (double[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.
                    sqrt(Math.
                         max(0.0,
                             Math.min(offset + bScale * (double)vals[y][x],
                                      uBound))) *
                    sqrtScaleFactor);
        }
      }

      return result;
    }

    public short[] squareScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor =  nBins / uBound;
      double squareScaleFactor = nBins / Math.pow(max - min, 2.0);
      double offset = bZero - min;
      double[][] vals = (double[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(Math.
                    pow(Math.
                        max(0.0,
                            Math.min(offset + bScale * (double)vals[y][x],
                                     uBound)),
                        2.0) *
                    squareScaleFactor);
        }
      }

      return result;
    }

    public short[] histEQScaleToUShort(Object data, short[] result,
                                       int width, int height,
                                       double bZero, double bScale,
                                       double min, double max,
                                       Histogram hist)
    {
      int arraySize = width * height * 3;
      double uBound = max - min;
      double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / uBound;
      double offset = bZero - min;
      double[][] vals = (double[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          double val = Math.max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound));
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)hist.
            getEqualizedValue((int)(val * linearScaleFactor));
        }
      }

      return result;
    }

    public short[] asinhScaleToUShort(Object data, short[] result,
                                      int width, int height,
                                      double bZero, double bScale,
                                      double min, double max,
                                      double sigma)
    {
      int arraySize = width * height * 3;
      double nBins = Math.pow(2.0, 16.0) - 1.0;
      double uBound = max - min;
      double linearScaleFactor = nBins / uBound;
      double asinhScaleFactor = nBins / arcsinh(uBound / sigma);

      double offset = bZero - min;
      double[][] vals = (double[][])data;

      if(result == null || result.length != arraySize)
      {
        result = new short[arraySize];
      }

      for(int y = 0; y < height; ++y)
      {
        for(int x = 0; x < width; ++x)
        {
          int bandedIndex = (y * width + x) * 3;
          result[bandedIndex] =
            result[bandedIndex + 1] =
            result[bandedIndex + 2] =
            (short)(arcsinh(Math.
                            max(0.0,
                                Math.min(offset + bScale * (double)vals[y][x],
                                         uBound)) / sigma) * asinhScaleFactor);
        }
      }

      return result;
    }
  }

  /**
     @return CVS Revision number.
  */
  public static String revision()
  {
    return "$Revision: 1.1 $";
  }
}
/**
   Revision History
   ================

   $Log: SlowScaleUtils.java,v $
   Revision 1.1  2004/07/23 18:52:35  carliles
   SlowFITSImage is done.

*/
