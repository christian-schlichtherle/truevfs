/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing.spi;

import global.namespace.service.wight.annotation.ServiceInterface;
import net.java.truecommons.key.swing.feedback.Feedback;
import net.java.truecommons.key.swing.sl.UnknownKeyFeedbackLocator;

import java.util.function.UnaryOperator;

/**
 * An abstract service for providing visible and/or audible feedback to the
 * user when prompting for an unknown key for the first time.
 * Decorator services are subject to service location by the
 * {@link UnknownKeyFeedbackLocator#SINGLETON}.
 * If multiple decorator services are locatable on the class path at run time, they are applied in ascending order of
 * their {@linkplain global.namespace.service.wight.annotation.ServiceImplementation#priority()} so that the product of
 * the decorator service with the greatest number becomes the head of the resulting product chain.
 *
 * @author Christian Schlichtherle
 * @see FeedbackFactory
 * @see InvalidKeyFeedbackDecorator
 */
@ServiceInterface
public interface UnknownKeyFeedbackDecorator extends UnaryOperator<Feedback> {
}
