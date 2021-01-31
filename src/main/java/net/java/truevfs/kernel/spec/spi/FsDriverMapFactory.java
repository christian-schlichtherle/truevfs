/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import global.namespace.service.wight.annotation.ServiceInterface;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A service for creating maps of file system schemes to file system drivers.
 * Subclasses annotated with {@link ServiceImplementation} are subject to service location by the
 * {@link FsDriverMapLocator#SINGLETON}.
 * <p>
 * If multiple factory services are located on the class path at run time, the service with the greatest
 * {@linkplain ServiceImplementation#priority()} gets selected.
 *
 * @author Christian Schlichtherle
 */
@ServiceInterface
@ServiceImplementation(priority = -100)
public class FsDriverMapFactory implements Supplier<Map<FsScheme, FsDriver>> {

    /**
     * Returns a new empty map for subsequent modification.
     */
    @Override
    public Map<FsScheme, FsDriver> get() {
        return new LinkedHashMap<>(32);
    }
}
