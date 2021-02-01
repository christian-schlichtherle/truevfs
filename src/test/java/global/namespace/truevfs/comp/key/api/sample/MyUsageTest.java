/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api.sample;

import global.namespace.truevfs.comp.key.api.AbstractKeyManagerMap;
import global.namespace.truevfs.comp.key.api.KeyManager;
import global.namespace.truevfs.comp.key.api.KeyManagerMap;
import global.namespace.truevfs.comp.key.api.spi.KeyManagerMapFactory;

import java.util.Collections;
import java.util.Map;

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
        public Map<Class<?>, KeyManager<?>> get() { return map; }
    }
}
