 /*******************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.resource.ClientResource;

import fr.cnes.sitools.extensions.common.AstroCoordinate;

/**
 * cut out Service from CDS.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CutOutCDS implements CutOutInterface {
    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(CutOutCDS.class.getName());

    /**
     * CDS service to get a preview.
     */
    private static String cdsService = "http://alasky.u-strasbg.fr/cgi/portal/aladin/get-preview-img.py?pos=<ra>,<dec>";
    /**
     * Right ascension in ICRS frame in decimal degrees.
     */
    private double rightAscension;
    /**
     * Declination in ICRS frame in decimal degrees.
     */
    private double declination;
    /**
     * Initialize formatOutput to Not defined.
     */
    private SupportedFileFormat formatOutput = SupportedFileFormat.NOT_DEFINED;

    /**
     * Maximum value for the right ascension sets to 360°.
     */
    private static final double MAX_RA = 360.0;

    /**
     * Minimum value for the right ascension sets to 0°.
     */
    private static final double MIN_RA = 0.;

    /**
     * Minimum value for the declination sets to -90°.
     */
    private static final double MIN_DEC = -90.;

    /**
     * Maximum value for the declination sets to 90°.
     */
    private static final double MAX_DEC = 90.;

    /**
     * Empty constructor.
     */
    protected CutOutCDS() {
        setRightAscension(Double.NaN);
        setDeclination(Double.NaN);
    }

    /**
     * Constructs a CutOut based on decimal coordinates.
     * @param raVal right ascension in decimal degree
     * @param decVal declination in decimal degree
     */
    public CutOutCDS(final double raVal, final double decVal) {
        setRightAscension(raVal);
        setDeclination(decVal);
        checkInputs();
    }

    /**
     * Constructor based on sexagesimal coordinates.
     * @param raVal right ascension in sexagesimal
     * @param decVal declination in sexagesimal
     * @RuntimeException - if coordinates format is not valid
     */
    public CutOutCDS(final String raVal, final String decVal) {
        final AstroCoordinate astro = new AstroCoordinate(raVal, decVal);
        setRightAscension(astro.getRaAsDecimal());
        setDeclination(astro.getDecAsDecimal());
        checkInputs();
    }
    /**
     * Check the input parameters.
     * <p>
     * The input parameters are considered as valid when:
     * <ul>
     * <li>the right ascension is included within [MIN_RA, MAX_RA]</li>
     * <li>the declination is included within [MIN_DEC, MAX_DEC]</li>
     * </ul>
     * </p>
     */
    protected final void checkInputs() {
        if (getRightAscension() >= MIN_RA && getRightAscension() <= MAX_RA && getDeclination() >= MIN_DEC && getDeclination() <= MAX_DEC) {
            LOG.log(Level.FINE, "Input : Right Ascension(°)", getRightAscension());
            LOG.log(Level.FINE, "Input : Declination(°)", getDeclination());
        } else {
            LOG.log(Level.SEVERE, String.format("Coordinates (ra,dec)=(%s,%s) are out valid range", getRightAscension(), getDeclination()));
            throw new IllegalArgumentException(String.format("Coordinates (ra,dec)=(%s,%s) are out valid range",
                                                getRightAscension(), getDeclination()));
        }
    }

    /**
     * Returns the right ascension in ICRS frame in decimal degrees.
     * @return the right ascension in ICRS frame in decimal degrees
     */
    protected final double getRightAscension() {
        return this.rightAscension;
    }
    /**
     * Returns the declination in ICRS frame in decimal degrees.
     * @return the declination in ICRS frame in decimal degrees
     */
    protected final double getDeclination() {
        return this.declination;
    }
    /**
     * Sets the right ascension in decimal degrees in ICRS frame.
     * @param raVal the right ascension in decimal degrees in ICRS frame
     */
    protected final void setRightAscension(final double raVal) {
        this.rightAscension = raVal;
    }
    /**
     * Sets the declination in decimal degrees in ICRS frame.
     * @param decVal the declination in decimal degrees in ICRS frame
     */
    protected final void setDeclination(final double decVal) {
        this.declination = decVal;
    }

    /**
     * Call CDS webservice.
     * @param out Output stream
     * @return False when an error occurs
     * @throws CutOutException - when an error occurs
     */
    public final boolean createCutoutStream(final OutputStream out) throws CutOutException {
        final String urlTmp = cdsService.replace("<ra>", String.valueOf(getRightAscension()));
        final String url = urlTmp.replace("<dec>", String.valueOf(getDeclination()));
        LOG.log(Level.INFO, "Calling URL for CDS cutout", url);
        final ClientResource client = new ClientResource(Method.GET, url);
        final Status status = client.getStatus();
        boolean isFine = status.isSuccess();
        if (isFine) {
            try {
                client.get().write(out);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, url, ex);
                throw new CutOutException(ex);
            }
        } else {
            LOG.log(Level.WARNING, "{0} : {1}", new Object[]{url, status.getDescription()});
            isFine = false;
        }
        return isFine;
    }

    @Override
    public final boolean isGraphicAvailable() {
        return true;
    }

    @Override
    public final boolean isFitsAvailable() {
        return false;
    }

    @Override
    public final void createCutoutPreview(final java.io.OutputStream outputStream) throws CutOutException {
        setFormatOutput(SupportedFileFormat.PNG);
        if (!this.createCutoutStream(outputStream)) {
            LOG.log(Level.WARNING, "Cannot get the response from CDS");
            throw new CutOutException("Cannot get the response from CDS");
        }
    }

    @Override
    public final void createCutoutFits(final java.io.OutputStream outputStream) throws CutOutException {
        setFormatOutput(SupportedFileFormat.NOT_DEFINED);
        LOG.log(Level.SEVERE, "This service does not provide a cut out as FITS");
        throw new CutOutException("This service does not provide a cut out as FITS");
    }

    @Override
    public final SupportedFileFormat getFormatOutput() {
        return this.formatOutput;
    }

    /**
     * Set the output format.
     * @param outputFormatVal Output format
     */
    private void setFormatOutput(final SupportedFileFormat outputFormatVal) {
        this.formatOutput = outputFormatVal;
    }

    @Override
    public final boolean isDataCube() {
        return false;
    }
}
