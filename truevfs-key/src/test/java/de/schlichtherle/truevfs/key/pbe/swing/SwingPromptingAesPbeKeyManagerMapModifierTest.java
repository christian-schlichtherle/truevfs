/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.swing;

import java.util.Map;
import javax.annotation.CheckForNull;
import net.truevfs.key.KeyManager;
import net.truevfs.key.PromptingKeyManager;
import net.truevfs.key.PromptingKeyManagerTestSuite;
import net.truevfs.key.param.AesPbeParameters;
import net.truevfs.key.spi.KeyManagerMapFactory;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Schlichtherle
 */
public class SwingPromptingAesPbeKeyManagerMapModifierTest
extends PromptingKeyManagerTestSuite {
    @Override
    protected @CheckForNull PromptingKeyManager<?> newKeyManager() {
        final Map<Class<?>, KeyManager<?>> map = new KeyManagerMapFactory().get();
        assertThat(new SwingPromptingAesPbeKeyManagerMapModifier().apply(map), is(sameInstance(map)));
        return (PromptingKeyManager<?>) map.get(AesPbeParameters.class);
    }
}
