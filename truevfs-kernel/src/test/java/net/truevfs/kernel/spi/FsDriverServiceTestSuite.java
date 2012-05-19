/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spi;

import net.truevfs.kernel.FsDriverProviderTestSuite;
import net.truevfs.kernel.FsScheme;
import net.truevfs.kernel.sl.FsDriverLocator;
import net.truevfs.kernel.util.ExtensionSet;
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
