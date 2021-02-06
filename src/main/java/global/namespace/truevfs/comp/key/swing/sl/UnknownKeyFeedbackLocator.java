/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.swing.sl;

import global.namespace.service.wight.core.ServiceLocator;
import global.namespace.truevfs.comp.key.swing.feedback.Feedback;
import global.namespace.truevfs.comp.key.swing.spi.FeedbackFactory;
import global.namespace.truevfs.comp.key.swing.spi.UnknownKeyFeedbackDecorator;

import java.util.function.Supplier;

/**
 * A container of the singleton visual and/or audible feedback to the user
 * when prompting for an unknown key for the first time.
 * The feedback is created by using a {@link ServiceLocator} to search for advertised
 * implementations of the factory service specification class
 * {@link FeedbackFactory}
 * and the decorator service specification class
 * {@link UnknownKeyFeedbackDecorator}.
 *
 * @author Christian Schlichtherle
 */
public final class UnknownKeyFeedbackLocator implements Supplier<Feedback> {

    /** The singleton instance of this class. */
    public static final UnknownKeyFeedbackLocator
            SINGLETON = new UnknownKeyFeedbackLocator();

    private UnknownKeyFeedbackLocator() { }

    @Override
    public Feedback get() {
        return Lazy.feedback;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Lazy {
        static final Feedback feedback =
                new ServiceLocator().provider(FeedbackFactory.class, UnknownKeyFeedbackDecorator.class).get();
    }
}
