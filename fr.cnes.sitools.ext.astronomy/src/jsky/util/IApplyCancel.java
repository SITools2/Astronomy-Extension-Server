// Copyright 2002
// Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: IApplyCancel.java,v 1.1.1.1 2009/02/17 22:24:39 abrighto Exp $

package jsky.util;

/**
 * An interface for dialogs that can be applied or canceled.
 */
public abstract interface IApplyCancel {
    public void apply();

    public void cancel();
}

