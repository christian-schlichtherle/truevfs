/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.swing;

import net.truevfs.key.PromptingKeyManager;
import net.truevfs.key.param.SafePbeParameters;
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
