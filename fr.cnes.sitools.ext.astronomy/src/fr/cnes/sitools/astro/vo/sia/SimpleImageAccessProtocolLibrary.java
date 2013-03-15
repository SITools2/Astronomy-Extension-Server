/*******************************************************************************
 * Copyright 2012 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.sitools.astro.representation.VOTableRepresentation;
import fr.cnes.sitools.dataset.DataSetApplication;
import fr.cnes.sitools.plugins.resources.model.ResourceModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.DataType;
import org.restlet.Context;
import org.restlet.Request;

/**
 *
 * @author malapert
 */
public class SimpleImageAccessProtocolLibrary {
    
    /**
     *
     */
    public static final String DICTIONARY = "PARAM_Dictionary";
    /**
     *
     */
    public static final String INTERSECT = "INTERSECT";  
    /**
     *
     */
    public static final String POS = "POS";
    /**
     *
     */
    public static final String VERB = "VERB";    
    /**
     *
     */
    public static final String SIZE = "SIZE";      
    /**
     *
     */
    public static final String FORMAT = "FORMAT";   
    
    /**
     *
     */
    public static final String RA_COL = "COLUMN_RA";    
    /**
     *
     */
    public static final String DEC_COL = "COLUMN_DEC";          
    
    /**
     *
     */
    public static final List REQUIRED_UCD_CONCEPTS = Arrays.asList("VOX:Image_Title","POS_EQ_RA_MAIN","POS_EQ_DEC_MAIN","VOX:Image_Naxes","VOX:Image_Naxis","VOX:Image_Scale","VOX:Image_Format","VOX:Image_AccessReference");
    /**
     *
     */
    public static final String RESPONSIBLE_PARTY = "Responsible party";
    /**
     *
     */
    public static final String SERVICE_NAME = "Image service";
    /**
     *
     */
    public static final String DESCRIPTION = "Description";
    /**
     *
     */
    public static final String INSTRUMENT = "Instrument";
    /**
     *
     */
    public static final String WAVEBAND = "Waveband Coverage";
    /**
     *
     */
    public static final String COVERAGE = "Spatial Coverage";
    /**
     *
     */
    public static final String TEMPORAL = "Temporal Coverage";
    /**
     *
     */
    public static final String MAX_QUERY_SIZE = "Max query size";
    /**
     *
     */
    public static final String MAX_IMAGE_SIZE = "Max image size";
    /**
     *
     */
    public static final String MAX_FILE_SIZE = "Max file size";
    /**
     *
     */
    public static final String MAX_RECORDS = "Max records";

    
    private DataSetApplication datasetApp;
    private ResourceModel resourceModel;
    private Request request;   
    private Context context;    
    
    /**
     *
     */
    public enum Intersect {
        /**
         *
         */
        COVERS, /* The candidate image covers or includes the entire ROI */
        /**
         *
         */
        ENCLOSED, /* The candidate image is entirely enclosed by the ROI */
        /**
         *
         */
        CENTER, /* The candidate image overlaps the center of the ROI */
        /**
         *
         */
        OVERLAPS /* The candidate image overlaps some part of the ROI */
    };
    
    /**
     *
     */
    public enum ImageService {
        /**
         *
         */
        IMAGE_CUTOUT_SERVICE("Image Cutout Service"),
        /**
         *
         */
        IMAGE_MOSAICING_SERVICE("Image Mosaicing Service"),
        /**
         *
         */
        ATLAS_IMAGE_ARCHIVE("Atlas Image Archive"),
        /**
         *
         */
        POINTED_IMAGE_ARCHIVE("Pointed Image Archive");
        
        /**
         *
         */
        public final String serviceName;
        
        ImageService(String serviceName) {
            this.serviceName = serviceName;
        }
        
        /**
         *
         * @return
         */
        public String getServiceName() {
            return this.serviceName;
        }
    }
    
    /**
     *
     */
    public enum ImageGenerationParameters {
        /**
         *
         */
        NAXIS,
        /**
         *
         */
        CFRAME,
        /**
         *
         */
        EQUINOX,
        /**
         *
         */
        CRPIX,
        /**
         *
         */
        CRVAL,
        /**
         *
         */
        CDELT,
        /**
         *
         */
        ROTANG,
        /**
         *
         */
        PROJ};
    
    /**
     *
     */
    public enum Verb {
        /**
         *
         */
        VERBOSITY_0(0), /* The output table should contain only the minimum columns required */
        /**
         *
         */
        VERBOSITY_1(1), /* In addition to level 0, the output table should contain columns sufficient for uniquely describing the image */
        /**
         *
         */
        VERBOSITY_2(2), /*  In addition to level 1, the output table should contain, if possible, columns that contain values for all parameters supported as query constraints */
        /**
         *
         */
        VERBOSITY_3(3); /* The output table should return as much information about the images as possible. A table metadata query automatically implies the highest level of verbosity */
        
        /**
         *
         */
        public final int verbosityMode;
        
        Verb(int verbosityMode) {
            this.verbosityMode = verbosityMode;
        }
        
        /**
         *
         * @return
         */
        public int getVerbosityMode() {
            return this.verbosityMode;
        }
    }
    
    /**
     * List of format for internet browser.
     */
    public enum GraphicBrowser{
        /**
         * JPEG format
         */
        JPEG("image/jpeg"),
        /**
         * PNG format.
         */
        PNG("image/png"),
        /**
         * GIF format.
         */
        GIF("image/gif");
        
        /**
         * Format.
         */
        private final String format;
        
        /**
         * new Instance of Graphic browser
         * @param format Format name.
         */
        GraphicBrowser(String format) {
            this.format = format;
        }
        
        /**
         * Get Format Name.
         * @return Returns format name.
         */
        public String getFormat() {
            return format;
        }
        
        /**
         * Check if val is a graphic format.p
         * @param val Format.
         * @return Returns True when val is contained in the list of Graphic format otherwise False.
         */
        public static boolean contains(String val) {
            for(GraphicBrowser it:GraphicBrowser.values()) {
                if(it.getFormat().equals(val)) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
    /**
     * List of supported format.
     */
    public enum ParamStandardFormat{
        /**
         * All format
         */
        ALL("ALL",new ArrayList<String>()),
        /**
         * Graphic format
         */
        GRAPHIC("GRAPHIC",new ArrayList<String>()),
        /**
         * METADATA format
         */
        METADATA("METADATA",new ArrayList<String>()),
        /**
         * GRAPHIC Format.
         */
        GRAPHIC_ALL("GRAPHIC-ALL",new ArrayList<String>());
        
        /**
         * format
         */
        private final String format;
        /**
         * All formats
         */
        private List<String> formats;
        
        /**
         * New instance of Standard Format.
         * @param val format name.
         * @param formats values for this format name.
         */
        ParamStandardFormat(String val, List<String> formats) {
            this.format = val;
            this.formats = formats;
        }
        
        /**
         * Get the format name
         * @return Returns the format name.
         */
        public String getFormatName() {
            return format;
        }
        
        /**
         * Get the list of format.
         * @return Returns the list of format for a format name.
         */
        public List<String> getFormats() {
            return Collections.unmodifiableList(formats);
        }
        
        
        /**
         * Add a format to the format name.
         * @param val new supported format value.
         */
        private void addFormat(String val) {
            this.formats.add(val);
        }
        
        /**
         * Set the list of format for a format name.
         * @param val Set a list of formats for a format name.
         */
        private void setFormat(List<String> val) {
            this.formats.addAll(val);
        }
        
        
        /**
         * Add a format value for a format name.
         * @param format format Name.
         * @param specialValue Value for a format name.
         */
        private static void addFormat(String format,ParamStandardFormat specialValue) {
            for(ParamStandardFormat it:ParamStandardFormat.values()){
                if(it.equals(specialValue) && !ParamStandardFormat.hasFormat(format)) {                    
                    it.addFormat(format);
                }
            }
        }
        
        /**
         * Set a list of format for a format name.
         * @param format Format name.
         * @param specialValue List of format.
         */
        private static void setFormat(List<String> format,ParamStandardFormat specialValue) {
            for(ParamStandardFormat it:ParamStandardFormat.values()){
                if(it.equals(specialValue)) {
                    it.setFormat(format);
                }
            }
        }
        
        /**
         * Get the list of supported format for a format name.
         * @param format Format name.
         * @return Returns the list of supported formats.
         */
        public static List<String> getFormats(ParamStandardFormat format) {
            for(ParamStandardFormat it:ParamStandardFormat.values()) {
                if(it.equals(format)) {
                    return it.getFormats();
                }
            }
            throw new IllegalAccessError();
        }
        
        
        /**
         * Get the list of supported format for a format name.
         * @param format Format name.
         * @return Returns the list of supported formats.
         */
        public static List<String> getFormats(String format) {
            for(ParamStandardFormat it:ParamStandardFormat.values()) {
                if(it.getFormatName().equals(format)) {
                    return it.getFormats();
                }
            }
            throw new IllegalAccessError();
        }
        
        /**
         * Get Format names.
         * @return Returns format names.
         */
        public static List<String> getCtes() {
            List<String> formats = new ArrayList<String>();
            for(ParamStandardFormat it:ParamStandardFormat.values()) {
                formats.add(it.getFormatName());
            }
            return formats;
        }
        
        /**
         * Test if format is included in the list of formats.
         * @param format format to test
         * @return Returns true when the format is included in the list otherwise false.
         */
        public static boolean hasFormat(String format) {
            for(ParamStandardFormat it:ParamStandardFormat.values()) {
                if(it.getFormats().contains(format)) {
                    return true;
                }
            }
            return false;
        }
        
        
        
    };       
    
    /**
     *
     */
    public enum ParamStandardInput{
        /**
         *
         */
        POS("POS",Arrays.asList(ImageService.POINTED_IMAGE_ARCHIVE,ImageService.IMAGE_MOSAICING_SERVICE,ImageService.IMAGE_CUTOUT_SERVICE,ImageService.ATLAS_IMAGE_ARCHIVE),
                "The position of the region of interest, expressed as the right-ascension and declination of the field center, in decimal degrees using the ICRS coordinate system. A comma should delimit the two values; embedded whitespace is not permitted.",null,DataType.DOUBLE),
        /**
         *
         */
        SIZE("SIZE",Arrays.asList(ImageService.POINTED_IMAGE_ARCHIVE,ImageService.IMAGE_MOSAICING_SERVICE,ImageService.IMAGE_CUTOUT_SERVICE,ImageService.ATLAS_IMAGE_ARCHIVE),
                "The coordinate angular size of the region given in decimal degrees. The region may be specified using either one or two values. If only one value is given it applies to both coordinate axes. If two values are given the first value is the angular width in degrees of the right-ascension axis of the region, and the second value is the angular width in degrees of the declination axis.",null,DataType.DOUBLE),
        /**
         *
         */
        INTERSECT("INTERSECT",Arrays.asList(ImageService.POINTED_IMAGE_ARCHIVE,ImageService.IMAGE_MOSAICING_SERVICE,ImageService.IMAGE_CUTOUT_SERVICE,ImageService.ATLAS_IMAGE_ARCHIVE),
                "A parameter that indicates how matched images should intersect the region of interest",Intersect.OVERLAPS.name(),DataType.CHAR),
        /**
         *
         */
        FORMAT("FORMAT",Arrays.asList(ImageService.POINTED_IMAGE_ARCHIVE,ImageService.IMAGE_MOSAICING_SERVICE,ImageService.IMAGE_CUTOUT_SERVICE,ImageService.ATLAS_IMAGE_ARCHIVE),
                "indicate the desired format or formats of the images referenced by the output table","ALL",DataType.CHAR),
        /**
         *
         */
        VERB("VERB",Arrays.asList(ImageService.POINTED_IMAGE_ARCHIVE,ImageService.IMAGE_MOSAICING_SERVICE,ImageService.IMAGE_CUTOUT_SERVICE,ImageService.ATLAS_IMAGE_ARCHIVE),
                "This parameter indicates the desired level of information to be returned in the output table, particularly the number of columns to be returned to describe each image.",null,DataType.INT),
        /**
         *
         */
        NAXIS("NAXIS",Arrays.asList(ImageService.IMAGE_CUTOUT_SERVICE,ImageService.IMAGE_MOSAICING_SERVICE),
                "The size of the output image in pixels. This is a vector-valued quantity, expressed as \"NAXIS=<width>,<height>\". If only one value is given it applies to both image axes",null,DataType.INT),
        /**
         *
         */
        CFRAME("CFRAME",Arrays.asList(ImageService.IMAGE_CUTOUT_SERVICE,ImageService.IMAGE_MOSAICING_SERVICE),
                "The coordinate system reference frame, selected from ICRS, FK5, FK4, ECL, GAL, and SGAL (these abbreviations follow CDS Aladin). Default: ICRS. ","ICRS",DataType.CHAR),
        /**
         *
         */
        EQUINOX("EQUINOX",Arrays.asList(ImageService.IMAGE_CUTOUT_SERVICE,ImageService.IMAGE_MOSAICING_SERVICE),
                "Epoch of the mean equator and equinox for the specified coordinate system reference frame (CFRAME). Not required for ICRS. Default: B1950 for FK4, otherwise J2000.","J2000",DataType.CHAR),
        /**
         *
         */
        CRPIX("CRPIX",Arrays.asList(ImageService.IMAGE_CUTOUT_SERVICE,ImageService.IMAGE_MOSAICING_SERVICE),
                "The coordinates of the reference pixel, expressed in the pixel coordinates of the output image, with [1,1] being the center of the first pixel of the first row of the image. This is a vector-valued quantity; if only one value is given it applies to both image axes. Default: the image center.",null,DataType.DOUBLE),
        /**
         *
         */
        CRVAL("CRVAL",Arrays.asList(ImageService.IMAGE_CUTOUT_SERVICE,ImageService.IMAGE_MOSAICING_SERVICE),
                "The world coordinates relative to CFRAME at the reference pixel. This is a vector-valued quantity; both array values are required. Default: the region center coordinates (POS) at the center of the image, transformed to the output coordinate system reference frame if other than ICRS. If CRPIX is specified to be other than the image center the corresponding CRVAL can be computed, but should be specified explicitly by the client.",null,DataType.DOUBLE),
        /**
         *
         */
        CDELT("CDELT",Arrays.asList(ImageService.IMAGE_CUTOUT_SERVICE,ImageService.IMAGE_MOSAICING_SERVICE),
                "The scale of the output image in decimal degrees per pixel. A negative value implies an axis flip. Since the default image orientation is N up and E to the left, the default sign of CDELT is [-1,1].",null,DataType.DOUBLE),
        /**
         *
         */
        ROTANG("ROTANG",Arrays.asList(ImageService.IMAGE_CUTOUT_SERVICE,ImageService.IMAGE_MOSAICING_SERVICE),
                "The rotation angle of the image in degrees relative to CFRAME (an image which is unrotated in one reference frame may be rotated in another). This is the rotation of the WCS declination or latitude axis with respect to the second axis of the image, measured in the counterclockwise direction (as for FITS WCS, which is in turn based on the old AIPS convention). Default: 0 (no rotation).","0",DataType.DOUBLE),
        /**
         *
         */
        PROJ("PROJ",Arrays.asList(ImageService.IMAGE_CUTOUT_SERVICE,ImageService.IMAGE_MOSAICING_SERVICE),
                "The celestial projection of the output image expressed as a three-character code as for FITS WCS, e.g., \"TAN\", \"SIN\", \"ARC\", and so forth. Default: TAN.","TAN",DataType.CHAR);
        
        private String inputParameterName;
        private List<ImageService> supportedImageService;
        private String description;
        private String defaultValue;
        private DataType dataType;
        
        ParamStandardInput(String inputParameterName, List<ImageService> supportedImageService, String description, String defaultValue, DataType dataType) {
            this.inputParameterName = inputParameterName;
            this.supportedImageService = supportedImageService;
            this.description = description;
            this.defaultValue = defaultValue;
            this.dataType = dataType;
        }
        
        /**
         *
         * @return
         */
        public String getInputParameterName() {
            return this.inputParameterName;
        }
        
        /**
         *
         * @return
         */
        public List<ImageService> getSupportedImageService() {
            return Collections.unmodifiableList(this.supportedImageService);
        }
        
        /**
         *
         * @return
         */
        public String getDescription(){
            return this.description;
        }
        
        /**
         *
         * @return
         */
        public String getDefaultValue(){
            return this.defaultValue;
        }
        
        /**
         *
         * @return
         */
        public DataType getDataType(){
            return this.dataType;
        }
        
    };
    //public enum ImageFormat{}
    
    /**
     * Constructor
     * @param datasetApp Dataset Application
     * @param resourceModel Data model
     * @param request Request
     * @param context Context
     */
    public SimpleImageAccessProtocolLibrary(DataSetApplication datasetApp, ResourceModel resourceModel, Request request, Context context) {
        this.datasetApp = datasetApp;
        this.resourceModel = resourceModel;
        this.request = request;
        this.context = context;        
    }    
    
    
    /**
     * Fill data Model that will be used in the template
     * @return data model for the template
     */
    private Map fillDataModel() {    
        // init
        Map dataModel;
        
        // Handling input parameters
        DataModelInterface inputParameters = new SimpleImageAccessInputParameters(datasetApp, request, this.context, this.resourceModel);
        
        // data model response
        if(inputParameters.getDataModel().containsKey("infos")) {            
            dataModel = inputParameters.getDataModel();
        } else {           
            SimpleImageAccessDataModelInterface response = new SimpleImageAccessResponse((SimpleImageAccessInputParameters)inputParameters, resourceModel);
            dataModel = response.getDataModel();            
        }
        return dataModel;        
    }
    
    /**
     * VOTable response
     * @return VOTable response
     */
    public VOTableRepresentation getResponse() {
        Map dataModel = fillDataModel();
        return new VOTableRepresentation(dataModel, "votable.ftl");
    }   
    private static final Logger LOG = Logger.getLogger(SimpleImageAccessProtocolLibrary.class.getName());
    
}
