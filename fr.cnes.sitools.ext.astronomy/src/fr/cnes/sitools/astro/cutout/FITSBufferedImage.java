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
import edu.jhu.pha.sdss.fits.Histogram;
import edu.jhu.pha.sdss.fits.ScaleUtils;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;

public class FITSBufferedImage extends FITSImage {
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(FITSBufferedImage.class.getName());
    /**
     * Constructor.
     * @param scaledImage Image buffer
     * @param scaleMethod 
     * @throws FitsException
     * @throws edu.jhu.pha.sdss.fits.FITSImage.DataTypeNotSupportedException
     * @throws edu.jhu.pha.sdss.fits.FITSImage.NoImageDataFoundException
     * @throws IOException 
     */
    public FITSBufferedImage(BufferedImage[] scaledImage, int scaleMethod)
            throws FitsException, FITSImage.DataTypeNotSupportedException, FITSImage.NoImageDataFoundException, IOException {
        super(null, scaledImage, scaleMethod);
    }

    /**
     * 
     * @param hdu
     * @return
     * @throws FitsException
     * @throws edu.jhu.pha.sdss.fits.FITSImage.DataTypeNotSupportedException 
     */
    public static BufferedImage[] createScaledImages(ImageHDU hdu) throws FitsException, FITSImage.DataTypeNotSupportedException {
        return createScaledImages(hdu, 0);
    }

    /**
     * 
     * @param hdu
     * @param dataCubeIndex
     * @return
     * @throws FitsException
     * @throws edu.jhu.pha.sdss.fits.FITSImage.DataTypeNotSupportedException 
     */
    public static BufferedImage[] createScaledImages(ImageHDU hdu, int dataCubeIndex) throws FitsException, FITSImage.DataTypeNotSupportedException {
        int bitpix = hdu.getBitPix();
        double bZero = hdu.getBZero();
        double bScale = hdu.getBScale();
        int[] naxes = hdu.getAxes();
        int width = naxes.length == 3 ? hdu.getAxes()[2] : hdu.getAxes()[1];
        int height = naxes.length == 3 ? hdu.getAxes()[1] : hdu.getAxes()[0];
        Object data = hdu.getData().getData();
        Histogram hist = null;
        short[][] scaledData = (short[][]) null;
        Object cimage = data;
        switch (bitpix) {
            case 8:
                hist = ScaleUtils.computeHistogram((byte[][]) (byte[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((byte[][]) (byte[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());

                break;
            case 16:
                hist = ScaleUtils.computeHistogram((short[][]) (short[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((short[][]) (short[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());

                break;
            case 32:
                hist = ScaleUtils.computeHistogram((int[][]) (int[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((int[][]) (int[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());

                break;
            case -32:
                hist = ScaleUtils.computeHistogram((float[][]) (float[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((float[][]) (float[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());

                break;
            case -64:
                hist = ScaleUtils.computeHistogram((double[][]) (double[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((double[][]) (double[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());

                break;
            default:
                throw new FITSImage.DataTypeNotSupportedException(bitpix);
        }

        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(1000), false, false, 1, 1);

        SampleModel sm = cm.createCompatibleSampleModel(width, height);

        Hashtable properties = new Hashtable();
        properties.put("histogram", hist);
        properties.put("imageHDU", hdu);

        BufferedImage[] result = new BufferedImage[scaledData.length];

        for (int i = 0; i < result.length; i++) {
            DataBuffer db = new DataBufferUShort(scaledData[i], height);
            WritableRaster r = Raster.createWritableRaster(sm, db, null);

            result[i] = new BufferedImage(cm, r, false, properties);
        }

        return result;
    }
}
