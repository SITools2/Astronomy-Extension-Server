/**
 * *****************************************************************************
 * Copyright 2010-2013 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *****************************************************************************
 */
package fr.cnes.sitools.extensions.astro.application.opensearch.processing;

import fr.cnes.sitools.extensions.common.Utility;
import fr.cnes.sitools.extensions.common.VoDictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.ivoa.xml.votable.v1.Field;

/**
 * Computes the dictionary from the server response.
 *
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class DictionaryDecorator extends VORequestDecorator {

    /**
     * Logger.
     */
    private static final Logger LOG = Logger.getLogger(DictionaryDecorator.class.getName());
    /**
     * Dictionary.
     */
    private Map<String, VoDictionary> dico;

    /**
     * Constructor.
     *
     * @param decorateVORequestVal Response of the VO server
     * @param dicoVal dictionary to fill
     */
    public DictionaryDecorator(final VORequestInterface decorateVORequestVal, final Map<String, VoDictionary> dicoVal) {
        super(decorateVORequestVal);
        setDico(dicoVal);
    }

    @Override
    public final Object getOutput() {
        final Object output = super.getOutput();
        final List<Map<Field, String>> model = (List<Map<Field, String>>) output;
        if (!Utility.isSet(model)) {
            LOG.log(Level.SEVERE, "the response from the server is null. This is not possible. Please, check if the server response is well parsed.");
        } else if (model.isEmpty()) {
        } else {
            fillDictionary(model.get(0).keySet());
        }
        return output;
    }

    /**
     * Parses the description TAG of each field and sets it to
     * <code>dico</code>.
     *
     * @param fields keywords of the response
     */
    private void fillDictionary(final Set<Field> fields) {
        final Iterator<Field> fieldIter = fields.iterator();
        while (fieldIter.hasNext()) {
            final Field field = fieldIter.next();
            if (!this.dico.containsKey(field.getName())) {
                final VoDictionary vodico = new VoDictionary();
                if (Utility.isSet(field.getDESCRIPTION())) {
                    vodico.setDescription(field.getDESCRIPTION().getContent().get(0).toString());
                    vodico.setUnit(field.getUnit());
                } else {
                    vodico.setDescription(null);
                    vodico.setUnit(field.getUnit());
                }
                this.dico.put(field.getName(), vodico);
            }
        }
    }

    /**
     * Returns the dictionary.
     *
     * @return the dico
     */
    protected final Map<String, VoDictionary> getDico() {
        return dico;
    }

    /**
     * Sets the dictionary.
     *
     * @param dicoVal the dico to set
     */
    protected final void setDico(final Map<String, VoDictionary> dicoVal) {
        this.dico = dicoVal;
    }
}
