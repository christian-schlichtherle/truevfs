/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import java.awt.Toolkit;
import javax.swing.JPanel;

/**
 * Provides run by beeping using the default toolkit.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class BasicFeedback implements Feedback {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in this class simply beeps using the default toolkit.
     */
    @Override
    public void run(JPanel panel) {
        Toolkit.getDefaultToolkit().beep();
    }
}
