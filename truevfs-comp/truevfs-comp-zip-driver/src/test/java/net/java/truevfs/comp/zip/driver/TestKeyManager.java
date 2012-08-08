/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip.driver;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.PromptingKeyManager;
import net.java.truevfs.key.spec.PromptingKeyProvider;
import net.java.truevfs.key.spec.PromptingKeyProvider.View;
import net.java.truevfs.key.spec.SafeKey;

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
