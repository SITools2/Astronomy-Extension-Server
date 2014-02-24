Astronomy-Extension-v2
======================

SITools2's Extension for Astronomy

## Description

SITools2's Extension for Astronomy provides a set of astronomical services for SITools2.

TO DO : define the list of services

## Building sources

### Getting the sources

	$ git clone https://github.com/SITools2/Astronomy-Extension-v2.git
	
### Building the sources

Build the sources using ant

  	$ cd Astronomy-Extension-v2/fr.cnes.sitools.ext.astronomy
  
Edit the `build.properties` file and update the `ROOT_DIRECTORY` value to the SITools2 path, then run ant

        $ ant

### Installing the plugin

Copy the contain in the `dist` directory to `<ROOT_DIRECTORY>/workspace/fr.cnes.sitools.core/ext/`

        $ cp -r dist/* <ROOT_DIRECTORY>/workspace/fr.cnes.sitools.core/ext/ 
