/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing.sl;

import net.java.truecommons.key.swing.feedback.Feedback;
import net.java.truecommons.key.swing.spi.FeedbackFactory;
import net.java.truecommons.key.swing.spi.InvalidKeyFeedbackDecorator;
import net.java.truecommons.services.Container;
import net.java.truecommons.services.ServiceLocator;

/**
 * A container of the singleton visual and/or audible feedback to the user
 * when prompting for a key again after an invalid key has been provided before.
 * The feedback is created by using a {@link ServiceLocator} to search for advertised
 * implementations of the factory service specification class
 * {@link FeedbackFactory}
 * and the decorator service specification class
 * {@link InvalidKeyFeedbackDecorator}.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
public final class InvalidKeyFeedbackLocator implements Container<Feedback> {

    /** The singleton instance of this class. */
    public static final InvalidKeyFeedbackLocator
            SINGLETON = new InvalidKeyFeedbackLocator();

    private InvalidKeyFeedbackLocator() { }

    @Override
    public Feedback get() {
        return Lazy.feedback;
    }

    /** A static data utility class used for lazy initialization. */
    private static final class Lazy {
        static final Feedback feedback =
                new ServiceLocator(InvalidKeyFeedbackLocator.class)
                .factory(FeedbackFactory.class, InvalidKeyFeedbackDecorator.class)
                .get();
    }
}
