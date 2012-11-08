/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.swing;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.prompting.PromptingKeyManager;
import net.java.truevfs.key.spec.prompting.PromptingPbeParameters;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class SwingKeyManager<P extends PromptingPbeParameters<P, ?>>
extends PromptingKeyManager<P> {

    SwingKeyManager(SwingPromptingPbeParametersView<P, ?> view) {
        super(view);
    }
}
