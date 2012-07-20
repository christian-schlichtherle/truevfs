/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymgr.swing;

import javax.annotation.CheckForNull;
import net.truevfs.keymgr.spec.PromptingKeyManager;
import net.truevfs.keymgr.spec.PromptingKeyManagerTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class SwingPromptingKeyManagerTest
extends PromptingKeyManagerTestSuite {
    @Override
    protected @CheckForNull PromptingKeyManager<?> newKeyManager() {
        return new SwingPromptingKeyManager<>(new SwingAesPbeParametersView());
    }
}
