/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.services.LocatableFactory;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truecommons.annotations.ServiceSpecification;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;

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
@Immutable
@ServiceSpecification
@ServiceImplementation
public class FsDriverMapFactory
extends LocatableFactory<Map<FsScheme, FsDriver>> {

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
