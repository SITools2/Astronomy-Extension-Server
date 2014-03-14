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
package fr.cnes.sitools.extensions.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Status validation of the validation decorator pattern.
 * @see Package Decorator pattern
 * @author Jean-Christophe Malapert <jean-christophe.malapert@cnes.fr>
 */
public class StatusValidation {

    /**
     * Error messages.
     */
    private Map<String, String> messages;

    /**
     * Constructs an empty StatusValidation.
     */
    public StatusValidation() {
        this.messages = new HashMap<String, String>();
    }

    /**
     * Constructs a StatusValidation with a set of error messages.
     * @param messageList error messages
     */
    public StatusValidation(final Map<String, String> messageList) {
        this.messages = new HashMap<String, String>();
        this.messages.putAll(messageList);
    }

    /**
     * Returns <code>True</code> when there is no error message otherwide <code>False</code>.
     * @return <code>True</code> when there is no error message otherwide <code>False</code>
     */
    public final boolean isValid() {
        return this.messages.isEmpty();
    }

    /**
     * Adds an error.
     * @param key key
     * @param message error
     */
    public final void add(final String key, final String message) {
        this.messages.put(key, message);
    }

    /**
     * Adds a set of errors.
     * @param messageList a set of errors
     */
    public final void addAll(final Map<String, String> messageList) {
        final Set<Entry<String, String>> entries = messageList.entrySet();
        for (Entry<String, String> entry : entries) {
            if (this.messages.containsKey(entry.getKey())) {
                this.messages.put(entry.getKey(), this.messages.get(entry.getKey()) + " - " + entry.getValue());
            } else {
                this.messages.put(entry.getKey(), entry.getValue());
            }
        }
        this.messages.putAll(messageList);
    }

    /**
     * Returns the error messages.
     * @return the error messages
     */
    public final Map<String, String> getMessages() {
        return this.messages;
    }

    /**
     * Sets the whole error messages.
     * @param messageList the whole error messages
     */
    public final void setMessages(final Map<String, String> messageList) {
        this.messages = messageList;
    }

    @Override
    public final String toString() {
        final StringBuilder builder = new StringBuilder();
        final Set<Entry<String, String>> entries = this.messages.entrySet();
        for (Entry<String, String> error : entries) {
            builder.append("[").append(error.getKey()).append("]");
            builder.append(" ").append(error.getValue());
            builder.append("\n");
        }
        return builder.toString();
    }
}
