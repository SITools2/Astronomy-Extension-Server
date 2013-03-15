/*
 * Copyright 2013 - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.Disposition;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * Publishes the Globweb configuration file.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class GlobWebResource extends SitoolsParameterizedResource {

  /**
   * the GlobWeb configuration file.
   */
  private String configurationFile;
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(GlobWebResource.class.getName());

  /**
   * Initialize the configuration file.
   */
  @Override
  public final void doInit() {
    super.doInit();
    this.configurationFile = this.getModel().getParameterByName(GlobWebResourcePlugin.CONF_ADM).getValue();
  }

  /**
   * Returns the configuration file that the administrator wants to publish.
   *
   * @return the representation
   */
  @Get
  public final Representation getconfigurationFile() {
    try {
      String uri = String.format("file://%s/%s/%s", SitoolsSettings.getInstance().getRootDirectory(), "data/freemarker", this.configurationFile);
      LOG.finest(String.format("File to publish: %s", uri));
      ClientResource client = new ClientResource(uri);
      Representation rep = new StringRepresentation(client.get().getText());
      if (fileName != null && !"".equals(fileName)) {
        Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
        disp.setFilename(fileName);
        rep.setDisposition(disp);
      }
      return rep;
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Please, contact the administrator. The service is not well configured");
    }
  }
}
