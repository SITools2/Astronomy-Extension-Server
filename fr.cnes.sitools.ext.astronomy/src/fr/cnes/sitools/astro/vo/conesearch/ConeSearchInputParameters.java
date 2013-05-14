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
import fr.cnes.sitools.extensions.common.InputsValidation;
import fr.cnes.sitools.extensions.common.NotNullAndNotEmptyValidation;
import fr.cnes.sitools.extensions.common.RangeValidation;
import fr.cnes.sitools.extensions.common.StatusValidation;
import fr.cnes.sitools.extensions.common.Validation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
  private transient int verb = DEFAULT_VERBOSITY_MODE;
  /**
   * Data model.
   */
  private final transient Map dataModel = new HashMap();
  /**
   * User request.
   */
  private final transient Request request;
  /**
   * Application context.
   */
  private final transient Context context;
  /**
   * Application.
   */
  private final transient DataSetApplication datasetApp;
  /**
   * Right ascension of the cone center.
   */
  private transient double rightAscension;
  /**
   * Declination of the cone center.
   */
  private transient double declination;
  /**
   * Radius of the cone.
   */
  private transient double radius;

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
    final String raInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(ConeSearchProtocolLibrary.RA);
    final String decInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(ConeSearchProtocolLibrary.DEC);
    final String srInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(ConeSearchProtocolLibrary.SR);
    final String verbInput = this.request.getResourceRef().getQueryAsForm().getFirstValue(ConeSearchProtocolLibrary.VERB);
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
    final List<Info> infos = new ArrayList<Info>();
    final Map<String, String> validationMap = new HashMap<String, String>();
    validationMap.put(ConeSearchProtocolLibrary.RA, raInput);
    validationMap.put(ConeSearchProtocolLibrary.DEC, decInput);
    validationMap.put(ConeSearchProtocolLibrary.SR, srInput);
    validationMap.put(ConeSearchProtocolLibrary.VERB, verbInput);
    validationMap.put(ConeSearchProtocolLibrary.VERBOSITY, String.valueOf(verbosity));
    Validation validation = new InputsValidation(validationMap);
    validation = new RangeValidation(validation, ConeSearchProtocolLibrary.RA, ConeSearchProtocolLibrary.RA_MIN, ConeSearchProtocolLibrary.RA_MAX);
    validation = new RangeValidation(validation, ConeSearchProtocolLibrary.DEC, ConeSearchProtocolLibrary.DEC_MIN, ConeSearchProtocolLibrary.DEC_MAX);
    validation = new RangeValidation(validation, ConeSearchProtocolLibrary.SR, 0, maxSr);
    validation = new NotNullAndNotEmptyValidation(validation, ConeSearchProtocolLibrary.VERBOSITY);
    validation = new NotNullAndNotEmptyValidation(validation, ConeSearchProtocolLibrary.VERB, String.valueOf(verbVal));
    final StatusValidation status = validation.validate();
    if (status.isValid()) {
        final Map<String, String> input = validation.getMap();
        this.rightAscension = Double.valueOf(input.get(ConeSearchProtocolLibrary.RA));
        this.declination = Double.valueOf(input.get(ConeSearchProtocolLibrary.DEC));
        this.radius = Double.valueOf(input.get(ConeSearchProtocolLibrary.SR));
        this.verb = Integer.valueOf(input.get(ConeSearchProtocolLibrary.VERB));        
    } else {       
        final Map<String, String> errors = status.getMessages();
        final Set<Entry<String, String>> entries = errors.entrySet();        
        for (Entry<String, String> entry : entries) {
            final Info info = new Info();
            info.setID(entry.getKey());
            info.setName("Error in " + entry.getKey());
            info.setValueAttribute("Error in input " + entry.getKey() + ": " + entry.getValue());
            infos.add(info);
            LOG.log(Level.FINEST, "{0}: {1}", new Object[]{entry.getKey(), entry.getValue()});            
        }        
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
    return this.rightAscension;
  }

  /**
   * Get Dec.
   *
   * @return Dec
   */
  public final double getDec() {
    return this.declination;
  }

  /**
   * Get Sr.
   *
   * @return Sr
   */
  public final double getSr() {
    return this.radius;
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
