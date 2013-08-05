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
import fr.cnes.sitools.extensions.astro.application.uws.common.UniversalWorkerException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;

/**
 * Provides capabilities to handle the storage of a jobID.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class JobIdDirectory extends SitoolsParameterizedResource {

    /**
     * Uws application.
     */
    private UwsApplicationPlugin app;
    /**
     * Directory in which the result must be stored.
     */
    private Object jobId;
    /**
     * File to copy.
     */
    private Object fileId;
    /**
    * Logger.
    */
    private static final Logger LOG = Logger.getLogger(JobIdDirectory.class.getName());

    /**
     * File/directory handling.
     * @throws ResourceException When an error happens
     */
    @Override
    public final void doInit() throws ResourceException {
        super.doInit();
        this.setApp((UwsApplicationPlugin) getApplication());
        this.setJobId(getRequestAttributes().get("job-id"));
        this.setFileId(getRequestAttributes().get("file-id"));
    }

    /**
     * Create a new directory in the Storage system.
     * @param jobIdVal Directory to create
     */
    @Post
    public final void createNewDirectory(final String jobIdVal) throws UniversalWorkerException {
        LOG.log(Level.FINER, "Creating {0} directory.", jobIdVal);
        final File directory = new File(this.getApp().getStorageDirectory() + File.separator + jobIdVal);
        if (!directory.mkdir()) {
            throw new UniversalWorkerException(Status.SERVER_ERROR_INTERNAL, "Cannot create " + jobIdVal);
        }
    }

    /**
     * File copy utility.
     * @param fileItemRep Representation of the file to copy
     * @throws ResourceException File copy error (Status 500)
     * @throws IOException When error happens
     */
    @Put
    public final void copyFile(final Representation fileItemRep) throws ResourceException, IOException {
        final File fileToCopy = new File(this.getApp().getStorageDirectory() + File.separator + getJobId() + File.separator + this.getFileId());
        try {
            if (!fileToCopy.createNewFile()) {
                throw new IOException("Cannot copy " + fileToCopy.getName());
            }
            final FileWriter fileToCopyWriter = new FileWriter(fileToCopy);
            fileToCopyWriter.write(fileItemRep.getText());
            fileToCopyWriter.close();
        } catch (IOException ex) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
        }
    }

    /**
     * Cleans up directory.
     * @throws ResourceException Clean up error
     */
    @Delete
    public final void removeDirectory() throws ResourceException {
        LOG.finer("Removing " + this.getApp().getStorageDirectory() + File.separator + this.getJobId());
        final boolean success = deleteDir(new File(this.getApp().getStorageDirectory() + File.separator + this.getJobId()));
        if (!success) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Cannot remove the directory");
        }

    }

    /**
     * Deletes a directory.
     * @param dir directory to delete
     * @return <code>True</code> when the directory is deleted otherwise <code>false</code>.
     */
    private static boolean deleteDir(final File dir) {
        if (dir.isDirectory()) {            
            final String[] childrenList = dir.list();
            LOG.log(Level.FINER, "Deleting {0}files in {1}", new Object[]{childrenList.length, dir.getPath()});
            for (String children : childrenList) {
                final boolean success = deleteDir(new File(dir, children));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Returns the Uws Application.
     * @return the app
     */
    private UwsApplicationPlugin getApp() {
        return app;
    }

    /**
     * Sets the Uws application.
     * @param appVal the appVal to set
     */
    private void setApp(final UwsApplicationPlugin appVal) {
        this.app = appVal;
    }

    /**
     * Returns the jobID.
     * @return the jobId
     */
    private Object getJobId() {
        return jobId;
    }

    /**
     * Sets the jobID.
     * @param jobIdVal the jobIdVal to set
     */
    private void setJobId(final Object jobIdVal) {
        this.jobId = jobIdVal;
    }

    /**
     * Returns the file identifier.
     * @return the fileId
     */
    private Object getFileId() {
        return fileId;
    }

    /**
     * Sets the file identifier.
     * @param fileIdVal the fileIdVal to set
     */
    private void setFileId(final Object fileIdVal) {
        this.fileId = fileIdVal;
    }
}
