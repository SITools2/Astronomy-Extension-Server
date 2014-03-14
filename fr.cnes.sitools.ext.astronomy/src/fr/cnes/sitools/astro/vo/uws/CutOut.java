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
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import org.restlet.engine.Engine;

import fr.cnes.sitools.astro.cutout.CutOutException;
import fr.cnes.sitools.astro.cutout.CutOutSITools2;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.xml.uws.v1.ImageFormatType;
import fr.cnes.sitools.xml.uws.v1.InputsType;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Latitude;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Longitude;
import fr.cnes.sitools.xml.uws.v1.InputsType.Geometry.Circle.Radius;
import fr.cnes.sitools.xml.uws.v1.Job;
import fr.cnes.sitools.xml.uws.v1.ObjectFactory;
import fr.cnes.sitools.xml.uws.v1.OutputsType;
import fr.cnes.sitools.xml.uws.v1.OutputsType.Image;
import fr.cnes.sitools.xml.uws.v1.ReferenceFrameType;

/**
 * Implements a FIT cutout in a UWS service.
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
public class CutOut extends AbstractJobTask {

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
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (FitsException ex) {
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (CutOutException ex) {
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (FileNotFoundException ex) {
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (IOException ex) {
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (Error error) {
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, error);
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
        final String uri = getParameterValue("uri");
        setOwnerId(getJobOwner(new URL(uri)));
        final double rightAscension = Double.valueOf(getParameterValue("ra"));
        final double declination = Double.valueOf(getParameterValue("dec"));
        final double radius = Double.valueOf(getParameterValue("radius"));
        final CutOutSITools2 cutout = new CutOutSITools2(new Fits(uri), rightAscension, declination, radius);
        final String outputFileJpeg = getStoragePathJob() + File.separator + getNameFrom(new URL(uri), "jpeg");
        final File fileJpeg = new File(outputFileJpeg);
        FileOutputStream fos = new FileOutputStream(fileJpeg);
        cutout.createCutoutPreview(fos);
        try {
            fos.close();
        } catch (IOException ex) {
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
        }
        final String outputFileFits = getStoragePathJob() + File.separator + getNameFrom(new URL(uri), "fits");
        final File fileFits = new File(outputFileFits);
        fos = new FileOutputStream(fileFits);
        cutout.createCutoutFits(fos);
        try {
            fos.close();
        } catch (IOException ex) {
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Arrays.asList(getNameFrom(new URL(uri), "fits"), getNameFrom(new URL(uri), "jpeg"));
    }

    /**
     * Returns the filename from an URL without the extension and add an extension.
     * @param url URL of the FITS file
     * @param ext Extension of the output file
     * @return the filename
     */
    private String getNameFrom(final URL url, final String ext) {
        final String filename = url.getFile();
        final int slashIndex = filename.lastIndexOf('/');
        final int pointIndex = filename.lastIndexOf('.');
        String filenameWithoutExt;
        if (pointIndex != -1) {
            filenameWithoutExt = filename.substring(slashIndex + 1, pointIndex);
        } else {
            filenameWithoutExt = filename.substring(slashIndex + 1);
        }
        return filenameWithoutExt + "_cutout." + ext;
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
        final Circle circle = new Circle();
        final Latitude latitude = new Latitude();
        latitude.setName("dec");
        latitude.setDocumentation("Declination of the center of the circle in degree in the Equatorial refrence frame");
        final Longitude longitude = new Longitude();
        longitude.setName("ra");
        longitude.setDocumentation("Right Ascension of the center of the circle in degree in the Equatorial refrence frame");
        final Radius radius = new Radius();
        radius.setName("radius");
        radius.setDocumentation("Radius of the circle in degree in the Equatorial refrence frame");
        circle.setLatitude(latitude);
        circle.setLongitude(longitude);
        circle.setRadius(radius);
        geom.setCircle(circle);
        // Create image where the geometry is applied
        final fr.cnes.sitools.xml.uws.v1.InputsType.Image imageInput = new fr.cnes.sitools.xml.uws.v1.InputsType.Image();
        imageInput.setName("uri");
        imageInput.setFormat(ImageFormatType.IMAGE_FITS);
        imageInput.setDocumentation("Supported Input format : Image/FITS");
        // Create inputs
        final InputsType inputs = new InputsType();
        inputs.setGeometry(geom);
        inputs.getImage().add(imageInput);
        // Create output
        final OutputsType outputs = new OutputsType();
        Image image = new Image();
        image.setFormat(ImageFormatType.IMAGE_FITS);
        outputs.getImage().add(image);
        image = new Image();
        image.setFormat(ImageFormatType.IMAGE_JPEG);
        outputs.getImage().add(image);
        // Create job
        job.setInputs(inputs);
        job.setOutputs(outputs);
        job.setName("FitsCutOut");
        job.setTitle("Creates a cutOut");
        return job;
    }
}
