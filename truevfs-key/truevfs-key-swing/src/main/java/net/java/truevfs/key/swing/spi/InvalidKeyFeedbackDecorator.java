/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.swing.spi;

import net.java.truecommons.services.DecoratorService;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.swing.feedback.Feedback;

/**
 * An abstract service for providing visible and/or audible feedback to the
 * user when prompting for a key again after an invalid key has been provided
 * before.
 * Decorator services are subject to service location by the
 * {@link InvalidKeyFeedbackLocator#SINGLETON}.
 * If multiple decorator services are locatable on the class path at run time,
 * they are applied in ascending order of their
 * {@linkplain #getPriority() priority} so that the product of the decorator
 * service with the greatest number becomes the head of the resulting product
 * chain.
 * <p>
 * Implementations should be thread-safe.
 * 
 * @see    FeedbackFactory
 * @see    UnknownKeyFeedbackDecorator
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class InvalidKeyFeedbackDecorator
extends DecoratorService<Feedback> {
}
