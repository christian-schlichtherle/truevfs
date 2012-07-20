/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.truevfs.keymgr.spec.AbstractKeyManagerContainer;
import net.truevfs.keymgr.spec.KeyManager;
import net.truevfs.keymgr.spec.MockView;
import net.truevfs.keymgr.spec.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public final class TestKeyManagerContainer extends AbstractKeyManagerContainer {

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
