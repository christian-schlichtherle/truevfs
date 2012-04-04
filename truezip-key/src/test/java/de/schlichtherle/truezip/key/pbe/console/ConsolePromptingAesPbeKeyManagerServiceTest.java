/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe.console;

import de.truezip.key.PromptingKeyManager;
import de.truezip.key.PromptingKeyManagerTestSuite;
import de.truezip.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public class ConsolePromptingAesPbeKeyManagerServiceTest
extends PromptingKeyManagerTestSuite {

    @Override
    protected PromptingKeyManager<?> newKeyManager() {
        return (PromptingKeyManager<?>) new ConsolePromptingAesPbeKeyManagerService()
                .get(AesPbeParameters.class);
    }
}