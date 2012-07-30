/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.console;

import javax.annotation.CheckForNull;
import net.java.truevfs.keymanager.spec.PromptingKeyManager;
import net.java.truevfs.keymanager.spec.PromptingKeyManagerTestSuite;
import net.java.truevfs.keymanager.console.ConsoleAesPbeParametersView;
import net.java.truevfs.keymanager.console.ConsolePromptingKeyManager;

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
