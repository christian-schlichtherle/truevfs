/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.key.SafeKey;
import de.truezip.kernel.key.impl.PromptingKeyManager;
import de.truezip.kernel.key.impl.PromptingKeyProvider;
import de.truezip.kernel.key.impl.PromptingKeyProviderView;
import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @param  <K> the type of the safe keys.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class TestKeyManager<K extends SafeKey<K>>
extends PromptingKeyManager<K> {

    public TestKeyManager(final PromptingKeyProviderView<K> view) {
        super(view);
    }

    @Override
    public synchronized void unlock(URI resource) {
        final PromptingKeyProvider<K> provider = get(resource);
        if (null != provider)
            provider.resetUnconditionally();
    }
}