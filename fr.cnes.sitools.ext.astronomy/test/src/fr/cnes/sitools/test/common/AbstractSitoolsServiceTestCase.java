package fr.cnes.sitools.test.common;

import com.thoughtworks.xstream.XStream;

import fr.cnes.sitools.AbstractSitoolsServerTestCase;
import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.common.SitoolsXStreamRepresentation;
import fr.cnes.sitools.common.XStreamFactory;
import fr.cnes.sitools.common.model.Resource;
import fr.cnes.sitools.common.model.Response;
import fr.cnes.sitools.project.model.Project;
import fr.cnes.sitools.project.model.ProjectModule;
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.server.Starter;
import fr.cnes.sitools.tasks.AbstractTaskResourceTestCase;
import fr.cnes.sitools.util.RIAPUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.restlet.data.MediaType;
import org.restlet.engine.Engine;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.ext.xstream.XstreamRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ClientResource;

/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
/**
 *
 * @author malapert
 */
public abstract class AbstractSitoolsServiceTestCase extends AbstractTaskResourceTestCase {
	
	/**
	   * Executed once before all test methods
	   */
	  @BeforeClass
	  public static void before() {
	    // change the server port to avoid collisions with the core tests
	    Engine.clearThreadLocalVariables();
	    settings = SitoolsSettings.getInstance("sitools", Starter.class.getClassLoader(), Locale.FRANCE, true);

	    //String source = settings.getRootDirectory() + "/workspace/fr.cnes.sitools.ext.astronomy/test/data";
            String source = "/extensions/astronomy/fr.cnes.sitools.ext.astronomy/test/data";
	    String cible = settings.getRootDirectory() + TEST_FILES_REPOSITORY;

	    LOGGER.info("COPY SOURCE:" + source + " CIBLE:" + cible);

	    File fileCible = new File(cible);
	    fileCible.mkdirs();

	    setUpDataDirectory(source, cible);
	    settings.setStoreDIR(TEST_FILES_REPOSITORY);
	    settings.setTmpFolderUrl(settings.getStoreDIR(Consts.APP_TMP_FOLDER_DIR));

	    AbstractSitoolsServerTestCase.start();
	  }
	
    /**
     * Create a project object for tests
     * 
     * @param id
     *          project id
     * @return Project
     */
    public Project createProjectObject(String id, String urlAttachment) {
        Project item = new Project();
        item.setId(id);
        item.setName(id);
        item.setDescription("project description");
        item.setSitoolsAttachementForUsers(urlAttachment);
        Resource image = new Resource();
        image.setUrl("http://uneimage.png");
        item.setImage(image);
        Resource dataset1 = new Resource();
        dataset1.setId("9991");
        Resource dataset2 = new Resource();
        dataset2.setId("9992");
        ArrayList<Resource> datasets = new ArrayList<Resource>();
        datasets.add(dataset1);
        datasets.add(dataset2);
        item.setDataSets(datasets);
        ArrayList<ProjectModule> modules = new ArrayList<ProjectModule>();
        ProjectModule projectModule1 = new ProjectModule();
        projectModule1.setId("12345");
        modules.add(projectModule1);
        return item;
    }

    /**
     * Invoke POST
     * 
     * @param item
     *          Project
     * @throws IOException Exception when copying configuration files from TEST to data/TESTS 
     */
    public void createProject(Project item) throws IOException, Exception {
        Representation rep = getRepresentation(item, getMediaTest());
        ClientResource cr = new ClientResource(getBaseUrl() + SitoolsSettings.getInstance().getString(Consts.APP_PROJECTS_URL));

        Representation result = cr.post(rep, getMediaTest());
        if (result == null) {
            throw new Exception();
        }
        if (!cr.getStatus().isSuccess()) {
            throw new Exception();
        }

        Response response = getResponse(getMediaTest(), result, Project.class);
        if (!response.getSuccess()) {
            throw new Exception();
        }
    }

    /**
     * Invoke DELETE
     * 
     * @param item
     *          Project
     * @throws IOException Exception when copying configuration files from TEST to data/TESTS 
     */
    public void deleteProject(Project item) throws IOException, Exception {
        String url = getBaseUrl() + SitoolsSettings.getInstance().getString(Consts.APP_PROJECTS_URL) + "/" + item.getId();
        ClientResource cr = new ClientResource(url);

        Representation result = cr.delete(getMediaTest());

        if (result == null) {
            throw new Exception();
        }
        if (!cr.getStatus().isSuccess()) {
            throw new Exception();
        }
    }

    /**
     * Activate a project
     * 
     * @param proj
     *          the project to activate
     * @throws IOException
     *           Exception when copying configuration files from TEST to data/TESTS
     */
    public void activateProject(Project proj) throws IOException, Exception {
        // TODO Auto-generated method stub
        StringRepresentation projRep = new StringRepresentation("");
        String url = getBaseUrl() + SitoolsSettings.getInstance().getString(Consts.APP_PROJECTS_URL) + "/" + proj.getId() + "/start";

        ClientResource cr = new ClientResource(url);
        Representation result = cr.put(projRep, getMediaTest());
        if (result == null) {
            throw new Exception();
        }
        if (!cr.getStatus().isSuccess()) {
            throw new Exception();
        }
    }

    public Representation getRepresentation(Project item, MediaType media) {
        if (media.equals(MediaType.APPLICATION_JSON)) {
            return new JsonRepresentation(item);
        } else if (media.equals(MediaType.APPLICATION_XML)) {
            XStream xstream = XStreamFactory.getInstance().getXStream(media, false);
            XstreamRepresentation<Project> rep = new XstreamRepresentation<Project>(media, item);
            configure(xstream);
            rep.setXstream(xstream);
            return rep;
        } else {
            Logger.getLogger(AbstractSitoolsServiceTestCase.class.getName()).warning("Only JSON or XML supported in tests");
            return null; // TODO complete test with ObjectRepresentation
        }
    }


    public static Response getResponse(MediaType media, Representation representation, Class<?> dataClass) {
        return getResponse(media, representation, dataClass, false);
    }

    /**
     * REST API Response Representation wrapper for single or multiple items expexted
     * 
     * @param media
     *          MediaType expected
     * @param representation
     *          service response representation
     * @param dataClass
     *          class expected for items of the Response object
     * @param isArray
     *          if true wrap the data property else wrap the item property
     * @return Response
     */
    public static Response getResponse(MediaType media, Representation representation, Class<?> dataClass, boolean isArray) {
        try {
            if (!media.isCompatible(getMediaTest()) && !media.isCompatible(MediaType.APPLICATION_XML)) {
                Logger.getLogger(AbstractSitoolsServiceTestCase.class.getName()).warning("Only JSON or XML supported in tests");
                return null;
            }

            XStream xstream = XStreamFactory.getInstance().getXStreamReader(media);
            xstream.autodetectAnnotations(false);
            xstream.alias("response", Response.class);
            xstream.alias("project", Project.class);
            xstream.alias("dataset", Resource.class);
            // xstream.alias("dataset", Resource.class);

            if (isArray) {
                xstream.addImplicitCollection(Response.class, "data", dataClass);
            } else {
                xstream.alias("item", dataClass);
                xstream.alias("item", Object.class, dataClass);

                if (media.equals(MediaType.APPLICATION_JSON)) {
                    xstream.addImplicitCollection(Project.class, "dataSets", Resource.class);
                    xstream.aliasField("dataSets", Project.class, "dataSets");
                }

                if (dataClass == Project.class) {
                    xstream.aliasField("project", Response.class, "item");
                    // if (dataClass == DataSet.class)
                    // xstream.aliasField("dataset", Response.class, "item");
                }
            }
            xstream.aliasField("data", Response.class, "data");

            SitoolsXStreamRepresentation<Response> rep = new SitoolsXStreamRepresentation<Response>(representation);
            rep.setXstream(xstream);

            if (media.isCompatible(getMediaTest())) {
                Response response = rep.getObject("response");

                return response;
            } else {
                Logger.getLogger(AbstractSitoolsServiceTestCase.class.getName()).warning("Only JSON or XML supported in tests");
                return null; // TODO complete test with ObjectRepresentation
            }
        } finally {
            RIAPUtils.exhaust(representation);
        }
    }

    /**
     * Configures XStream mapping for Response object with Project content.
     * 
     * @param xstream
     *          XStream
     */
    public void configure(XStream xstream) {
        xstream.autodetectAnnotations(false);
        xstream.alias("response", Response.class);
        xstream.alias("project", Project.class);
        xstream.alias("dataset", Resource.class);
    }
}
