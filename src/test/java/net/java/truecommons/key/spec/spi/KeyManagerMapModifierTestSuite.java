/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.spi;

import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.sl.KeyManagerMapLocator;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Schlichtherle
 */
public abstract class KeyManagerMapModifierTestSuite {

    protected KeyManagerMapModifier modifier;

    protected abstract Iterable<Class<?>> getClasses();

    protected abstract KeyManagerMapModifier newModifier();

    @Before
    public void before() {
        modifier = newModifier();
    }

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
            assertThat(located.get(clazz),
                    instanceOf(modified.get(clazz).getClass()));
    }
}
