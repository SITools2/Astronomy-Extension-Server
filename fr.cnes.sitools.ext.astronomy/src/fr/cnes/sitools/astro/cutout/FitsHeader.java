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
package fr.cnes.sitools.astro.cutout;

import java.util.logging.Logger;
import jsky.coords.WCSKeywordProvider;
import nom.tam.fits.Header;

/**
 * Implements the WCS Keyword Provider interface to use this object
 * for WCS processing.
 * @author Jean-Christophe Malapert
 */
public class FitsHeader implements WCSKeywordProvider{
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(FitsHeader.class.getName());
    /**
     * Initialize Header FITS.
     */
    private Header fitsHdr = null;

    /**
     * Constructs a new FitsHeader.
     * @param fitsHeader header FITS
     */
    public FitsHeader(final Header fitsHeader) {
        this.fitsHdr = fitsHeader;
    }

    @Override
    public final boolean findKey(final String key) {
        return this.fitsHdr.containsKey(key);
    }

    @Override
    public final String getStringValue(final String key) {
        return this.fitsHdr.getStringValue(key);
    }

    @Override
    public final String getStringValue(final String key, final String defaultValue) {
        return this.fitsHdr.getStringValue(key);
    }

    @Override
    public final double getDoubleValue(final String key) {
        return this.fitsHdr.getDoubleValue(key);
    }

    @Override
    public final double getDoubleValue(final String key, final double defaultValue) {
        return this.fitsHdr.getDoubleValue(key, defaultValue);
    }

    @Override
    public final float getFloatValue(final String key) {
        return this.fitsHdr.getFloatValue(key);
    }

    @Override
    public final float getFloatValue(final String key, final float defaultValue) {
        return this.fitsHdr.getFloatValue(key, defaultValue);
    }

    @Override
    public final int getIntValue(final String key) {
        return this.fitsHdr.getIntValue(key);
    }

    @Override
    public final int getIntValue(final String key, final int defaultValue) {
        return this.fitsHdr.getIntValue(key, defaultValue);
    }
}
