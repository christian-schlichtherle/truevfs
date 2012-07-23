/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;
import net.truevfs.keymanager.spec.PromptingKeyManager;
import net.truevfs.keymanager.spec.PromptingKeyProvider;
import net.truevfs.keymanager.spec.PromptingKeyProvider.View;
import net.truevfs.keymanager.spec.SafeKey;

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
