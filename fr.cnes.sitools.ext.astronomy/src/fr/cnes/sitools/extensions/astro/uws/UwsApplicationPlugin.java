/**
 * *****************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * 
 * This file is part of SITools2. 
 * 
 * SITools2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * SITools2. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.uws;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import fr.cnes.sitools.common.model.Category;
import fr.cnes.sitools.extensions.astro.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.uws.services.DestructionResource;
import fr.cnes.sitools.extensions.astro.uws.services.ErrorResource;
import fr.cnes.sitools.extensions.astro.uws.services.ExecutiondurationResource;
import fr.cnes.sitools.extensions.astro.uws.services.JobResource;
import fr.cnes.sitools.extensions.astro.uws.services.JobsResource;
import fr.cnes.sitools.extensions.astro.uws.services.OwnerResource;
import fr.cnes.sitools.extensions.astro.uws.services.ParameterNameResource;
import fr.cnes.sitools.extensions.astro.uws.services.ParametersResource;
import fr.cnes.sitools.extensions.astro.uws.services.PhaseResource;
import fr.cnes.sitools.extensions.astro.uws.services.QuoteResource;
import fr.cnes.sitools.extensions.astro.uws.services.ResultsResource;
import fr.cnes.sitools.extensions.astro.uws.storage.JobIdDirectory;
import fr.cnes.sitools.extensions.astro.uws.storage.StoreObject;
import fr.cnes.sitools.plugins.applications.business.AbstractApplicationPlugin;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.uws.v1.ExecutionPhase;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Reference;
import org.restlet.ext.wadl.ApplicationInfo;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.GrammarsInfo;
import org.restlet.ext.wadl.IncludeInfo;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 *
 * @author malapert
 */
public class UwsApplicationPlugin extends AbstractApplicationPlugin {
    
    private boolean isAllowedDestructionTimePostMethod = true;
    private boolean isAllowedExecutionTimePostMethod = true;
    private boolean isAllowedParameterNamePutMethod = true;
    public static final String APP_URL_UWS_SERVICE="uwsService";
    public static final String APP_DESTRUCTION_DELAY="604800000";
    public static final String APP_UWS_TITLE="UWS application";
    public static final String APP_UWS="UWS";
    public static final String JOB_TASK_IMPLEMENTATION="fr.cnes.sitools.extensions.astro.uws.test.CutOut";
    private String storageDirectory = null;
            
    /**
     * Constructor.
     */
    public UwsApplicationPlugin() {
        super();
        constructor();
    }

    /**
     * Constructor.
     *
     * @param context Context
     */
    public UwsApplicationPlugin(final Context context) {
        super(context);
        constructor();
        
    }

    /**
     * Constructor.
     *
     * @param context Context
     * @param model Plugin model
     */
    public UwsApplicationPlugin(final Context context, final ApplicationPluginModel model) {
        super(context, model);
        try {
            final Category category = Category.valueOf(getParameter("category").getValue());
            if (model.getCategory() == null) {
                model.setCategory(category);
            }
            setCategory(category);
        } catch (Exception ex) {
        }
        loadPersistence();
        //this.isAllowedDestructionTimePostMethod = Boolean.parseBoolean(String.valueOf(getContext().getAttributes().get(ContextAttributes.DESTRUCTION_TIME_ALLOWED_METHOD_POST)));
        //this.isAllowedExecutionTimePostMethod = Boolean.parseBoolean(String.valueOf(getContext().getAttributes().get(ContextAttributes.EXECUTION_DURATION_ALLOWED_METHOD_POST)));
        //this.isAllowedParameterNamePutMethod = Boolean.parseBoolean(String.valueOf(getContext().getAttributes().get(ContextAttributes.PARAMETER_NAME_ALLOWED_METHOD_PUT)));
        long destructionDelay;
//        try {
//            destructionDelay = Long.parseLong(String.valueOf(context.getAttributes().get(ContextAttributes.APP_DESTRUCTION_DELAY)));
//        } catch (NumberFormatException nfe) {
            destructionDelay = Constants.DESTRUCTION_DELAY;
//        }
        // TODO check
        getContext().getAttributes().put(APP_URL_UWS_SERVICE, model.getUrlAttach());
        getContext().getAttributes().put(APP_DESTRUCTION_DELAY, destructionDelay);
        getContext().getAttributes().put(APP_UWS, this);
        JobTaskManager.getInstance().init(getContext());         
        register();
    }    

    @Override
    public void sitoolsDescribe() {
        this.setName("UWS Application");
        this.setAuthor("J-C Malapert");
        this.setOwner("CNES");
        this.setDescription("UWS application");
    }

    private void constructor() {       
        this.getModel().setClassAuthor("J-C Malapert");
        this.getModel().setClassName("UWS Application");
        this.getModel().setClassOwner("CNES");
        this.getModel().setClassVersion("0.1");                

        ApplicationPluginParameter param = new ApplicationPluginParameter();
        param.setName("storageDirectory");
        param.setDescription("File path for the storage directory (must start with file://");        
        this.addParameter(param);
    }

    /**
     * Load persistance when the server starts
     */
    private void loadPersistence() {
        //File file = new File(Messages.getString("Starter.ROOT_DIRECTORY") + Messages.getString("Starter.APP_STORE_DIR") + "/save.xml");
        String directory = this.getParameter("storageDirectory").getValue();
        if (directory.endsWith(File.separator)) {
            this.storageDirectory = directory.substring(directory.length() - 1, directory.length());           
        } else {
            this.storageDirectory = directory;
        }
        this.storageDirectory = this.storageDirectory.substring(6, this.storageDirectory.length());
        File file = new File(this.storageDirectory + File.separator + "save.xml");        
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                XStream xs = new XStream(new DomDriver());
                Map<String, AbstractJobTask> map = (Map<String, AbstractJobTask>) xs.fromXML(fis);
                Set<String> jobsId = map.keySet();
                for (Iterator<String> iterJobId = jobsId.iterator(); iterJobId.hasNext();) {
                    String jobId = iterJobId.next();
                    AbstractJobTask jobTask = map.get(jobId);
                    ExecutionPhase phase = jobTask.getJobSummary().getPhase();
                    if (phase.equals(ExecutionPhase.QUEUED) || phase.equals(ExecutionPhase.EXECUTING) || phase.equals(ExecutionPhase.HELD) || phase.equals(ExecutionPhase.UNKNOWN)) {
                        JobTaskManager.getInstance().setPhase(jobTask);
                        map.put(jobId, jobTask);
                    }
                }
                fis.close();
                JobTaskManager.getInstance().setTasks(map);
            } catch (IOException ex) {
                Logger.getLogger(UwsApplicationPlugin.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fis.close();
                } catch (IOException ex) {
                    Logger.getLogger(UwsApplicationPlugin.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    /**
     * Load or not the POST methode for the ExecutionTime resource
     * @return Returns true when POST method can be used otherwise false
     */
    public boolean isAllowedExecutionTimePostMethod() {
        return this.isAllowedExecutionTimePostMethod;
    }

    /**
     * Load or not the POST methode for the DestructionTime resource
     * @return Returns true when POST method can be used otherwise false
     */
    public boolean isAllowedDestructionTimePostMethod() {
        return this.isAllowedDestructionTimePostMethod;
    }

    public boolean isAllowedParameterNamePutMethod() {
        return this.isAllowedParameterNamePutMethod;
    }
    
    public String getStorageDirectory() {
        return this.storageDirectory;
    }
    
    @Override
    public final Restlet createInboundRoot() {
        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
        router.attachDefault(JobsResource.class);
        Directory appStorage = new Directory(getContext(), "file://" + getStorageDirectory());
        appStorage.setListingAllowed(true);
        appStorage.setDeeplyAccessible(true) ;       
        router.attach("/storage",appStorage);
        router.attach("/jobCache/{job-id}/{file-id}",JobIdDirectory.class);
        router.attach("/jobCache/{job-id}",JobIdDirectory.class);
        router.attach("/jobCache",JobIdDirectory.class);
        router.attach("/cache",StoreObject.class);        
        router.attach("/{job-id}/phase", PhaseResource.class);
        router.attach("/{job-id}/executionduration", ExecutiondurationResource.class);
        router.attach("/{job-id}/destruction", DestructionResource.class);
        router.attach("/{job-id}/error", ErrorResource.class);
        router.attach("/{job-id}/quote", QuoteResource.class);
        router.attach("/{job-id}/results", ResultsResource.class);
        router.attach("/{job-id}/parameters", ParametersResource.class);
        router.attach("/{job-id}/parameters/{parameter-name}", ParameterNameResource.class);
        router.attach("/{job-id}/owner", OwnerResource.class);        
        router.attach("/{job-id}", JobResource.class);
        attachParameterizedResources(router);
        return router;
    }

    @Override
    public Application getApplication() {
        return super.getApplication(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ApplicationInfo getApplicationInfo(Request request, Response response) {
        ApplicationInfo result = super.getApplicationInfo(request, response);
        DocumentationInfo docInfo = new DocumentationInfo("The Universal Worker Service (UWS) pattern defines how to manage asynchronous execution of jobs on a service");
        docInfo.setTitle(this.getName());
        docInfo.setTextContent(this.getDescription());
        result.setDocumentation(docInfo);

        result.getNamespaces().put("xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\"", "uws");
        result.getNamespaces().put("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "xsi");
        result.getNamespaces().put("xmlns:xlink=\"http://www.w3.org/1999/xlink\"", "xlink");
        GrammarsInfo grammar = new GrammarsInfo();
        IncludeInfo include = new IncludeInfo();
        include.setTargetRef(new Reference("http://ivoa.net/xml/UWS/UWS-v1.0.xsd"));
        grammar.getIncludes().add(include);
        result.setGrammars(grammar);
        return result;
    }    
}
