/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.spi;

import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.key.PromptingKeyProviderView;
import de.truezip.key.KeyManager;
import de.truezip.key.param.AesPbeParameters;
import de.truezip.key.spi.KeyManagerService;
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