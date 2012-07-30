/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.spec.spi;

import net.java.truevfs.keymanager.spec.spi.KeyManagerMapModifier;
import net.java.truevfs.keymanager.spec.spi.KeyManagerMapFactory;
import java.util.Map;
import net.java.truevfs.keymanager.spec.KeyManager;
import net.java.truevfs.keymanager.spec.sl.KeyManagerMapLocator;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class KeyManagerMapModifierTestSuite {
    protected abstract Iterable<Class<?>> getClasses();
    protected abstract KeyManagerMapModifier newModifier();

    @Test
    public void testApply() {
        final Map<Class<?>, KeyManager<?>> map = new KeyManagerMapFactory().get();
        assertThat(newModifier().apply(map), is(sameInstance(map)));
        assertThat(map.size(), is(not(0)));
        for (final Class<?> clazz : getClasses())
            assertThat(map.get(clazz), notNullValue());
    }

    @Test
    public void testIsLocatable() {
        final Map<Class<?>, KeyManager<?>>
                modified = newModifier().apply(new KeyManagerMapFactory().get());
        final Map<Class<?>, KeyManager<?>>
                located = KeyManagerMapLocator.SINGLETON.get();
        for (final Class<?> clazz : modified.keySet())
            assertThat( located.get(clazz),
                        instanceOf(modified.get(clazz).getClass()));
    }
}
