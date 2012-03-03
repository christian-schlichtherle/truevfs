/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

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
 * @version $Id$
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
