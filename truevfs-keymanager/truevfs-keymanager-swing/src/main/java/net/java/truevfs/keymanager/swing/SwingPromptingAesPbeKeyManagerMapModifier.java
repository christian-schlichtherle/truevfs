/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.swing;

import java.awt.GraphicsEnvironment;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truevfs.keymanager.spec.KeyManager;
import net.java.truevfs.keymanager.spec.param.AesPbeParameters;
import net.java.truevfs.keymanager.spec.spi.KeyManagerMapModifier;

/**
 * A service provider for a Swing prompting key manager for
 * {@link AesPbeParameters}.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class SwingPromptingAesPbeKeyManagerMapModifier
extends KeyManagerMapModifier {
    @Override
    public Map<Class<?>, KeyManager<?>> apply(Map<Class<?>, KeyManager<?>> map) {
        map.put(AesPbeParameters.class,
                new SwingPromptingKeyManager<>(new SwingAesPbeParametersView()));
        return map;
    }

    /** @return zero if the graphics environment is available, -200 otherwise. */
    @Override
    public int getPriority() {
        return GraphicsEnvironment.isHeadless() ? -200 : 0;
    }
}
