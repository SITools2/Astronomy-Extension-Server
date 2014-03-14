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
package fr.cnes.sitools.extensions.astro.application.uws.common;

import org.restlet.data.Status;

/**
 * {Insert class description here}
 *
 * @author Jean-Christophe Malapert
 */
public class UniversalWorkerException extends Exception {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    private final Status status;

    /**
     * Constructor with message and throwable.
     *
     * @param message message to be sent
     * @param cause what causes exception
     */
    public UniversalWorkerException(Status status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Constructor with message.
     *
     * @param message message to be sent
     */
    public UniversalWorkerException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public UniversalWorkerException(Status status, Throwable ex) {
        super(ex);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
