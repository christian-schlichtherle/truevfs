/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.swing.feedback;

import javax.swing.*;
import java.awt.*;

/**
 * Provides feedback by beeping using the default toolkit.
 *
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
