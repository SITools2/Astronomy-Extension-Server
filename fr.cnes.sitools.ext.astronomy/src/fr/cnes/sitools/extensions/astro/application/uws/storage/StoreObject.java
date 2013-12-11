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
package fr.cnes.sitools.extensions.astro.application.uws.storage;

import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.UwsApplicationPlugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.logging.Logger;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

/**
 * Resource that handles the system cache.
 *
 * @author Jean-Christophe Malapert
 */
public class StoreObject extends SitoolsParameterizedResource {

    /**
     * Uws application.
     */
    private UwsApplicationPlugin app;
    /**
     * The file where the Job manager saves its content.
     */
    private static final String CACHED_FILENAME = "save.xml";
    /**
    * Logger.
    */
    private static final Logger LOG = Logger.getLogger(StoreObject.class.getName());
    

    @Override
    public final void doInit() throws ResourceException {
        super.doInit();
        this.setApp((UwsApplicationPlugin) getApplication());
    }

    /**
     * Jobs caching. 
     * <p>
     * This methods handles conccurent access by the use of FileLock object
     * </p>
     * @param object XML representation of the cache
     */
    @Post
    public final void acceptObject(final String object) {
        LOG.finest(object);
        FileOutputStream fout = null;
        FileLock lock = null;
        try {
            fout = new FileOutputStream(this.getApp().getStorageDirectory() + File.separator + CACHED_FILENAME);
            lock = fout.getChannel().lock();
            fout.write(object.getBytes());
        } catch (FileNotFoundException ex) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex); 
        } catch (IOException ex) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);        
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
                if (lock != null) {
                    lock.release();
                }
            } catch (IOException ex) {
                LOG.fine(ex.getMessage());
            }
        }
    }

    /**
     * Returns the Uws application.
     * @return the app
     */
    private UwsApplicationPlugin getApp() {
        return app;
    }

    /**
     * Sets the Uws application.
     * @param app the app to set
     */
    private void setApp(final UwsApplicationPlugin app) {
        this.app = app;
    }
}
