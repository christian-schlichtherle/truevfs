/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.zipdriver;

import global.namespace.truevfs.commons.key.api.AbstractKeyManagerMap;
import global.namespace.truevfs.commons.key.api.KeyManager;
import global.namespace.truevfs.commons.key.api.common.AesPbeParameters;
import global.namespace.truevfs.commons.key.api.prompting.TestView;

import java.util.Collections;
import java.util.Map;

/**
 * @author Christian Schlichtherle
 */
public final class TestKeyManagerMap extends AbstractKeyManagerMap {

    private final TestView<AesPbeParameters> view;
    private final Map<Class<?>, KeyManager<?>> managers;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TestKeyManagerMap() {
        final Class clazz = AesPbeParameters.class;
        final KeyManager manager = new TestKeyManager(this.view = newView());
        managers = Collections.<Class<?>, KeyManager<?>>singletonMap(clazz, manager);
    }

    private static TestView<AesPbeParameters> newView() {
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("top secret".toCharArray());
        final TestView<AesPbeParameters> view = new TestView<>();
        view.setKey(key);
        return view;
    }

    public TestView<AesPbeParameters> getView() { return view; }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() { return managers; }
}
