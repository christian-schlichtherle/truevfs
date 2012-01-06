/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import org.netbeans.jemmy.operators.JFileChooserOperator;

/**
 * A file chooser operator which always uses the cross platform look and feel.
 *
 * @author Christian Schlichtherle
 * @author Michael Hall
 * @version $Id$
 */
final class FileChooserOperator extends JFileChooserOperator {

    FileChooserOperator() {
        LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf.isNativeLookAndFeel()) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                super.updateUI();
                UIManager.setLookAndFeel(laf);
            } catch (Exception ex) {
                throw new AssertionError(ex);
            }
        }
    }
}
