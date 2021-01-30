/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.swing.feedback;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons3.annotations.ServiceImplementation;
import net.java.truecommons3.key.swing.spi.InvalidKeyFeedbackDecorator;

/**
 * Decorates any given feedback with a
 * {@link TemporarilyDisableDefaultButtonFeedback}.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class TemporarilyDisableDefaultButtonFeedbackDecorator
extends InvalidKeyFeedbackDecorator {

    @Override
    public Feedback apply(Feedback feedback) {
        return new TemporarilyDisableDefaultButtonFeedback(feedback);
    }

    /** Returns -200. */
    @Override
    public int getPriority() { return -200; }
}
