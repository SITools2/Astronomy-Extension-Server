/**
 * *****************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of SITools2.
 *
 * SITools2 is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * SITools2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SITools2. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package fr.cnes.sitools.astro.vo.conesearch;

import fr.cnes.sitools.dataset.DataSetApplication;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Info;
import org.restlet.Context;
import org.restlet.Request;

/**
 * This object provides methods to handle input parameters for the cone search protocol.
 *
 * @author Jean-Christophe Malapert
 */
public class ConeSearchInputParameters implements ConeSearchDataModelInterface {

  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(ConeSearchInputParameters.class.getName());
  /**
   * Default verbosity mode.
   */
  private static final int DEFAULT_VERBOSITY_MODE = 0;
  /**
   * Verbosity mode.
   */
  private int verb = DEFAULT_VERBOSITY_MODE;
  /**
   * Data model.
   */
  private Map dataModel = new HashMap();
  /**
   * User request.
   */
  private final Request request;
  /**
   * Application context.
   */
  private final Context context;
  /**
   * Application.
   */
  private final DataSetApplication datasetApp;
  /**
   * Right ascension of the cone center.
   */
  private double ra;
  /**
   * Declination of the cone center.
   */
  private double dec;
  /**
   * Radius of the cone.
   */
  private double sr;

  /**
   * Constructor.
   *
   * @param datasetAppVal dataset application
   * @param requestVal Request
   * @param contextVal Context
   * @param maxSr maxSr for the cone search
   * @param verbosity verbosity
   * @param verbVal verbosity level
   */
  public ConeSearchInputParameters(final DataSetApplication datasetAppVal, final Request requestVal, final Context contextVal,
          final double maxSr, final boolean verbosity, final int verbVal) {
    this.datasetApp = datasetAppVal;
    this.context = contextVal;
    this.request = requestVal;
    String raInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(ConeSearchProtocolLibrary.RA);
    String decInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(ConeSearchProtocolLibrary.DEC);
    String srInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(ConeSearchProtocolLibrary.SR);
    String verbInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(ConeSearchProtocolLibrary.VERB);
    checkInputParameters(raInput, decInput, srInput, maxSr, verbInput, verbosity, verbVal);
  }

  /**
   * Check input parameters.
   *
   * @param raInput Ra
   * @param decInput Dec
   * @param srInput Sr
   * @param maxSr maxSr
   * @param verbosity verbosity
   * @param verbInput user inpur verb
   * @param verbVal verbosity from admin
   */
  private void checkInputParameters(final String raInput, final String decInput, final String srInput, final double maxSr,
          final String verbInput, final boolean verbosity, final int verbVal) {
    List<Info> infos = new ArrayList<Info>();

    try {
      this.ra = Double.valueOf(raInput);
      if (this.ra > ConeSearchProtocolLibrary.RA_MAX || this.ra < ConeSearchProtocolLibrary.RA_MIN) {
        throw new Exception(this.ra + " for RA parameter is not allowed. RA must be in [0,360]");
      }
      this.datasetApp.getLogger().log(Level.FINEST, "RA: {0}", raInput);
    } catch (Exception ex) {
      Info info = new Info();
      info.setID("RA");
      info.setName("Error in RA");
      info.setValueAttribute("Error in input RA: " + ex.getMessage());
      infos.add(info);
      this.datasetApp.getLogger().log(Level.FINEST, "Error RA: {0}", ex.getMessage());
    }

    try {
      this.dec = Double.valueOf(decInput);
      if (this.dec > ConeSearchProtocolLibrary.DEC_MAX || this.dec < ConeSearchProtocolLibrary.DEC_MIN) {
        throw new Exception(this.dec + " for DEC parameter is not allowed. DEC must be in [-90,90]");
      }
      this.datasetApp.getLogger().log(Level.FINEST, "DEC: {0}", decInput);
    } catch (Exception ex) {
      Info info = new Info();
      info.setID("DEC");
      info.setName("Error in DEC");
      info.setValueAttribute("Error in input DEC: " + ex.getMessage());
      infos.add(info);
      this.datasetApp.getLogger().log(Level.FINEST, "Error DEC: {0}", ex.getMessage());
    }

    try {
      this.sr = Double.valueOf(srInput);
      if (this.sr <= 0.0) {
        throw new Exception(this.sr + " for SR parameter is not allowed. SR must be a positive value");
      } else if (this.sr > maxSr) {
        throw new Exception(this.sr
                + " for SR parameter is not allowed. SR must be a positive value inferior or equal to " + maxSr);
      }
      this.datasetApp.getLogger().log(Level.FINEST, "SR: {0}", srInput);
    } catch (Exception ex) {
      Info info = new Info();
      info.setID("SR");
      info.setName("Error in SR");
      info.setValueAttribute("Error in input SR: " + ex.getMessage());
      infos.add(info);
      this.datasetApp.getLogger().log(Level.FINEST, "Error Sr: {0}", ex.getMessage());
    }

    try {
      if (verbosity == true) {
        if (verbInput == null) {
          this.verb = verbVal;
        } else {
          this.verb = Integer.valueOf(verbInput);
        }
      } else {
        this.verb = verbVal;
      }
      this.datasetApp.getLogger().log(Level.FINEST, "VERB: {0}", this.verb);
    } catch (Exception ex) {
      this.verb = verbVal;
      this.datasetApp.getLogger().log(Level.FINEST, "Error VERB: {0}", ex.getMessage());
    }
    if (!infos.isEmpty()) {
      this.dataModel.put("infos", infos);
    }
  }

  /**
   * Get Ra.
   *
   * @return Ra
   */
  public final double getRa() {
    return this.ra;
  }

  /**
   * Get Dec.
   *
   * @return Dec
   */
  public final double getDec() {
    return this.dec;
  }

  /**
   * Get Sr.
   *
   * @return Sr
   */
  public final double getSr() {
    return this.sr;
  }

  /**
   * Get verbosity level.
   *
   * @return verbosity level
   */
  public final int getVerb() {
    return this.verb;
  }

  @Override
  public final Map getDataModel() {
    return Collections.unmodifiableMap(this.dataModel);
  }

  /**
   * Returns the request.
   *
   * @return Request
   */
  public final Request getRequest() {
    return this.request;
  }

  /**
   * Returns the context.
   *
   * @return Context
   */
  public final Context getContext() {
    return this.context;
  }

  /**
   * Get DatasetApplication.
   *
   * @return Dataset application
   */
  public final DataSetApplication getDatasetApplication() {
    return this.datasetApp;
  }
}
