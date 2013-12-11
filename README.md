Astronomy-Extension-v2
======================

SITools2's Extension for Astronomy

## Description

SITools2's Extension for Astronomy provides a set of astronomical services for SITools2.

TO DO : define the list of services

## Building sources

### Getting the sources

	$ git clone https://github.com/SITools2/Astronomy-Extension-v2.git
	
### Build the sources

Build the sources using ant

  $ cd Astronomy-Extension-v2/fr.cnes.sitools.ext.astronomy
  
  $ ant -Dnb.internal.action.name=rebuild clean jar -Dplatforms.JDK_1.6.home=<jdk_home> -Dlibs.SITools2.classpath=<SITool2_PATH>/workspace/fr.cnes.sitools.core/fr.cnes.sitools.core.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/org.restlet.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/org.restlet.ext.xstream.jar:<SITool2_PATH>/workspace/org.restlet.ext.wadl/org.restlet.ext.wadl_2.0.1.jar:<SITool2_PATH>/workspace/org.restlet.patched/org.restlet.patched_1.0.3.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/org.json_2.0/org.json.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/org.restlet.ext.freemarker.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/org.freemarker_2.3/org.freemarker.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/com.thoughtworks.xstream_1.3/com.thoughtworks.xstream.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/org.restlet.ext.json.jar:<SITool2_PATH>/workspace/libraries/org.apache.solr_4.5.0/solr-dataimporthandler-4.5.0.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/org.restlet.ext.fileupload.jar:<SITool2_PATH>/cots/restlet-2.0.5-patched/org.apache.commons.fileupload_1.2/org.apache.commons.fileupload.jar
