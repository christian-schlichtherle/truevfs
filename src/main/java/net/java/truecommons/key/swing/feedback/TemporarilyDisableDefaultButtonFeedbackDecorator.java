/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing.feedback;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.key.swing.spi.InvalidKeyFeedbackDecorator;

/**
 * Decorates any given feedback with a
 * {@link TemporarilyDisableDefaultButtonFeedback}.
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -200)
public final class TemporarilyDisableDefaultButtonFeedbackDecorator implements InvalidKeyFeedbackDecorator {

    @Override
    public Feedback apply(Feedback feedback) {
        return new TemporarilyDisableDefaultButtonFeedback(feedback);
    }
}
