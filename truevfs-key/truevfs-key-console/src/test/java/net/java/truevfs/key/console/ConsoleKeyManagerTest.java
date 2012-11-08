/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.console;

import javax.annotation.CheckForNull;
import net.java.truevfs.key.spec.prompting.PromptingKeyManager;
import net.java.truevfs.key.spec.prompting.PromptingKeyManagerTestSuite;

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
