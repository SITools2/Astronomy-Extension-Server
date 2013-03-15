/*
 * $Id: Interruptable.java,v 1.2 2009/04/21 13:31:17 abrighto Exp $
 */

package jsky.util;

/**
 * An interface for objects that can be interrupted (to stop whatever they
 * are doing).
 */
public abstract interface Interruptable {

    /**
     * Interrupt the current background thread.
     */
    public void interrupt();
}
