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
package fr.cnes.sitools.extensions.astro.uws.storage;

import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.uws.UwsApplicationPlugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

/**
 * Resource that handles the system cache.
 *
 * @author Jean-Christophe Malapert
 */
public class StoreObject extends SitoolsParameterizedResource {

    private UwsApplicationPlugin app;
    private static final String cachedFileName = "save.xml";

    @Override
    public void doInit() throws ResourceException {
        super.doInit();
        this.app = (UwsApplicationPlugin) getApplication();
    }

    /**
     * Jobs caching. This methods handles conccurent access by the use of FileLock object
     * @param object XML representation of the cache
     */
    @Post
    public void acceptObject(String object) throws IOException {
        FileOutputStream fout = null;
        FileLock lock = null;
        try {
            fout = new FileOutputStream(this.app.getStorageDirectory() + File.separator + cachedFileName);
            lock = fout.getChannel().lock();
            fout.write(object.getBytes());
        } catch (FileNotFoundException ex) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,ex);
        } catch (IOException ex) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL,ex);
        } finally {
            try {
                fout.close();
                lock.release();
            } catch (IOException ex) {                
            }
        }
    }
}
