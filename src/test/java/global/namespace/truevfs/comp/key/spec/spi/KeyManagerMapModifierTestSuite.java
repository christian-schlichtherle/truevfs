/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.spec.spi;

import global.namespace.service.wight.annotation.ServiceImplementation;
import lombok.val;
import global.namespace.truevfs.comp.key.spec.KeyManager;
import global.namespace.truevfs.comp.key.spec.sl.KeyManagerMapLocator;
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
        assertTrue(modifier.getClass().getAnnotation(ServiceImplementation.class).priority() < 0);
    }

    @Test
    public void testIsLocatable() {
        val modified = modifier.apply(new KeyManagerMapFactory().get());
        val located = KeyManagerMapLocator.SINGLETON.get();
        for (Class<?> klass : modified.keySet()) {
            assertThat(located.get(klass), instanceOf(modified.get(klass).getClass()));
        }
    }
}
