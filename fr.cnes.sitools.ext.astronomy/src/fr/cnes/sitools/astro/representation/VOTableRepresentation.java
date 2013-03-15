/*******************************************************************************
 * Copyright 2012, 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.astro.representation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Logger;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * Class VOTableRepresentation - Create a VOTable representation with
 * a votable.ftl template and a data model.
 * The data model is the following :
 * <pre>
 * root
 *    |__ totalResults
 *    |__ features (List)            
 *              |__ geometry
 *              |       |__ coordinates
 *              |       |__ type
 *              |__ properties
 *                      |__ keyword/value(List)    
 * </pre> 
 * Provide a VOTable representation by streaming based on Freemarker To have a dataModel by streaming, dataModel for
 * rows element must use the DatabaseRequestModel adapter
 * 
 * @author Jean-Christophe Malapert
 */
public class VOTableRepresentation extends OutputRepresentation {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(VOTableRepresentation.class.getName());
  /**
   * Data model that contains the information to represent.
   */
  private final Map dataModel;
  /**
   * Template file.
   */
  private final String ftl;

  /**
   * Create a VOTableRepresentation based on a dataModel and a templateFile. 
   * 
   * @param dataModelVal DataModel
   * @param ftlVal template File
   */
  public VOTableRepresentation(final Map dataModelVal, final String ftlVal) {
    super(MediaType.TEXT_XML);
    this.dataModel = dataModelVal;
    this.ftl = ftlVal;
  }

    /**
     * Writes the representation.
     * @param outputStream output stream
     * @throws IOException Exception
     */
    @Override
  public final void write(final OutputStream outputStream) throws IOException {
    Representation metadataFtl = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/"
        + ftl).get();
    TemplateRepresentation tpl = new TemplateRepresentation(metadataFtl, dataModel, MediaType.TEXT_XML);
    outputStream.write(tpl.getText().getBytes());
    outputStream.flush();
  }
}
