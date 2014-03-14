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

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.engine.Engine;
import org.restlet.representation.OutputRepresentation;

import fr.cnes.sitools.astro.cutout.CutOutException;
import fr.cnes.sitools.astro.cutout.CutOutInterface;

/**
 * Creates a cutOut on a FITS image.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CutOutRepresentation extends OutputRepresentation {
  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(CutOutRepresentation.class.getName());
  /**
   * Interface.
   */
    private CutOutInterface cutout;
    /**
     * Default output filename.
     */
    private String filename = "cutOut.fits";

    /**
     * Constructor.
     * @param media media type
     */
    protected CutOutRepresentation(final MediaType media) {
        super(media);
    }
    /**
     * Constructs a new cutOut representation.
     *
     * @param media media type
     * @param cutOut curoff object
     */
    public CutOutRepresentation(final MediaType media, final CutOutInterface cutOut) {
        super(media);
        this.cutout = cutOut;
    }
    /**
     * Sets the output filename.
     * @param filenameVal output filename
     */
    public final void setFilename(final String filenameVal) {
        this.filename = filenameVal;
    }
    /**
     * Returns the output filename.
     * @return the output filename
     */
    public final String getFilename() {
        return this.filename;
    }

    /**
     * Returns the cutOut object.
     * @return the cutout
     */
    protected final CutOutInterface getCutout() {
        return cutout;
    }

    /**
     * Sets the cutout.
     * @param cutoutVal the cutout to set
     */
    protected final void setCutout(final CutOutInterface cutoutVal) {
        this.cutout = cutoutVal;
    }

    /**
     * Writes the response.
     * <p>
     * A RuntimeException is raised when a problem happens while the cut out is processed.
     * </p>
     *
     * @param out output stream
     * @throws IOException IO Exception
     */
    @Override
    public final void write(final OutputStream out) throws IOException {
        try {
            if (getMediaType().equals(MediaType.IMAGE_PNG)
                || getMediaType().equals(MediaType.IMAGE_JPEG)
                || getMediaType().equals(MediaType.IMAGE_GIF)) {
                this.getCutout().createCutoutPreview(out);
            } else {
                final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
                disp.setFilename(getFilename());
                this.setDisposition(disp);
                this.getCutout().createCutoutFits(out);
            }
        } catch (CutOutException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new RepresentationRuntimeException(ex);
        }
    }
}
