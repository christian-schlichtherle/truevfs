/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.console;

import javax.annotation.CheckForNull;
import net.truevfs.keymanager.spec.PromptingKeyManager;
import net.truevfs.keymanager.spec.PromptingKeyManagerTestSuite;
import net.truevfs.keymanager.console.ConsoleAesPbeParametersView;
import net.truevfs.keymanager.console.ConsolePromptingKeyManager;

/**
 * @author Christian Schlichtherle
 */
public class ConsolePromptingKeyManagerTest
extends PromptingKeyManagerTestSuite {
    @Override
    protected @CheckForNull PromptingKeyManager<?> newKeyManager() {
        return new ConsolePromptingKeyManager<>(new ConsoleAesPbeParametersView());
    }
}
