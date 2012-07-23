/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymgr.swing.feedback;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.swing.JPanel;

/**
 * An abstract decorator for a feedback.
 *
 * @author Christian Schlichtherle
 */
public abstract class DecoratingFeedback implements Feedback {

    /** The nullable decorated feedback. */
    protected @CheckForNull Feedback feedback;

    public DecoratingFeedback() { }

    public DecoratingFeedback(final Feedback feedback) {
        this.feedback = Objects.requireNonNull(feedback);
    }

    @Override
    public void run(JPanel panel) {
        feedback.run(panel);
    }
}
