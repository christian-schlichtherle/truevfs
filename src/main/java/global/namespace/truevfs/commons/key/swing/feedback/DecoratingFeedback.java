/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.swing.feedback;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;

/**
 * An abstract decorator for a feedback.
 *
 * @author Christian Schlichtherle
 */
public abstract class DecoratingFeedback implements Feedback {

    /** The nullable decorated feedback. */
    protected @Nullable Feedback feedback;

    public DecoratingFeedback() { }

    public DecoratingFeedback(final Feedback feedback) {
        this.feedback = Objects.requireNonNull(feedback);
    }

    @Override
    public void run(JPanel panel) {
        feedback.run(panel);
    }
}
