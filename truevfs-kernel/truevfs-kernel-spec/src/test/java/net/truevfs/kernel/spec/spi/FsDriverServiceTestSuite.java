/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import net.truevfs.kernel.spec.sl.FsDriverLocator;
import net.truevfs.kernel.spec.FsDriverProviderTestSuite;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.util.ExtensionSet;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class FsDriverServiceTestSuite extends FsDriverProviderTestSuite {
    @Test
    public void testIsLocatable() {
        for (final String extension : new ExtensionSet(getExtensions()))
            assertNotNull(FsDriverLocator.SINGLETON.getDrivers().get(FsScheme.create(extension)));
    }
}
