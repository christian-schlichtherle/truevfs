/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.swing.feedback;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truevfs.key.swing.spi.InvalidKeyFeedbackDecorator;

/**
 * Decorates any given feedback with a
 * {@link TemporarilyDisableDefaultButtonFeedback}.
 *
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
