/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.swing.sl;

import net.java.truecommons.services.Container;
import net.java.truecommons.services.Locator;
import net.truevfs.keymanager.swing.feedback.Feedback;
import net.truevfs.keymanager.swing.spi.FeedbackFactory;
import net.truevfs.keymanager.swing.spi.UnknownKeyFeedbackDecorator;

/**
 * A container of the singleton visual and/or audible feedback to the user
 * when prompting for an unknown key for the first time.
 * The feedback is created by using a {@link Locator} to search for advertised
 * implementations of the factory service specification class
 * {@link FeedbackFactory}
 * and the decorator service specification class
 * {@link UnknownKeyFeedbackDecorator}.
 *
 * @author Christian Schlichtherle
 */
public class UnknownKeyFeedbackLocator implements Container<Feedback> {

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
                new Locator(UnknownKeyFeedbackLocator.class)
                .factory(FeedbackFactory.class, UnknownKeyFeedbackDecorator.class)
                .get();
    }
}
