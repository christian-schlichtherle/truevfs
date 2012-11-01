/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.sample;

import java.util.Collections;
import java.util.Map;
import net.java.truevfs.key.spec.AbstractKeyManagerMap;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.KeyManagerMap;
import net.java.truevfs.key.spec.spi.KeyManagerMapFactory;

/** @author Christian Schlichtherle */
public class MyUsageTest extends UsageTestSuite {

    @Override
    KeyManagerMap getKeyManagerMap() { return new MyKeyManagerMap(); }

    private static class MyKeyManagerMap extends AbstractKeyManagerMap {

        final Map<Class<?>, KeyManager<?>> map =
                Collections.unmodifiableMap(
                    new MyKeyManagerMapModifier().apply(
                        new KeyManagerMapFactory().get()));

        @Override
        public Map<Class<?>, KeyManager<?>> get() {
            return map;
        }
    }
}
