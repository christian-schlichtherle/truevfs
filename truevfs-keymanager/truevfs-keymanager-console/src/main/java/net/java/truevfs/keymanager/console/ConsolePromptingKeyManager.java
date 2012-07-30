/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.console;

import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.keymanager.spec.PromptingKeyManager;
import net.java.truevfs.keymanager.spec.param.SafePbeParameters;

/**
 * @author Christian Schlichtherle
 */
@ThreadSafe
final class ConsolePromptingKeyManager<P extends SafePbeParameters<P, ?>>
extends PromptingKeyManager<P> {

    ConsolePromptingKeyManager(ConsoleSafePbeParametersView<P, ?> view) {
        super(view);
    }
}
