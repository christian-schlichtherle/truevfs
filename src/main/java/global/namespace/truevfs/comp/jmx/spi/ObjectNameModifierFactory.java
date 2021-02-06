/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import global.namespace.truevfs.comp.jmx.ObjectNameModifier;
import global.namespace.truevfs.comp.jmx.sl.ObjectNameModifierLocator;

import java.util.function.Supplier;

/**
 * An abstract service for creating object name codecs.
 * Factory services are subject to service location by the
 * {@link ObjectNameModifierLocator#SINGLETON}.
 * <p>
 * If multiple factory services are locatable on the class path at run time, the service with the greatest
 * {@linkplain ServiceImplementation#priority()} gets selected.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
public interface ObjectNameModifierFactory extends Supplier<ObjectNameModifier> { }
