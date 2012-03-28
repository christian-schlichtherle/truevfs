/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.key.KeyManager;
import de.truezip.key.MockView;
import de.truezip.key.param.AesPbeParameters;
import de.truezip.key.spi.KeyManagerService;
import java.util.Map;

/**
 * @author Christian Schlichtherle
 */
public final class TestKeyManagerService extends KeyManagerService {

    private final MockView<AesPbeParameters> view;
    private final Map<Class<?>, KeyManager<?>> managers;

    public TestKeyManagerService() {
        this.managers = newMap(new Object[][]{{
            AesPbeParameters.class,
            new TestKeyManager<AesPbeParameters>(this.view = newView())
        }});
    }

    private static MockView<AesPbeParameters> newView() {
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("top secret".toCharArray());
        final MockView<AesPbeParameters> view = new MockView<AesPbeParameters>();
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
