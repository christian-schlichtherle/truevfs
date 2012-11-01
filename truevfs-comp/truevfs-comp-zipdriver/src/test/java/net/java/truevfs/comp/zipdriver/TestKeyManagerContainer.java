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
import net.java.truevfs.key.spec.MockView;
import net.java.truevfs.key.spec.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public final class TestKeyManagerContainer extends AbstractKeyManagerMap {

    private final MockView<AesPbeParameters> view;
    private final Map<Class<?>, KeyManager<?>> managers;

    public TestKeyManagerContainer() {
        final Map<Class<?>, KeyManager<?>> map = new HashMap<>(2);
        map.put(AesPbeParameters.class, new TestKeyManager<>(this.view = newView()));
        managers = Collections.unmodifiableMap(map);
    }

    private static MockView<AesPbeParameters> newView() {
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("top secret".toCharArray());
        final MockView<AesPbeParameters> view = new MockView<>();
        view.setKey(key);
        return view;
    }

    public MockView<AesPbeParameters> getView() {
        return view;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}
