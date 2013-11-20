/**
 * *****************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *****************************************************************************
 */
package fr.cnes.sitools.astro.vo.uws;

import fr.cnes.sitools.astro.image.ZScale;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.xml.uws.v1.ImageFormatType;
import fr.cnes.sitools.xml.uws.v1.InputsType;
import fr.cnes.sitools.xml.uws.v1.InputsType.Keyword;
import fr.cnes.sitools.xml.uws.v1.Job;
import fr.cnes.sitools.xml.uws.v1.ObjectFactory;
import fr.cnes.sitools.xml.uws.v1.OutputsType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;
import net.ivoa.xml.uws.v1.ErrorSummary;
import net.ivoa.xml.uws.v1.ErrorType;
import net.ivoa.xml.uws.v1.ExecutionPhase;
import net.ivoa.xml.uws.v1.ResultReference;
import net.ivoa.xml.uws.v1.Results;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.ImageHDU;

/**
 * Computes the zscale of a FITS image.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ZScaleProcessing extends AbstractJobTask {

    /**
     * Constrast for Zscale algorithm.
     */
    private static final double CONTRAST = 0.25;
    /**
     * Desired number of pixels in sample.
     */
    private static final int OPT_SIZE = 600;
    /**
     * Optimal number of pixels per line.
     */
    private static final int LEN_STDLINE = 120;

    @Override
    public final void run() {
        try {
            setBlinker(Thread.currentThread());
            setStartTime(Util.convertIntoXMLGregorian(new Date()));
            setPhase(ExecutionPhase.EXECUTING);
            final String uri = getParameterValue("uri");
            final Fits fits = new Fits(new URL(uri));
            BasicHDU basicHDU = fits.readHDU();
            ImageHDU image = null;
            while (basicHDU != null && image == null) {
                if (basicHDU instanceof ImageHDU && basicHDU.getData().getSize() != 0) {
                    image = (ImageHDU) basicHDU;
                }
                basicHDU = fits.readHDU();
            }
            final ZScale zscale = new ZScale(image, CONTRAST, OPT_SIZE, LEN_STDLINE);
            final ZScale.ZscaleResult result = zscale.compute();
            final double zMin = result.getZ1();
            final double zMax = result.getZ2();
            final Results uwsResults = new Results();
            final ResultReference resultReferenceZ1 = new ResultReference();
            final ResultReference resultReferenceZ2 = new ResultReference();
            resultReferenceZ1.setId("z1");
            resultReferenceZ1.setType("xs:double");
            resultReferenceZ1.setHref(String.valueOf(zMin));
            resultReferenceZ2.setId("z2");
            resultReferenceZ2.setType("xs:double");
            resultReferenceZ2.setHref(String.valueOf(zMax));
            uwsResults.getResult().add(resultReferenceZ1);
            uwsResults.getResult().add(resultReferenceZ2);
            setResults(uwsResults);
            setPhase(ExecutionPhase.COMPLETED);
            setEndTime(Util.convertIntoXMLGregorian(new Date()));
        } catch (DatatypeConfigurationException ex) {
            Logger.getLogger(ZScaleProcessing.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (FitsException ex) {
            Logger.getLogger(ZScaleProcessing.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (MalformedURLException ex) {
            Logger.getLogger(ZScaleProcessing.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        } catch (IOException ex) {
            Logger.getLogger(ZScaleProcessing.class.getName()).log(Level.SEVERE, null, ex);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        }
    }

    @Override
    public final Job getCapabilities() {
        final ObjectFactory objFactory = new ObjectFactory();
        final Job job = objFactory.createJob();
        final fr.cnes.sitools.xml.uws.v1.InputsType.Image imageInput = new fr.cnes.sitools.xml.uws.v1.InputsType.Image();
        imageInput.setName("uri");
        imageInput.setFormat(ImageFormatType.IMAGE_FITS);
        imageInput.setDocumentation("Supported Input format : Image/FITS");
        // Create inputs
        final InputsType inputs = new InputsType();
        inputs.getImage().add(imageInput);
        // Create outputs
        final OutputsType outputs = new OutputsType();
        Keyword keyword = new Keyword();
        keyword.setDocumentation("z1 Value");
        keyword.setName("z1");
        outputs.getKeyword().add(keyword);
        keyword = new Keyword();
        keyword.setDocumentation("z2 Value");
        keyword.setName("z2");
        outputs.getKeyword().add(keyword);
        // Create job
        job.setInputs(inputs);
        job.setOutputs(outputs);
        job.setName("ComputesZScale");
        job.setTitle("Computes the ZScale algorithm");
        return job;
    }
}
