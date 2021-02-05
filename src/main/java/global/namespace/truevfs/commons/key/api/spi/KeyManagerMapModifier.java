/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.api.spi;

import global.namespace.service.wight.annotation.ServiceInterface;
import global.namespace.truevfs.commons.key.api.KeyManager;
import global.namespace.truevfs.commons.key.api.sl.KeyManagerMapLocator;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * An abstract service for modifying maps of classes to key managers.
 * Modifier services are subject to service location by the
 * {@link KeyManagerMapLocator#SINGLETON}.
 * If multiple modifier services are locatable on the class path at run time, they are applied in ascending order of
 * their {@linkplain global.namespace.service.wight.annotation.ServiceImplementation#priority()} so that the result of
 * the modifier service with the greatest number becomes the result of the entire modifier chain.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
public interface KeyManagerMapModifier extends UnaryOperator<Map<Class<?>, KeyManager<?>>> {
}
