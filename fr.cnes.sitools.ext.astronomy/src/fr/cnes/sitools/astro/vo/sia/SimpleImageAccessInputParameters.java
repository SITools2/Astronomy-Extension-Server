/*******************************************************************************
 * Copyright 2012, 2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.astro.vo.sia;

import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import fr.cnes.sitools.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.AnyTEXT;
import net.ivoa.xml.votable.v1.DataType;
import net.ivoa.xml.votable.v1.Info;
import net.ivoa.xml.votable.v1.Option;
import net.ivoa.xml.votable.v1.Param;
import net.ivoa.xml.votable.v1.Values;
import org.restlet.Context;
import org.restlet.Request;

/**
 * Input parameters for SIA.
 * @author Jean-Crhistophe Malapert
 */
public class SimpleImageAccessInputParameters implements DataModelInterface {

    private double ra = 0.0;
    private double dec = 0.0;
    private double[] size;
    private int verb = 0;
    private Map dataModel = new HashMap();
    private Request request;
    private Context context;
    private DataSetApplication datasetApp;
    private ResourceModel resourceModel;

    /**
     *
     * @param datasetApp
     * @param request
     * @param context
     * @param resourceModel
     */
    public SimpleImageAccessInputParameters(final DataSetApplication datasetApp, final Request request, final Context context, final ResourceModel resourceModel) {
        this.datasetApp = datasetApp;
        this.context = context;
        this.request = request;
        this.resourceModel = resourceModel;
        String pos_input = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.POS);
        String size_input = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.SIZE);
        String format = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.FORMAT);
        String intersect = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.INTERSECT);
        String verbosity = this.request.getResourceRef().getQueryAsForm().getFirstValue(SimpleImageAccessProtocolLibrary.VERB);
        //TODO check the differentParameters
        if (Util.isSet(format) && format.equals(SimpleImageAccessProtocolLibrary.ParamStandardFormat.METADATA.name())) {
            fillMetadataFormat();
        } else {
            checkInputParameters(pos_input, size_input);
        }
    }

    private void fillMetadataFormat() {
        this.dataModel.put("description", this.resourceModel.getParameterByName(SimpleImageAccessProtocolLibrary.DESCRIPTION).getValue());

        Info info = new Info();
        info.setName("QUERY_STATUS");
        info.setValueAttribute("OK");
        List<Info> listInfos = new ArrayList<Info>();
        listInfos.add(info);
        this.dataModel.put("infos", listInfos);

        List<Param> listParam = new ArrayList<Param>();
        Param param = new Param();
        param.setName("INPUT:POS");
        param.setValue("0,0");
        param.setDatatype(DataType.DOUBLE);
        AnyTEXT anyText = new AnyTEXT();
        anyText.getContent().add("Search Position in the form ra,dec where ra and dec are given in decimal degrees in the ICRS coordinate system.");
        param.setDESCRIPTION(anyText);
        listParam.add(param);

        param = new Param();
        param.setName("INPUT:SIZE");
        param.setValue("0.05");
        param.setDatatype(DataType.DOUBLE);
        anyText = new AnyTEXT();
        anyText.getContent().add("Size of search region in the RA and Dec directions.");
        param.setDESCRIPTION(anyText);
        listParam.add(param);

        param = new Param();
        param.setName("INPUT:FORMAT");
        param.setValue(SimpleImageAccessProtocolLibrary.ParamStandardFormat.ALL.name());
        param.setDatatype(DataType.CHAR);
        param.setArraysize("*");
        anyText = new AnyTEXT();
        anyText.getContent().add("Requested format of images.");
        param.setDESCRIPTION(anyText);
        List<String> formatList = SimpleImageAccessProtocolLibrary.ParamStandardFormat.getCtes();
        Values values = new Values();
        for (String formatIter : formatList) {
            Option option = new Option();
            option.setValue(formatIter);
            values.getOPTION().add(option);
        }
        param.setVALUES(values);
        //TODO : le faire pour chaque format
        listParam.add(param);

        param = new Param();
        param.setName("INPUT:INTERSECT");
        param.setValue("OVERLAPS");
        param.setDatatype(DataType.CHAR);
        anyText = new AnyTEXT();
        anyText.getContent().add("Choice of overlap with requested region.");
        param.setDESCRIPTION(anyText);
        listParam.add(param);

        param = new Param();
        param.setName("INPUT:VERB");
        param.setValue("1");
        param.setDatatype(DataType.INT);
        anyText = new AnyTEXT();
        anyText.getContent().add("Verbosity level, controlling the number of columns returned.");
        param.setDESCRIPTION(anyText);
        listParam.add(param);

        dataModel.put("params", listParam);
    }

    private void checkInputParameters(final String pos_Input, final String size_Input) {
        List<Info> infos = new ArrayList<Info>();

        if (!Util.isSet(pos_Input)) {
            Info info = new Info();
            info.setID("POS");
            info.setName("Error in POS");
            info.setValueAttribute("POS must be set");
            infos.add(info);
            this.datasetApp.getLogger().log(Level.WARNING, "POS must be set");
        } else {
            String[] coord = pos_Input.split(",");

            try {
                if (coord.length != 2) {
                    throw new Exception("Wrong argument number for POS");
                }
            } catch (Exception ex) {
                Info info = new Info();
                info.setID("POS");
                info.setName("Error in POS");
                info.setValueAttribute("Error in input POS: " + ex.getMessage());
                infos.add(info);
                this.datasetApp.getLogger().log(Level.WARNING, "Error POS: {0}", ex.getMessage());
            }

            try {
                this.ra = Double.valueOf(coord[0]);
                if (this.ra > 360 || this.ra < 0) {
                    throw new Exception(this.ra + " for RA parameter is not allowed. RA must be in [0,360]");
                }
                this.datasetApp.getLogger().log(Level.FINEST, "RA: {0}", coord[0]);
            } catch (Exception ex) {
                Info info = new Info();
                info.setID("RA");
                info.setName("Error in RA");
                info.setValueAttribute("Error in input RA: " + ex.getMessage());
                infos.add(info);
                this.datasetApp.getLogger().log(Level.WARNING, "Error RA: {0}", ex.getMessage());
            }

            try {
                this.dec = Double.valueOf(coord[1]);
                if (this.dec > 90 || this.dec < -90) {
                    throw new Exception(this.dec + " for DEC parameter is not allowed. DEC must be in [-90,90]");
                }
                this.datasetApp.getLogger().log(Level.FINEST, "DEC: {0}", coord[1]);
            } catch (Exception ex) {
                Info info = new Info();
                info.setID("DEC");
                info.setName("Error in DEC");
                info.setValueAttribute("Error in input DEC: " + ex.getMessage());
                infos.add(info);
                this.datasetApp.getLogger().log(Level.WARNING, "Error DEC: {0}", ex.getMessage());
            }
        }

        if (!Util.isSet(size_Input)) {
            Info info = new Info();
            info.setID("SIZE");
            info.setName("Error in SIZE");
            info.setValueAttribute("SIZE must be set");
            infos.add(info);
            this.datasetApp.getLogger().log(Level.WARNING, "SIZE must be set");
        } else {

            try {
                String[] size_bbox = size_Input.split(",");
                if (size_bbox.length != 1 && size_bbox.length != 2) {
                    throw new Exception("Wrong argument number for SIZE ");
                } else if (size_bbox.length == 1) {
                    this.size = new double[1];
                    this.size[0] = Double.valueOf(size_bbox[0]);
                } else {
                    this.size = new double[2];
                    this.size[0] = Double.valueOf(size_bbox[0]);
                    this.size[1] = Double.valueOf(size_bbox[1]);
                }
            } catch (Exception ex) {
                Info info = new Info();
                info.setID("SIZE");
                info.setName("Error in SIZE");
                info.setValueAttribute("Error in input SIZE: " + ex.getMessage());
                infos.add(info);
                this.datasetApp.getLogger().log(Level.WARNING, "Error SIZE: {0}", ex.getMessage());
            }
        }

        if (!infos.isEmpty()) {
            this.dataModel.put("infos", infos);
        }

    }

    @Override
    public Map getDataModel() {
        return Collections.unmodifiableMap(this.dataModel);
    }

    /**
     * Get Ra
     * @return Ra 
     */
    public double getRa() {
        return this.ra;
    }

    /**
     * Get Dec
     * @return Dec 
     */
    public double getDec() {
        return this.dec;
    }

    /**
     * Get Sr
     * @return Sr 
     */
    public double[] getSize() {
        return this.size;
    }

    /**
     * Get the request
     * @return Request
     */
    public Request getRequest() {
        return this.request;
    }

    /**
     * Get the context
     * @return Context
     */
    public Context getContext() {
        return this.context;
    }

    /**
     * Get DatasetApplication
     * @return Dataset application
     */
    public DataSetApplication getDatasetApplication() {
        return this.datasetApp;
    }

    /**
     *
     * @return
     */
    public int getVerb() {
        return this.verb;
    }
    private static final Logger LOG = Logger.getLogger(SimpleImageAccessInputParameters.class.getName());
}

