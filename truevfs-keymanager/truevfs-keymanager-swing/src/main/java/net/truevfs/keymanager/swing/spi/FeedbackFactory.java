/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.swing.spi;

import de.schlichtherle.truecommons.services.FactoryService;
import javax.annotation.concurrent.Immutable;
import net.truevfs.keymanager.swing.feedback.BeepFeedback;
import net.truevfs.keymanager.swing.feedback.Feedback;
import net.truevfs.keymanager.swing.sl.InvalidKeyFeedbackLocator;
import net.truevfs.keymanager.swing.sl.UnknownKeyFeedbackLocator;

/**
 * A service for creating visual and/or audible feedback to the user
 * when prompting for unknown or invalid keys.
 * Note that you can't subclass this class for customization.
 * It solely exists in order to support the 
 * {@link UnknownKeyFeedbackLocator#SINGLETON} and the
 * {@link InvalidKeyFeedbackLocator#SINGLETON}, which will use it to create the
 * root of the feedback chain which gets subsequently decorated by the
 * {@link UnknownKeyFeedbackDecorator} and {@link InvalidKeyFeedbackDecorator}
 * implementations found on the class path.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class FeedbackFactory
extends FactoryService<Feedback> {

    /**
     * Returns a new {@link BeepFeedback}.
     *
     * @return A new {@link BeepFeedback}.
     */
    @Override
    public Feedback get() {
        return new BeepFeedback();
    }
}
