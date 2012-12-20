/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.spi;

import java.util.Map;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.sl.FsDriverMapLocator;
import static org.hamcrest.CoreMatchers.*;
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
        final Map<FsScheme, FsDriver> map = new FsDriverMapFactory().get();
        assertThat(newModifier().apply(map), is(sameInstance(map)));
        assertThat(map.size(), is(not(0)));
        for (final String extension : new ExtensionSet(getExtensions()))
            assertThat(map.get(FsScheme.create(extension)), notNullValue());
    }

    @Test
    public void testIsLocatable() {
        final Map<FsScheme, FsDriver>
                modified = newModifier().apply(new FsDriverMapFactory().get());
        final Map<FsScheme, FsDriver>
                located = FsDriverMapLocator.SINGLETON.get();
        for (final FsScheme scheme : modified.keySet())
            assertThat( located.get(scheme),
                        instanceOf(modified.get(scheme).getClass()));
    }
}
