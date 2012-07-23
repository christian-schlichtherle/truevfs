/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymgr.swing.spi;

import de.schlichtherle.truecommons.services.DecoratorService;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.keymgr.swing.feedback.Feedback;
import net.truevfs.keymgr.swing.sl.UnknownKeyFeedbackLocator;

/**
 * An abstract service for providing visible and/or audible feedback to the
 * user when prompting for an unknown key for the first time.
 * Decorator services are subject to service location by the
 * {@link UnknownKeyFeedbackLocator#SINGLETON}.
 * If multiple decorator services are locatable on the class path at run time,
 * they are applied in ascending order of their
 * {@linkplain #getPriority() priority} so that the product of the decorator
 * service with the greatest number becomes the head of the resulting product
 * chain.
 * <p>
 * Implementations should be thread-safe.
 * 
 * @see    FeedbackFactory
 * @see    InvalidKeyFeedbackDecorator
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class UnknownKeyFeedbackDecorator
extends DecoratorService<Feedback> {
}
