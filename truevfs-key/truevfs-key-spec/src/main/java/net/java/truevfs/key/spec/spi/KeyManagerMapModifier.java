/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.spi;

import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.services.ModifierService;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.sl.KeyManagerMapLocator;

/**
 * An abstract service for modifying maps of classes to key managers.
 * Modifier services are subject to service location by the
 * {@link KeyManagerMapLocator#SINGLETON}.
 * If multiple modifier services are locatable on the class path at run
 * time, they are applied in ascending order of their
 * {@linkplain #getPriority() priority} so that the result of the modifier
 * service with the greatest number becomes the result of the entire
 * modifier chain.
 * <p>
 * Implementations should be thread-safe.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class KeyManagerMapModifier
extends ModifierService<Map<Class<?>, KeyManager<?>>> {
}
