/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.spi;

import de.truezip.kernel.FsDriverProviderTestSuite;
import de.truezip.kernel.addr.FsScheme;
import de.truezip.kernel.sl.FsDriverLocator;
import de.truezip.kernel.util.SuffixSet;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class FsDriverServiceTestSuite extends FsDriverProviderTestSuite {

    @Test
    public void testLocatability() {
        for (final String suffix : new SuffixSet(getSuffixes()))
            assertNotNull(FsDriverLocator.SINGLETON.get().get(FsScheme.create(suffix)));
    }
}
