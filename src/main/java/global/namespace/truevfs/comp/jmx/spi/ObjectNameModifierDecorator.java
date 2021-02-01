/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import global.namespace.truevfs.comp.jmx.ObjectNameModifier;
import global.namespace.truevfs.comp.jmx.sl.ObjectNameModifierLocator;

import java.util.function.UnaryOperator;

/**
 * An abstract service for decorating object name codecs.
 * Decorator services are subject to service location by the
 * {@link ObjectNameModifierLocator#SINGLETON}.
 * <p>
 * If multiple decorator services are locatable on the class path at run time, they are applied in ascending order of
 * their {@linkplain ServiceImplementation#priority()} so that the product of the decorator service with the greatest
 * number becomes the head of the resulting product chain.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
public interface ObjectNameModifierDecorator extends UnaryOperator<ObjectNameModifier> { }
