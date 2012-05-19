/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.console;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.key.KeyManager;
import net.truevfs.key.param.AesPbeParameters;
import net.truevfs.key.spi.KeyManagerService;

/**
 * A service provider for a console prompting key manager for
 * {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class ConsolePromptingAesPbeKeyManagerService
extends KeyManagerService {

    private final Map<Class<?>, KeyManager<?>> managers;

    public ConsolePromptingAesPbeKeyManagerService() {
        this.managers = newMap(new Object[][] {{
            AesPbeParameters.class,
            new ConsolePromptingKeyManager<>(new ConsoleAesPbeParametersView())
        }});
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> getKeyManagers() {
        return managers;
    }

    /** @return -150 */
    @Override
    public int getPriority() {
        return -150;
    }
}
