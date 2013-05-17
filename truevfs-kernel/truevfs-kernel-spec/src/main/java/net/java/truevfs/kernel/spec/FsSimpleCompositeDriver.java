/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.util.Map;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.services.Container;

/**
 * Uses a given file system driver provider to lookup the appropriate driver
 * for the scheme of a given mount point.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsSimpleCompositeDriver
extends FsAbstractCompositeDriver {

    private final Container<Map<FsScheme, FsDriver>> container;

    /**
     * Constructs a new simple meta driver which will query the given
     * {@code provider} for an appropriate file system driver for the scheme
     * of a given mount point.
     *
     * @param container the driver map container.
     */
    public FsSimpleCompositeDriver(
            final Container<Map<FsScheme, FsDriver>> container) {
        this.container = Objects.requireNonNull(container);
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return container.get();
    }
}
