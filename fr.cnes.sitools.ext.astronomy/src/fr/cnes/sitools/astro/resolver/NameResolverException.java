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
package fr.cnes.sitools.astro.resolver;

import java.util.logging.Logger;
import org.restlet.data.Status;

/**
 * Name Resolver Exception
 * 
 * @author Jean-Christophe Malapert
 */
public class NameResolverException extends Exception {
    
    private Status status;

    /**
     *
     * @param status
     */
    public NameResolverException(Status status) {
        this.status = status;
    }

    /**
     *
     * @param status
     * @param string
     * @param thrw
     */
    public NameResolverException(Status status, String string, Throwable thrw) {
        super(string, thrw);
        this.status = status;
    }

    /**
     *
     * @param status
     * @param string
     */
    public NameResolverException(Status status, String string) {
        super(string);
        this.status = status;
    }

    /**
     *
     * @param status
     * @param thrw
     */
    public NameResolverException(Status status, Throwable thrw) {
        super(thrw);
        this.status = status;
    }
    
    /**
     *
     * @return
     */
    public Status getStatus() {
        return this.status;
    }
    private static final Logger LOG = Logger.getLogger(NameResolverException.class.getName());
}
