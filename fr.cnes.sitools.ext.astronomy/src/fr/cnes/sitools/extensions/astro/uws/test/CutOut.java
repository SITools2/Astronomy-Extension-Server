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
package fr.cnes.sitools.extensions.astro.uws.test;

import fr.cnes.sitools.astro.cutout.CutOutException;
import fr.cnes.sitools.astro.cutout.CutOutSITools2;
import fr.cnes.sitools.extensions.astro.uws.common.UniversalWorkerException;
import fr.cnes.sitools.extensions.astro.uws.common.Util;
import fr.cnes.sitools.extensions.astro.uws.jobmanager.AbstractJobTask;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;
import net.ivoa.xml.uws.v1.ExecutionPhase;
import net.ivoa.xml.uws.v1.ResultReference;
import net.ivoa.xml.uws.v1.Results;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

/**
 * curl -v -X POST -d "PHASE=RUN&uri=/sitools/datastorage/user/tmp/test.fits&ra=57&dec=88&radius=1" "http://localhost:8182/uws"
 * @author malapert
 */
public class CutOut extends AbstractJobTask {
    
    @Override
    public void run() {
        try {
            setBlinker(Thread.currentThread());
            setStartTime(Util.convertIntoXMLGregorian(new Date()));
            setPhase(ExecutionPhase.EXECUTING);
            String uri = "http://localhost:8182"+getParameterValue("uri");
            double ra = Double.valueOf(getParameterValue("ra"));
            double dec = Double.valueOf(getParameterValue("dec"));
            double radius = Double.valueOf(getParameterValue("radius"));
            CutOutSITools2 cutout = new CutOutSITools2(new Fits(uri), ra, dec, radius);
            File file = new File("/tmp/toto.jpeg");            
            cutout.createCutoutPreview(new FileOutputStream(file));
            this.moveFile(file);                      
            Results r = new Results();
            ResultReference rf = new ResultReference();
            rf.setId("toto.jpeg");
            rf.setHref(getStoragePublicUrl() + "/" + getJobTaskId() + "/toto.jpeg");
            r.getResult().add(rf);
            setResults(r);            
            setEndTime(Util.convertIntoXMLGregorian(new Date()));
            setPhase(ExecutionPhase.COMPLETED);            
        } catch (DatatypeConfigurationException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR);  
        } catch (FitsException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR); 
        } catch (CutOutException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR); 
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR); 
        //} catch (UniversalWorkerException ex) {
        //    Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
        //    setPhase(ExecutionPhase.ERROR); 
        //}
    }   catch (UniversalWorkerException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR);
        } catch (IOException ex) {
            Logger.getLogger(CutOut.class.getName()).log(Level.SEVERE, null, ex);
            setPhase(ExecutionPhase.ERROR); 
        }
    }
}
