/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.swing.feedback;

import java.awt.Toolkit;
import javax.swing.JPanel;

/**
 * Provides feedback by beeping using the default toolkit.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public final class BeepFeedback implements Feedback {

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
