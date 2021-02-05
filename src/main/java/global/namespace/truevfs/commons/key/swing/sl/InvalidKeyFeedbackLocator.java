/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.swing.sl;

import global.namespace.service.wight.core.ServiceLocator;
import global.namespace.truevfs.commons.key.swing.feedback.Feedback;
import global.namespace.truevfs.commons.key.swing.spi.FeedbackFactory;
import global.namespace.truevfs.commons.key.swing.spi.InvalidKeyFeedbackDecorator;

import java.util.function.Supplier;

/**
 * A container of the singleton visual and/or audible feedback to the user
 * when prompting for a key again after an invalid key has been provided before.
 * The feedback is created by using a {@link ServiceLocator} to search for advertised
 * implementations of the factory service specification class
 * {@link FeedbackFactory}
 * and the decorator service specification class
 * {@link InvalidKeyFeedbackDecorator}.
 *
 * @author Christian Schlichtherle
 */
public final class InvalidKeyFeedbackLocator implements Supplier<Feedback> {

    /**
     * The singleton instance of this class.
     */
    public static final InvalidKeyFeedbackLocator
            SINGLETON = new InvalidKeyFeedbackLocator();

    private InvalidKeyFeedbackLocator() {
    }

    @Override
    public Feedback get() {
        return Lazy.feedback;
    }

    /**
     * A static data utility class used for lazy initialization.
     */
    private static final class Lazy {
        static final Feedback feedback =
                new ServiceLocator().provider(FeedbackFactory.class, InvalidKeyFeedbackDecorator.class).get();
    }
}
