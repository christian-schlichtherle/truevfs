/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import de.schlichtherle.truecommons.services.FactoryService;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.sl.FsDriverMapLocator;

/**
 * A service for creating maps of file system schemes to file system drivers.
 * Note that you can't subclass this class for customization.
 * It solely exists in order to support the 
 * {@link FsDriverMapLocator#SINGLETON}, which will use it to create the root
 * of the driver map which gets subsequently decorated by the
 * {@link FsDriverMapModifier} implementations found on the class path.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class FsDriverMapFactory
extends FactoryService<Map<FsScheme, FsDriver>> {

    /**
     * Returns a new empty map for subsequent modification.
     *
     * @return A new empty map for subsequent modification.
     */
    @Override
    public Map<FsScheme, FsDriver> get() {
        return new LinkedHashMap<>(32);
    }
}
