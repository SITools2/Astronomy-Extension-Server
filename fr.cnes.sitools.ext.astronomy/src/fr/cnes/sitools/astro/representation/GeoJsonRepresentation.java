/**
 * *****************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
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
 * Creates a geoJson representation based on both a template and a data model.
 *
 * <p> Data model must have the following structure :
 * <pre>
 * root
 *    |__ totalResults (mandatory)
 *    |__ features (List)
 *              |__ geometry
 *              |       |__ coordinates (mandatory)
 *              |       |__ type (mandatory)
 *              |       |__ crs (mandatory)
 *              |__ properties
 *              |       |__ keyword/value(List) (identifier is mandatory)
 *              |__ services
 *                      |__ download
 *                              |__ mimetype
 *                              |__ url
 * </pre> </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class GeoJsonRepresentation extends OutputRepresentation {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(GeoJsonRepresentation.class.getName());
  /**
   * Default template = GeoJson.ftl.
   */
  public static final String DEFAULT_TEMPLATE = "GeoJson.ftl";
  /**
   * Data model for the GeoJson representation.
   */
  private final Map dataModel;
  /**
   * Template file.
   */
  private final String ftl;

  /**
   * Creates a GeoJson representation with a data model and a template as parameter.
   *
   * <p> The template must be located in this package. </p>
   *
   * @param dataModelVal the data model
   * @param ftlVal the template
   */
  public GeoJsonRepresentation(final Map dataModelVal, final String ftlVal) {
    super(MediaType.APPLICATION_JSON);
    this.dataModel = dataModelVal;
    this.ftl = ftlVal;
  }

  /**
   * Creates a GeoJson representation with the default template (<code>DEFAULT_TEMPLATE</code>).
   *
   * @param dataModelVal the data model
   */
  public GeoJsonRepresentation(final Map dataModelVal) {
    this(dataModelVal, DEFAULT_TEMPLATE);
  }

  /**
   * Writes the representation.
   *
   * @param out Output filename
   * @throws IOException Exception
   */
  @Override
  public final void write(final OutputStream out) throws IOException {
    Representation metadataFtl = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/"
            + ftl).get();
    TemplateRepresentation tpl = new TemplateRepresentation(metadataFtl, dataModel, getMediaType());
    out.write(tpl.getText().getBytes());
    out.flush();
  }
}
