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
package fr.cnes.sitools.extensions.astro.application;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import fr.cnes.sitools.common.model.Category;
import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.extensions.astro.application.uws.common.Constants;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.JobTaskManager;
import fr.cnes.sitools.extensions.astro.application.uws.representation.CapabilitiesRepresentation;
import fr.cnes.sitools.extensions.astro.application.uws.services.DestructionResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.ErrorResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.ExecutiondurationResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.JobApi;
import fr.cnes.sitools.extensions.astro.application.uws.services.JobResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.JobsResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.OwnerResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.ParameterNameResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.ParametersResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.PhaseResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.QuoteResource;
import fr.cnes.sitools.extensions.astro.application.uws.services.ResultsResource;
import fr.cnes.sitools.extensions.astro.application.uws.storage.JobIdDirectory;
import fr.cnes.sitools.extensions.astro.application.uws.storage.StoreObject;
import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.plugins.applications.business.AbstractApplicationPlugin;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginModel;
import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import fr.cnes.sitools.xml.uws.v1.Job;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import net.ivoa.xml.uws.v1.ExecutionPhase;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.wadl.ApplicationInfo;
import org.restlet.ext.wadl.DocumentationInfo;
import org.restlet.ext.wadl.GrammarsInfo;
import org.restlet.ext.wadl.IncludeInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.Directory;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.xml.sax.SAXException;

/**
 * Provides a UWS service as a SITools2 plugin.
 * <p>
 * UWS (Universal Worker Service) is a service that allows to run, monitor and
 * retrieve results from a Job.
 * </p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class UwsApplicationPlugin extends AbstractApplicationPlugin {

    /**
     * Allows the destruction time.
     */
    private boolean isAllowedDestructionTimePostMethod = true;
    /**
     * Allows the execution time.
     */
    private boolean isAllowedExecutionTimePostMethod = true;
    /**
     * Allows the use of PUT verb to update parameter.
     */
    private boolean isAllowedParameterNamePutMethod = true;
    /**
     * Keyword provding the attached URL inputStream the context.
     */
    public static final String APP_URL_UWS_SERVICE = "uwsService";
    /**
     * Persistence of the results is one week.
     */
    public static final String APP_DESTRUCTION_DELAY = "604800000";
    /**
     * Title of the UWS application.
     */
    public static final String APP_UWS_TITLE = "UWS application";
    /**
     * Keyword of this class inputStream the context.
     */
    public static final String APP_UWS = "UWS";
    /**
     * Parameter name of the Job task implemententation.
     */
    private static final String JOB_TASK_IMPL_PARAMETER = "jobTaskImplementation";
    /**
     * Parameter name of the storage directory.
     */
    private static final String STORAGE_DIRECTORY_PARAMETER = "storageDirectory";
    /**
     * Storage directory where the results are stored.
     */
    private String storageDirectory = null;
    /**
     * Loading persistence.
     */
    private boolean isLoadPersistence = false;
    /**
     * Class of the job Task implementation.
     */
    private String jobTaskImplementation = null;
   /**
    * Logger.
    */
    private static final Logger LOG = Logger.getLogger(UwsApplicationPlugin.class.getName());
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
        setCategory(Category.USER_DYNAMIC);
        setJobTaskImplementation();
        final long destructionDelay = Constants.DESTRUCTION_DELAY;
        //getContext().getAttributes().put(APP_URL_UWS_SERVICE, model.getUrlAttach());
        //getContext().getAttributes().put(APP_DESTRUCTION_DELAY, destructionDelay);
        //getContext().getAttributes().put(APP_UWS, this);
        setStorageDirectory();
        //JobTaskManager.getInstance().init(this);
        register();
    }

    @Override
    public final void sitoolsDescribe() {
        this.setName("UWS Application");
        this.setAuthor("J-C Malapert");
        this.setOwner("CNES");
        this.setDescription("UWS application");
    }

    /**
     * Constructor.
     */
    private void constructor() {
        this.getModel().setClassAuthor("J-C Malapert");
        this.getModel().setClassName("UWS Application");
        this.getModel().setClassOwner("CNES");
        this.getModel().setClassVersion("1.0");

        ApplicationPluginParameter param = new ApplicationPluginParameter();
        param.setName(STORAGE_DIRECTORY_PARAMETER);
        param.setDescription("File path for the storage directory (must start with file://)");
        this.addParameter(param);
        param = new ApplicationPluginParameter();
        param.setName(JOB_TASK_IMPL_PARAMETER);
        param.setDescription("Job Task implementation");
        this.addParameter(param);
    }

    /**
     * Sets the Job task implementation.
     */
    private void setJobTaskImplementation() {
        this.jobTaskImplementation = this.getParameter(JOB_TASK_IMPL_PARAMETER).getValue();
    }

    /**
     * Returns the job task implementation.
     * @return the job task implementation
     */
    public final String getJobTaskImplementation() {
        return this.jobTaskImplementation;
    }

    /**
     * Sets the storage directory.
     */
    private void setStorageDirectory() {
        final String directory = this.getParameter(STORAGE_DIRECTORY_PARAMETER).getValue();
        if (directory.endsWith(File.separator)) {
            this.storageDirectory = directory.substring(directory.length() - 1, directory.length());
        } else {
            this.storageDirectory = directory;
        }
        this.storageDirectory = this.storageDirectory.substring("file://".length(), this.storageDirectory.length());
    }

    /**
     * Returns the storage directory.
     * @return the storage directory
     */
    public final String getStorageDirectory() {
        return this.storageDirectory;
    }

    /**
     * Loads persistence when the server starts.
     */
    public final void loadPersistence() {
        if (isLoadPersistence()) {
            return;
        }
        initDataStorage();
        initPersistence();
    }

    /**
     * Init the persistence by loading save.xml file when it exists.
     */
    private void initPersistence() {
        final File file = new File(this.storageDirectory + File.separator + "save.xml");
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                final XStream xstream = new XStream(new DomDriver());
                final Map<String, AbstractJobTask> map = (Map<String, AbstractJobTask>) xstream.fromXML(fis);
                for (Map.Entry<String, AbstractJobTask> entryAbstractJobTask : map.entrySet()) {
                    final String jobId = entryAbstractJobTask.getKey();
                    final AbstractJobTask jobTask = entryAbstractJobTask.getValue();
                    final ExecutionPhase phase = jobTask.getJobSummary().getPhase();
                    if (phase.equals(ExecutionPhase.QUEUED) || phase.equals(ExecutionPhase.EXECUTING) || phase.equals(ExecutionPhase.HELD) || phase.equals(ExecutionPhase.UNKNOWN)) {
                        JobTaskManager.getInstance().setPhase(jobTask);
                        map.put(jobId, jobTask);
                    }
                }
                fis.close();
                JobTaskManager.getInstance().setTasks(map);
            } catch (IOException ex) {
                LOG.warning(ex.getMessage());
            } finally {
                setLoadPersistence(true);
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException ex) {
                    LOG.warning(ex.getMessage());
                }
            }
        }
    }

    /**
     * Init data storage.
     * <p>
     * If the storageDirectory does not exist, then the storage is created.
     * If the storagedirectory exists but it is not readable and writable then
     * an Exception is raised.
     * </p>
     */
    private void initDataStorage() {
        final File storageDirectoryObj = new File(this.storageDirectory);
        if (storageDirectoryObj.exists()) {
            if (!storageDirectoryObj.canRead() || !storageDirectoryObj.canWrite()) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Cannot read or write on " + this.storageDirectory + "directory");
            }
        } else {
            final boolean status = storageDirectoryObj.mkdir();
            if (!status) {
                throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Cannot create the " + this.storageDirectory + "directory");
            }
        }
    }

    /**
     * Loads or not the POST method for the ExecutionTime resource.
     *
     * @return Returns true when POST method can be used otherwise false
     */
    public final boolean isAllowedExecutionTimePostMethod() {
        return this.isAllowedExecutionTimePostMethod;
    }

    /**
     * Loads or not the POST methode for the DestructionTime resource.
     *
     * @return Returns true when POST method can be used otherwise false
     */
    public final boolean isAllowedDestructionTimePostMethod() {
        return this.isAllowedDestructionTimePostMethod;
    }

    /**
     * Returns True if parameter can be updated.
     * @return True if parameter can be updated
     */
    public final boolean isAllowedParameterNamePutMethod() {
        return this.isAllowedParameterNamePutMethod;
    }

    /**
     * Returns True if the persistence is loaded.
     * @return True if the persistence is loaded
     */
    public final boolean isLoadPersistence() {
        return this.isLoadPersistence;
    }

    /**
     * Sets True if the persistence is loaded.
     * @param loadPersistence persistence
     */
    private void setLoadPersistence(final boolean loadPersistence) {
        this.isLoadPersistence = loadPersistence;
    }

    @Override
    public final Restlet createInboundRoot() {
        final Router router = new Router(getContext());
        router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
        router.attachDefault(JobsResource.class);
        final Directory appStorage = new Directory(getContext(), "file://" + getStorageDirectory());
        appStorage.setListingAllowed(true);
        appStorage.setDeeplyAccessible(true);
        router.attach("/storage", appStorage);
        router.attach("/jobCache/{job-id}/{file-id}", JobIdDirectory.class);
        router.attach("/jobCache/{job-id}", JobIdDirectory.class);
        router.attach("/jobCache", JobIdDirectory.class);
        router.attach("/cache", StoreObject.class);
        router.attach("/jobApi", JobApi.class);
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
    public final ApplicationInfo getApplicationInfo(final Request request, final Response response) {
        final ApplicationInfo result = super.getApplicationInfo(request, response);
        final DocumentationInfo docInfo = new DocumentationInfo("The Universal Worker Service (UWS) pattern defines how to manage asynchronous execution of jobs on a service");
        docInfo.setTitle(this.getName());
        docInfo.setTextContent(this.getDescription());
        result.setDocumentation(docInfo);

        result.getNamespaces().put("xmlns:uws=\"http://www.ivoa.net/xml/UWS/v1.0\"", "uws");
        result.getNamespaces().put("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "xsi");
        result.getNamespaces().put("xmlns:xlink=\"http://www.w3.org/1999/xlink\"", "xlink");
        final GrammarsInfo grammar = new GrammarsInfo();
        final IncludeInfo include = new IncludeInfo();
        include.setTargetRef(new Reference("http://ivoa.net/xml/UWS/UWS-v1.0.xsd"));
        grammar.getIncludes().add(include);
        result.setGrammars(grammar);
        return result;
    }

    @Override
    public final Validator<AbstractApplicationPlugin> getValidator() {
        return new Validator<AbstractApplicationPlugin>() {

            @Override
            public Set<ConstraintViolation> validate(final AbstractApplicationPlugin item) {
                final Set<ConstraintViolation> constraints = new HashSet<ConstraintViolation>();
                ApplicationPluginParameter param = item.getParameter(STORAGE_DIRECTORY_PARAMETER);
                final String storageDirectoryValue = param.getValue();
                if (storageDirectoryValue.isEmpty()) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("This parameter must be set");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setInvalidValue(storageDirectoryValue);
                    constraint.setValueName(param.getName());
                    constraints.add(constraint);
                } else if (!storageDirectoryValue.startsWith("file://")) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("The parameter value must start with file://");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setInvalidValue(storageDirectoryValue);
                    constraint.setValueName(param.getName());
                    constraints.add(constraint);
                } else {
                    final File file = new File(storageDirectoryValue.substring(7));
                    if (!file.canRead() || !file.canWrite()) {
                        final ConstraintViolation constraint = new ConstraintViolation();
                        constraint.setMessage("Cannot read or write on " + storageDirectoryValue);
                        constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                        constraint.setInvalidValue(storageDirectoryValue);
                        constraint.setValueName(param.getName());
                        constraints.add(constraint);
                    }
                }
                param = item.getParameter(JOB_TASK_IMPL_PARAMETER);
                final String value = param.getValue();
                if (value.isEmpty()) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("This parameter must be set");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setInvalidValue(value);
                    constraint.setValueName(param.getName());
                    constraints.add(constraint);
                } else {
                    final Job job = AbstractJobTask.getCapabilities(value);
                    if (!Utility.isSet(job)) {
                        final ConstraintViolation constraint = new ConstraintViolation();
                        constraint.setMessage("Cannot retrieve the getCapabilities from " + value);
                        constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                        constraint.setInvalidValue(value);
                        constraint.setValueName(param.getName());
                        constraints.add(constraint);
                    } else {
                        try {
                            final Representation rep = new CapabilitiesRepresentation(job);
                            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                            final URL xsdSchema = Job.class.getResource("UwsGetCapabilities.xsd");
                            final Schema schema = schemaFactory.newSchema(xsdSchema);
                            final javax.xml.validation.Validator validator = schema.newValidator();
                            final InputStream inputStream = rep.getStream();
                            validator.validate(new StreamSource(inputStream));
                        } catch (SAXException ex) {
                            LOG.severe(ex.getMessage());
                            final ConstraintViolation constraint = new ConstraintViolation();
                            constraint.setMessage(ex.getMessage());
                            constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                            constraint.setInvalidValue(value);
                            constraint.setValueName(param.getName());
                            constraints.add(constraint);
                        } catch (IOException ex) {
                            LOG.severe(ex.getMessage());
                            final ConstraintViolation constraint = new ConstraintViolation();
                            constraint.setMessage(ex.getMessage());
                            constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                            constraint.setInvalidValue(value);
                            constraint.setValueName(param.getName());
                            constraints.add(constraint);
                        }
                    }
                }
                return constraints;
            }
        };
    }
}
