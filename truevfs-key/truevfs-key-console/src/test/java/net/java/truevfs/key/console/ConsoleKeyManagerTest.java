/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.console;

import javax.annotation.CheckForNull;
import net.java.truevfs.key.spec.PromptingKeyManager;
import net.java.truevfs.key.spec.PromptingKeyManagerTestSuite;
import net.java.truevfs.key.console.ConsoleAesPbeParametersView;
import net.java.truevfs.key.console.ConsoleKeyManager;

/**
 * @author Christian Schlichtherle
 */
public class ConsoleKeyManagerTest
extends PromptingKeyManagerTestSuite {
    @Override
    protected @CheckForNull PromptingKeyManager<?> newKeyManager() {
        return new ConsoleKeyManager<>(new ConsoleAesPbeParametersView());
    }
}
