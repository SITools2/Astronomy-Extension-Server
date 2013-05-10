/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.astro.cutoff;

import edu.jhu.pha.sdss.fits.FITSImage;
import edu.jhu.pha.sdss.fits.FITSImage.DataTypeNotSupportedException;
import edu.jhu.pha.sdss.fits.FITSImage.NoImageDataFoundException;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import jsky.coords.WCSKeywordProvider;
import jsky.coords.WCSTransform;
import jsky.coords.worldpos;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.image.ImageTiler;
import nom.tam.util.Cursor;

/**
 * Class for handling cutoutProcessing feature on FITS file.
 * @author Jean-Christophe Malapert
 */
public class CutOffSITools2 implements CutOffInterface {
    /**
     * Maximum value for the right ascension.
     */
    private static final double MAX_RA = 360.0;

    /**
     * Minimum value for the right ascension.
     */
    private static final double MIN_RA = 0.;

    /**
     * Minimum value for the declination.
     */
    private static final double MIN_DEC = -90.;

    /**
     * Maximum value for the declination.
     */
    private static final double MAX_DEC = 90.;

    /**
     * Minimum radius.
     */
    private static final double MIN_RADIUS = 0.;
    /**
     * BitPix value in FITS for short datatype.
     */
    private static final int FITS_SHORT = 16;
    /**
     * BitPix value in FITS for int datatype.
     */
    private static final int FITS_INT = 32;
    /**
     * BitPix value in FITS for float datatype.
     */
    private static final int FITS_FLOAT = -32;
    /**
     * BitPix value in FITS for double datatype.
     */
    private static final int FITS_DOUBLE = -64;
    /**
     * BitPix value in FITS for bit datatype.
     */
    private static final int FITS_BIT = 8;
    /**
     * Initialize keywords that will not be overidden .
     * while the cut off FITS is generated
     */
    private final static List keysToNotOverride = Arrays.asList("SIMPLE", "NAXIS", "NAXIS1", "NAXIS2", "NAXIS3", "EXTEND", "END", "EXTEND", "BITPIX", "CRPIX1", "CRPIX2");
    /**
     * Initialize Fits object.
     */
    private Fits fits = null;
    /**
     * Initialize x Position in the CDD frame.
     */
    private int xPos = 0;
    /**
     * Initialize y Position in the CCD frame.
     */
    private int yPos = 0;
    /**
     * Initialize the number of pixels along X on the CCD.
     */
    private int width = 0;
    /**
     * Initialize the number of pixels along Y on the CCD.
     */
    private int height = 0;
    /**
     * Initialize the HDU number to 0.
     */
    private int hduNumber = 0;
    /**
     * Initialize isDataCurbe to false.
     */
    private boolean isDataCube = false;
    /**
     * Initialize the output format to Not defined.
     */
    private SupportedFileFormat formatOutput = SupportedFileFormat.NOT_DEFINED;
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(CutOffSITools2.class.getName());
    /**
     * Cutoff constructor.
     *
     * <p>
     * A cutout can be defined by a cone search on a specific HDU.
     * HDU number is required because each FITS extension could have
     * its own projection or astrometry
     * </p>
     * @param fitsObj Fits file to cut
     * @param ra Center Ra for cone search in decimal degree
     * @param dec Center dec for cone search in decimal degree
     * @param radius radius in decimal degree
     * @param hduNumber HDU number to process (HDU number starts at 0)
     * @throws CutOffException
     */
    public CutOffSITools2(final Fits fitsObj, final double ra, final double dec, final double radius, final int hduNumber) throws CutOffException {        
        try {
            init(fitsObj, ra, dec, radius, hduNumber);
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }

    /**
     * Cutoff constructor.
     *
     * <p>
     * A cutout can be defined by a cone search on a specific HDU.
     * HDU number is required because each FITS extension could have
     * its own projection or astrometry
     * </p>
     * @param fitsObjUrl URL of the Fits file to cut
     * @param ra Center Ra for cone search in decimal degree
     * @param dec Center dec for cone search in decimal degree
     * @param radius radius in decimal degree
     * @param hduNumber HDU number to process (HDU number starts at 0)
     * @throws CutOffException
     */
    public CutOffSITools2(final URL fitsObjUrl, final double ra, final double dec, final double radius, final int hduNumber) throws CutOffException {        
        try {
            final Fits fitsObj = new Fits(fitsObjUrl);
            init(fitsObj, ra, dec, radius, hduNumber);
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }
    
    /**
     * Cut off service based on ra and dec in sexagecimal.
     * @param fits Fits file
     * @param ra ra in sexagecimal
     * @param dec dec in sexagecimal
     * @param radius radius in degree
     * @param hduNumber hdu Number
     * @throws CutOffException CutOff Exception
     */
    public CutOffSITools2(final Fits fits, final String ra, final String dec, final double radius, final int hduNumber) throws CutOffException {
        try {
            AstroCoordinate astro = new AstroCoordinate(ra, dec);
            init(fits, astro.getRaAsDecimal(), astro.getDecAsDecimal(), radius, hduNumber);
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }
    /**
     * Cut off service based on ra and dec in sexagecimal.
     * @param fitsUrl URL of the Fits file
     * @param ra ra in sexagecimal
     * @param dec dec in sexagecimal
     * @param radius radius in degree
     * @param hduNumber hdu Number
     * @throws CutOffException CutOff Exception
     */
    public CutOffSITools2(final URL fitsUrl, final String ra, final String dec, final double radius, final int hduNumber) throws CutOffException {
        try {
            AstroCoordinate astro = new AstroCoordinate(ra, dec);
            Fits fitsObj = new Fits(fitsUrl);
            init(fitsObj, astro.getRaAsDecimal(), astro.getDecAsDecimal(), radius, hduNumber);
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }    

    /**
     * Initialize the constructor.
     * @param fits Fits object
     * @param ra right ascension
     * @param dec declination
     * @param radius radius
     * @param hduNumber HDU number
     * @throws FitsException Fits Exception
     * @throws IOException  IO Exception
     */
    protected final void init(final Fits fits, final double ra, final double dec, final double radius, final int hduNumber) throws FitsException, IOException {
        if (ra < MIN_RA || ra > MAX_RA || dec > MAX_DEC || dec < MIN_DEC || radius < MIN_RADIUS) {
            throw new IllegalArgumentException("wrong parameters in cutout");
        }

        double ramin = (ra - radius < MIN_RA) ? (MAX_RA - (ra - radius)) : ra - radius;
        ramin %= MAX_RA;
        double ramax = (ra + radius) % MAX_RA;
        double decmax = (dec + radius > MAX_DEC) ? MAX_DEC : dec + radius;
        double decmin = (dec - radius < MIN_DEC) ? MIN_DEC : dec - radius;

        this.fits = fits;
        this.hduNumber = hduNumber;
        BasicHDU basicHdu = this.fits.getHDU(hduNumber);
        WCSKeywordProvider wcsProvider = new FitsHeader(basicHdu.getHeader());
        WCSTransform wcs = new WCSTransform(wcsProvider);

        Point2D.Double posUpper1 = worldpos.getPixels(ramax, decmax, wcs);
        Point2D.Double posUpper2 = worldpos.getPixels(ramin, decmax, wcs);

        Point2D.Double posLower1 = worldpos.getPixels(ramax, decmin, wcs);
        Point2D.Double posLower2 = worldpos.getPixels(ramin, decmin, wcs);

        double widthUpper = Math.abs(posUpper1.getX() - posUpper2.getX());
        double widthLower = Math.abs(posLower1.getX() - posLower2.getX());
        double heighLeft = Math.abs(posUpper1.getY() - posLower1.getY());
        double heighRight = Math.abs(posUpper2.getY() - posLower2.getY());

        // TO DO : To be checked
        double widthQuery = (widthLower > widthUpper) ? widthLower : widthUpper;
        double heightQuery = (heighLeft > heighRight) ? heighLeft : heighRight;
        Point2D.Double center = worldpos.getPixels(ra, dec, wcs);
        double xPosQuery = center.getX() - widthQuery / 2.0;
        double yPosQuery = center.getY() - heightQuery / 2.0;

        this.xPos = (int) xPosQuery;
        this.yPos = (int) yPosQuery;
        this.width = (int) widthQuery;
        this.height = (int) heightQuery;

        this.isDataCube = basicHdu.getHeader().containsKey("NAXIS3");
    }

    /**
     * Constructor.
     * @param fits Fits file
     * @param xPos x value in pixels of the lower corner on the left
     * @param yPos y value in pixels of the lower corner on the left
     * @param width length of the image according to X axe
     * @param height length of the image according to Y axe
     * @param hduNumber HDU number to process (HDU number starts at 0)
     * @throws CutOffException CutOff exception
     * NB: xpos and ypos start at 0
     */
    public CutOffSITools2(final Fits fits, final int xPos, final int yPos, final int width, final int height, final int hduNumber) throws CutOffException {
        try {
            assert fits!=null && xPos>=0 && yPos >=0 && width > 0 && height >0 && hduNumber>=0;        
            this.fits = fits;
            this.xPos = xPos;
            this.yPos = yPos;
            this.width = width;
            this.height = height;
            this.hduNumber = hduNumber;
            this.isDataCube = fits.getHDU(hduNumber).getHeader().containsKey("NAXIS3");
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }

    /**
     * Constructor.
     * @param url URL where the Fits file is located
     * @param xPos x value in pixels of the lower corner on the left
     * @param yPos y value in pixels of the lower corner on the left
     * @param width length of the image according to X axe
     * @param height length of the image according to Y axe
     * @param hduNumber HDU number to process (HDU number starts at 0)
     * @throws CutOffException CutOffException     
     * NB: xpos and ypos start at 0     
     */
    public CutOffSITools2(final URL url, final int xPos, final int yPos, final int width, final int height, final int hduNumber) throws CutOffException {
        try {
            assert url!=null && xPos>=0 && yPos >=0 && width > 0 && height >0 && hduNumber>=0;
            Fits fitsFile = new Fits(url);
            this.fits = fitsFile;
            this.xPos = xPos;
            this.yPos = yPos;
            this.width = width;
            this.height = height;
            this.hduNumber = hduNumber;
            this.isDataCube = fits.getHDU(hduNumber).getHeader().containsKey("NAXIS3");
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }

    /**
     * Constructor.
     * @param is Input Stream of a Fits file
     * @param xPos x value in pixels of the lower corner on the left
     * @param yPos y value in pixels of the lower corner on the left
     * @param width length of the image according to X axe
     * @param height length of the image according to Y axe
     * @param hduNumber HDU number to process ((HDU number starts at 0)
     * @throws CutOffException CutOff Exception
     * NB: xpos and ypos start at 0     
     */
    public CutOffSITools2(final InputStream is, final int xPos, final int yPos, final int width, final int height, final int hduNumber) throws CutOffException {
        try {
            assert (xPos >=0 && yPos>=0 && width >0 && height >0 && hduNumber >=0);
            Fits fitsFile = new Fits(is);
            this.fits = fitsFile;
            this.xPos = xPos;
            this.yPos = yPos;
            this.width = width;
            this.height = height;
            this.hduNumber = hduNumber;
            this.isDataCube = fits.getHDU(hduNumber).getHeader().containsKey("NAXIS3");
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }
    }

    /**
     * Compute the lower left position as well as width
     * and height of the cut off.
     * @param naxis1 Number of pixels in X axis
     * @param naxis2 Number of pixels in Y axis
     * @return Returns a list such as [xpos, ypos, width, height]
     */
    private List<Integer> getBorders(final int naxis1, final int naxis2) {
        assert naxis1 >0 && naxis2 >0;
        int xpos = this.xPos;
        int ypos = this.yPos;
        int widthCut = this.width;
        int heightCut = this.height;

        if (this.width >= naxis1 && this.height >= naxis2) {
            xpos = 0;
            ypos = 0;
            widthCut = naxis1;
            heightCut = naxis2;
        } else {
            if (this.width > naxis1) {
                widthCut = naxis1;
                xpos = 0;
            }
            if (this.height > naxis2) {
                heightCut = naxis2;
                ypos = 0;
            }
            if (this.xPos + this.width > naxis1) {
                xpos = naxis1 - this.width;
            }
            if (this.yPos + this.height > naxis2) {
                ypos = naxis2 - this.height;
            }
        }
        return Arrays.asList(xpos, ypos, widthCut, heightCut);
    }

    /**
     * cutoutProcessing function.
     * @return Returns the data part that is asked
     * @throws FitsException
     * @throws IOException IO Exception
     */
    private Object cutoutProcessing(final BasicHDU basicHdu) throws FitsException, IOException {
        assert basicHdu != null;
        Object imageData = null;
        ImageHDU imageHdu = (ImageHDU) basicHdu;
        Header hdr = basicHdu.getHeader();
        int naxis1 = hdr.getIntValue("NAXIS1");
        int naxis2 = hdr.getIntValue("NAXIS2");

        List<Integer> borders = getBorders(naxis1, naxis2);
        int widthCut = borders.get(2);
        int heightCut = borders.get(3);
        int xpos = borders.get(0);
        int ypos = borders.get(1);

        if (this.width == widthCut && this.height == heightCut) {
            return basicHdu.getData().getData();
        }

        ImageTiler imageTiler = imageHdu.getTiler();
        Object data = imageTiler.getTile(new int[]{ypos, xpos}, new int[]{heightCut, widthCut});

        int bitpix = basicHdu.getBitPix();

        // Wrap the image in the right datatype according to bitpix
        switch (bitpix) {
            case FITS_SHORT:
                short[][] imageShort = new short[heightCut][widthCut];
                int indexShort = 0;
                for (int i = 0; i < heightCut; i++) {
                    for (int j = 0; j < widthCut; j++) {
                        imageShort[i][j] = ((short[]) data)[indexShort];
                        indexShort++;
                    }
                }
                imageData = imageShort;
                break;
            case FITS_INT:
                int[][] image = new int[heightCut][widthCut];
                int indexInt = 0;
                for (int i = 0; i < heightCut; i++) {
                    for (int j = 0; j < widthCut; j++) {
                        image[i][j] = ((int[]) data)[indexInt];
                        indexInt++;
                    }
                }
                imageData = image;
                break;
            case FITS_FLOAT:
                float[][] imageFloat = new float[heightCut][widthCut];
                int indexFloat = 0;
                for (int i = 0; i < heightCut; i++) {
                    for (int j = 0; j < widthCut; j++) {
                        imageFloat[i][j] = ((float[]) data)[indexFloat];
                        indexFloat++;
                    }
                }
                imageData = imageFloat;
                break;
            case FITS_DOUBLE:
                double[][] imageDouble = new double[heightCut][widthCut];
                int indexDouble = 0;
                for (int i = 0; i < heightCut; i++) {
                    for (int j = 0; j < widthCut; j++) {
                        imageDouble[i][j] = ((double[]) data)[indexDouble];
                        indexDouble++;
                    }
                }
                imageData = imageDouble;
                break;
            case FITS_BIT:
                byte[][] imageByte = new byte[heightCut][widthCut];
                int index = 0;
                for (int i = 0; i < heightCut; i++) {
                    for (int j = 0; j < widthCut; j++) {
                        imageByte[i][j] = ((byte[]) data)[index];
                        index++;
                    }
                }
                imageData = imageByte;
                break;
            default:
                Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, "cutoutProcessing: the format is not recogized and this should not possible");
                throw new Error("cutoutProcessing: bitpix (" + bitpix + ") is unknown.");

        }
        return imageData;
    }

    /**
     * Cut off processing function.
     * @param imageData Image data
     * @param naxis1 Number of pixels in X axis
     * @param naxis2 Number of pixels in Y axis
     * @return Returns the pixels that corresponds to cut off
     * @throws FitsException FITS Exception
     */
    private Object cutoutProcessing(final Object imageData, final int naxis1, final int naxis2) throws FitsException {
        assert imageData!=null && naxis1 >0 && naxis2 > 0;
        List<Integer> borders = getBorders(naxis1, naxis2);
        int widthCut = borders.get(2);
        int heightCut = borders.get(3);
        int xpos = borders.get(0);
        int ypos = borders.get(1);

        Object imageDataToReturn = null;
        if (imageData instanceof byte[][]) {
            byte[][] cutImageData = new byte[heightCut][widthCut];
            byte[][] imageDataByte = (byte[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    cutImageData[i][j] = imageDataByte[i + ypos][j + xpos];
                }
            }
            imageDataToReturn = cutImageData;
        } else if (imageData instanceof int[][]) {
            int[][] cutImageData = new int[heightCut][widthCut];
            int[][] imageDataByte = (int[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    cutImageData[i][j] = imageDataByte[i + ypos][j + xpos];
                }
            }
            imageDataToReturn = cutImageData;
        } else if (imageData instanceof float[][]) {
            float[][] cutImageData = new float[heightCut][widthCut];
            float[][] imageDataByte = (float[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    cutImageData[i][j] = imageDataByte[i + ypos][j + xpos];
                }
            }
            imageDataToReturn = cutImageData;
        } else if (imageData instanceof double[][]) {
            double[][] cutImageData = new double[heightCut][widthCut];
            double[][] imageDataByte = (double[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    cutImageData[i][j] = imageDataByte[i + ypos][j + xpos];
                }
            }
            imageDataToReturn = cutImageData;
        } else {
            Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, "cutoutProcessing: the format is not recogized and this should not possible");
            throw new Error("cutoutProcessing: unknown format.");
        }
        return imageDataToReturn;
    }

    /**
     * Extract an image from a data cube.
     * @param basicHdu basicHDU
     * @param nbExtension frame number in Z axis
     * @return Returns a from from a data cube
     * @throws FitsException FITS Exception
     */
    private Object extractImageFromCube(final BasicHDU basicHdu, final int nbExtension) throws FitsException {
        assert basicHdu!=null && nbExtension >=0;
        Object imageData = null;
        Object data = basicHdu.getData().getData();
        int bitpix = basicHdu.getBitPix();
        // Wrap the image in the right datatype according to bitpix
        if (bitpix == FITS_SHORT) {
            short[][] image = ((short[][][]) data)[nbExtension];
            imageData = image;
        } else if (bitpix == FITS_INT) {
            int[][] image = ((int[][][]) data)[nbExtension];
            imageData = image;
        } else if (bitpix == FITS_FLOAT) {
            float[][] image = ((float[][][]) data)[nbExtension];
            imageData = image;
        } else if (bitpix == FITS_DOUBLE) {
            double[][] image = ((double[][][]) data)[nbExtension];
            imageData = image;
        } else if (bitpix == FITS_BIT) {
            byte[][] image = ((byte[][][]) data)[nbExtension];
            imageData = image;
        } else {
            throw new FitsException("Bitpix: " + bitpix + " not recognized");
        }
        return imageData;
    }

    /**
     * Propagate FITS keywords.
     * @param originalHeader Original Header to copy
     * @param imageData Image data where the header must be added
     * @return Returns Returns a new BasicHDU (header + image data)
     * @throws FitsException FITS Exception
     * @throws IOException IO Exception
     */
    private BasicHDU copyAndComputeKeywords(final Header originalHeader, final Object imageData) throws FitsException, IOException {
        assert originalHeader!=null && imageData!=null;
        List<Integer> borders = getBorders(originalHeader.getIntValue("NAXIS1"), originalHeader.getIntValue("NAXIS2"));
        int xpos = borders.get(0);
        int ypos = borders.get(1);

        BasicHDU hduOutput = Fits.makeHDU(imageData);
        Cursor iter = originalHeader.iterator();
        while (iter.hasNext()) {
            HeaderCard card = (HeaderCard) iter.next();
            String key = card.getKey();
            if (!CutOffSITools2.keysToNotOverride.contains(key)) {
                String value = (card.isKeyValuePair()) ? card.getValue() : null;
                if (value == null || value.isEmpty()) {
                } else {
                    if (card.isStringValue()) {
                        hduOutput.addValue(key, value, card.getComment());
                    } else {
                        try {
                            int numericalValue = Integer.parseInt(value);
                            hduOutput.addValue(key, numericalValue, card.getComment());
                        } catch (NumberFormatException ex) {
                            try {
                                double numericalValue = Double.parseDouble(value);
                                hduOutput.addValue(key, numericalValue, card.getComment());
                            } catch (NumberFormatException ex1) {
                                hduOutput.addValue(key, value, card.getComment());
                            }
                        }
                    }
                }
            } else if (key.equals("CRPIX1")) {
                double value = Double.parseDouble(card.getValue());
                hduOutput.addValue("CRPIX1", value - xpos, card.getComment());
            } else if (key.equals("CRPIX2")) {
                double value = Double.parseDouble(card.getValue());
                hduOutput.addValue("CRPIX2", value - ypos, card.getComment());
            }
        }
        return hduOutput;
    }

    /**
     * Create a FITS buffer image.
     * @param imageData image data
     * @return Returns a FITS image data
     * @throws FitsException
     * @throws edu.jhu.pha.sdss.fits.FITSImage.DataTypeNotSupportedException
     * @throws edu.jhu.pha.sdss.fits.FITSImage.NoImageDataFoundException
     * @throws IOException IO Exception
     */
    private FITSImage createFitsBufferedImage(final Object imageData) throws FitsException, DataTypeNotSupportedException, NoImageDataFoundException, IOException {
        assert imageData!=null;
        BasicHDU hduOutput = Fits.makeHDU(imageData);
        BufferedImage[] image = FITSBufferedImage.createScaledImages((ImageHDU) hduOutput);
        return new FITSBufferedImage(image, 2);
    }

    /**
     * Cut off a data cube.
     *
     * <p>
     * Cube is cut by plane and each plane is cut. Then each plane is copied in
     * a new cube.
     * This algorithm can create Memory problem because the whole cube is in
     * memory
     * NB: A solution could be to create a extension for each plane
     * </p>
     * @param basicHdu current HDU
     * @return Returns Returns a basicHDU
     * @throws FitsException
     * @throws IOException IO Exception
     */
    private BasicHDU cutoutCube(final BasicHDU basicHdu) throws FitsException, IOException {
        assert basicHdu !=null;
        Object imageData = null;
        Header hdr = basicHdu.getHeader();
        int naxis3 = hdr.getIntValue("NAXIS3");
        int bitpix = basicHdu.getBitPix();

        switch (bitpix) {
            case FITS_SHORT:
                short[][][] imageTmpShort = new short[naxis3][this.height][this.width];
                for (int axeElt = 0; axeElt < naxis3; axeElt++) {
                    Object image2d = extractImageFromCube(basicHdu, axeElt);
                    imageTmpShort[axeElt] = (short[][]) cutoutProcessing(image2d, hdr.getIntValue("NAXIS1"), hdr.getIntValue("NAXIS2"));
                }
                imageData = imageTmpShort;
                break;
            case FITS_INT:
                int[][][] imageTmpInt = new int[naxis3][this.height][this.width];
                for (int axeElt = 0; axeElt < naxis3; axeElt++) {
                    Object image2d = extractImageFromCube(basicHdu, axeElt);
                    imageTmpInt[axeElt] = (int[][]) cutoutProcessing(image2d, hdr.getIntValue("NAXIS1"), hdr.getIntValue("NAXIS2"));
                }
                imageData = imageTmpInt;
                break;
            case FITS_FLOAT:
                float[][][] imageTmpFloat = new float[naxis3][this.height][this.width];
                for (int axeElt = 0; axeElt < naxis3; axeElt++) {
                    Object image2d = extractImageFromCube(basicHdu, axeElt);
                    imageTmpFloat[axeElt] = (float[][]) cutoutProcessing(image2d, hdr.getIntValue("NAXIS1"), hdr.getIntValue("NAXIS2"));
                }
                imageData = imageTmpFloat;
                break;
            case FITS_DOUBLE:
                double[][][] imageTmpDouble = new double[naxis3][this.height][this.width];
                for (int axeElt = 0; axeElt < naxis3; axeElt++) {
                    Object image2d = extractImageFromCube(basicHdu, axeElt);
                    imageTmpDouble[axeElt] = (double[][]) cutoutProcessing(image2d, hdr.getIntValue("NAXIS1"), hdr.getIntValue("NAXIS2"));
                }
                imageData = imageTmpDouble;
                break;
            case FITS_BIT:
                byte[][][] imageTmpByte = new byte[naxis3][this.height][this.width];
                for (int axeElt = 0; axeElt < naxis3; axeElt++) {
                    Object image2d = extractImageFromCube(basicHdu, axeElt);
                    imageTmpByte[axeElt] = (byte[][]) cutoutProcessing(image2d, hdr.getIntValue("NAXIS1"), hdr.getIntValue("NAXIS2"));
                }
                imageData = imageTmpByte;
                break;
            default:
                Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, "cutoutAndFlipImage: the format is not recogized and this should not possible");
                throw new Error("flipImageForGraphicCreation: the format is not recogized and this should not be possible");
        }
        return copyAndComputeKeywords(hdr, imageData);
    }

    /**
     * Generic cut off data to data image.
     * @param basicHdu current HDU
     * @return Returns a HDU as a cut off of basicHDU
     * @throws FitsException FITS Exception
     * @throws IOException IO Exception
     */
    private ImageHDU cutoutImage(final BasicHDU basicHdu) throws FitsException, IOException {
        assert basicHdu != null;
        Object imageData = cutoutProcessing(basicHdu);
        ImageData imgData = new ImageData(imageData);
        ImageHDU imageHDU = new ImageHDU(basicHdu.getHeader(), imgData);
        return imageHDU;
    }

    /**
     * Generic cut off data to handle data cube and data image.
     * @param basicHdu current HDU
     * @param axesLength number of axes in FITS
     * @return Returns a HDU as a cut off of basicHDU
     * @throws FitsException FITS Exception
     * @throws IOException IO Exception
     */
    private BasicHDU cutoutData(final BasicHDU basicHdu, int axesLength) throws FitsException, IOException {
        assert basicHdu != null;
        if (axesLength == 2) {
            return cutoutImage(basicHdu);
        } else if (axesLength == 3) {
            return cutoutCube(basicHdu);
        } else {
            return basicHdu;
        }
    }

    /**
     * Check if the current HDU is an image.
     * @param basicHdu current HDU
     * @return Returns true when the current HDU is an image otherwise false;
     */
    private boolean isAnImage(final BasicHDU basicHdu) {
        assert basicHdu != null;
        try {
            return (basicHdu instanceof ImageHDU && basicHdu.getAxes() != null) ? true : false;
        } catch (FitsException ex) {
            return false;
        }
    }

    /**
     * Add a SITools2 as creator in the keyword list.
     * @param basicHdu current HDU
     */
    private void createSItools2Creator(final BasicHDU basicHdu) {
        assert basicHdu != null;
        try {
            basicHdu.addValue("CREATOR", "SITools2", "http://sitools2.sourceforge.net");
        } catch (HeaderCardException ex) {
            Logger.getLogger(CutOffSITools2.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    /**
     * Returns the supported file format.
     * @return supported file format
     */
    public final SupportedFileFormat getFileFormat() {
        return this.formatOutput;
    }

    /**
     * FlipImage to convert imageData from FITS to graphic formats
     * because the position (0,0) for FITS is not located at the same
     * place for a graphic format.
     * @param imageData Image data part
     * @return Returns the converted image data part for graphic formats
     */
    private Object flipImageForGraphicCreation(final Object imageData) {
        assert imageData != null;
        Object flipImageDataToReturn = null;
        // detect when imageData is an array of bytes and apply both cutoutProcessing and flip
        if (imageData instanceof byte[][]) {
            byte[][] flipImageData = new byte[this.height][this.width];
            byte[][] imageDataByte = (byte[][]) imageData;
            for (int i = 0; i < this.height; i++) {
                for (int j = 0; j < this.width; j++) {
                    int inverseCoord = this.height - i - 1;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j];
                }
            }
            flipImageDataToReturn = flipImageData;
            // detect when imageData is an array of integers and apply both cutoutProcessing and flip
        } else if (imageData instanceof int[][]) {
            int[][] flipImageData = new int[this.height][this.width];
            int[][] imageDataByte = (int[][]) imageData;
            for (int i = 0; i < this.height; i++) {
                for (int j = 0; j < this.width; j++) {
                    int inverseCoord = this.height - i - 1;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j];
                }
            }
            flipImageDataToReturn = flipImageData;
            // detect when imageData is an array of floats and apply both cutoutProcessing and flip
        } else if (imageData instanceof float[][]) {
            float[][] flipImageData = new float[this.height][this.width];
            float[][] imageDataByte = (float[][]) imageData;
            for (int i = 0; i < this.height; i++) {
                for (int j = 0; j < this.width; j++) {
                    int inverseCoord = this.height - i - 1;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j];
                }
            }
            flipImageDataToReturn = flipImageData;
            // detect when imageData is an array of doubles and apply both cutoutProcessing and flip
        } else if (imageData instanceof double[][]) {
            double[][] flipImageData = new double[this.height][this.width];
            double[][] imageDataByte = (double[][]) imageData;
            for (int i = 0; i < this.height; i++) {
                for (int j = 0; j < this.width; j++) {
                    int inverseCoord = this.height - i - 1;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j];
                }
            }
            flipImageDataToReturn = flipImageData;
        } else {
            // this case is not possible           
            Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, "cutoutAndFlipImage: the format is not recogized and this shoudl not possible");
            throw new Error("flipImageForGraphicCreation: the format is not recogized and this should not be possible");
        }
        return flipImageDataToReturn;
    }

    /**
     * Cut off the image according to xpox, ypos, height and width parameters.
     * Then FlipImage to convert imageData from FITS to graphic formats.
     * This is needed because the position (0,0) for FITS is not located at the
     * same place for Graphics format
     * @param imageData image data coming from FITS
     * @param naxis1 naxis1
     * @param naxis2 naxis2
     * @return the cutoutProcessing image Data for graphics format
     */
    private Object cutoutAndFlipImage(final Object imageData, int naxis1, int naxis2) {
        assert imageData != null && naxis1 != 0 && naxis2!=0;        
        List<Integer> borders = getBorders(naxis1, naxis2);
        int widthCut = borders.get(2);
        int heightCut = borders.get(3);
        int xpos = borders.get(0);
        int ypos = borders.get(1);

        Object flipImageDataToReturn = null;
        // detect when imageData is an array of bytes and apply both cutoutProcessing and flip
        if (imageData instanceof byte[][]) {
            byte[][] flipImageData = new byte[heightCut][widthCut];
            byte[][] imageDataByte = (byte[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    int inverseCoord = heightCut - i - 1 + ypos;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j + xpos];
                }
            }
            flipImageDataToReturn = flipImageData;
            // detect when imageData is an array of integers and apply both cutoutProcessing and flip
        } else if (imageData instanceof short[][]) {
            short[][] flipImageData = new short[heightCut][widthCut];
            short[][] imageDataByte = (short[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    int inverseCoord = heightCut - i - 1 + ypos;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j + xpos];
                }
            }
            flipImageDataToReturn = flipImageData;
        } else if (imageData instanceof int[][]) {
            int[][] flipImageData = new int[heightCut][widthCut];
            int[][] imageDataByte = (int[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    int inverseCoord = heightCut - i - 1 + ypos;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j + xpos];
                }
            }
            flipImageDataToReturn = flipImageData;
            // detect when imageData is an array of floats and apply both cutoutProcessing and flip    
        } else if (imageData instanceof float[][]) {
            float[][] flipImageData = new float[heightCut][widthCut];
            float[][] imageDataByte = (float[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    int inverseCoord = heightCut - i - 1 + ypos;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j + xpos];
                }
            }
            flipImageDataToReturn = flipImageData;
            // detect when imageData is an array of doubles and apply both cutoutProcessing and flip
        } else if (imageData instanceof double[][]) {
            double[][] flipImageData = new double[heightCut][widthCut];
            double[][] imageDataByte = (double[][]) imageData;
            for (int i = 0; i < heightCut; i++) {
                for (int j = 0; j < widthCut; j++) {
                    int inverseCoord = heightCut - i - 1 + ypos;
                    flipImageData[i][j] = imageDataByte[inverseCoord][j + xpos];
                }
            }
            flipImageDataToReturn = flipImageData;
        } else {
            // this case is not possible           
            Logger.getLogger(CutOffSITools2.class.getName()).log(Level.SEVERE, null, "cutoutAndFlipImage: the format is not recogized and this should not possible");
            throw new Error("cutoutAndFlipImage: the format is not recogized and this should not be possible");
        }
        return flipImageDataToReturn;
    }

    @Override
    public void createCutoutPreview(final OutputStream os) throws CutOffException {
       assert os != null;      
        try {

            //BasicHDU basicHdu = this.fits.getHDU(0);
            BasicHDU basicHdu = this.fits.getHDU(this.hduNumber);
            //do {
            Header hdr = basicHdu.getHeader();
            if (basicHdu instanceof ImageHDU && basicHdu.getAxes() != null) {
                int[] axes = basicHdu.getAxes();
                if (axes.length == 2) {
                    this.setFormatOutput(SupportedFileFormat.PNG);
                    Object imageData = cutoutProcessing(basicHdu);
                    imageData = flipImageForGraphicCreation(imageData);
                    FITSImage fitsImage = createFitsBufferedImage(imageData);
                    if (!ImageIO.write(fitsImage, "png", os)) {
                        throw new CutOffException("Cannot create PNG");
                    }
                } else if (axes.length == 3) {
                    this.setFormatOutput(SupportedFileFormat.GIF_ANIMATED);
                    int naxis3 = axes[0];
                    AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
                    gifEncoder.setDelay(1000);
                    gifEncoder.setRepeat(0);
                    gifEncoder.start(os);
                    for (int j = 0; j < naxis3; j++) {
                        Object imageData = extractImageFromCube(basicHdu, j);
                        imageData = cutoutAndFlipImage(imageData, hdr.getIntValue("NAXIS1"), hdr.getIntValue("NAXIS2"));
                        FITSImage fitsImage = createFitsBufferedImage(imageData);
                        gifEncoder.addFrame(fitsImage);
                    }
                    if (!gifEncoder.finish()) {
                        throw new CutOffException("Problem when GIF creation");
                    }
                }
            }
        } catch (DataTypeNotSupportedException ex) {
            throw new CutOffException(ex);
        } catch (NoImageDataFoundException ex) {
            throw new CutOffException(ex);
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        } catch (IOException ex) {
            throw new CutOffException(ex);
        }        
    }

    @Override
    public void createCutoutFits(final OutputStream os) throws CutOffException {
        assert os != null;
        this.setFormatOutput(SupportedFileFormat.FITS);

        try {
            Fits outputFits = new Fits();
            BasicHDU basicHdu = this.fits.getHDU(this.hduNumber);
            if (isAnImage(basicHdu)) {
                BasicHDU currentHdu = cutoutData(basicHdu, basicHdu.getAxes().length);
                createSItools2Creator(currentHdu);
                outputFits.addHDU(currentHdu);
            } else {
                outputFits.addHDU(basicHdu);
            }
            DataOutputStream dos = new DataOutputStream(os);
            outputFits.write(dos);
            dos.close();
        } catch (IOException ex) {
            throw new CutOffException(ex);
        } catch (FitsException ex) {
            throw new CutOffException(ex);
        }
    }

    @Override
    public final boolean isGraphicAvailable() {
        return true;
    }

    @Override
    public final boolean isFitsAvailable() {
        return true;
    }

    @Override
    public final SupportedFileFormat getFormatOutput() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Set the output format.
     * @param format output format
     */
    private void setFormatOutput(final SupportedFileFormat format) {
        this.formatOutput = format;
    }

    @Override
    public final boolean getIsDataCube() {
        return this.isDataCube;
    }
}
