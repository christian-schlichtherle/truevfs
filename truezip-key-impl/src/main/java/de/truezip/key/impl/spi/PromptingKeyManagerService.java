/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.key.impl.spi;

import de.truezip.key.KeyManager;
import de.truezip.key.impl.PromptingKeyManager;
import de.truezip.key.impl.PromptingKeyProviderView;
import de.truezip.key.impl.pbe.console.ConsoleAesPbeParametersView;
import de.truezip.key.impl.pbe.swing.SwingAesPbeParametersView;
import de.truezip.key.param.AesPbeParameters;
import de.truezip.key.spi.KeyManagerService;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * A container for a prompting key manager implementation for
 * {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class PromptingKeyManagerService extends KeyManagerService {

    private final Map<Class<?>, KeyManager<?>> managers;

    /**
     * Constructs a new prompting key manager service using the default view.
     * If this JVM is running {@link GraphicsEnvironment#isHeadless() headless},
     * then the view of the prompting key provider of the prompting key manager
     * is an instance of {@link ConsoleAesPbeParametersView}.
     * Otherwise, it's an instance of {@link SwingAesPbeParametersView}.
     */
    public PromptingKeyManagerService() {
        this.managers = newMap(new Object[][] {
            {
                AesPbeParameters.class,
                new PromptingKeyManager<AesPbeParameters>(
                    GraphicsEnvironment.isHeadless()
                        ? new ConsoleAesPbeParametersView()
                        : new SwingAesPbeParametersView())
            }
        });
    }

    /**
     * Constructs a new prompting key manager service using the given view.
     * 
     * @param view the view for the prompting key providers of the prompting
     *        key manager.
     */
    public PromptingKeyManagerService(PromptingKeyProviderView<AesPbeParameters> view) {
        this.managers = newMap(new Object[][] {
            {
                AesPbeParameters.class,
                new PromptingKeyManager<AesPbeParameters>(view)
            }
        });
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}