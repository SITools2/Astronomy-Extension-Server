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
package fr.cnes.sitools.astro.vo.uws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import javax.xml.datatype.DatatypeConfigurationException;

import net.ivoa.xml.uws.v1.ErrorSummary;
import net.ivoa.xml.uws.v1.ErrorType;
import net.ivoa.xml.uws.v1.ExecutionPhase;
import net.ivoa.xml.uws.v1.ResultReference;
import net.ivoa.xml.uws.v1.Results;
import nom.tam.fits.FitsException;

import org.restlet.engine.Engine;

import fr.cnes.sitools.astro.cutout.CutOutException;
import fr.cnes.sitools.astro.cutout.CutOutInterface;
import fr.cnes.sitools.astro.cutout.HealpixMap;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.extensions.common.AstroCoordinate;
import fr.cnes.sitools.xml.uws.v1.ImageFormatType;
import fr.cnes.sitools.xml.uws.v1.InputsType;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Polygon;
import fr.cnes.sitools.xml.uws.v1.Job;
import fr.cnes.sitools.xml.uws.v1.ObjectFactory;
import fr.cnes.sitools.xml.uws.v1.OutputsType;
import fr.cnes.sitools.xml.uws.v1.OutputsType.Image;
import fr.cnes.sitools.xml.uws.v1.ReferenceFrameType;

/**
 * Implements a Healpix cutout in a UWS service.
 * <p>
 * Here is an example to run the cutout from a UWS service
 * <code>
 * curl -v -X POST -d
 * "PHASE=RUN&uri=http://localhost:8182/sitools/datastorage/user/tmp/test.fits&rightAscension=57&declination=88&radius=1"
 * "http://localhost:8182/uws"
 * </code>
 * </p>
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class HealpixCutOut extends AbstractJobTask {

    @Override
    public final void run() {
        try {
            setBlinker(Thread.currentThread());
            setStartTime(Util.convertIntoXMLGregorian(new Date()));
            setPhase(ExecutionPhase.EXECUTING);
            final List<String> filenameList = createJob();
            createResults(filenameList);
            setEndTime(Util.convertIntoXMLGregorian(new Date()));
            setPhase(ExecutionPhase.COMPLETED);
        } catch (DatatypeConfigurationException ex) {
            Engine.getLogger(HealpixCutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (FitsException ex) {
            Engine.getLogger(HealpixCutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (CutOutException ex) {
            Engine.getLogger(HealpixCutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (FileNotFoundException ex) {
            Engine.getLogger(HealpixCutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (IOException ex) {
            Engine.getLogger(HealpixCutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (Error error) {
            Engine.getLogger(HealpixCutOut.class.getName()).log(Level.SEVERE, null, error);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(error.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);           
        }
        // TODO : RuntimeException must be handled on UWS framework.
    }

    /**
     * Runs the cutout and returns the filename of the created files.
     * @return the cutout and returns the filename of the created files
     * @throws CutOutException when a cutout probem occurs
     * @throws MalformedURLException when the URL of the input file is wrong
     * @throws FitsException when a Fits error happens
     * @throws FileNotFoundException when the file is not found
     */
    private List<String> createJob() throws CutOutException, MalformedURLException, FitsException, FileNotFoundException {                
        setOwnerId("NULL");
        final double cdelt1 = Double.valueOf(getParameterValue("cdelt1"));
        final double cdelt2 = Double.valueOf(getParameterValue("cdelt2"));
        final double long1 = Double.valueOf(getParameterValue("long1"));
        final double long2 = Double.valueOf(getParameterValue("long2"));
        final double long3 = Double.valueOf(getParameterValue("long3"));
        final double long4 = Double.valueOf(getParameterValue("long4"));
        final double lat1 = Double.valueOf(getParameterValue("lat1"));
        final double lat2 = Double.valueOf(getParameterValue("lat2"));
        final double lat3 = Double.valueOf(getParameterValue("lat3"));
        final double lat4 = Double.valueOf(getParameterValue("lat4"));
        final double rotation = Double.valueOf(getParameterValue("rotation"));
        final String filename = getParameterValue("filename");
        final AstroCoordinate.CoordinateSystem coordinateSystem = AstroCoordinate.CoordinateSystem.valueOf(getParameterValue("coordSystem"));
        final double[] fov = new double[]{long1, lat1, long2, lat2, long3, lat3, long4, lat4};
        final String path = this.getStoragePathJob().replace(getJobTaskId(), "");
        final File file = new File(path + File.separator + filename);
        final CutOutInterface cutout = new HealpixMap(cdelt1, cdelt2, fov, rotation, file, coordinateSystem);
        //final String outputFileJpeg = getStoragePathJob() + File.separator + getNameFrom(new URL(uri), "jpeg");
        //final File fileJpeg = new File(outputFileJpeg);
        //FileOutputStream fos = new FileOutputStream(fileJpeg);
        //cutout.createCutoutPreview(fos);
        //try {
        //    fos.close();
        //} catch (IOException ex) {
        //    Engine.getLogger(HealpixCutOut.class.getName()).log(Level.SEVERE, null, ex);
       // }
        final String outputFileFits = getStoragePathJob() + File.separator + "result.fits";
        final File fileFits = new File(outputFileFits);
        final FileOutputStream fos = new FileOutputStream(fileFits);
        cutout.createCutoutFits(fos);
        try {
            fos.close();
        } catch (IOException ex) {
            Engine.getLogger(HealpixCutOut.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Arrays.asList("result.fits");
    }

    /**
     * Returns the job owner from the URL.
     * @param url url
     * @return the job owner
     */
    private String getJobOwner(final URL url) {
        return url.getUserInfo();
    }

    /**
     * Creates the UWS results.
     * @param filenameList filenames to add in the result
     */
    private void createResults(final List<String> filenameList) {
        final Results uwsResults = new Results();
        for (final String filename : filenameList) {
            final ResultReference resultReference = new ResultReference();
            resultReference.setId(filename);
            resultReference.setHref(getStoragePublicUrl() + "/" + getJobTaskId() + "/" + filename);
            uwsResults.getResult().add(resultReference);
        }
        setResults(uwsResults);
    }

    @Override
    public final Job getCapabilities() {
        final ObjectFactory objFactory = new ObjectFactory();
        final Job job = objFactory.createJob();
        // Create geometry
        final Geometry geom = new Geometry();
        geom.setReferenceFrame(ReferenceFrameType.EQUATORIAL);
        final Polygon polygon = new Polygon();
        final Polygon.Longitude1 longitude1 = new Polygon.Longitude1();
        longitude1.setName("long1");
        longitude1.setDocumentation("longitude of the first point in decimal degree");
        final Polygon.Longitude2 longitude2 = new Polygon.Longitude2();
        longitude2.setName("long2");
        longitude2.setDocumentation("longitude of the second point in decimal degree");
        final Polygon.Longitude3 longitude3 = new Polygon.Longitude3();
        longitude3.setName("long3");
        longitude3.setDocumentation("longitude of the third point in decimal degree");
        final Polygon.Longitude4 longitude4 = new Polygon.Longitude4();
        longitude4.setName("long4");
        longitude4.setDocumentation("longitude of the fourth point in decimal degree");
        final Polygon.Latitude1 latitude1 = new Polygon.Latitude1();
        latitude1.setName("lat1");
        latitude1.setDocumentation("latitude of the first point in decimal degree");
        final Polygon.Latitude2 latitude2 = new Polygon.Latitude2();
        latitude2.setName("lat2");
        latitude2.setDocumentation("latitude of the second point in decimal degree");
        final Polygon.Latitude3 latitude3 = new Polygon.Latitude3();
        latitude3.setName("lat3");
        latitude3.setDocumentation("latitude of the third point in decimal degree");
        final Polygon.Latitude4 latitude4 = new Polygon.Latitude4();
        latitude4.setName("lat4");
        latitude4.setDocumentation("latitude of the fourth point in decimal degree");
        final Polygon.Rotation rotation = new Polygon.Rotation();
        rotation.setName("rotation");
        rotation.setDocumentation("Rotation of the polygon. The rotation is counted positevely from North to East");
        polygon.setLatitude1(latitude1);
        polygon.setLatitude2(latitude2);
        polygon.setLatitude3(latitude3);
        polygon.setLatitude4(latitude4);
        polygon.setLongitude1(longitude1);
        polygon.setLongitude2(longitude2);
        polygon.setLongitude3(longitude3);
        polygon.setLongitude4(longitude4);
        polygon.setRotation(rotation);
        geom.setPolygon(polygon);
        final InputsType.Keyword cdelt1 = new InputsType.Keyword();
        cdelt1.setName("cdelt1");
        cdelt1.setDocumentation("Arcsec per pixel along X axis");
        final InputsType.Keyword cdelt2 = new InputsType.Keyword();
        cdelt2.setName("cdelt2");
        cdelt2.setDocumentation("Arcsec per pixel along X axis");
        // Create inputs
        final InputsType inputs = new InputsType();
        inputs.getKeyword().add(cdelt1);
        inputs.getKeyword().add(cdelt2);
        inputs.setGeometry(geom);
        // Create output
        final OutputsType outputs = new OutputsType();
        final Image image = new Image();
        image.setFormat(ImageFormatType.IMAGE_FITS);
        outputs.getImage().add(image);
        // Create job
        job.setInputs(inputs);
        job.setOutputs(outputs);
        job.setName("HealpixCutOut");
        job.setTitle("Creates an image from a Healpix FITS");
        return job;
    }
}
