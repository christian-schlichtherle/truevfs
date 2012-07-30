/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.swing;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.keymanager.spec.PromptingKeyManager;
import net.java.truevfs.keymanager.spec.param.SafePbeParameters;

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
