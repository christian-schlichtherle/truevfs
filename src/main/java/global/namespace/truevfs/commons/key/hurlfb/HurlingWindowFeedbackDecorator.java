/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.hurlfb;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.truevfs.commons.key.swing.feedback.Feedback;
import global.namespace.truevfs.commons.key.swing.spi.InvalidKeyFeedbackDecorator;

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
