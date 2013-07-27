/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.extensions.astro.application;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.common.application.ContextAttributes;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.extensions.cache.SingletonCacheShortnerURL;
import fr.cnes.sitools.server.Consts;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;

/**
 * Resource that handles the shortener URL.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ShorteningResource extends SitoolsParameterizedResource {

    private String urlShortener;

    @Override
    public final void doInit() throws ResourceException {
        this.urlShortener = (String) getRequestAttributes().get("shortenerId");
    }

    /**
     * Creates a shortener URL.
     * @param entity Entity coming form the user
     * @return the shortener URL
     * @throws ResourceException When an error happens
     */
    @Post("form")
    public final Representation acceptJob(final Representation entity) throws ResourceException {
        final Form form = new Form(entity);
        final Parameter urlParameter = form.getFirst("url");
        final SitoolsSettings sitoolsSettings = (SitoolsSettings) getContext().getAttributes().get(ContextAttributes.SETTINGS);       
        final String url = urlParameter.getValue();
        final String shortenerId = SingletonCacheShortnerURL.putUrl(url);
        return new StringRepresentation(getReference().getIdentifier() + '/' + shortenerId);
    }
    
    /**
     * Redirect to the bookmarked URL.
     */
    @Get
    public final void redirecturl() {
        final Redirector redirector = new Redirector(getContext(), SingletonCacheShortnerURL.getUrl(urlShortener), Redirector.MODE_CLIENT_PERMANENT);
        redirector.handle(getRequest(), getResponse());
    }

}
