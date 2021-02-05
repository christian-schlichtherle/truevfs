/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.swing.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import global.namespace.truevfs.commons.key.swing.feedback.BeepFeedback;
import global.namespace.truevfs.commons.key.swing.feedback.Feedback;

import java.util.function.Supplier;

/**
 * A service for creating visual and/or audible feedback to the user
 * when prompting for unknown or invalid keys.
 * <p>
 * Note that you can't subclass this class for customization.
 * Instead, you should implement a custom {@link UnknownKeyFeedbackDecorator}
 * or {@link InvalidKeyFeedbackDecorator} and advertise them on the class path.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
@ServiceImplementation(priority = -100)
public class FeedbackFactory implements Supplier<Feedback> {

    /**
     * Returns a new {@link BeepFeedback}.
     */
    @Override
    public Feedback get() { return new BeepFeedback(); }
}
