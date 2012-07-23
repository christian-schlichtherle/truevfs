/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.swing.sl;

import de.schlichtherle.truecommons.services.Container;
import de.schlichtherle.truecommons.services.Locator;
import net.truevfs.keymanager.swing.feedback.Feedback;
import net.truevfs.keymanager.swing.spi.FeedbackFactory;
import net.truevfs.keymanager.swing.spi.InvalidKeyFeedbackDecorator;

/**
 * A container of the singleton visual and/or audible feedback to the user
 * when prompting for a key again after an invalid key has been provided before.
 * The feedback is created by using a {@link Locator} to search for advertised
 * implementations of the factory service specification class
 * {@link FeedbackFactory}
 * and the decorator service specification class
 * {@link InvalidKeyFeedbackDecorator}.
 *
 * @author Christian Schlichtherle
 */
public class InvalidKeyFeedbackLocator implements Container<Feedback> {

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
                new Locator(InvalidKeyFeedbackLocator.class)
                .factory(FeedbackFactory.class, InvalidKeyFeedbackDecorator.class)
                .get();
    }
}
