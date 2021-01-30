/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing.spi;

import net.java.truecommons.annotations.ServiceSpecification;
import net.java.truecommons.key.swing.feedback.Feedback;
import net.java.truecommons.key.swing.sl.InvalidKeyFeedbackLocator;
import net.java.truecommons.services.LocatableDecorator;

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
 *
 * @see    FeedbackFactory
 * @see    UnknownKeyFeedbackDecorator
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@ServiceSpecification
public abstract class InvalidKeyFeedbackDecorator
extends LocatableDecorator<Feedback> { }
