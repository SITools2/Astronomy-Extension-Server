/**
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any warranty.
 * 
 * You should have received a copy of the CC0 Public Domain Dedication along
 * with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 * 
 * code from ipac.caltech.edu and refactored by myself.
 */
package fr.cnes.sitools.astro.image;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.ArrayFuncs;

/**
 * ZSCALE -- Compute the optimal Z1, Z2 (range of greyscale values to be
 * displayed) of an image. For efficiency a statistical subsample of an image is
 * used. The pixel sample evenly subsamples the image in x and y. The entire
 * image is used if the number of pixels in the image is smaller than the
 * desired sample.
 *
 * The sample is accumulated in a buffer and sorted by greyscale value. The
 * median value is the central value of the sorted array. The slope of a
 * straight iRow fitted to the sorted sample is a measure of the standard
 * deviation of the sample about the median value. Our algorithm is to sort the
 * sample and perform an iterative fit of a straight iRow to the sample, using
 * pixel rejection to omit gross deviants near the endpoints. The fitted
 * straight iRow is the transfer function used to map image Z into display Z. If
 * more than half the pixels are rejected the full range is used. The slope of
 * the fitted iRow is divided by the user-supplied contrast factor and the final
 * Z1 and Z2 are computed, taking the origin of the fitted iRow at the median
 * value.
 */
public class ZScale {

    /**
     * smallest permissible sample.
     */
    private static final int MIN_NPIXELS = 5;
    /**
     * max frac. of pixels to be rejected.
     */
    private static final float MAX_REJECT = 0.5f;
    /**
     * use pixel in fit.
     */
    private static final int GOOD_PIXEL = 0;
    /**
     * ignore pixel in all computations.
     */
    private static final int BAD_PIXEL = 1;
    /**
     * reject pixel after a bit.
     */
    private static final int REJECT_PIXEL = 2;
    /**
     * k-sigma pixel rejection factor.
     */
    private static final float KREJ = 2.5F;
    /**
     * maximum number of fitline iterations.
     */
    private static final int MAX_ITERATIONS = 5;
    /**
     * INDEF value flag.
     */
    private static final int INDEF = -999;

    private final ImageHDU image;

    private final double contrast;

    private final int pixelsNumberInSample;

    private final int pixelNumberRowSample;

    public ZScale(final ImageHDU image, final double contrast, final int opt_size, final int len_stdline) {
        this.image = image;
        this.contrast = contrast;
        this.pixelsNumberInSample = opt_size;
        this.pixelNumberRowSample = len_stdline;
    }

    /**
     * Sorts the sample.
     * @param sampleResult sample
     * @return the sorted sample
     */
    private List<Float> sortSampleIndex(SampleResult sampleResult) {
        int npix = sampleResult.getNpix();
        Float[] sample = new Float[npix];
        for (int i = 0; i < npix; i++) {
            sample[i] = sampleResult.getSample(i);
        }
        List<Float> list = Arrays.asList(sample);
        Collections.sort(list);
        return list;
    }
    
 
    private int computeMaxValidIndexPixels(List<Float> listSample, int npix) {
        for (int i = 0; i < npix; i++) {
            if (listSample.get(i).isNaN()) {
                npix = i;
                break;
            }
        }
        return npix;
    }

    /**
     * The median value is the average of the two central values.
     * @param listSample the sorted sample
     * @param centerPixel centerPixel
     * @param npix number of pixels in the sample
     * @return the median of the sample
     */
    private float computeMedian(final List<Float> listSample, final int centerPixel, final int npix) {        
        final int left = centerPixel - 1;
        float median;
        if ((npix % 2) == 1 || centerPixel >= npix) {
            median = listSample.get(left);
        } else {
            median = (listSample.get(left) + listSample.get(left + 1)) / 2;
        }
        return median;
    }

    /**
     * Computes the zscale.
     * @return the zscale result
     * @throws FitsException 
     */
    public ZscaleResult compute() throws FitsException {
        final SampleResult sample = sampleImage();
        int npix = sample.getNpix();
        final List<Float> listSample = sortSampleIndex(sample);
        npix = computeMaxValidIndexPixels(listSample, npix);

        final float zmin = listSample.get(0);
        final float zmax = listSample.get(npix - 1);
        final int centerPixel = Math.max(1, (npix + 1) / 2);
        final float median = computeMedian(listSample, centerPixel, npix);

        /* Fit a iRow to the sorted sample vector.  
         */
        final FitLineResult fitLineResult = fitLine(listSample, npix, KREJ, MAX_ITERATIONS);
        return computeZscale(zmin, zmax, median, npix, centerPixel, fitLineResult);
    }
    /**
     *If more than half of the
     * pixels in the sample are rejected give up and return the full range.
     * If the user-supplied contrast factor is not 1.0 adjust the scale
     * accordingly and compute Z1 and Z2, the y intercepts at indices 1 and
     * npix.
     * @param zmin
     * @param zmax
     * @param median
     * @param npix
     * @param centerPixel
     * @param fitLineResult
     * @return 
     */
    private ZscaleResult computeZscale(double zmin, double zmax, double median, int npix, int centerPixel, FitLineResult fitLineRetval) {
        float zslope = fitLineRetval.getZSlope();
        int minpix = Math.max(MIN_NPIXELS, (int) (npix * MAX_REJECT));
        ZscaleResult result;
        if (fitLineRetval.getNGoodPix() < minpix) {
            result = new ZscaleResult(zmin, zmax);
        } else {
            if (contrast > 0) {
                zslope = (float) (zslope / contrast);
            }
            double z1 = Math.max(zmin, median - (centerPixel - 1) * zslope);
            double z2 = Math.min(zmax, median + (npix - centerPixel) * zslope);
            result = new ZscaleResult(z1, z2);
        }
        return result;
    }

    private SampleResult sampleImage() throws FitsException {
        Object oneDimData = ArrayFuncs.flatten(image.getData().getData());
        int optNpixPerRow, nPixPerRow, npix = 0;
        float sample[];

        int ncols = image.getAxes()[0];
        int nRows = image.getAxes()[1];
        int bitpix = image.getBitPix();
        double blank_value = image.getHeader().getDoubleValue("BLANK", Double.NaN);

        /* Compute the number of pixels each iRow will contribute to the sample,
         * and the subsampling step size for a iRow.  The sampling grid must
         * span the whole iRow on a uniform grid.
         */
        optNpixPerRow = Math.max(1, Math.min(ncols, pixelNumberRowSample));
        int colStep = Math.max(2, (ncols + optNpixPerRow - 1) / optNpixPerRow);
        nPixPerRow = Math.max(1, (ncols + colStep - 1) / colStep);


        /* Compute the number of lines to sample and the spacing between lines.
         * We must ensure that the image is adequately sampled despite its
         * size, hence there is a lower limit on the number of lines in the
         * sample.  We also want to minimize the number of lines accessed when
         * accessing a large image, because each disk seek and read is ex-
         * pensive. The number of lines extracted will be roughly the sample
         * size divided by pixelNumberRowSample, possibly more if the lines are very
         * short.
         */
        int minNRowsInSample = Math.max(1, pixelsNumberInSample / pixelNumberRowSample);
        int optNRowsInSample = Math.max(minNRowsInSample, Math.min(nRows, (pixelsNumberInSample + nPixPerRow - 1) / nPixPerRow));
        int stepRow = Math.max(2, nRows / (optNRowsInSample));
        int maxNRowsInSample = (nRows + stepRow - 1) / stepRow;

        /* Allocate space for the output vector.  Buffer must be freed by our
         * caller.
         */
        int maxpix = nPixPerRow * maxNRowsInSample;
        sample = new float[maxpix];
        float[] row = new float[ncols];

        /* Extract the vector. */
        int op = 0;
        for (int iRow = (stepRow + 1) / 2; iRow < nRows; iRow += stepRow) {
            /* Load a row of float values from the image */
            switch (bitpix) {
                case 8:
                    byte[] bData = (byte[]) oneDimData;
                    int bPixelIndex = (iRow - 1) * ncols;
                    for (int i = 0; i < ncols; i++) {
                        if ((float) (bData[bPixelIndex] & 0xff) == blank_value) {
                            row[i] = Float.NaN;
                        } else {
                            row[i] = (float) ((bData[bPixelIndex] & 0xff));
                        }
                        bPixelIndex++;
                    }
                    break;
                case 16:
                    short[] sData = (short[]) oneDimData;
                    int sPixelIndex = (iRow - 1) * ncols;
                    for (int i = 0; i < ncols; i++) {
                        if (((double) sData[sPixelIndex]) == blank_value) {
                            row[i] = Float.NaN;
                        } else {
                            row[i] = (float) (sData[sPixelIndex]);
                        }
                        sPixelIndex++;
                    }
                    break;
                case 32:
                    int[] iData = (int[]) oneDimData;
                    int iPixelIndex = (iRow - 1) * ncols;
                    for (int i = 0; i < ncols; i++) {
                        if (((float) iData[iPixelIndex]) == blank_value) {
                            row[i] = Float.NaN;
                        } else {
                            row[i] = (float) (iData[iPixelIndex]);
                        }
                        iPixelIndex++;
                    }
                    break;
                case -32:
                    float[] fData = (float[]) oneDimData;
                    int fPixelIndex = (iRow - 1) * ncols;
                    for (int i = 0; i < ncols; i++) {
                        row[i] = fData[fPixelIndex];
                        fPixelIndex++;
                    }
                    break;
                case -64:
                    double[] dData = (double[]) oneDimData;
                    int dPixelIndex = (iRow - 1) * ncols;
                    for (int i = 0; i < ncols; i++) {
                        row[i] = (new Double(dData[dPixelIndex])).floatValue();
                        dPixelIndex++;
                    }
                    break;
            }

            subSample(row, sample, op, nPixPerRow, colStep);
            op += nPixPerRow;
            npix += nPixPerRow;
            if (npix > maxpix) {
                break;
            }
        }
        return new SampleResult(npix, sample);
    }

    /**
     * subSample -- Subsample an image iRow. Extract the first pixel and every
     * "colStep"th pixel thereafter for a total of nPixPerRow pixels.
     */
    private void subSample(float row[], float sample[], int op, int nPixPerRow, int colStep) {
        int ip, i;
        if (colStep <= 1) {
            System.arraycopy(row, 0, sample, op, nPixPerRow);
        } else {
            ip = 0;
            for (i = 0; i < nPixPerRow; i++) {
                sample[op] = row[ip];
                ip += colStep;
                op++;
            }
        }
    }

    /**
     * fitLine -- Fit a straight iRow to a data array of type real. This is an
     * iterative fitting algorithm, wherein points further than ksigma from the
     * current fit are excluded from the next fit. Convergence occurs when the
     * next iteration does not decrease the number of pixels in the fit, or when
     * there are no pixels left. The number of pixels left after pixel rejection
     * is returned as the function value.
     */
    private FitLineResult fitLine(
            List<Float> data, /* data to be fitted	  		  */
            int npix, /* number of pixels before rejection	  */
            float krej, /* k-sigma pixel rejection factor	  */         
            int maxiter /* max iterations			  */
            ) {
        int i, ngoodpix, last_ngoodpix, minpix, niter;
        double xscale, z0, dz, o_dz, x, z, mean, sigma, threshold;
        double sumxsqr, sumxz, sumz, sumx, rowrat;
        float zstart;
        float zslope;
        int ngrow = Math.max(1, (int) Math.round((npix * .01)));
        if (npix <= 0) {
            return new FitLineResult(1, 0.0F, 0.0F);
        } else if (npix == 1) {
            zstart = data.get(1);
            zslope = 0.0F;
            return new FitLineResult(1, zstart, zslope);
        } else {
            xscale = 2.0 / (npix - 1);
        }

        /* Allocate a buffer for data minus fitted curve, another for the
         * normalized X values, and another to flag rejected pixels.
         */
        float[] flat = new float[npix];
        float[] normx = new float[npix];
        byte[] badpix = new byte[npix];

        /* Compute normalized X vector.  The data X values [1:npix] are
         * normalized to the range [-1:1].  This diagonalizes the lsq matrix
         * and reduces its condition number.
         */
        for (i = 0; i < npix; i++) {
            normx[i] = (float) (i * xscale - 1.0);
        }

        /* Fit a iRow with no pixel rejection.  Accumulate the elements of the
         * matrix and data vector.  The matrix M is diagonal with
         * M[1,1] = sum x**2 and M[2,2] = ngoodpix.  The data vector is
         * DV[1] = sum (data[i] * x[i]) and DV[2] = sum (data[i]).
         */
        sumxsqr = 0;
        sumxz = 0;
        sumx = 0;
        sumz = 0;

        for (i = 0; i < npix; i++) {
            x = normx[i];
            z = data.get(i);
            sumxsqr = sumxsqr + (x * x);
            sumxz = sumxz + z * x;
            sumz = sumz + z;
        }

        /* Solve for the coefficients of the fitted iRow. */
        z0 = sumz / npix;
        dz = o_dz = sumxz / sumxsqr;

        /* Iterate, fitting a new iRow in each iteration.  Compute the flattened
         * data vector and the sigma of the flat vector.  Compute the lower and
         * upper k-sigma pixel rejection thresholds.  Run down the flat array
         * and detect pixels to be rejected from the fit.  Reject pixels from
         * the fit by subtracting their contributions from the matrix sums and
         * marking the pixel as rejected.
         */
        ngoodpix = npix;
        minpix = Math.max(MIN_NPIXELS, (int) (npix * MAX_REJECT));

        for (niter = 0; niter < maxiter; niter++) {
            last_ngoodpix = ngoodpix;

            /* Subtract the fitted iRow from the data array. */
            flattenData(data, flat, normx, npix, z0, dz);

            /* Compute the k-sigma rejection threshold.  In principle this
             * could be more efficiently computed using the matrix sums
             * accumulated when the iRow was fitted, but there are problems with
             * numerical stability with that approach.
             */
            ComputeSigmaResult compute_sigma_retval = computeSigma(flat, badpix, npix);
            ngoodpix = compute_sigma_retval.ngoodpix;
            mean = compute_sigma_retval.mean;
            sigma = compute_sigma_retval.sigma;

            threshold = sigma * krej;

            /* Detect and reject pixels further than ksigma from the fitted
             * iRow.
             */
            RejectPixelsResult reject_pixels_retval = rejectPixels(data, flat, normx,
                    badpix, npix, sumxsqr, sumxz, sumx, sumz, threshold,
                    ngrow);
            ngoodpix = reject_pixels_retval.ngoodpix;
            sumxsqr = reject_pixels_retval.sumxsqr;
            sumxz = reject_pixels_retval.sumxz;
            sumx = reject_pixels_retval.sumx;
            sumz = reject_pixels_retval.sumz;

            /* Solve for the coefficients of the fitted iRow.  Note that after
             * pixel rejection the sum of the X values need no longer be zero.
             */
            if (ngoodpix > 0) {
                rowrat = sumx / sumxsqr;
                z0 = (sumz - rowrat * sumxz) / (ngoodpix - rowrat * sumx);
                dz = (sumxz - z0 * sumx) / sumxsqr;
            }

            if (ngoodpix >= last_ngoodpix || ngoodpix < minpix) {
                break;
            }
        }

        /* Transform the iRow coefficients back to the X range [1:npix]. */
        zstart = (float) (z0 - dz);
        zslope = (float) (dz * xscale);
        if (Math.abs(zslope) < 0.001) {
            zslope = (float) (o_dz * xscale);
        }
        return new FitLineResult(ngoodpix, zstart, zslope);
    }

    /**
     * flattenData -- Compute and subtract the fitted iRow from the data array,
     * returned the flattened data in FLAT.
     */
    private void flattenData(
            List<Float> data, /* raw data array			*/
            float flat[], /* flattened data  (output)		*/
            float x[], /* x value of each pixel		*/
            int npix, /* number of pixels			*/
            double z0,
            double dz /* z-intercept, dz/dx of fitted iRow	*/
            ) {
        int i;

        for (i = 0; i < npix; i++) {
            flat[i] = (float) (data.get(i) - (x[i] * dz + z0));
        }
    }

    /**
     * computeSigma -- Compute the root mean square deviation from the mean of a
     * flattened array. Ignore rejected pixels.
     */
    private ComputeSigmaResult computeSigma(
            float a[], /* flattened data array			*/
            byte badpix[], /* bad pixel flags (!= 0 if bad pixel)	*/
            int npix) {
        float mean, sigma;
        float pixval;
        int i, ngoodpix = 0;
        double sum = 0.0, sumsq = 0.0, temp;

        /* Accumulate sum and sum of squares. */
        for (i = 0; i < npix; i++) {
            if (badpix[i] == GOOD_PIXEL) {
                pixval = a[i];
                ngoodpix = ngoodpix + 1;
                sum = sum + pixval;
                sumsq = sumsq + pixval * pixval;
            }
        }

        /* Compute mean and sigma. */
        switch (ngoodpix) {
            case 0:
                mean = INDEF;
                sigma = INDEF;
                break;
            case 1:
                mean = (float) sum;
                sigma = INDEF;
                break;
            default:
                mean = (float) (sum / (double) ngoodpix);
                temp = sumsq / (double) (ngoodpix - 1)
                        - (sum * sum) / (double) (ngoodpix * (ngoodpix - 1));
                if (temp < 0) /* possible with roundoff error */ {
                    sigma = 0.0F;
                } else {
                    sigma = (float) Math.sqrt(temp);
                }
        }

        return new ComputeSigmaResult(ngoodpix, mean, sigma);
    }

    /**
     * rejectPixels -- Detect and reject pixels more than "threshold" greyscale
     * units from the fitted iRow. The residuals about the fitted iRow are given
     * by the "flat" array, while the raw data is in "data". Each time a pixel
     * is rejected subtract its contributions from the matrix sums and flag the
     * pixel as rejected. When a pixel is rejected reject its neighbors out to a
     * specified radius as well. This speeds up convergence considerably and
     * produces a more stringent rejection criteria which takes advantage of the
     * fact that bad pixels tend to be clumped. The number of pixels left in the
     * fit is returned as the function value.
     */
    private RejectPixelsResult rejectPixels(
            List<Float> data, /* raw data array			*/
            float flat[], /* flattened data array			*/
            float normx[], /* normalized x values of pixels	*/
            byte badpix[], /* bad pixel flags (!= 0 if bad pixel)	*/
            int npix,
            double sumxsqr,
            double sumxz,
            double sumx,
            double sumz, /* matrix sums				*/
            double threshold, /* threshold for pixel rejection	*/
            int ngrow /* number of pixels of growing		*/
            ) {
        int ngoodpix, i, j;
        float residual, lcut, hcut;
        double x, z;

        ngoodpix = npix;
        lcut = (float) -threshold;
        hcut = (float) threshold;

        for (i = 0; i < npix; i++) {
            if (badpix[i] == BAD_PIXEL) {
                ngoodpix = ngoodpix - 1;
            } else {
                residual = flat[i];
                if (residual < lcut || residual > hcut) {
                    /* Reject the pixel and its neighbors out to the growing
                     * radius.  We must be careful how we do this to avoid
                     * directional effects.  Do not turn off thresholding on
                     * pixels in the forward direction; mark them for rejection
                     * but do not reject until they have been thresholded.
                     * If this is not done growing will not be symmetric.
                     */
                    for (j = Math.max(0, i - ngrow); j < Math.min(npix, i + ngrow); j++) {
                        if (badpix[j] != BAD_PIXEL) {
                            if (j <= i) {
                                x = (double) normx[j];
                                z = (double) data.get(j);
                                sumxsqr = sumxsqr - (x * x);
                                sumxz = sumxz - z * x;
                                sumx = sumx - x;
                                sumz = sumz - z;
                                badpix[j] = BAD_PIXEL;
                                ngoodpix = ngoodpix - 1;
                            } else {
                                badpix[j] = REJECT_PIXEL;
                            }
                        }
                    }
                }
            }
        }

        return new RejectPixelsResult(ngoodpix, sumxsqr, sumxz, sumx, sumz);
    }

    /**
     * Provides the sample result.
     */
    public class SampleResult {

        /**
         * Number of pixels in the sample.
         */
        private final int npix;
        /**
         * Sample of the FITS file.
         */
        private final float[] sample;

        /**
         * Constructor.
         * @param npixVal number of pixels in the sample
         * @param sampleVal sample
         */
        public SampleResult(final int npixVal, final float sampleVal[]) {
            npix = npixVal;
            sample = sampleVal;
        }
        /**
         * Returns the number of pixels.
         * @return the number of pixels
         */
        public final int getNpix() {
            return npix;
        }
        
        /**
         * Returns the sample.
         * @return the sample
         */
        public final float[] getSample() {
            return sample;
        }

        /**
         * Returns an element of the sample.
         * @param i pixel in the sample
         * @return an element of the sample
         */
        public final float getSample(final int i) {
            return sample[i];
        }

    }

    private class RejectPixelsResult {

        int ngoodpix;
        double sumxsqr;
        double sumxz;
        double sumx;
        double sumz;


        public RejectPixelsResult(final int ngoodpixVal, final double sumxsqrVal, final double sumxzVal, final double sumxVal, final double sumzVal) {
            ngoodpix = ngoodpixVal;
            sumxsqr = sumxsqrVal;
            sumxz = sumxzVal;
            sumx = sumxVal;
            sumz = sumzVal;
        }
        
        public final int getNGoodPixel() {
            return ngoodpix;
        }
        
        public final double getSumXsqr() {
            return sumxsqr;
        }
        
        public final double getSumXZ() {
            return sumxz;
        }
        
        public final double getSumX() {
            return sumx;
        }
        
        public final double getSumZ() {
            return sumz;
        }
    }

    /**
     * Provides mean and sigma result.
     */
    private class ComputeSigmaResult {
        /**
         * Number of good pixels.
         */
        private final int ngoodpix;
        /**
         * Mean.
         */
        private final float mean;
        /**
         * Sigma.
         */
        private final float sigma;

        /**
         * Provides sigma result.
         * @param ngoodpixVal number of good pixels
         * @param meanVal mean value
         * @param sigmaVal sigma value
         */
        public ComputeSigmaResult(final int ngoodpixVal, final float meanVal, final float sigmaVal) {
            ngoodpix = ngoodpixVal;
            mean = meanVal;
            sigma = sigmaVal;
        }
        /**
         * Returns the number of good pixels.
         * @return the number of good pixels
         */
        public final int getNGoodPixel() {
            return ngoodpix;
        }

        /**
         * Returns the mean.
         * @return the mean
         */
        public final float getMean() {
            return mean;
        }

        /**
         * Returns the sigma.
         * @return the sigma
         */
        public final float getSigma() {
            return sigma;
        }
    }

    /**
     * Provides the result of the Fit line algorithm.
     */
    private class FitLineResult {
        /**
         * Number of good pixels.
         */
        private final int ngoodpix;
        /**
         * Start of the fit line.
         */
        private final float zstart;
        /**
         * Slope of the fit line.
         */
        private final float zslope;

        /**
         * Constructs the Fit line Result.
         * @param ngoodpixVal number of good pixels
         * @param zstartVal z start
         * @param zslopeVal z slope
         */
        public FitLineResult(final int ngoodpixVal, final float zstartVal, final float zslopeVal) {
            ngoodpix = ngoodpixVal;
            zstart = zstartVal;
            zslope = zslopeVal;
        }

        /**
         * Returns the number of good pixels.
         * @return the number of good pixels
         */
        public final int getNGoodPix() {
            return this.ngoodpix;
        }

        /**
         * Returns the start of the fit line.
         * @return the start of the fit line
         */
        public final float getZStart() {
            return this.zstart;
        }

        /**
         * Returns the slope of the fit line.
         * @return the slope of the fit line
         */
        public final float getZSlope() {
            return this.zslope;
        }
    }

    /**
     * Provides the zmin, zmax of the Zscale algorithm.
     */
    public class ZscaleResult {
        /**
         * zmin.
         */
        private final double z1;
        /**
         * zmax.
         */
        private final double z2;

        /**
         * Constructs a zscale result.
         * @param z1Val zmin
         * @param z2Val zmax
         */
        public ZscaleResult(final double z1Val, final double z2Val) {
            z1 = z1Val;
            z2 = z2Val;
        }

        /**
         * Returns z1.
         * @return z1
         */
        public final double getZ1() {
            return z1;
        }

        /**
         * Returns z2.
         * @return z2
         */
        public final double getZ2() {
            return z2;
        }
    }
}
