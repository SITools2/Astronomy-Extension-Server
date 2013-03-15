/**
 * *****************************************************************************
 * Copyright 2011 2012 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.astro.cutoff.CutOffException;
import fr.cnes.sitools.astro.cutoff.CutOffInterface;
import fr.cnes.sitools.astro.cutoff.CutOffSITools2;
import fr.cnes.sitools.astro.representation.CutOffRepresentation;
import fr.cnes.sitools.common.exception.SitoolsException;
import fr.cnes.sitools.common.resource.SitoolsParameterizedResource;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.dataset.converter.business.ConverterChained;
import fr.cnes.sitools.dataset.database.DatabaseRequest;
import fr.cnes.sitools.dataset.database.DatabaseRequestFactory;
import fr.cnes.sitools.dataset.database.DatabaseRequestParameters;
import fr.cnes.sitools.dataset.database.common.DataSetExplorerUtil;
import fr.cnes.sitools.datasource.jdbc.model.AttributeValue;
import fr.cnes.sitools.datasource.jdbc.model.Record;
import fr.cnes.sitools.plugins.resources.model.ResourceParameter;
import fr.cnes.sitools.proxy.ProxySettings;
import fr.cnes.sitools.util.RIAPUtils;
import fr.cnes.sitools.util.Util;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.data.ClientInfo;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.wadl.MethodInfo;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

/**
 * Cuts a FITS file according to a search area.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class CutOffResource extends SitoolsParameterizedResource {
  
  /**
   * Logger.
   */
  private static final Logger LOG = Logger.getLogger(CutOffResource.class.getName());

  /**
   * Initialize.
   */
  @Override
  public final void doInit() {
    super.doInit();
  }

  /**
   * Returns the representation for GET method.
   *
   * @return the representation
   */
  @Override
  public final Representation get() {
    CutOffInterface cutOff = null;

    // Get the datasetApplication
    DataSetApplication datasetApp = (DataSetApplication) getApplication();

    //Get the pipeline to convert the information
    ConverterChained converterChained = datasetApp.getConverterChained();

    DataSetExplorerUtil dsExplorerUtil = new DataSetExplorerUtil((DataSetApplication) getApplication(), getRequest(), getContext());

    // Get DatabaseRequestParameters
    DatabaseRequestParameters params = dsExplorerUtil.getDatabaseParams();

    // Retrieve param from model
    String raString = getRequest().getResourceRef().getQueryAsForm().getFirstValue(CutOffResourcePlugin.RA_INPUT_PARAMETER);
    if (raString == null || "".equals(raString)) {
      ResourceParameter raParam = this.getModel().getParameterByName(CutOffResourcePlugin.RA_INPUT_PARAMETER);
      raString = raParam.getValue();
    }
    double ra = Double.valueOf(raString);


    String decString = getRequest().getResourceRef().getQueryAsForm().getFirstValue(CutOffResourcePlugin.DEC_INPUT_PARAMETER);
    if (decString == null || "".equals(raString)) {
      ResourceParameter decParam = this.getModel().getParameterByName(CutOffResourcePlugin.DEC_INPUT_PARAMETER);
      decString = decParam.getValue();
    }
    double dec = Double.valueOf(decString);

    String radiusString = getRequest().getResourceRef().getQueryAsForm().getFirstValue(CutOffResourcePlugin.RADIUS_INPUT_PARAMETER);
    if (radiusString == null || "".equals(radiusString)) {
      ResourceParameter radiusParam = this.getModel().getParameterByName(CutOffResourcePlugin.RADIUS_INPUT_PARAMETER);
      radiusString = radiusParam.getValue();
    }
    double radius = Double.valueOf(radiusString);

    ResourceParameter hduNumberParam = this.getModel().getParameterByName(CutOffResourcePlugin.HDU_NUMBER_INPUT_PARAMETER);
    int hduNumber = Integer.parseInt(hduNumberParam.getValue());

    DatabaseRequest databaseRequest = DatabaseRequestFactory.getDatabaseRequest(params);
    try {
      if (params.getDistinct()) {
        databaseRequest.createDistinctRequest();
      } else {
        databaseRequest.createRequest();
      }

      databaseRequest.nextResult();

      Record record = databaseRequest.getRecord();
      if (Util.isSet(converterChained)) {
        record = converterChained.getConversionOf(record);
      }

      AttributeValue attributeValue = this.getInParam(this.getModel().getParameterByName(CutOffResourcePlugin.FITS_FILE_INPUT_PARAMETER), record);
      Representation file = CutOffResource.getFile(String.valueOf(attributeValue.getValue()), Request.getCurrent().getClientInfo(), getContext());
      Fits fits = new Fits(file.getStream());
      cutOff = new CutOffSITools2(fits, ra, dec, radius, hduNumber);

    } catch (FitsException ex) {
      LOG.log(Level.SEVERE, null, ex);
      try {
        databaseRequest.close();
      } catch (SitoolsException ex1) {
        LOG.log(Level.SEVERE, null, ex1);
      } finally {
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      try {
        databaseRequest.close();
      } catch (SitoolsException ex1) {
        LOG.log(Level.SEVERE, null, ex1);
      } finally {
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    } catch (CutOffException ex) {
      Logger.getLogger(CutOffResource.class.getName()).log(Level.SEVERE, null, ex);
      try {
        databaseRequest.close();
      } catch (SitoolsException ex1) {
        LOG.log(Level.SEVERE, null, ex1);
      } finally {
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    } catch (SitoolsException ex) {
      Logger.getLogger(CutOffResource.class.getName()).log(Level.SEVERE, null, ex);
      try {
        databaseRequest.close();
      } catch (SitoolsException ex1) {
        LOG.log(Level.SEVERE, null, ex1);
      } finally {
        throw new ResourceException(Status.SERVER_ERROR_INTERNAL, ex);
      }
    }

    boolean fitsAvaialble = cutOff.isFitsAvailable();
    Representation rep = null;
    if (fitsAvaialble) {
      rep = new CutOffRepresentation(MediaType.ALL, cutOff);
    } else if (cutOff.getIsDataCube()) {
      rep =  new CutOffRepresentation(MediaType.IMAGE_GIF, cutOff);
    } else {
      rep =  new CutOffRepresentation(MediaType.IMAGE_PNG, cutOff);
    }
    if (fileName != null && !"".equals(fileName)) {
      Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
      disp.setFilename(fileName);
      rep.setDisposition(disp);
    }
    return rep;
  }

  /**
   * Returns the attribute of the record that matches the param.
   * @param param Fits file attribute that should map a record's attribute
   * @param rec record
   * @return the attribute of the record that matches the param
   */
  private AttributeValue getInParam(final ResourceParameter param, final Record rec) {
    List<AttributeValue> listRecord = rec.getAttributeValues();
    boolean found = false;
    AttributeValue attr = null;
    for (Iterator<AttributeValue> it = listRecord.iterator(); it.hasNext() && !found;) {
      attr = it.next();
      if (attr.getName().equals(param.getValue())) {
        found = true;
      }
    }
    if (found) {
      return attr;
    } else {
      return null;
    }
  }

  /**
   * Retrives the representation of a File.
   *
   * @param fileUrl the url of the file
   * @param clientInfo the ClientInfo to check if the user has the rights to get the file
   * @param context The context
   * @return the Representation of a File
   * @throws SitoolsException if there is an error while getting the file
   */
  public static Representation getFile(final String fileUrl, final ClientInfo clientInfo, final Context context) throws SitoolsException {
    Request reqGET;
    if (fileUrl.contains("http://")) {
      reqGET = new Request(Method.GET, fileUrl);
      if ((ProxySettings.getProxyAuthentication() != null) && reqGET.getProxyChallengeResponse() == null) {
        reqGET.setProxyChallengeResponse(ProxySettings.getProxyAuthentication());
      }
    } else {
      reqGET = new Request(Method.GET, RIAPUtils.getRiapBase() + fileUrl);
      reqGET.setClientInfo(clientInfo);
    }
    org.restlet.Response r = context.getClientDispatcher().handle(reqGET);

    if (r == null) {
      throw new SitoolsException("ERROR GETTING FILE : " + fileUrl);
    } else if (Status.CLIENT_ERROR_FORBIDDEN.equals(r.getStatus())) {
      throw new SitoolsException("CLIENT_ERROR_FORBIDDEN : " + fileUrl);
    } else if (Status.CLIENT_ERROR_UNAUTHORIZED.equals(r.getStatus())) {
      throw new SitoolsException("CLIENT_ERROR_UNAUTHORIZED : " + fileUrl);
    } else if (Status.isError(r.getStatus().getCode())) {
      throw new SitoolsException("ERROR : " + r.getStatus() + " getting file : " + fileUrl);
    }

    return r.getEntity();

  }

  /**
   * General WADL description.
   */
  @Override
  public final void sitoolsDescribe() {
    setName("CutOff service");
    setDescription("Provide a cutOff service");
    setNegotiated(false);
  }

  /**
   * Describes the GET method in WADL.
   *
   * @param info information
   */
  @Override
  protected final void describeGet(final MethodInfo info) {
    this.addInfo(info);
  }
}
