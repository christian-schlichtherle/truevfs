/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.swing;

import java.awt.GraphicsEnvironment;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons3.annotations.ServiceImplementation;
import net.java.truecommons3.key.spec.KeyManager;
import net.java.truecommons3.key.spec.common.AesPbeParameters;
import net.java.truecommons3.key.spec.prompting.PromptingKeyManager;
import net.java.truecommons3.key.spec.spi.KeyManagerMapModifier;

/**
 * A service provider for a Swing prompting key manager for
 * {@link AesPbeParameters}.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class SwingAesPbeKeyManagerMapModifier
extends KeyManagerMapModifier {

    @Override
    public Map<Class<?>, KeyManager<?>> apply(Map<Class<?>, KeyManager<?>> map) {
        if (!GraphicsEnvironment.isHeadless())
            map.put(AesPbeParameters.class,
                    new PromptingKeyManager<>(new SwingAesPbeParametersView()));
        return map;
    }

    /** @return -200 */
    @Override
    public int getPriority() { return -200; }
}
