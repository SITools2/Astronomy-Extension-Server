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
 * Creates an openSearch Description representation based on both a template and a data model.
 * 
 * <p>
 * Data model must have the following structure :
 * <pre>
 * root
 *    |__ shortName (required)
 *    |__ description (required)
 *    |__ templateURL (required)
 *    |__ describe (optional)
 *    |__ contact (optional)
 *    |__ tags (optional)
 *    |__ longName (optional)
 *    |__ imagePng (optional)
 *    |__ imageIcon (optional)
 *    |__ syndicationRight (required)
 *    |__ clusterTemplateURL (optional)
 *    |__ mocdescribe (optional)
 *    |__ dicodescribe (optional)
 *    |__ referenceSystem (required)
 * </pre> 
 * </p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchDescriptionRepresentation extends OutputRepresentation {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OpenSearchDescriptionRepresentation.class.getName());
  
  /**
   * Default Template file that is used for the representation.
   */
  public static final String DEFAULT_TEMPLATE = "openSearchDescription.ftl";
  
  /**
   * Data model for the GeoJson representation.
   */
  private final transient Map dataModel;
  /**
   * Template file.
   */
  private final transient String ftl;

  /**
   * Creates an OpenSearch description representation with a template and a data model as parameters.
   *
   * @param dataModelVal the data model
   * @param ftlVal the template
   */
  public OpenSearchDescriptionRepresentation(final Map dataModelVal, final String ftlVal) {
    super(MediaType.TEXT_XML);
    this.dataModel = dataModelVal;
    this.ftl = ftlVal;
  }
  
  /**
   * Creates an OpenSearch description representation with a data model and the default template file.
   *
   * @param dataModelVal the data model  
   */
  public OpenSearchDescriptionRepresentation(final Map dataModelVal) {
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
        final Representation metadataFtl = new ClientResource(LocalReference.createClapReference(getClass().getPackage()) + "/"
                + ftl).get();
        final TemplateRepresentation tpl = new TemplateRepresentation(metadataFtl, dataModel, getMediaType());
        LOG.log(Level.FINEST, ftl, tpl);
        out.write(tpl.getText().getBytes());
        out.flush();
  }
}
