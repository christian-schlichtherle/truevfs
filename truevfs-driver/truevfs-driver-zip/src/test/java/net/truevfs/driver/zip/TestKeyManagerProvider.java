/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import java.util.Map;
import net.truevfs.key.AbstractKeyManagerProvider;
import net.truevfs.key.KeyManager;
import net.truevfs.key.MockView;
import net.truevfs.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public final class TestKeyManagerProvider extends AbstractKeyManagerProvider {

    private final MockView<AesPbeParameters> view;
    private final Map<Class<?>, KeyManager<?>> managers;

    public TestKeyManagerProvider() {
        this.managers = newMap(new Object[][]{{
            AesPbeParameters.class,
            new TestKeyManager<>(this.view = newView())
        }});
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
    public Map<Class<?>, KeyManager<?>> getKeyManagers() {
        return managers;
    }
}