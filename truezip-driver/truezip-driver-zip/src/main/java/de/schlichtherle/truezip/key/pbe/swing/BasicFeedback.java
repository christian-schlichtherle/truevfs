/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe.swing;

import java.awt.Toolkit;
import javax.swing.JPanel;

/**
 * Provides run by beeping using the default toolkit.
 *
 * @author  Christian Schlichtherle
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