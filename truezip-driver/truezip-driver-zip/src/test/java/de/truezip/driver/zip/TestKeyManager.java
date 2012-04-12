/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.key.PromptingKeyManager;
import de.truezip.key.PromptingKeyProvider;
import de.truezip.key.PromptingKeyProvider.View;
import de.truezip.key.SafeKey;
import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @param  <K> the type of the safe keys.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class TestKeyManager<K extends SafeKey<K>>
extends PromptingKeyManager<K> {

    public TestKeyManager(final View<K> view) {
        super(view);
    }

    @Override
    public synchronized void unlock(URI resource) {
        final PromptingKeyProvider<K> provider = get(resource);
        if (null != provider)
            provider.resetUnconditionally();
    }
}