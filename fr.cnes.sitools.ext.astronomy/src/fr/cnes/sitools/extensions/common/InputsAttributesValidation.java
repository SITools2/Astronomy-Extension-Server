/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.cnes.sitools.extensions.common;

import fr.cnes.sitools.plugins.applications.model.ApplicationPluginParameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author malapert
 */
public class InputsAttributesValidation extends Validation {
    
    public InputsAttributesValidation(final Map<String, Object> attributes) {
        final Map<String, String> mapToTest = new HashMap<String, String>();
        final Set<Map.Entry<String, Object>> entries = attributes.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            mapToTest.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        setMap(mapToTest);        
    }

    @Override
    protected void processValidation() {
        if (getMap() == null) {
            throw new IllegalArgumentException("the map cannot be null.");
        }        
    }
    
}
