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

import java.util.HashSet;
import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.representation.Representation;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import fr.cnes.sitools.common.model.Category;
import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.extensions.cache.SingletonCacheHealpixDataAccess;
import fr.cnes.sitools.plugins.applications.business.AbstractApplicationPlugin;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import fr.cnes.sitools.proxy.RedirectorProxy;

/**
 * Plugin Application for Healpix server to redirect client request to target
 * template url in Redirector mode.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 *
 */
public final class ProxyHealpixCacheApplicationPlugin extends AbstractApplicationPlugin {

    /**
     * PARAM_URLCLIENT.
     */
    private static final String PARAM_URLCLIENT = "urlClient";
    /**
     * PARAM_USEPROXY.
     */
    private static final String PARAM_USEPROXY = "useProxy";
    /**
     * PARAM_CATEGORY.
     */
    private static final String PARAM_CATEGORY = "category";

    /**
     * Default constructor.
     *
     * @param context context
     */
    public ProxyHealpixCacheApplicationPlugin(final Context context) {
        super(context);
        constructor();
    }

    /**
     * Default constructor Used when getting parameters for generic
     * configuration of an application instance.
     */
    public ProxyHealpixCacheApplicationPlugin() {
        super();
        constructor();
    }

    /**
     * Constructor with context and model of the application configuration used
     * when actually creating application instance.
     *
     * @param arg0 Restlet context
     * @param model model contains configuration parameters of the application
     * instance
     */
    public ProxyHealpixCacheApplicationPlugin(final Context arg0, final ApplicationPluginModel model) {
        super(arg0, model);

        // Category parameter of ProxyApp
        try {
            final Category category = Category.valueOf(getParameter(PARAM_CATEGORY).getValue());

            if (null == model.getCategory()) {
                model.setCategory(category);
            }
            setCategory(category);

        } catch (Exception e) {
            getLogger().severe(e.getMessage());
        }

        register();
    }

    /**
     * the common part of constructor.
     */
    public void constructor() {

        this.getModel().setClassAuthor("J-C Malapert");
        this.getModel().setClassVersion("1.0");
        this.getModel().setClassOwner("CNES");

        final ApplicationPluginParameter param1 = new ApplicationPluginParameter();
        param1.setName(PARAM_URLCLIENT);
        param1.setDescription("template for client URL");
        this.addParameter(param1);

        final ApplicationPluginParameter param2 = new ApplicationPluginParameter();
        param2.setName(PARAM_USEPROXY);
        param2.setDescription("TRUE for proxy usage");
        this.addParameter(param2);

        final ApplicationPluginParameter param4 = new ApplicationPluginParameter();
        param4.setName(PARAM_CATEGORY);
        param4.setDescription("ADMIN");
        this.addParameter(param4);
        SingletonCacheHealpixDataAccess.create();
    }

    @Override
    public void sitoolsDescribe() {
        this.setName("Proxy Healpix Image cache");
        this.setAuthor("J-C Malapert");
        this.setOwner("CNES");
        this.setDescription("Proxy Healpix Image cache");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restlet.Application#createInboundRoot()
     */
    @Override
    public Restlet createInboundRoot() {
        Restlet redirector;

        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);

        final ApplicationPluginParameter param1 = this.getParameter("urlClient");
        final ApplicationPluginParameter param2 = this.getParameter("useProxy");

        if (Boolean.parseBoolean(param2.getValue())) {
            redirector = new RedirectorProxy(getContext(), param1.getValue().concat("{rr}"), RedirectorProxy.MODE_SERVER_OUTBOUND);
        } else {
            redirector = new Redirector(getContext(), param1.getValue().concat("{rr}"), Redirector.MODE_SERVER_OUTBOUND);
        }

        // return redirector;
        router.attachDefault(redirector);
        return router;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.restlet.ext.wadl.WadlApplication#handle(org.restlet.Request, org.restlet.Response)
     */
    @Override
    public void handle(final Request request, final Response response) {
        final Response cachedRep = response;
        final CacheManager cacheManager = SingletonCacheHealpixDataAccess.getInstance();
        final Cache cache = cacheManager.getCache("healpixImage");
        synchronized (cache) {
            final String uniqueId = request.getOriginalRef().toString();
            if (cache.isKeyInCache(uniqueId)) {
                final Representation rep = (Representation) cache.get(uniqueId).getObjectValue();
                cachedRep.setEntity(rep);
            } else {
                final Representation rep = response.getEntity();
                final Element element = new Element(uniqueId, rep);
                cache.put(element);
            }
        }
        super.handle(request, cachedRep);
    }

    @Override
    public Validator<AbstractApplicationPlugin> getValidator() {
        return new Validator<AbstractApplicationPlugin>() {

            @Override
            public Set<ConstraintViolation> validate(final AbstractApplicationPlugin item) {
                final Set<ConstraintViolation> constraints = new HashSet<ConstraintViolation>();
                ApplicationPluginParameter param = item.getParameter(PARAM_URLCLIENT);
                String value = param.getValue();
                if (value.isEmpty()) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("This parameter must be set");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setInvalidValue(value);
                    constraint.setValueName(param.getName());
                    constraints.add(constraint);
                }

                param = item.getParameter(PARAM_USEPROXY);
                value = param.getValue();
                if (!"TRUE".equals(value) && !"FALSE".equals(value)) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("This parameter must be either TRUE or FALSE");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setInvalidValue(value);
                    constraint.setValueName(param.getName());
                    constraints.add(constraint);
                }

                param = item.getParameter(PARAM_CATEGORY);
                value = param.getValue();
                if (value.isEmpty()) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("This parameter should be set");
                    constraint.setLevel(ConstraintViolationLevel.WARNING);
                    constraint.setValueName(param.getName());
                    constraints.add(constraint);
                }
                return constraints;
            }

        };
    }
}
