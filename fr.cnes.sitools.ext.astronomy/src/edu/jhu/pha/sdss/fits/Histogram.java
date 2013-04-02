package edu.jhu.pha.sdss.fits;


/**
   <PRE>
   Current Version
   ===============
   ID:            $Id: Histogram.java,v 1.12 2004/07/23 21:54:11 carliles Exp $
   Revision:      $Revision: 1.12 $
   Date/time:     $Date: 2004/07/23 21:54:11 $
   </PRE>
*/
public class Histogram
{
  public Histogram(double min, double max, int totalCount, int[] counts)
  {
    if(max < min)
    {
      throw new IllegalArgumentException("max is less than min: max = " + max +
                                         " min = " + min);
    }

    setMin(min);
    setMax(max);
    setRange(max - min);
    setTotalCount(totalCount);
    setCounts(counts);
    _cdf = calculateCdf(counts, totalCount);
  }

  public double getMin()
  {
    return _min;
  }

  public double getMax()
  {
    return _max;
  }

  public double getRange()
  {
    return _range;
  }

  /*
  public int getCount(short value)
  {
    return _counts[value - Short.MIN_VALUE];
  }
  */

  public int getTotalCount()
  {
    return _totalCount;
  }

  public int[] getCounts()
  {
    return _counts;
  }

  /**
     @return A histogram equalized value for <CODE>val</CODE>.  <CODE>val</CODE>
     must be in the range [0, 65535], and is interpreted to be a linear scaling
     of an original data value.
   */
  public int getEqualizedValue(int val)
  {
    double v = _cdf[val];
    double vMin = _cdf[0];

    return
      (int)(((v - vMin) / (1.0 - vMin)) * (double)(_counts.length - 1) + 0.5);
  }

  /**
     @return A reasonable guess at a good sigma value for the inverse hyperbolic
     sine scaling.  The estimate is made by finding the mode value of the
     histogram, then moving to the next bin and returning the value (in the
     original data range with bZero and bScale applied) at the "bottom" of that
     bin.  The current implementation is actually a mangled implementation which
     happens to produce better sigmas than the correct implementation.  An
     explanation for this behavior is currently being investigated.
   */
  public double estimateSigma()
  {
    double modeIndex = (double)findModeIndex(_counts);
    double linearScaleFactor = (Math.pow(2.0, 16.0) - 1.0) / (_max - _min);

    return (modeIndex + 1.0) / linearScaleFactor;
  }

  protected static int findModeIndex(int[] counts)
  {
    int index = 0;

    for(int i = 0; i < counts.length; ++i)
    {
      if(counts[i] > counts[index])
      {
        index = i;
      }
    }

    return index;
  }

  /**
     @return An instance of <CODE>Bounds</CODE> containing the min and max
     values that will result in keeping <CODE>percentKept</CODE> percent of the
     values in the image.  <CODE>percentKept</CODE> is a slightly deceptive
     name, as it must be in the range [0, 1].
   */
  public Bounds calculateBounds(double percentKept)
    throws IllegalArgumentException
  {
    if(percentKept < 0.0 || percentKept > 1.0)
    {
      throw new IllegalArgumentException("percentKept must be in [0 ... 1]");
    }

    double halfPercentDiscarded = (1.0 - percentKept) / 2.0;

    int lowIndex = 0;
    double lowCount = 0.0;
    double totalCount = (double)getTotalCount();
    boolean done = false;

    while(!done && lowIndex < _counts.length &&
          (lowCount / totalCount < halfPercentDiscarded))
    {
      lowCount += (double)_counts[lowIndex];

      if(lowCount / totalCount < halfPercentDiscarded)
      {
        ++lowIndex;
      }
      else
      {
        done = true;
      }
    }

    int highIndex = getCounts().length - 1;
    double highCount = 0.0;
    done = false;

    while(!done && highIndex >= 0 &&
          (highCount / totalCount < halfPercentDiscarded))
    {
      highCount += (double)_counts[highIndex];

      if(highCount / totalCount < halfPercentDiscarded)
      {
        --highIndex;
      }
      else
      {
        done = true;
      }
    }

    return new Bounds(lowIndex, highIndex);
  }

  /**
     @return CVS Revision number.
  */
  public static String revision()
  {
    return "$Revision: 1.12 $";
  }

  /**
     A container class used by the <CODE>calculateBounds</CODE> method.
   */
  public static class Bounds
  {
    public Bounds(double low, double high)
    {
      this.low = low;
      this.high = high;
    }

    public String toString()
    {
      return getClass().getName() + " low = " + low + " high = " + high;
    }

    public double low;
    public double high;
  }

  protected static double[] calculateCdf(int[] counts, int totalCount)
  {
    double[] result = new double[counts.length];
    int cumulativeCount = 0;

    for(int i = 0; i < result.length; ++i)
    {
      cumulativeCount += counts[i];
      result[i] = (double)cumulativeCount / (double)totalCount;
    }

    return result;
  }

  protected void setMin(double min)
  {
    _min = min;
  }

  protected void setMax(double max)
  {
    _max = max;
  }

  protected void setRange(double range)
  {
    _range = range;
  }

  protected void setTotalCount(int totalCount)
  {
    _totalCount = totalCount;
  }

  protected void setCounts(int[] counts)
  {
    _counts = counts;
  }

  protected double _min;
  protected double _max;
  protected double _range;
  protected int[] _counts;
  protected double[] _cdf;
  protected int _totalCount;
}

/**
   Revision History
   ================

   $Log: Histogram.java,v $
   Revision 1.12  2004/07/23 21:54:11  carliles
   Added javadocs.

   Revision 1.11  2004/07/21 18:03:55  carliles
   Added asinh with sigma estimation.

   Revision 1.10  2004/07/16 02:48:53  carliles
   Hist EQ doesn't look quite right, but there's nothing to compare it to, and the
   math looks right.

   Revision 1.9  2004/07/14 02:40:49  carliles
   Scaling should be done once and for all, with all possible accelerations.  Now
   just have to add hist eq and asinh.

   Revision 1.8  2004/07/09 02:22:31  carliles
   Added log/sqrt maps, fixed wrong output for byte images (again).

   Revision 1.7  2004/06/21 05:38:39  carliles
   Got rescale lookup acceleration working for short images.  Also in theory for
   int images, though I can't test because of dynamic range of my int image.

   Revision 1.6  2004/06/08 17:28:17  carliles
   A little more refactoring.

   Revision 1.5  2004/06/07 23:00:54  carliles
   Moved AffineTransform construction into Histogram.

   Revision 1.4  2004/06/04 03:00:28  carliles
   Fixed histogram bound calculation.

   Revision 1.3  2004/06/04 01:01:36  carliles
   Got rid of some overmodelling.

   Revision 1.2  2004/06/02 22:17:37  carliles
   Got the hang of cut levels.  Need to implement widely and as efficiently as
   possible.

   Revision 1.1  2004/06/02 19:39:36  carliles
   Adding histogram crap.

*/
