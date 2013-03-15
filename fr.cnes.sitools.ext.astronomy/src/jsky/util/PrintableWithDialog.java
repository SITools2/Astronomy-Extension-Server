/*
 * $Id: PrintableWithDialog.java,v 1.2 2009/04/21 13:31:17 abrighto Exp $
 */

package jsky.util;

import java.awt.print.PrinterException;


/**
 * An interface for widgets that can pop up a print dialog to send their
 * contents to the printer.
 */
public abstract interface PrintableWithDialog {

    /**
     * Display a print dialog to print the contents of this object.
     */
    public void print() throws PrinterException;
}
