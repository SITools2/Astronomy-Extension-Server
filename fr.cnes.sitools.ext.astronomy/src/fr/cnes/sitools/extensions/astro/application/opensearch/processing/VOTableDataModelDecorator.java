 /*******************************************************************************
 * Copyright 2010-2014 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.sitools.extensions.astro.application.opensearch.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.ivoa.xml.votable.v1.Field;

/**
 * Transforms the server response in a data model that allows the transformation in VOTable format.
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class VOTableDataModelDecorator extends VORequestDecorator {

    /**
     * Constructor.
     * @param decorateVORequestVal the server response
     */
    public VOTableDataModelDecorator(final VORequestInterface decorateVORequestVal) {
        super(decorateVORequestVal);
    }

    @Override
    public final Object getOutput() {
        final Object output = super.getOutput();
        final List<Map<Field, String>> response = (List<Map<Field, String>>) output;
        return computeVotableDataModel(response);
    }
    /**
     * Computes the server response in a data model that allows the transformation in VOTable format.
     * @param response server response
     * @return the server response in a data model that allows the transformation in VOTable format
     */
    public final Map computeVotableDataModel(final List<Map<Field, String>> response) {
        return computeVotableFromDataModel(response);
    }
    /**
     * Computes the server response in a data model that allows the transformation in VOTable format.
     * @param response server response
     * @return the server response in a data model that allows the transformation in VOTable format
     */
    public static Map computeVotableFromDataModel(final List<Map<Field, String>> response) {
        final Map dataModel = new HashMap();
        final List<Field> fields = new ArrayList<Field>();
        final List rows = new ArrayList();
        final List<String> sqlColAlias = new ArrayList<String>();
        for (Map<Field, String> record : response) {
            final Set<Entry<Field, String>> entries = record.entrySet();
            final Map row = new HashMap();
            for (Entry<Field, String> entry : entries) {
                final Field fieldRecorded = entry.getKey();
                if (!fields.contains(fieldRecorded)) {
                    fields.add(fieldRecorded);
                }
                row.put(fieldRecorded.getName(), entry.getValue());
                if (!sqlColAlias.contains(fieldRecorded.getName())) {
                    sqlColAlias.add(fieldRecorded.getName());
                }
            }
            rows.add(row);
        }
        dataModel.put("fields", fields);
        dataModel.put("rows", rows);
        dataModel.put("sqlColAlias", sqlColAlias);
        return dataModel;
    }
}
