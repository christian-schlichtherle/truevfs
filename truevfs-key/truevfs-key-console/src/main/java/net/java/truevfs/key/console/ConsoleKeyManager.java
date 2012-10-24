/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.console;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.key.spec.PromptingKeyManager;
import net.java.truevfs.key.spec.param.SafePbeParameters;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ConsoleKeyManager<P extends SafePbeParameters<P, ?>>
extends PromptingKeyManager<P> {

    ConsoleKeyManager(ConsoleSafePbeParametersView<P, ?> view) {
        super(view);
    }
}
