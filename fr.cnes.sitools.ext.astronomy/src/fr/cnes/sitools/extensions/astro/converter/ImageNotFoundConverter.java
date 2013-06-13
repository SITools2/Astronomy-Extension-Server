/******************************************************************************
 * Copyright 2011-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES.
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
 ******************************************************************************/
package fr.cnes.sitools.extensions.astro.converter;

import fr.cnes.sitools.common.SitoolsSettings;
import fr.cnes.sitools.common.application.ContextAttributes;
import fr.cnes.sitools.common.validator.ConstraintViolation;
import fr.cnes.sitools.common.validator.ConstraintViolationLevel;
import fr.cnes.sitools.common.validator.Validator;
import fr.cnes.sitools.dataset.converter.business.AbstractConverter;
import fr.cnes.sitools.dataset.converter.model.ConverterParameter;
import fr.cnes.sitools.dataset.converter.model.ConverterParameterType;
import fr.cnes.sitools.datasource.jdbc.model.AttributeValue;
import fr.cnes.sitools.datasource.jdbc.model.Record;
import fr.cnes.sitools.server.Consts;
import fr.cnes.sitools.service.storage.model.StorageDirectory;
import fr.cnes.sitools.util.RIAPUtils;
import fr.cnes.sitools.util.Util;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Add a default image when a image on the data storage is not found.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class ImageNotFoundConverter extends AbstractConverter {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(ImageNotFoundConverter.class.getName());

    /**
     * Constructor.
     */
    public ImageNotFoundConverter() {
        setName("ImageNotFoundConverter");
        setDescription("Set a default image when the image is not found in a datastorage");
        setClassAuthor("Jean-Christophe Malapert");
        setClassOwner("CNES");
        setClassVersion("0.2");
        final ConverterParameter url = new ConverterParameter("Url", "Relative Image URL",
                ConverterParameterType.CONVERTER_PARAMETER_IN);
        final ConverterParameter dataStorageName = new ConverterParameter("DataStorageName", "Data storage name",
                ConverterParameterType.CONVERTER_PARAMETER_INTERN);
        final ConverterParameter imageNotFoundUrl = new ConverterParameter("ImageNotFoundUrl", "Image not found URL",
                ConverterParameterType.CONVERTER_PARAMETER_INTERN);
        this.addParam(url);
        this.addParam(dataStorageName);
        this.addParam(imageNotFoundUrl);
    }

    @Override
    public final Record getConversionOf(final Record record) throws Exception {
        final Record out = record;
        final SitoolsSettings sitoolsSettings = (SitoolsSettings) getContext().getAttributes().get(ContextAttributes.SETTINGS);
        final String dataStorageUrl = sitoolsSettings.getString(Consts.APP_DATASTORAGE_ADMIN_URL) + "/directories";
        final String dataStorageRelativePart = sitoolsSettings.getString(Consts.APP_DATASTORAGE_URL);
        final String sitoolsUrl = sitoolsSettings.getString(Consts.APP_URL);
        final String dataStorageName = getInternParam("DataStorageName").getValue();
        final StorageDirectory storageDirectory = RIAPUtils.getObjectFromName(dataStorageUrl, dataStorageName, getContext());
        final String dataStorageAttachUrl = sitoolsUrl + dataStorageRelativePart + storageDirectory.getAttachUrl();
        LOG.log(Level.FINER, "dataStorageAttachUrl: {0}", dataStorageAttachUrl);
        final String dataStorageLocalPath = storageDirectory.getLocalPath();
        final AttributeValue urlAttr = getInParam("Url", record);
        final Object urlObj = urlAttr.getValue();
        if (Util.isSet(urlObj)) {
            final String url = String.valueOf(urlObj);
            final String filename = url.replace(dataStorageAttachUrl, "");
            LOG.log(Level.FINER, "filename: {0}", filename);
            String absoluteFileName = dataStorageLocalPath.concat(filename);
            LOG.log(Level.FINER, "absoluteFileName: {0}", absoluteFileName);
            absoluteFileName = absoluteFileName.replace("file://", "");
            final File file = new File(absoluteFileName);
            if (!(file.exists() && file.isFile())) {
                urlAttr.setValue(getInternParam("ImageNotFoundUrl").getValue());
            }
        }
        return out;
    }

    @Override
    public final Validator<?> getValidator() {
        return new Validator<AbstractConverter>() {

            @Override
            public final Set<ConstraintViolation> validate(final AbstractConverter item) {
                final Set<ConstraintViolation> constraints = new HashSet<ConstraintViolation>();
                final Map<String, ConverterParameter> params = item.getParametersMap();
                ConverterParameter param = params.get("Url");
                if (param.getAttachedColumn().isEmpty()) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("URL must be set");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setValueName(param.getName());
                    constraint.setInvalidValue(param.getValue());
                    constraints.add(constraint);                    
                }
                param = params.get("DataStorageName");
                if (!Util.isNotEmpty(param.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("A datastorage name must be set.");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setValueName(param.getName());
                    constraint.setInvalidValue(param.getValue());
                    constraints.add(constraint);                    
                }
                param = params.get("ImageNotFoundUrl");
                if (!Util.isNotEmpty(param.getValue())) {
                    final ConstraintViolation constraint = new ConstraintViolation();
                    constraint.setMessage("An URL (absolute or relative) must be set.");
                    constraint.setLevel(ConstraintViolationLevel.CRITICAL);
                    constraint.setValueName(param.getName());
                    constraint.setInvalidValue(param.getValue());
                    constraints.add(constraint);                    
                }                 
                return constraints;
            }

        };
    }
}
