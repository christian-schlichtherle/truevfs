/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.truezip.key.PromptingKeyManager;
import de.truezip.key.param.SafePbeParameters;
import javax.annotation.concurrent.ThreadSafe;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class SwingPromptingKeyManager<P extends SafePbeParameters<P, ?>>
extends PromptingKeyManager<P> {

    SwingPromptingKeyManager(SwingSafePbeParametersView<P, ?> view) {
        super(view);
    }
}
