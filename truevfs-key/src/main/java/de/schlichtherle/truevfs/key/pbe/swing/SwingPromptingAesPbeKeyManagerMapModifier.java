/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.swing;

import java.awt.GraphicsEnvironment;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.truevfs.key.KeyManager;
import net.truevfs.key.param.AesPbeParameters;
import net.truevfs.key.spi.KeyManagerMapModifier;

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

    /** @return zero if the graphics environment is available, -100 otherwise. */
    @Override
    public int getPriority() {
        return GraphicsEnvironment.isHeadless() ? -100 : 0;
    }
}
