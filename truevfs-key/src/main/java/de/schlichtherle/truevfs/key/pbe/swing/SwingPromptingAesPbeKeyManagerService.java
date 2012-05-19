/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.swing;

import net.truevfs.key.KeyManager;
import net.truevfs.key.param.AesPbeParameters;
import net.truevfs.key.spi.KeyManagerService;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * A service provider for a Swing prompting key manager for
 * {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class SwingPromptingAesPbeKeyManagerService
extends KeyManagerService {

    private final Map<Class<?>, KeyManager<?>> managers;

    public SwingPromptingAesPbeKeyManagerService() {
        this.managers = newMap(new Object[][] {{
            AesPbeParameters.class,
            new SwingPromptingKeyManager<>(new SwingAesPbeParametersView())
        }});
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> getKeyManagers() {
        return managers;
    }

    /** @return -200 iff the graphics environment is headless, -100 otherwise */
    @Override
    public int getPriority() {
        return GraphicsEnvironment.isHeadless() ? -200 : -100;
    }
}
