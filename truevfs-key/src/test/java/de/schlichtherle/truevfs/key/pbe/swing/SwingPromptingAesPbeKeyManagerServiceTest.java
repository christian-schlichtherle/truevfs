/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.key.pbe.swing;

import net.truevfs.key.PromptingKeyManager;
import net.truevfs.key.PromptingKeyManagerTestSuite;
import net.truevfs.key.param.AesPbeParameters;
import javax.annotation.CheckForNull;

/**
 * @author Christian Schlichtherle
 */
public class SwingPromptingAesPbeKeyManagerServiceTest
extends PromptingKeyManagerTestSuite {

    @Override
    protected @CheckForNull PromptingKeyManager<?> newKeyManager() {
        return (PromptingKeyManager<?>) new SwingPromptingAesPbeKeyManagerService()
                .keyManager(AesPbeParameters.class);
    }
}