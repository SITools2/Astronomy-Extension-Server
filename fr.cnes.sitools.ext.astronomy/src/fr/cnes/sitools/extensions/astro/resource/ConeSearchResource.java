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

package fr.cnes.sitools.extensions.astro.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.Engine;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.Get;

import fr.cnes.sitools.astro.vo.conesearch.ConeSearchProtocolLibrary;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.dataset.DataSetApplication;

/**
 * Executes the cone search and displays the result.
 * @see ConeSearchResourcePlugin the plugin
 * @see ConeSearchProtocolLibrary the CSP library
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ConeSearchResource extends SitoolsParameterizedResource {

  /**
   * Logger.
   */
  private static final Logger LOG = Engine.getLogger(ConeSearchResource.class.getName());

  /**
   * Initialize.
   */
  @Override
  public final void doInit() {
    super.doInit();
  }

  /**
   * Returns the supported media type.
   *
   * @param variant variant
   * @return XML media type
   */
  @Override
  protected final Representation head(final Variant variant) {
    final Representation repr = super.head();
    repr.setMediaType(MediaType.TEXT_XML);
    return repr;
  }

  /**
   * Returns a VOTable response.
   *
   * @return VOTable response
   */
  @Get
  public final Representation getVOResponse() {
    LOG.info(this.getRequest().getEntityAsText());
    final ConeSearchProtocolLibrary coneSearch = new ConeSearchProtocolLibrary((DataSetApplication) this.getApplication(),
            this.getModel(), this.getRequest(), this.getContext());
    final Representation rep = coneSearch.getResponse();
    if (fileName != null && !"".equals(fileName)) {
      final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
      disp.setFilename(fileName);
      rep.setDisposition(disp);
    }
    return rep;
  }

  /**
   * General description in WADL.
   */
  @Override
  public final void sitoolsDescribe() {
    setName("Cone Search Protocol");
    setDescription("Implements the Cone Search Protocol for Virtual Observatory. "
            + "See http://ivoa.net web site for information about this protocol.");
  }

  /**
   * Describes the GET method in WADL.
   *
   * @param info information
   */
  @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("ConeSearchProtocol");
    info.setDocumentation("Interoperability service to distribute data through the Cone Search Protocol");

    final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo("RA", true, "double", ParameterStyle.QUERY,
            "Right Ascension (decimal degree) in ICRS frame. RA varies from 0 to 360."));
    parametersInfo.add(new ParameterInfo("DEC", true, "double", ParameterStyle.QUERY,
            "Declination (decimal degree) in ICRS frame. DEC varies from -90 to 90."));
    parametersInfo.add(new ParameterInfo("SR", true, "double", ParameterStyle.QUERY,
            "Radius of the cone search in decimal degree."));
    info.getRequest().setParameters(parametersInfo);

    info.getResponse().getStatuses().add(Status.SUCCESS_OK);

    final DocumentationInfo documentation = new DocumentationInfo();
    documentation.setTitle("VOTable");
    documentation.setTextContent("VOTable format for interoperability");

    final List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    final RepresentationInfo representationInfo = new RepresentationInfo(MediaType.TEXT_XML);
    representationInfo.setDocumentation(documentation);
    representationsInfo.add(representationInfo);
    info.getResponse().setRepresentations(representationsInfo);
  }
}
