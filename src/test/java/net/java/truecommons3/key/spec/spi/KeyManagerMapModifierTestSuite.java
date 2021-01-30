/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.spi;

import java.util.Map;
import net.java.truecommons3.key.spec.KeyManager;
import net.java.truecommons3.key.spec.sl.KeyManagerMapLocator;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class KeyManagerMapModifierTestSuite {

    protected KeyManagerMapModifier modifier;

    protected abstract Iterable<Class<?>> getClasses();
    protected abstract KeyManagerMapModifier newModifier();

    @Before
    public void before() { modifier = newModifier(); }

    @Test
    public void testApply() {
        final Map<Class<?>, KeyManager<?>> map = new KeyManagerMapFactory().get();
        assertThat(modifier.apply(map), is(sameInstance(map)));
    }

    @Test
    public void testPriority() {
        assertTrue(modifier.getPriority() < 0);
    }

    @Test
    public void testIsLocatable() {
        final Map<Class<?>, KeyManager<?>>
                modified = modifier.apply(new KeyManagerMapFactory().get());
        final Map<Class<?>, KeyManager<?>>
                located = KeyManagerMapLocator.SINGLETON.get();
        for (final Class<?> clazz : modified.keySet())
            assertThat( located.get(clazz),
                        instanceOf(modified.get(clazz).getClass()));
    }
}
