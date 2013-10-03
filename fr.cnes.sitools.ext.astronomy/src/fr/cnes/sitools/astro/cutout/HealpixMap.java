/**
 * *****************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * SITools2. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package fr.cnes.sitools.astro.cutout;

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.extensions.common.Utility;
import healpix.core.HealpixIndex;
import healpix.essentials.Pointing;
import healpix.essentials.Scheme;
import java.awt.geom.Point2D;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;

/**
 * Creates a map from Planck data.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class HealpixMap implements CutOutInterface {

    /**
     * Planck data for testing.
     */
    public static final String FILENAME = "/media/malapert/Data/Herschel/HFI_SkyMap_857_2048_R1.10_nominal.fits";

    /**
     * Arcsec per pixel along X axis.
     */
    private double cdelt1;
    /**
     * Arcsec per pixel along Y axis.
     */
    private double cdelt2;
    /**
     * Shape coordinates.
     */
    private double[] fovCoordinates;
    /**
     * Rotation.
     */
    private double rotation;
    /**
     * Coordinate system input.
     */
    protected AstroCoordinate.CoordinateSystem coordinateSystemInput;
    /**
     * Coordinate system output.
     */
    private AstroCoordinate.CoordinateSystem coordinateSystemOutput;    
    /**
     * HDU that contains the Planck data.
     */
    private transient BinaryTableHDU hdu;
    /**
     * WCS.
     */
    private transient WcsComputation wcs;
    /**
     * Fits file.
     */
    private File file;

    /**
     * Contructs a background map.
     *
     * @param cdelt1 Arcsec per pixel along X axis
     * @param cdelt2 Arcsec per pixel along Y axis
     * @param coordinates shape
     * @param file FITS to cut
     * @throws IOException
     * @throws Exception
     */    
    public HealpixMap(final double cdelt1, final double cdelt2, final double[] coordinates, final double rotation, final File file, final AstroCoordinate.CoordinateSystem coordinateSystemInput) throws CutOutException {
        try {
            setCdelt1(cdelt1);
            setCdelt2(cdelt2);
            setRotation(rotation);
            setCoordinateSystemInput(coordinateSystemInput);
            setFile(file);
            final BinaryTableHDU hduTable = loadBinaryHDU(new Fits(file));
//        final String coordSystem = hduTable.getHeader().getStringValue("COORDSYS");
            setHdu(hduTable);
            setFovCoordinates(coordinates);
            setWcs(new WcsComputation(coordinates, cdelt1, cdelt2, rotation, coordinateSystemInput));
        } catch (FitsException ex) {
            Logger.getLogger(HealpixMap.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOutException(ex.getMessage());
        } catch (Exception ex) {
            Logger.getLogger(HealpixMap.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOutException(ex.getMessage());
        }
    }

    /**
     * Loads the first binary table extension.
     *
     * @param fitsToCut fits where the information is located
     * @return the first binary table extension
     * @throws FitsException
     * @throws IOException
     */
    private BinaryTableHDU loadBinaryHDU(final Fits fitsToCut) throws FitsException, IOException {
        BinaryTableHDU binaryHdu = null;
        final int numberHDUs = 2;
        for (int i = 0; i < numberHDUs; i++) {
            final BasicHDU basicHDU = fitsToCut.getHDU(i);
            final Header headerHDU = basicHDU.getHeader();
            if (headerHDU.containsKey("XTENSION") && "BINTABLE".equals(headerHDU.getStringValue("XTENSION"))) {
                binaryHdu = (BinaryTableHDU) basicHDU;
                break;
            }
        }
        if (Utility.isSet(binaryHdu)) {
            return binaryHdu;
        } else {
            throw new FitsException("binaryHdu not found");
        }
    }
    
    private BasicHDU createPrimaryHDU(File filename, double cdelt1, double cdelt2, double[] fov, double rotation) throws HeaderCardException, FitsException {
        final Header hdr = new Header();
        hdr.addValue("SIMPLE", "T", "");
        hdr.addValue("BITPIX", 8, "");
        hdr.addValue("NAXIS", 0, "");
        hdr.addValue("ORIGIN", "SITools2", "http://sitools2.sourceforge.net");
        hdr.addValue("EXTEND", "T", "");
        hdr.insertHistory("CUT FITS DATE : " + GregorianCalendar.getInstance().getTime().toString());
        hdr.insertHistory("Processed from " + filename.getName());
        hdr.insertHistory("Input parameters:");
        hdr.insertHistory("         - Arsec per pixel along x axis: " + cdelt1);
        hdr.insertHistory("         - Arsec per pixel along Y axis: " + cdelt2);
        hdr.insertHistory("         - rotation in degree: " + rotation);
        hdr.insertHistory(String.format("         - fov:[[%s,%s];[%s,%s];[%s,%s],[%s,%s]]", fov[0], fov[1], fov[2], fov[3], fov[4], fov[5], fov[6], fov[7]));
        hdr.insertHistory("         - Reference system of the FOV: " + getCoordinateSystemInput());
        return Fits.makeHDU(hdr);
    }
    
    private BasicHDU createExtension(Object data, String extName, String unit, WcsComputation wcs) throws FitsException {
        final BasicHDU hduExt = Fits.makeHDU(data);
        hduExt.addValue("EXTNAME", extName, "");
        hduExt.addValue("CRVAL1", wcs.getCrval1(), "Longitude at reference pixel (deg)");
        hduExt.addValue("CRVAL2", wcs.getCrval2(), "Latitude at reference pixel (deg)");
        hduExt.addValue("CRPIX1", wcs.getCrpix1(), "Reference pixel on the first axis");
        hduExt.addValue("CRPIX2", wcs.getCrpix2(), "Reference pixel on the second axis");
        hduExt.addValue("CD1_1", wcs.getCd()[0], "partial of the right ascension w.r.t. x");
        hduExt.addValue("CD1_2", wcs.getCd()[1], "partial of the right ascension w.r.t. y");
        hduExt.addValue("CD2_1", wcs.getCd()[2], "partial of the declination w.r.t. x");
        hduExt.addValue("CD2_2", wcs.getCd()[3], "partial of the declination w.r.t. y");
        hduExt.addValue("CTYPE1", wcs.getCtype1(), "first coordinate type");
        hduExt.addValue("CTYPE2", wcs.getCtype2(), "seconde coordinate type");        
        hduExt.addValue("CUNIT1", "deg", "Unit of the first axis");
        hduExt.addValue("CUNIT2", "deg", "Unit of the second axis");
        hduExt.addValue("UNIT", unit, "Unit of the density");
        return hduExt;
    }

    
    public Fits compute() throws FitsException, IllegalAccessException, Exception {        
        final Fits fitsOutput = new Fits();
        final Header hdr = getHdu().getHeader();
        final String pixType = (hdr.containsKey("PIXTYPE")) ? hdr.getStringValue("PIXTYPE") : "NONE";
        if (! pixType.equals("HEALPIX")) {
            throw new CutOutException(getFile() + " is not a Healpix Map");
        }
        final String ordering = (hdr.containsKey("ORDERING")) ? hdr.getStringValue("ORDERING") : "NESTED";
        final int nbCols = getHdu().getNCols();
        final int nside = hdr.getIntValue("NSIDE");
        final double notANumber = (hdr.containsKey("BAD_DATA")) ? hdr.getDoubleValue("BAD_DATA") : Double.NaN;
        final HealpixIndex index = new HealpixIndex(nside, Scheme.valueOf(ordering));
        fitsOutput.addHDU(createPrimaryHDU(getFile(), getCdelt1(), getCdelt2(), getFovCoordinates(), getRotation()));
        Object data;

        for (int col = 0; col < nbCols; col++) {
            final String name = getHdu().getColumnName(col);
            final String format = getHdu().getColumnFormat(col);
            final String unit = getHdu().getColumnMeta(col, "TUNIT");
            if ("I".equals(format)) {
                short[][] dataShort = new short[this.getWcs().getNaxis2()][this.getWcs().getNaxis1()];
                 for (int y = 1; y <= this.getWcs().getNaxis2(); y++) {
                    for (int x = 1; x <= this.getWcs().getNaxis1(); x++) {
                        final Point2D.Double skyPos = this.getWcs().pix2wcs(x, y);
                        switch(getCoordinateSystemInput()) {
                            case GALACTIC:                                
                                break;
                            case EQUATORIAL:
                                AstroCoordinate astro = new AstroCoordinate(skyPos.getX(), skyPos.getY());
                                astro.processTo(AstroCoordinate.CoordinateSystem.GALACTIC);
                                skyPos.setLocation(astro.getRaAsDecimal(), astro.getDecAsDecimal());
                                break;
                            default:
                                throw new IllegalAccessException(getCoordinateSystemInput() + " is not supported");
                        }
                        final long healpixPixel = index.ang2pix(new Pointing(Math.PI / 2.0 - Math.toRadians(skyPos.getY()), Math.toRadians(skyPos.getX())));
                        final Object[] row = this.getHdu().getRow((int) healpixPixel);
                        final short[] val = (short[]) row[col];                        
                        dataShort[y - 1][x - 1] = val[0];
                    }
                }
                data = dataShort;
                fitsOutput.addHDU(createExtension(data, name, unit, getWcs()));
            } else if ("J".equals(format)) {
                int[][] dataInt = new int[this.getWcs().getNaxis2()][this.getWcs().getNaxis1()];
                for (int y = 1; y <= this.getWcs().getNaxis2(); y++) {
                    for (int x = 1; x <= this.getWcs().getNaxis1(); x++) {
                        final Point2D.Double skyPos = this.getWcs().pix2wcs(x, y);
                        switch(getCoordinateSystemInput()) {
                            case GALACTIC:
                                break;
                            case EQUATORIAL:
                                final AstroCoordinate astro = new AstroCoordinate(skyPos.getX(), skyPos.getY());
                                astro.processTo(AstroCoordinate.CoordinateSystem.GALACTIC);
                                skyPos.setLocation(astro.getRaAsDecimal(), astro.getDecAsDecimal());
                                break;
                            default:
                                throw new IllegalAccessException(getCoordinateSystemInput() + " is not supported");
                        }
                        final long healpixPixel = index.ang2pix(new Pointing(Math.PI / 2.0 - Math.toRadians(skyPos.getY()), Math.toRadians(skyPos.getX())));
                        final Object[] row = this.getHdu().getRow((int) healpixPixel);
                        final int[] val = (int[]) row[col];
                        dataInt[y - 1][x - 1] = val[0];
                    }
                }
                data = dataInt;
                fitsOutput.addHDU(createExtension(data, name, unit, getWcs()));
            } else if ("K".equals(format)) {
                long[][] dataLong = new long[this.getWcs().getNaxis2()][this.getWcs().getNaxis1()];
                 for (int y = 1; y <= this.getWcs().getNaxis2(); y++) {
                    for (int x = 1; x <= this.getWcs().getNaxis1(); x++) {
                        final Point2D.Double skyPos = this.getWcs().pix2wcs(x, y);
                        switch(getCoordinateSystemInput()) {
                            case GALACTIC:                                
                                break;
                            case EQUATORIAL:
                                AstroCoordinate astro = new AstroCoordinate(skyPos.getX(), skyPos.getY());
                                astro.processTo(AstroCoordinate.CoordinateSystem.GALACTIC);
                                skyPos.setLocation(astro.getRaAsDecimal(), astro.getDecAsDecimal());
                                break;
                            default:
                                throw new IllegalAccessException(getCoordinateSystemInput() + " is not supported");
                        }                        
                        final long healpixPixel = index.ang2pix(new Pointing(Math.PI / 2.0 - Math.toRadians(skyPos.getY()), Math.toRadians(skyPos.getX())));
                        final Object[] row = this.getHdu().getRow((int) healpixPixel);
                        final long[] val = (long[]) row[col];
                        dataLong[y - 1][x - 1] = val[0];
                    }
                }
                data = dataLong;
                fitsOutput.addHDU(createExtension(data, name, unit, getWcs()));
            } else if ("E".equals(format)) {
                float[][] dataFloat = new float[this.getWcs().getNaxis2()][this.getWcs().getNaxis1()];
                for (int y = 1; y <= this.getWcs().getNaxis2(); y++) {
                    for (int x = 1; x <= this.getWcs().getNaxis1(); x++) {
                        final Point2D.Double skyPos = this.getWcs().pix2wcs(x, y);
                        switch(getCoordinateSystemInput()) {
                            case GALACTIC:                                
                                break;
                            case EQUATORIAL:
                                AstroCoordinate astro = new AstroCoordinate(skyPos.getX(), skyPos.getY());
                                astro.processTo(AstroCoordinate.CoordinateSystem.GALACTIC);
                                skyPos.setLocation(astro.getRaAsDecimal(), astro.getDecAsDecimal());
                                break;
                            default:
                                throw new IllegalAccessException(getCoordinateSystemInput() + " is not supported");
                        }                        
                        final long healpixPixel = index.ang2pix(new Pointing(Math.PI / 2.0 - Math.toRadians(skyPos.getY()), Math.toRadians(skyPos.getX())));
                        final Object[] row = this.getHdu().getRow((int) healpixPixel);
                        final float[] val = (float[]) row[col];
                        dataFloat[y - 1][x - 1] = (val[0] == notANumber) ? Float.NaN : val[0];
                    }
                }
                data = dataFloat;
                fitsOutput.addHDU(createExtension(data, name, unit, getWcs()));
            } else if ("D".equals(format)) {
                double[][] dataDouble = new double[this.getWcs().getNaxis2()][this.getWcs().getNaxis1()];
                for (int y = 1; y <= this.getWcs().getNaxis2(); y++) {
                    for (int x = 1; x <= this.getWcs().getNaxis1(); x++) {
                        final Point2D.Double skyPos = this.getWcs().pix2wcs(x, y);
                        switch(getCoordinateSystemInput()) {
                            case GALACTIC:                                
                                break;
                            case EQUATORIAL:
                                AstroCoordinate astro = new AstroCoordinate(skyPos.getX(), skyPos.getY());
                                astro.processTo(AstroCoordinate.CoordinateSystem.GALACTIC);
                                skyPos.setLocation(astro.getRaAsDecimal(), astro.getDecAsDecimal());
                                break;
                            default:
                                throw new IllegalAccessException(getCoordinateSystemInput() + " is not supported");
                        }                        
                        final long healpixPixel = index.ang2pix(new Pointing(Math.PI / 2.0 - Math.toRadians(skyPos.getY()), Math.toRadians(skyPos.getX())));
                        final Object[] row = this.getHdu().getRow((int) healpixPixel);
                        final double[] val = (double[]) row[col];
                        dataDouble[y - 1][x - 1] = (val[0] == notANumber) ? Double.NaN : val[0];
                    }
                }
                data = dataDouble;
                fitsOutput.addHDU(createExtension(data, name, unit, getWcs()));
            } else {
                throw new IllegalAccessException("Format " + format + "is not supported");
            }
        }
        return fitsOutput;
    }

    /**
     * @return the fovCoordinates
     */
    protected final double[] getFovCoordinates() {
        return fovCoordinates;
    }

    /**
     * @param fovCoordinates the fovCoordinates to set
     */
    protected final void setFovCoordinates(final double[] fovCoordinates) {
        this.fovCoordinates = fovCoordinates;
    }

    public static void main(String[] args) throws FitsException, Exception {
        File file = new File(FILENAME);
        HealpixMap fitsOutput = new HealpixMap(30, 30, new double[]{265.6, -28.43, 257.11, -23.45, 262.96, -15.23, 271.3, -20.10}, 0, file, AstroCoordinate.CoordinateSystem.EQUATORIAL);
        fitsOutput.createCutoutFits(new FileOutputStream(new File("/tmp/testEQ.fits")));
        //HealpixMap fitsOutput = new HealpixMap(30, 30, new double[]{0, 0, 0, 10, 10, 10, 10, 0}, 0, file, AstroCoordinate.CoordinateSystem.GALACTIC);
    }

    /**
     * @return the coordinateSystemInput
     */
    protected final AstroCoordinate.CoordinateSystem getCoordinateSystemInput() {
        return coordinateSystemInput;
    }

    /**
     * @param coordinateSystemInput the coordinateSystemInput to set
     */
    protected final void setCoordinateSystemInput(final AstroCoordinate.CoordinateSystem coordinateSystem) {
        this.coordinateSystemInput = coordinateSystem;
    }

    /**
     * @return the cdelt1
     */
    protected final double getCdelt1() {
        return cdelt1;
    }

    /**
     * @param cdelt1 the cdelt1 to set
     */
    protected final void setCdelt1(final double cdelt1) {
        this.cdelt1 = cdelt1;
    }

    /**
     * @return the cdelt2
     */
    protected final double getCdelt2() {
        return cdelt2;
    }

    /**
     * @param cdelt2 the cdelt2 to set
     */
    protected final void setCdelt2(final double cdelt2) {
        this.cdelt2 = cdelt2;
    }

    /**
     * @return the hdu
     */
    protected final BinaryTableHDU getHdu() {
        return hdu;
    }

    /**
     * @param hdu the hdu to set
     */
    protected final void setHdu(final BinaryTableHDU hdu) {
        this.hdu = hdu;
    }

    /**
     * @return the wcs
     */
    protected final WcsComputation getWcs() {
        return wcs;
    }

    /**
     * @param wcs the wcs to set
     */
    protected final void setWcs(final WcsComputation wcs) {
        this.wcs = wcs;
    }

    /**
     * @return the fits
     */
    protected final File getFile() {
        return file;
    }

    /**
     * @param fits the fits to set
     */
    protected final void setFile(final File file) {
        this.file = file;
    }

    /**
     * @return the rotation
     */
    protected final double getRotation() {
        return rotation;
    }

    /**
     * @param rotation the rotation to set
     */
    protected final void setRotation(final double rotation) {
        this.rotation = rotation;
    }

    @Override
    public SupportedFileFormat getFormatOutput() {
        return SupportedFileFormat.FITS;
    }

    @Override
    public void createCutoutPreview(OutputStream outputStream) throws CutOutException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createCutoutFits(OutputStream outputStream) throws CutOutException {
        try {
            Fits fits = compute();            
            final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);        
            fits.write(dataOutputStream);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(HealpixMap.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOutException(ex);
        } catch (Exception ex) {
            Logger.getLogger(HealpixMap.class.getName()).log(Level.SEVERE, null, ex);
            throw new CutOutException(ex);
        }
    }

    @Override
    public final boolean isGraphicAvailable() {
        return false;
    }

    @Override
    public final boolean isFitsAvailable() {
        return true;
    }

    @Override
    public final boolean isDataCube() {
        return false;
    }
}
