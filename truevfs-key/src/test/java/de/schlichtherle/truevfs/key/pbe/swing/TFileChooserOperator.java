/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.swing;

import java.awt.Dialog;
import java.awt.Window;
import javax.swing.JFileChooser;
import org.netbeans.jemmy.DialogWaiter;
import org.netbeans.jemmy.operators.JFileChooserOperator;
import org.netbeans.jemmy.operators.WindowOperator;

/**
 * An operator for the first file chooser which is owned by the given window.
 *
 * @author  Christian Schlichtherle
 */
final class TFileChooserOperator extends JFileChooserOperator {

    TFileChooserOperator(final WindowOperator parent) {
	super((JFileChooser) waitComponent(
                waitJFileChooserDialog(parent),
                new JFileChooserFinder(),
		0, parent.getTimeouts(), parent.getOutput()));
	super.copyEnvironment(parent);
    }

    private static Dialog waitJFileChooserDialog(final WindowOperator parent) {
	try {
	    final DialogWaiter waiter = new DialogWaiter();
	    waiter.setTimeouts(parent.getTimeouts());
	    waiter.setOutput(parent.getOutput());
	    return waiter.waitDialog(
                    (Window) parent.getSource(),
                    new JFileChooserJDialogFinder(parent.getOutput()));
	} catch (InterruptedException ex) {
	    parent.getOutput().printStackTrace(ex);
	    return(null);
	}
    }
}