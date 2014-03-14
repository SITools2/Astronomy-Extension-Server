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

import java.util.logging.Logger;

import org.restlet.engine.Engine;

/**
 * Exception.
 * @author Jean-Christophe Malapert
 */
public class CutOutException extends Exception {
    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(CutOutException.class.getName());
    /**
     * Constructor.
     */
    public CutOutException() {
        super();
    }

    /**
     * Constructor.
     * @param message Message
     */
    public CutOutException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     * @param thrw Throwable Exception
     */
    public CutOutException(final Throwable thrw) {
        super(thrw);
    }

    /**
     * Constructor.
     * @param message message
     * @param thrw Throwable Exception
     */
    public CutOutException(final String message, final Throwable thrw) {
        super(message, thrw);
    }
}
