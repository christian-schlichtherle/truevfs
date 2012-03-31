/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.addr.FsScheme;
import de.truezip.kernel.util.SuffixSet;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class FsDriverProviderTestSuite {

    private FsDriverProvider provider;

    protected abstract String getSuffixes();
    protected abstract FsDriverProvider newDriverProvider();

    @Before
    public void setUp() {
        provider = newDriverProvider();
    }

    @Test
    public void testGet() {
        for (final String suffix : new SuffixSet(getSuffixes()))
            assertNotNull(provider.get().get(FsScheme.create(suffix)));
    }

    @Test
    public void testImmutability() {
        for (final String suffix : new SuffixSet(getSuffixes()) ) {
            try {
                provider.get().remove(FsScheme.create(suffix));
                fail();
            } catch (UnsupportedOperationException expected) {
            }
        }
    }
}
