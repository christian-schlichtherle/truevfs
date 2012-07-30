/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.spec.spi;

import net.java.truecommons.services.FactoryService;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.keymanager.spec.KeyManager;
import net.java.truevfs.keymanager.spec.sl.KeyManagerMapLocator;

/**
 * A service for creating maps of classes to key managers.
 * Note that you can't subclass this class for customization.
 * It solely exists in order to support the 
 * {@link KeyManagerMapLocator#SINGLETON}, which will use it to create the root
 * of the driver map which gets subsequently decorated by the
 * {@link KeyManagerMapModifier} implementations found on the class path.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class KeyManagerMapFactory
extends FactoryService<Map<Class<?>, KeyManager<?>>> {

    /**
     * Returns a new empty map for subsequent modification.
     *
     * @return A new empty map for subsequent modification.
     */
    @Override
    public Map<Class<?>, KeyManager<?>> get() {
        return new LinkedHashMap<>(32);
    }
}
