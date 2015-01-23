/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.key.spec.prompting.PromptingKey;
import net.java.truecommons.key.spec.prompting.PromptingKey.View;
import net.java.truecommons.key.spec.prompting.PromptingKeyManager;

/**
 * @param  <K> the type of the safe keys.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class TestKeyManager<K extends PromptingKey<K>>
extends PromptingKeyManager<K> {

    public TestKeyManager(View<K> view) { super(view); }

    @Override
    public synchronized void release(final URI resource) {
        super.resetUnconditionally(resource);
    }
}
