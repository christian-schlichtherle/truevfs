/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.keymanager.swing;

import net.truevfs.keymanager.swing.SwingPromptingKeyManager;
import net.truevfs.keymanager.swing.SwingAesPbeParametersView;
import javax.annotation.CheckForNull;
import net.truevfs.keymanager.spec.PromptingKeyManager;
import net.truevfs.keymanager.spec.PromptingKeyManagerTestSuite;

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
