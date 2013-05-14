/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.extensions.common;

import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author malapert
 */
public class ApplicationPluginParameterValidation extends Validation {

    public ApplicationPluginParameterValidation(final Map<String, ApplicationPluginParameter> parameters) {
        final Map<String, String> mapToTest = new HashMap<String, String>();
        final Set<Entry<String, ApplicationPluginParameter>> entries = parameters.entrySet();
        for (Entry<String, ApplicationPluginParameter> entry : entries) {
            mapToTest.put(entry.getKey(), entry.getValue().getValue());
        }
        setMap(mapToTest);
    }

    @Override
    protected final void processValidation() {
        if (getMap() == null) {
            throw new IllegalArgumentException("the map cannot be null.");
        }
    }
}
