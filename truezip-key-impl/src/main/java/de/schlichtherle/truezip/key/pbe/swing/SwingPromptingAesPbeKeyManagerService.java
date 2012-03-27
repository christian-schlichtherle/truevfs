/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.truezip.key.KeyManager;
import de.truezip.key.param.AesPbeParameters;
import de.truezip.key.spi.KeyManagerService;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * A service provider for a Swing prompting key manager for
 * {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class SwingPromptingAesPbeKeyManagerService extends KeyManagerService {
    private final Map<Class<?>, KeyManager<?>> managers;

    public SwingPromptingAesPbeKeyManagerService() {
        this.managers = newMap(new Object[][] {{
            AesPbeParameters.class,
            new SwingPromptingKeyManager<AesPbeParameters>(new SwingAesPbeParametersView())
        }});
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}