/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.swing;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.PromptingKeyManager;
import net.java.truevfs.key.spec.param.SafePbeParameters;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class SwingKeyManager<P extends SafePbeParameters<P, ?>>
extends PromptingKeyManager<P> {

    SwingKeyManager(SwingSafePbeParametersView<P, ?> view) {
        super(view);
    }
}
