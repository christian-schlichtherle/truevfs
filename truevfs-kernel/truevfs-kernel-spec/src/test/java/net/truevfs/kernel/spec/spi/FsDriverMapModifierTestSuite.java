/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec.spi;

import java.util.Map;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.sl.FsDriverMapLocator;
import net.truevfs.kernel.spec.util.ExtensionSet;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class FsDriverMapModifierTestSuite {

    protected abstract String getExtensions();
    protected abstract FsDriverMapModifier newModifier();

    @Test
    public void testApply() {
        final Map<FsScheme, FsDriver> map = new FsDriverMapFactory().apply();
        assertThat(newModifier().apply(map), is(sameInstance(map)));
        assertThat(map.size(), is(not(0)));
        for (final String extension : new ExtensionSet(getExtensions()))
            assertNotNull(map.get(FsScheme.create(extension)));
    }

    @Test
    public void testIsLocatable() {
        final Map<FsScheme, FsDriver> map = FsDriverMapLocator.SINGLETON.apply();
        for (final String extension : new ExtensionSet(getExtensions()))
            assertNotNull(map.get(FsScheme.create(extension)));
    }
}
