/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.key.PromptingKeyProvider.View;
import de.schlichtherle.truezip.key.spi.KeyManagerService;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import javax.annotation.concurrent.Immutable;

/**
 * A container for a prompting key manager implementation for
 * {@link AesCipherParameters}.
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
     * is an instance of
     * {@link de.schlichtherle.truezip.crypto.raes.param.console.AesCipherParametersView}.
     * Otherwise, it's an instance of
     * {@link de.schlichtherle.truezip.crypto.raes.param.swing.AesCipherParametersView}.
     */
    public PromptingKeyManagerService() {
        this.managers = newMap(new Object[][] {
            {
                AesCipherParameters.class,
                new PromptingKeyManager<AesCipherParameters>(
                    GraphicsEnvironment.isHeadless()
                        ? new de.schlichtherle.truezip.crypto.raes.param.console.AesCipherParametersView()
                        : new de.schlichtherle.truezip.crypto.raes.param.swing.AesCipherParametersView())
            }
        });
    }

    /**
     * Constructs a new prompting key manager service using the given view.
     * 
     * @param view the view for the prompting key providers of the prompting
     *        key manager.
     */
    public PromptingKeyManagerService(View<AesCipherParameters> view) {
        this.managers = newMap(new Object[][] {
            {
                AesCipherParameters.class,
                new PromptingKeyManager<AesCipherParameters>(view)
            }
        });
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<Class<?>, KeyManager<?>> get() {
        return managers;
    }
}
