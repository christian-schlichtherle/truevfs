/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing.util;

import java.awt.Dialog;
import java.awt.Window;
import javax.swing.JFileChooser;
import org.netbeans.jemmy.DialogWaiter;
import org.netbeans.jemmy.operators.JFileChooserOperator;
import org.netbeans.jemmy.operators.WindowOperator;

/**
 * An operator for the first file chooser which is owned by the given window.
 *
 * @author Christian Schlichtherle
 */
public class FileChooserOfWindowOperator extends JFileChooserOperator {

    public FileChooserOfWindowOperator(final WindowOperator parent) {
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
