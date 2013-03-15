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

import java.util.logging.Logger;

/**
 * Exception.
 * @author Jean-Christophe Malapert
 */
public class CutOffException extends Exception {
    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(CutOffException.class.getName());
    /**
     * Constructor.
     */
    public CutOffException() {
    };

    /**
     * Constructor.
     * @param message Message
     */
    public CutOffException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param thrw Throwable Exception
     */
    public CutOffException(final Throwable thrw) {
        super(thrw);
    }

    /**
     * Constructor.
     * @param message message
     * @param thrw Throwable Exception
     */
    public CutOffException(final String message, final Throwable thrw) {
        super(message, thrw);
    }
}
