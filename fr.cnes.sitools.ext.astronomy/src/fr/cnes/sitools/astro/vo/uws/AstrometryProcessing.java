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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.datatype.DatatypeConfigurationException;

import net.ivoa.xml.uws.v1.ErrorSummary;
import net.ivoa.xml.uws.v1.ErrorType;
import net.ivoa.xml.uws.v1.ExecutionPhase;

import org.restlet.data.Form;
import org.restlet.engine.Engine;

import fr.cnes.sitools.extensions.astro.application.uws.client.ClientUWS;
import fr.cnes.sitools.extensions.astro.application.uws.client.ClientUWSException;
import fr.cnes.sitools.extensions.astro.application.uws.common.Util;
import fr.cnes.sitools.extensions.astro.application.uws.jobmanager.AbstractJobTask;
import fr.cnes.sitools.xml.uws.v1.ImageFormatType;
import fr.cnes.sitools.xml.uws.v1.InputsType;
import fr.cnes.sitools.xml.uws.v1.Job;
import fr.cnes.sitools.xml.uws.v1.ObjectFactory;
import fr.cnes.sitools.xml.uws.v1.OutputsType;

/**
 * Astrometric calibration for FITS images.
 * <p>
 * This service uses the UWS service from VOparis.
 * </p>
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class AstrometryProcessing extends AbstractJobTask {
    /**
     * URL of the server at obspm.
     */
    private static final String ASTRO_CALIB_SERVER = "http://voparis-uws.obspm.fr/uws-v1.0/astrometry";
    /**
     * Reference catalog for the calibration.
     */
    private static final Map<String, Integer> CATALOG = new HashMap<String, Integer>();
    /**
     * Parameter for obspm.
     */
    private static final String IMAGE_PARAM = "image";
    /**
     * Parameter for obspm.
     */
    private static final String CAT_PARAM = "cat";
    /**
     * Parameter for obspm.
     */
    private static final String ORDER_PARAM = "order";
    /**
     * Parameter for obspm.
     */
    private static final int DEFAULT_ORDER = 3;
    /**
     * Time in milliseconds between two calls to obspm.
     */
    private static final long ELAPSED_TIME_BETWEEN_CALL_MILLISEC = 30000;

    static {
        CATALOG.put("UCAC2", 1);
        CATALOG.put("2MASS", 2);
    }

    @Override
    public final void run() {
        try {
            setBlinker(Thread.currentThread());
            setStartTime(Util.convertIntoXMLGregorian(new Date()));
            setPhase(ExecutionPhase.QUEUED);
            final ClientUWS uws = new ClientUWS(ASTRO_CALIB_SERVER);
            final Form form = new Form();
            form.add(IMAGE_PARAM, getParameterValue(IMAGE_PARAM));
            form.add(CAT_PARAM, String.valueOf(CATALOG.get("2MASS")));
            form.add(ORDER_PARAM, String.valueOf(DEFAULT_ORDER));
            form.add("PHASE", "RUN");
            final String jobId = uws.createJob(form);
            jobFinished(uws, jobId);
            final ExecutionPhase phaseResult = uws.getJobPhase(jobId);
            if (phaseResult.equals(ExecutionPhase.COMPLETED)) {
                setPhase(ExecutionPhase.COMPLETED);
                setResults(uws.getJobResults(jobId));
            } else {
                setPhase(phaseResult);
                setError(uws.getJobError(jobId));
            }
        } catch (ClientUWSException ex) {
            Engine.getLogger(AstrometryProcessing.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
        } catch (InterruptedException ex) {
            Engine.getLogger(AstrometryProcessing.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
        } catch (DatatypeConfigurationException ex) {
            Engine.getLogger(AstrometryProcessing.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(ex.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
        } catch (Error error) {
            Engine.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, error);
            final ErrorSummary errorSumm = new ErrorSummary();
            errorSumm.setMessage(error.getMessage());
            errorSumm.setType(ErrorType.FATAL);
            errorSumm.setHasDetail(true);
            setError(errorSumm);
            setPhase(ExecutionPhase.ERROR);
        }
    }

    /**
     * Waits until job is processed.
     * @param uws uws client
     * @param jobId jobId
     * @throws InterruptedException when thread error happens
     * @throws ClientUWSException  when a client error happens
     */
    private void jobFinished(final ClientUWS uws, final String jobId) throws InterruptedException, ClientUWSException {
        boolean isFinished = false;
        while (!isFinished) {
            Thread.sleep(ELAPSED_TIME_BETWEEN_CALL_MILLISEC);
            final ExecutionPhase phase = uws.getJobPhase(jobId);
            isFinished = isProcessingFinished(phase);
        }
    }

    /**
     * Returns false when the job is not being processed.
     * @param phase job status
     * @return false when the job is not being processed
     */
    private boolean isProcessingFinished(final ExecutionPhase phase) {
        return !(phase.equals(ExecutionPhase.PENDING)
            || phase.equals(ExecutionPhase.QUEUED)
            || phase.equals(ExecutionPhase.EXECUTING));
    }

    @Override
    public final Job getCapabilities() {
        final ObjectFactory objFactory = new ObjectFactory();
        final Job job = objFactory.createJob();
        // Input image
        final fr.cnes.sitools.xml.uws.v1.InputsType.Image imageInput = new fr.cnes.sitools.xml.uws.v1.InputsType.Image();
        imageInput.setName(IMAGE_PARAM);
        imageInput.setFormat(ImageFormatType.IMAGE_FITS);
        imageInput.setDocumentation("Supported Input format : Image/FITS");
        // Create inputs
        final InputsType inputs = new InputsType();
        inputs.getImage().add(imageInput);
        // Create output
        final OutputsType outputs = new OutputsType();
        final OutputsType.Image image = new OutputsType.Image();
        image.setFormat(ImageFormatType.IMAGE_FITS);
        outputs.getImage().add(image);
        // Create job
        job.setInputs(inputs);
        job.setOutputs(outputs);
        job.setName("Astrometric Calibration");
        job.setTitle("Astrometric Calibration of an Image");
        return job;
    }
}
