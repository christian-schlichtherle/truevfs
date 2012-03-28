/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.key.PromptingKeyProviderView;
import de.truezip.key.KeyManager;
import de.truezip.key.param.AesPbeParameters;
import de.truezip.key.spi.KeyManagerService;
import java.util.Map;

/**
 * @author Christian Schlichtherle
 */
public final class TestKeyManagerService extends KeyManagerService {
    private final Map<Class<?>, KeyManager<?>> managers;

    public TestKeyManagerService(
            final PromptingKeyProviderView<AesPbeParameters> view) {
        this.managers = newMap(new Object[][]{{
            AesPbeParameters.class,
            new TestKeyManager<AesPbeParameters>(view)
        }});
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}
