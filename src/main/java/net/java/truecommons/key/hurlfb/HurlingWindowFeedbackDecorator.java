/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.hurlfb;

import javax.annotation.concurrent.Immutable;
import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.key.swing.feedback.Feedback;
import net.java.truecommons.key.swing.spi.InvalidKeyFeedbackDecorator;

/**
 * Decorates any given feedback with a
 * {@link HurlingWindowFeedback}.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class HurlingWindowFeedbackDecorator implements InvalidKeyFeedbackDecorator {

    @Override
    public Feedback apply(Feedback feedback) {
        return new HurlingWindowFeedback(feedback);
    }
}
