/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.swing.spi;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons3.annotations.ServiceImplementation;
import net.java.truecommons3.annotations.ServiceSpecification;
import net.java.truecommons3.key.swing.feedback.BeepFeedback;
import net.java.truecommons3.key.swing.feedback.Feedback;
import net.java.truecommons3.services.LocatableFactory;

/**
 * A service for creating visual and/or audible feedback to the user
 * when prompting for unknown or invalid keys.
 * <p>
 * Note that you can't subclass this class for customization.
 * Instead, you should implement a custom {@link UnknownKeyFeedbackDecorator}
 * or {@link InvalidKeyFeedbackDecorator} and advertise them on the class path.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceSpecification
@ServiceImplementation
public class FeedbackFactory
extends LocatableFactory<Feedback> {

    /**
     * Returns a new {@link BeepFeedback}.
     *
     * @return A new {@link BeepFeedback}.
     */
    @Override
    public Feedback get() { return new BeepFeedback(); }

    /**
     * {@inheritDoc}
     * <p>
     * If the {@linkplain #getClass() runtime class} of this object is
     * {@link FeedbackFactory}, then {@code -100} gets returned.
     * Otherwise, zero gets returned.
     */
    @Override
    public int getPriority() {
        return FeedbackFactory.class.equals(getClass()) ? -100 : 0;
    }
}
