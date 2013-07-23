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
import java.io.FileWriter;
import java.io.IOException;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

/**
 *
 * @author Jean-Christophe Malpaert
 */
public class JobIdDirectory extends SitoolsParameterizedResource {

    private UwsApplicationPlugin app;
    private Object jobId;
    private Object fileId;

    /**
     * File/directory handling
     * @throws ResourceException
     */
    @Override
    public void doInit() throws ResourceException {
        super.doInit();
        this.app = (UwsApplicationPlugin) getApplication();
        this.jobId = getRequestAttributes().get("job-id");
        this.fileId = getRequestAttributes().get("file-id");
    }

    /**
     * Create a new directory in the Storage system
     * @param jobId Directory to create
     */
    @Post
    public void createNewDirectory(String jobId) {      
        //File fb = new File(this.app.getStorageDirectory() + File.separator + jobId);
        File fb = new File(this.app.getStorageDirectory() + File.separator + jobId);
        fb.mkdir();
    }

    /**
     * File copy utility
     * @param fileItemRep Representation of the file to copy
     * @throws ResourceException File copy error (Status 500)
     */
    @Put
    public void copyFile(Representation fileItemRep) throws ResourceException, IOException {
        FileWriter fileToCopyWriter = null;
        //File fileToCopy = new File(this.app.getStorageDirectory() + File.separator + this.jobId + File.separator + this.fileId);
        File fileToCopy = new File(this.app.getStorageDirectory() + File.separator + jobId + File.separator + this.fileId);
        try {
            fileToCopy.createNewFile();
            fileToCopyWriter = new FileWriter(fileToCopy);
            fileToCopyWriter.write(fileItemRep.getText());
            fileToCopyWriter.close();
        } catch (IOException ex) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }
    }

    /**
     * Clean up directory
     * @throws ResourceException Clean up error
     */
    @Delete
    public void removeDirectory() throws ResourceException {
        boolean success = deleteDir(new File(this.app.getStorageDirectory() + File.separator + this.jobId));
        //boolean success = deleteDir(new File(this.app.getStorageDirectory() + File.separator + this.jobId));
        if (!success) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Cannot remove the directory");
        }

    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

}
