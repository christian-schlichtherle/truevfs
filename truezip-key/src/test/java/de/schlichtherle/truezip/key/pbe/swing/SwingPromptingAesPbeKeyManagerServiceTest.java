/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.truezip.key.PromptingKeyManager;
import de.truezip.key.PromptingKeyManagerTestSuite;
import de.truezip.key.param.AesPbeParameters;
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