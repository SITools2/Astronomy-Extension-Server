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
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;
import net.ivoa.xml.uws.v1.ErrorSummary;
import net.ivoa.xml.uws.v1.ErrorType;
import net.ivoa.xml.uws.v1.ExecutionPhase;
import net.ivoa.xml.uws.v1.ResultReference;
import net.ivoa.xml.uws.v1.Results;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

/**
 * curl -v -X POST -d
 * "PHASE=RUN&uri=/sitools/datastorage/user/tmp/test.fits&ra=57&dec=88&radius=1"
 * "http://localhost:8182/uws"
 *
 * @author malapert
 */
public class CutOut extends AbstractJobTask {

    @Override
    public void run() {
        try {
            setBlinker(Thread.currentThread());
            setStartTime(Util.convertIntoXMLGregorian(new Date()));           
            setPhase(ExecutionPhase.EXECUTING);            
            final List<String> filenameList = createJob();
            createResults(filenameList);
            setEndTime(Util.convertIntoXMLGregorian(new Date()));
            setPhase(ExecutionPhase.COMPLETED);
        } catch (DatatypeConfigurationException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (FitsException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (CutOutException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (IOException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (Error error) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, error);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(error.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);           
        }        
    }

    private List<String> createJob() throws CutOutException, MalformedURLException, FitsException, FileNotFoundException {        
        
        final String uri = getParameterValue("uri");
        setOwnerId(getJobOwner(new URL(uri)));
        final double ra = Double.valueOf(getParameterValue("ra"));
        final double dec = Double.valueOf(getParameterValue("dec"));
        final double radius = Double.valueOf(getParameterValue("radius"));
        final CutOutSITools2 cutout = new CutOutSITools2(new Fits(uri), ra, dec, radius);
        final String outputFileJpeg = getStoragePathJob() + File.separator + getNameFrom(new URL(uri), "jpeg");
        final File fileJpeg = new File(outputFileJpeg);
        cutout.createCutoutPreview(new FileOutputStream(fileJpeg));
        final String outputFileFits = getStoragePathJob() + File.separator + getNameFrom(new URL(uri), "fits");
        final File fileFits = new File(outputFileFits);        
        cutout.createCutoutFits(new FileOutputStream(fileFits));
        return Arrays.asList(getNameFrom(new URL(uri), "fits"), getNameFrom(new URL(uri), "jpeg"));
    }

    private String getNameFrom(final URL url, final String ext) {
        final String filename = url.getFile();
        final int slashIndex = filename.lastIndexOf('/');
        String filenameWithoutExt = filename.substring(slashIndex+1, filename.lastIndexOf('.'));
        return filenameWithoutExt + "." + ext;
    }

    private String getJobOwner(final URL url) {
        return url.getUserInfo();
    }

    private void createResults(final List<String> filenameList) {
        final Results r = new Results();
        for (String filename:filenameList) {
            final ResultReference rf = new ResultReference();
            rf.setId(filename);
            rf.setHref(getStoragePublicUrl() + "/" + getJobTaskId() + "/" + filename);
            r.getResult().add(rf);
        }
        setResults(r);
    }
    
    @Override
    public Job getCapabilities() {
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
