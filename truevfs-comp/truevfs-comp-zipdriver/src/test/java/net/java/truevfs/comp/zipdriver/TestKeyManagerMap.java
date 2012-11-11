/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.java.truevfs.key.spec.AbstractKeyManagerMap;
import net.java.truevfs.key.spec.KeyManager;
import net.java.truevfs.key.spec.common.AesPbeParameters;
import net.java.truevfs.key.spec.prompting.TestView;

/**
 * @author Christian Schlichtherle
 */
public final class TestKeyManagerMap extends AbstractKeyManagerMap {

    private final TestView<AesPbeParameters> view;
    private final Map<Class<?>, KeyManager<?>> managers;

    public TestKeyManagerMap() {
        final Map<Class<?>, KeyManager<?>> map = new HashMap<>(2);
        map.put(AesPbeParameters.class, new TestKeyManager<>(this.view = newView()));
        managers = Collections.unmodifiableMap(map);
    }

    private static TestView<AesPbeParameters> newView() {
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("top secret".toCharArray());
        final TestView<AesPbeParameters> view = new TestView<>();
        view.setKey(key);
        return view;
    }

    public TestView<AesPbeParameters> getView() {
        return view;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}
