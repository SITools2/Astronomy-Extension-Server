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
package fr.cnes.sitools.astro.representation;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * Creates a VOTable representation with a template and a data model. The data model is the following :
 * <pre>
 * root
 *    |__ totalResults
 *    |__ features (List)
 *              |__ geometry
 *              |       |__ coordinates
 *              |       |__ type
 *              |__ properties
 *                      |__ keyword/value(List)
 * </pre> Provide a VOTable representation by streaming based on Freemarker To have a dataModel by streaming, dataModel for rows element
 * must use the DatabaseRequestModel adapter
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class VOTableRepresentation extends OutputRepresentation {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(VOTableRepresentation.class.getName());
  /**
   * Default template file = votable.ftl.
   */
  public static final String DEFAULT_TEMPLATE = "votable.ftl";
  /**
   * Data model that contains the information to represent.
   */
  private final transient Map dataModel;
  /**
   * Template file.
   */
  private final transient String ftl;

  /**
   * Creates a VOTableRepresentation based on a dataModel and a templateFile.
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
   * Creates a GeoJson representation with the default template (<code>DEFAULT_TEMPLATE</code>).
   *
   * @param dataModelVal the data model
   */
  public VOTableRepresentation(final Map dataModelVal) {
    this(dataModelVal, DEFAULT_TEMPLATE);
  }  

  /**
   * Writes the representation.
   *
   * @param outputStream output stream
   * @throws IOException Exception
   */
  @Override
  public final void write(final OutputStream outputStream) throws IOException {
    final Representation metadataFtl = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/"
            + ftl).get();
    final TemplateRepresentation tpl = new TemplateRepresentation(metadataFtl, dataModel, getMediaType());
    LOG.log(Level.FINEST, ftl, tpl);
    outputStream.write(tpl.getText().getBytes());
    outputStream.flush();
  }
}
