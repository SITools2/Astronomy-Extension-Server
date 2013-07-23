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

package fr.cnes.sitools.astro.representation;

import cds.moc.HealpixMoc;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

/**
 * FITS Representation for Healpix multi-resolution order.
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class FitsMocRepresentation extends OutputRepresentation {

    static {
        MediaType.register("image/fits", "FITS image");
    }
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(FitsMocRepresentation.class.getName());
    /**
     * output filename.
     */
    private String filename;
    /**
     * Moc : information where the information is stored.
     */
    private HealpixMoc moc;

    /**
     * Constructs a new FITS representation with MOC.
     * @param mocVal healpix MOC
     */
    public FitsMocRepresentation(final HealpixMoc mocVal) {
        super(MediaType.valueOf("image/fits"));
        setMoc(mocVal);
    }

    /**
     * Constructs a new FITS representation for which the filename
     * is given by the user.
     * @param filenameVal FITS filename
     * @param mocVal healpix MOC
     */
    public FitsMocRepresentation(final String filenameVal, final HealpixMoc mocVal) {
        this(mocVal);
        setFilename(filenameVal);
        final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
        disp.setFilename(filenameVal);
        this.setDisposition(disp);
    }

    @Override
    public final void write(final OutputStream out) throws IOException {
        try {            
            getMoc().writeFits(out);
            LOG.log(Level.FINEST, filename, out);
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Returns the FITS filename.
     * @return the filename
     */
    protected final String getFilename() {
        return filename;
    }

    /**
     * Sets the output filename.
     * @param filenameVal the filename to set
     */
    protected final void setFilename(final String filenameVal) {
        this.filename = filenameVal;
    }

    /**
     * Returns the Healpix MOC.
     * @return the Healpix MOC
     */
    protected final HealpixMoc getMoc() {
        return moc;
    }

    /**
     * Sets the Healpix MOC to convert into FITS.
     * @param mocVal the moc to set
     */
    protected final void setMoc(final HealpixMoc mocVal) {
        this.moc = mocVal;
    }    
}
