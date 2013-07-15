/**
 * *****************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * ****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.resource;

import fr.cnes.sitools.astro.cutoff.CutOffException;
import fr.cnes.sitools.astro.cutoff.CutOffInterface;
import fr.cnes.sitools.astro.cutoff.CutOffSITools2;
import fr.cnes.sitools.astro.cutoff.CutOffSITools2Bis;
import fr.cnes.sitools.astro.representation.CutOffRepresentation;
import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.common.application.ContextAttributes;
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
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.service.storage.model.StorageDirectory;
import fr.cnes.sitools.util.RIAPUtils;
import fr.cnes.sitools.util.Util;
import java.io.File;
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
    private transient double rightAscension;
    private transient double declination;
    private transient double radius;
//    private transient int hduNumber;
    private transient String dataSorageName;

    /**
     * Initialize.
     */
    @Override
    public final void doInit() {
        super.doInit();
        // Retrieve param from model
        String raString = getRequest().getResourceRef().getQueryAsForm().getFirstValue(CutOffResourcePlugin.RA_INPUT_PARAMETER);
        if (raString == null || "".equals(raString)) {
            raString = getParameterValue(CutOffResourcePlugin.RA_INPUT_PARAMETER);
        }
        this.rightAscension = Double.valueOf(raString);

        String decString = getRequest().getResourceRef().getQueryAsForm().getFirstValue(CutOffResourcePlugin.DEC_INPUT_PARAMETER);
        if (decString == null || "".equals(raString)) {
            decString = getParameterValue(CutOffResourcePlugin.DEC_INPUT_PARAMETER);
        }
        this.declination = Double.valueOf(decString);

        String radiusString = getRequest().getResourceRef().getQueryAsForm().getFirstValue(CutOffResourcePlugin.RADIUS_INPUT_PARAMETER);
        if (radiusString == null || "".equals(radiusString)) {
            radiusString = getParameterValue(CutOffResourcePlugin.RADIUS_INPUT_PARAMETER);
        }
        this.radius = Double.valueOf(radiusString);

//        String hduNumberString = getRequest().getResourceRef().getQueryAsForm().getFirstValue(CutOffResourcePlugin.HDU_NUMBER_INPUT_PARAMETER);
//        if (hduNumberString == null || "".equals(hduNumberString)) {
//            hduNumberString = getParameterValue(CutOffResourcePlugin.HDU_NUMBER_INPUT_PARAMETER);
//        }
//        this.hduNumber = Integer.parseInt(hduNumberString);
        this.dataSorageName = getParameterValue(CutOffResourcePlugin.DATA_STORAGE_NAME_PARAMETER);
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
        final DataSetApplication datasetApp = (DataSetApplication) getApplication();

        //Get the pipeline to convert the information
        final ConverterChained converterChained = datasetApp.getConverterChained();

        final DataSetExplorerUtil dsExplorerUtil = new DataSetExplorerUtil((DataSetApplication) getApplication(), getRequest(), getContext());

        // Get DatabaseRequestParameters
        final DatabaseRequestParameters params = dsExplorerUtil.getDatabaseParams();

        final DatabaseRequest databaseRequest = DatabaseRequestFactory.getDatabaseRequest(params);
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
            final AttributeValue attributeValue = this.getInParam(this.getModel().getParameterByName(CutOffResourcePlugin.FITS_FILE_INPUT_PARAMETER), record);
            final Fits fits;
            if (this.dataSorageName == null || "".equals(this.dataSorageName)) {
                final Representation file = CutOffResource.getFile(String.valueOf(attributeValue.getValue()), Request.getCurrent().getClientInfo(), getContext());
                fits = new Fits(file.getStream());                
            } else {                
                final SitoolsSettings sitoolsSettings = (SitoolsSettings) getContext().getAttributes().get(ContextAttributes.SETTINGS);
                final String dataStorageUrl = sitoolsSettings.getString(Consts.APP_DATASTORAGE_ADMIN_URL) + "/directories";
                final String dataStorageRelativePart = sitoolsSettings.getString(Consts.APP_DATASTORAGE_URL);
                final String sitoolsUrl = sitoolsSettings.getString(Consts.APP_URL);                
                final StorageDirectory storageDirectory = RIAPUtils.getObjectFromName(dataStorageUrl, this.dataSorageName, getContext());
                final String dataStorageAttachUrl = sitoolsUrl + dataStorageRelativePart + storageDirectory.getAttachUrl();
                LOG.log(Level.FINER, "dataStorageAttachUrl: {0}", dataStorageAttachUrl);
                final String dataStorageLocalPath = storageDirectory.getLocalPath();                
                final String filename = storageDirectory.getLocalPath() + File.separator + attributeValue.getValue();
                fits = new Fits(filename);
            }
            cutOff = new CutOffSITools2Bis(fits, rightAscension, declination, radius);

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

        final boolean fitsAvailable = cutOff.isFitsAvailable();
        Representation rep = null;
        if (fitsAvailable) {
            rep = new CutOffRepresentation(MediaType.ALL, cutOff);
        //} else if (cutOff.getIsDataCube()) {
        //    rep = new CutOffRepresentation(MediaType.IMAGE_GIF, cutOff);
        } else {
            rep = new CutOffRepresentation(MediaType.IMAGE_JPEG, cutOff);
        }
        if (fileName != null && !"".equals(fileName)) {
            final Disposition disp = new Disposition(Disposition.TYPE_ATTACHMENT);
            disp.setFilename(fileName);
            rep.setDisposition(disp);
        }
        return rep;
    }

    /**
     * Returns the attribute of the record that matches the param.
     *
     * @param param Fits file attribute that should map a record's attribute
     * @param rec record
     * @return the attribute of the record that matches the param
     */
    private AttributeValue getInParam(final ResourceParameter param, final Record rec) {
        final List<AttributeValue> listRecord = rec.getAttributeValues();
        boolean found = false;
        AttributeValue attr = null;
        for (final Iterator<AttributeValue> it = listRecord.iterator(); it.hasNext() && !found;) {
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
     * @param clientInfo the ClientInfo to check if the user has the rights to
     * get the file
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
        final org.restlet.Response response = context.getClientDispatcher().handle(reqGET);

        if (response == null) {
            throw new SitoolsException("ERROR GETTING FILE : " + fileUrl);
        } else if (Status.CLIENT_ERROR_FORBIDDEN.equals(response.getStatus())) {
            throw new SitoolsException("CLIENT_ERROR_FORBIDDEN : " + fileUrl);
        } else if (Status.CLIENT_ERROR_UNAUTHORIZED.equals(response.getStatus())) {
            throw new SitoolsException("CLIENT_ERROR_UNAUTHORIZED : " + fileUrl);
        } else if (Status.isError(response.getStatus().getCode())) {
            throw new SitoolsException("ERROR : " + response.getStatus() + " getting file : " + fileUrl);
        }

        return response.getEntity();

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
