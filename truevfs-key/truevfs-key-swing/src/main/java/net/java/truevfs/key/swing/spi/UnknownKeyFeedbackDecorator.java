/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.swing.spi;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.services.LocatableDecorator;
import net.java.truecommons.services.annotations.ServiceSpecification;
import net.java.truevfs.key.swing.feedback.Feedback;
import net.java.truevfs.key.swing.sl.UnknownKeyFeedbackLocator;

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
 *
 * @see    FeedbackFactory
 * @see    InvalidKeyFeedbackDecorator
 * @author Christian Schlichtherle
 */
@ServiceSpecification
public abstract class UnknownKeyFeedbackDecorator
extends LocatableDecorator<Feedback> {
}
