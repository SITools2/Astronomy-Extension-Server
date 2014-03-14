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
package fr.cnes.sitools.extensions.astro.application;

import java.util.logging.Logger;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.engine.Engine;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import fr.cnes.sitools.common.model.Category;
import fr.cnes.sitools.extensions.cache.SingletonCacheShortnerURL;
import fr.cnes.sitools.plugins.applications.business.AbstractApplicationPlugin;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;

/**
 * Plugin that provides a shortner URL based on a MIZAR context.
 * <p>
 * Here is an example of usage:<br/>
 * <pre>
 * <code>
 *   curl -X POST -d "context={toto:\"fdfd\"}" "http://localhost:8182/shorturl"
 *   => returns 972
 *   curl -X GET "http://localhost:8182/shorturl/972"
 *   => returns {toto:"fdfd"}
 * </code>
 * </pre>
 * </p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ShorteningUrlApplicationPlugin extends AbstractApplicationPlugin {

    /**
     * Logger.
     */
    private static final Logger LOG = Engine.getLogger(OpenSearchApplicationPlugin.class.getName());

    /**
     * Constructor.
     */
    public ShorteningUrlApplicationPlugin() {
        super();
        constructor();
    }

    /**
     * Constructor.
     *
     * @param context Context
     */
    public ShorteningUrlApplicationPlugin(final Context context) {
        super(context);
        constructor();

    }

    /**
     * Constructor.
     *
     * @param context Context
     * @param model Plugin model
     */
    public ShorteningUrlApplicationPlugin(final Context context, final ApplicationPluginModel model) {
        super(context, model);
        try {
            final Category category = Category.valueOf(getParameter("category").getValue());
            if (model.getCategory() == null) {
                model.setCategory(category);
            }
            setCategory(category);
        } catch (Exception ex) {
            LOG.warning(ex.getMessage());
        }
    }

    @Override
    public final void sitoolsDescribe() {
        this.setName("Shortening URL Application");
        this.setAuthor("J-C Malapert");
        this.setOwner("CNES");
        this.setDescription("This plugin caches a JSON configuration sent by"
                + " a client and returns a shortenerID. "
                + "Then the client can retrieve the JSON configuration by calling the "
                + "resource according to the shortenerID");
    }

    /**
     * Constructor.
     */
    private void constructor() {
        this.getModel().setClassAuthor("J-C Malapert");
        this.getModel().setClassName("Shortening URL Application");
        this.getModel().setClassOwner("CNES");
        this.getModel().setClassVersion("1.0");
        SingletonCacheShortnerURL.create();
    }

    @Override
    public final Restlet createInboundRoot() {
            final Router router = new Router(getContext());
            router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
            router.attachDefault(fr.cnes.sitools.extensions.astro.application.shortenerurl.ShorteningResource.class);
            router.attach("/{shortenerId}", fr.cnes.sitools.extensions.astro.application.shortenerurl.ShorteningResource.class);
        return router;
    }
}
