/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import java.util.Collections;
import java.util.Map;
import net.java.truecommons.key.spec.AbstractKeyManagerMap;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.prompting.TestView;

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
        managers = (Map) Collections.singletonMap(clazz, manager);
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
