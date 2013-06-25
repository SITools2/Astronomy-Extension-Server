/******************************************************************************
 * Copyright 2011-2013 - CENTRE NATIONAL d'ETUDES SPATIALES
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
 ******************************************************************************/
package fr.cnes.sitools.extensions.astro.application;

import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.cache.CacheBrowser;
import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.extensions.common.VoDictionary;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

/**
 * Provides the definition of the keywords that are located in the openSearch response.
 * 
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class OpenSearchVOSiaSearchDico extends SitoolsParameterizedResource {
  
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(OpenSearchVOSiaSearchDico.class.getName());  
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
    this.dico = ((OpenSearchVOSiaSearchApplicationPlugin) getApplication()).getDico();
    this.name = String.valueOf(this.getRequestAttributes().get("name"));
  }
  
  /**
   * Returns the dictionary representation.
   * @return the dictionary representation
   */
  @Get  
  public final Representation getDico() {
    if (!Utility.isSet(this.name)) {
      LOG.log(Level.SEVERE, "name must be set as parameter.");
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "name must be set as parameter");
    }
    if (!this.dico.containsKey(this.name)) {
      LOG.log(Level.WARNING, "Cannot find {0} in the dictionary.", name);
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }
    final VoDictionary voDico = this.dico.get(this.name);
    String output;
    if (voDico.getDescription() != null && voDico.getUnit() != null) {
      output = String.format("%s - unit: %s", voDico.getDescription(), voDico.getUnit());
    } else if (voDico.getDescription() != null && voDico.getUnit() == null) {
      output = String.format("%s", voDico.getDescription());
    } else if (voDico.getUnit() != null) {
      output = String.format("unit: %s", voDico.getUnit());
    } else {
      output = "No definition found";
    }    
    Representation rep = new StringRepresentation(output, MediaType.TEXT_PLAIN);
    rep = useCacheBrowser(rep, cacheIsEnabled());
    return rep;
  }
  
  /**
   * Returns the representation with cache directives cache parameter is set to enable.
   *
   * @param rep representation to cache
   * @param isEnabled True when the cache is enabled
   * @return the representation with the cache directive when the cache is enabled
   */
  private Representation useCacheBrowser(final Representation rep, final boolean isEnabled) {
    Representation cachedRepresentation = rep;
    if (isEnabled) {
      final CacheBrowser cache = CacheBrowser.createCache(CacheBrowser.CacheDirectiveBrowser.DAILY, rep);
      getResponse().setCacheDirectives(cache.getCacheDirectives());
      cachedRepresentation = cache.getRepresentation();
    }
    return cachedRepresentation;
  }

  /**
   * Returns True when the cache is enabled otherwise False.
   * @return True when the cache is enabled otherwise False
   */
  private boolean cacheIsEnabled() {
    return Boolean.parseBoolean(((OpenSearchVOSiaSearchApplicationPlugin) getApplication()).getParameter("cacheable").getValue());
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
