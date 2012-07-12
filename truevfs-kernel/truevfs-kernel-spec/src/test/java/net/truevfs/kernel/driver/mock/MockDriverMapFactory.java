/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.driver.mock;

import java.util.Map;
import java.util.Objects;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsDriverMapProviders;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.spi.FsDriverMapFactory;

/**
 * Creates maps with a mock driver.
 *
 * @author Christian Schlichtherle
 */
public final class MockDriverMapFactory extends FsDriverMapFactory {
    private final String extensions;

    public MockDriverMapFactory(final String extensions) {
        this.extensions = Objects.requireNonNull(extensions);
    }

    @Override
    public Map<FsScheme, FsDriver> drivers() {
        return FsDriverMapProviders.newMap(new Object[][] {
            { extensions, new MockDriver() },
        });
    }
}
