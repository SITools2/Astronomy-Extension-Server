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

import fr.cnes.sitools.extensions.common.AstroCoordinate;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

/**
 * cut out Service from CDS.
 * @author Jean-Christophe Malapert
 */
public class CutOutCDS implements CutOutInterface {
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(CutOutCDS.class.getName());

    /**
     * CDS service to get a preview.
     */
    private static String cdsService = "http://alasky.u-strasbg.fr/cgi/portal/aladin/get-preview-img.py?pos=<ra>,<dec>";
    /**
     * Initialize right ascension to 0.
     */
    private double ra = 0;
    /**
     * Initialize declination to 0.
     */
    private double dec = 0;
    /**
     * Initialize outputFormat to Not defined.
     */
    private SupportedFileFormat outputFormat = SupportedFileFormat.NOT_DEFINED;

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
     * Constructs a CutOut based on decimal coordinates.
     * @param raVal right ascension in decimal degree
     * @param decVal declination in decimal degree
     */
    public CutOutCDS(final double raVal, final double decVal) {
        if(raVal >= MIN_RA && raVal <= MAX_RA && decVal >= MIN_DEC && decVal <= MAX_DEC) {
            this.ra = raVal;
            this.dec = decVal;
        } else {
            throw new IllegalArgumentException(String.format("Coordinates (ra,dec)=(%s,%s) are out valid range", raVal,decVal));
        }
    }

    /**
     * Constructor based on sexagesimal coordinates.
     * @param raVal right ascension in sexagesimal
     * @param decVal declination in sexagesimal
     * @RuntimeException - if coordinates format is not valid
     */
    public CutOutCDS(final String raVal, final String decVal) {
        AstroCoordinate astro = new AstroCoordinate(raVal, decVal);
        this.ra = astro.getRaAsDecimal();
        this.dec = astro.getDecAsDecimal();
    }

    /**
     * Call CDS webservice.
     * @param out Output stream
     * @return False when an error occurs
     * @throws CutOutException - when an error occurs
     */
    public final boolean createCutoutStream(final OutputStream out) throws CutOutException {
        String url = cdsService.replace("<ra>", String.valueOf(ra));
        url = url.replace("<dec>", String.valueOf(dec));
        ClientResource client = new ClientResource(Method.GET, url);
        Status status = client.getStatus();
        boolean isFine = status.isSuccess();
        if (isFine) {
            try {
                client.get().write(out);
            } catch (IOException ex) {
                throw new CutOutException(ex);
            }
        } else {
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
    public final void createCutoutPreview(final java.io.OutputStream os) throws CutOutException {
        setFormatOutput(SupportedFileFormat.PNG);
        String url = cdsService.replace("<ra>", String.valueOf(ra));
        url = url.replace("<dec>", String.valueOf(dec));
        ClientResource client = new ClientResource(Method.GET, url);
        Status status = client.getStatus();
        boolean isFine = status.isSuccess();
        if (isFine) {
            try {
                client.get().write(os);
            } catch (IOException ex) {
                throw new CutOutException(ex);
            }
        } else {
            throw new CutOutException("Cannot get the response from CDS");
        }
    }

    @Override
    public void createCutoutFits(final java.io.OutputStream os) throws CutOutException {
        setFormatOutput(SupportedFileFormat.NOT_DEFINED);
        throw new CutOutException("This service does not provide a cut out as FITS");
    }

    @Override
    public final SupportedFileFormat getFormatOutput() {
        return this.outputFormat;
    }

    /**
     * Set the output format.
     * @param outputFormatVal Output format
     */
    private void setFormatOutput(final SupportedFileFormat outputFormatVal) {
        this.outputFormat = outputFormatVal;
    }

    @Override
    public final boolean getIsDataCube() {
        return false;
    }
}
