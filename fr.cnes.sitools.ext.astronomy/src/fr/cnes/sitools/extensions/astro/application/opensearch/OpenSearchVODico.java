 /*******************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.extensions.astro.application.opensearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.ext.wadl.ParameterInfo;
import org.restlet.ext.wadl.ParameterStyle;
import org.restlet.ext.wadl.RepresentationInfo;
import org.restlet.ext.wadl.ResponseInfo;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.astro.application.OpenSearchVOApplicationPlugin;
import fr.cnes.sitools.extensions.cache.CacheBrowser;
import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.extensions.common.VoDictionary;

/**
 * Provides a dictionary for open search services that use Virtual Observatory.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVODico extends SitoolsParameterizedResource {
  /**
   * VO dictionary.
   */
  private transient Map<String, VoDictionary> dico;
  /**
   * Name to search in the VO dictionary.
   */
  private transient String name;

  @Override
  public final void doInit() {
    super.doInit();
    this.dico = ((OpenSearchVOApplicationPlugin) getApplication()).getDico();
    this.name = String.valueOf(this.getRequestAttributes().get("name"));
  }
  /**
   * Returns the dictionary representation.
   * @return the dictionary representation
   */
  @Get
  public final Representation getDico() {
    if (!Utility.isSet(this.name)) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "name parameter must be set.");
    }
    if (!this.dico.containsKey(this.name)) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "Cannot find " + name + " in the dictionary.");
    }
    final VoDictionary voDico = this.dico.get(this.name);
    String output;
    if (Utility.isSet(voDico.getDescription()) && Utility.isSet(voDico.getUnit())) {
      output = String.format("%s - unit: %s", voDico.getDescription(), voDico.getUnit());
    } else if (Utility.isSet(voDico.getDescription())) {
      output = String.format("%s", voDico.getDescription());
    } else if (Utility.isSet(voDico.getUnit())) {
      output = String.format("unit: %s", voDico.getUnit());
    } else {
      output = "No definition found";
    }
    final Representation rep = new StringRepresentation(output, MediaType.TEXT_PLAIN);
    return useCacheBrowser(rep, cacheIsEnabled());
  }
  /**
   * Returns the representation with cache directives cache parameter is set to enable.
   *
   * @param rep representation to cache
   * @param isEnabled True when the cache is enabled
   * @return the representation with the cache directive when the cache is enabled
   */
  private Representation useCacheBrowser(final Representation rep, final boolean isEnabled) {
    Representation cachedRepresentation;
    if (isEnabled) {
      final CacheBrowser cache = CacheBrowser.createCache(CacheBrowser.CacheDirectiveBrowser.DAILY, rep);
      getResponse().setCacheDirectives(cache.getCacheDirectives());
      cachedRepresentation = cache.getRepresentation();
    } else {
        cachedRepresentation = rep;
    }
    return cachedRepresentation;
  }

  /**
   * Returns True when the cache is enabled otherwise False.
   * @return True when the cache is enabled otherwise False
   */
  private boolean cacheIsEnabled() {
    return Boolean.parseBoolean(((OpenSearchVOApplicationPlugin) getApplication()).getParameter("cacheable").getValue());
  }
  /**
   * General WADL description.
   */
  @Override
  public final void sitoolsDescribe() {
    setName("OpenSearch dictionary for VO services");
    setDescription("Provides description of keywords that are provided in the open search response");
  }

  @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
    info.setIdentifier("Dictionary");
    info.setDocumentation("Retrieving the keyword definition.");

    // represensation when the response is fine
    final ResponseInfo responseOK = new ResponseInfo();

    final DocumentationInfo documentationTxt = new DocumentationInfo();
    documentationTxt.setTitle("txt");
    documentationTxt.setTextContent("Returns the keyword definition with the following syntax : %s - unit: %s");

    final RepresentationInfo representationInfoTxt = new RepresentationInfo(MediaType.TEXT_PLAIN);
    representationInfoTxt.setDocumentation(documentationTxt);

    final List<RepresentationInfo> representationsInfo = new ArrayList<RepresentationInfo>();
    representationsInfo.add(representationInfoTxt);

    final List<ParameterInfo> parametersInfo = new ArrayList<ParameterInfo>();
    parametersInfo.add(new ParameterInfo("name", true, "xs:string", ParameterStyle.PLAIN, "keyword name for which the definition must be found."));

    responseOK.setParameters(parametersInfo);
    responseOK.setRepresentations(representationsInfo);
    responseOK.getStatuses().add(Status.SUCCESS_OK);

    // represensation when the response is not fine
    final ResponseInfo responseNOK = new ResponseInfo();
    final RepresentationInfo representationInfoError = new RepresentationInfo(MediaType.TEXT_HTML);
    representationInfoError.setReference("error");

    responseNOK.getRepresentations().add(representationInfoError);
    responseNOK.getStatuses().add(Status.SERVER_ERROR_INTERNAL);
    responseNOK.getStatuses().add(Status.CLIENT_ERROR_BAD_REQUEST);
    responseNOK.getStatuses().add(Status.CLIENT_ERROR_NOT_FOUND);

    info.setResponses(Arrays.asList(responseOK, responseNOK));
  }
}
