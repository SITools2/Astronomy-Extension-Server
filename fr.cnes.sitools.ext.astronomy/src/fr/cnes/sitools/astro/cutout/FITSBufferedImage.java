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

import org.restlet.engine.Engine;

import edu.jhu.pha.sdss.fits.FITSImage;
import edu.jhu.pha.sdss.fits.Histogram;
import edu.jhu.pha.sdss.fits.ScaleUtils;

/**
 * Scales FITS images.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class FITSBufferedImage extends FITSImage {
    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(FITSBufferedImage.class.getName());
    /**
     * BYTE datatype in FITS.
     */
    private static final int BYTE = 8;
    /**
     * SHORT datatype in FITS.
     */
    private static final int SHORT = 16;
    /**
     * INT datatype in FITS.
     */
    private static final int INT = 32;
    /**
     * FLOAT datatype in FITS.
     */
    private static final int FLOAT = -32;
    /**
     * DOUBLE datatype in FITS.
     */
    private static final int DOUBLE = -64;
    /**
     * Construct a FITS image on which a scale method is applied.
     * @param scaledImage image to transform
     * @param scaleMethod scale method to apply
     * @throws FitsException FITS exception
     * @throws FITSImage.DataTypeNotSupportedException DataTypeNotSupported
     * @throws FITSImage.NoImageDataFoundException NoImageDataFound
     * @throws IOException
     */
    public FITSBufferedImage(final BufferedImage[] scaledImage, final int scaleMethod)
            throws FitsException, FITSImage.DataTypeNotSupportedException, FITSImage.NoImageDataFoundException, IOException {
        super(null, scaledImage, scaleMethod);
    }

    /**
     * Scales the image and returns it.
     * @param hdu FITS HDU to scale
     * @return the scaled image
     * @throws FitsException FITS exception
     * @throws FITSImage.DataTypeNotSupportedException DataTypeNotSupported
     */
    public static BufferedImage[] createScaledImages(final ImageHDU hdu) throws FitsException, FITSImage.DataTypeNotSupportedException {
        return createScaledImages(hdu, 0);
    }

    /**
     * Scales the data cube image and returns it.
     * @param hdu FITS HDU to scale
     * @param dataCubeIndex index of the data cube to scale
     * @return the scaled data cube image and returns i
     * @throws FitsException FITS exception
     * @throws FITSImage.DataTypeNotSupportedException DataTypeNotSupported
     */
    public static BufferedImage[] createScaledImages(final ImageHDU hdu, final int dataCubeIndex) throws FitsException, FITSImage.DataTypeNotSupportedException {
        final int bitpix = hdu.getBitPix();
        final double bZero = hdu.getBZero();
        final double bScale = hdu.getBScale();
        final int[] naxes = hdu.getAxes();
        final int width = naxes.length == 3 ? hdu.getAxes()[2] : hdu.getAxes()[1];
        final int height = naxes.length == 3 ? hdu.getAxes()[1] : hdu.getAxes()[0];
        final Object data = hdu.getData().getData();        
        short[][] scaledData = (short[][]) null;
        final Object cimage = data;
        Histogram hist;
        switch (bitpix) {
            case BYTE:
                hist = ScaleUtils.computeHistogram((byte[][]) (byte[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((byte[][]) (byte[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());
                break;
            case SHORT:
                hist = ScaleUtils.computeHistogram((short[][]) (short[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((short[][]) (short[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());
                break;
            case INT:
                hist = ScaleUtils.computeHistogram((int[][]) (int[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((int[][]) (int[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());
                break;
            case FLOAT:
                hist = ScaleUtils.computeHistogram((float[][]) (float[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((float[][]) (float[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());
                break;
            case DOUBLE:
                hist = ScaleUtils.computeHistogram((double[][]) (double[][]) cimage, bZero, bScale);
                scaledData = ScaleUtils.scaleToUShort((double[][]) (double[][]) cimage, hist, width, height, bZero, bScale, hist.getMin(), hist.getMax(), hist.estimateSigma());
                break;
            default:
                throw new FITSImage.DataTypeNotSupportedException(bitpix);
        }
        final ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(1000), false, false, 1, 1);
        final SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
        final Hashtable properties = new Hashtable();
        properties.put("histogram", hist);
        properties.put("imageHDU", hdu);
        final BufferedImage[] result = new BufferedImage[scaledData.length];
        for (int i = 0; i < result.length; i++) {
            final DataBuffer dataBuffer = new DataBufferUShort(scaledData[i], height);
            final WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, null);
            result[i] = new BufferedImage(colorModel, raster, false, properties);
        }
        return result;
    }
}
