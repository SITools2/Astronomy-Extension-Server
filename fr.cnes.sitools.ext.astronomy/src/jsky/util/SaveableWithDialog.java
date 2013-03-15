/*
 * $Id: SaveableWithDialog.java,v 1.2 2009/04/21 13:31:17 abrighto Exp $
 */

package jsky.util;


/**
 * An interface for widgets that can pop up a dialog to save their
 * contents to a file.
 */
public abstract interface SaveableWithDialog {

    /**
     * Display a dialog to save the contents of this object to a file.
     */
    public void saveAs();
}
