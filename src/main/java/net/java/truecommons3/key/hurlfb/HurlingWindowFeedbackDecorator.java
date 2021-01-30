/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.hurlfb;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons3.annotations.ServiceImplementation;
import net.java.truecommons3.key.swing.feedback.Feedback;
import net.java.truecommons3.key.swing.spi.InvalidKeyFeedbackDecorator;

/**
 * Decorates any given feedback with a
 * {@link HurlingWindowFeedback}.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class HurlingWindowFeedbackDecorator
extends InvalidKeyFeedbackDecorator {

    @Override
    public Feedback apply(Feedback feedback) {
        return new HurlingWindowFeedback(feedback);
    }

    /** Returns -100. */
    @Override
    public int getPriority() { return -100; }
}
