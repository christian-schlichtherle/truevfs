/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.key.spec.prompting;

import net.java.truecommons3.key.spec.KeyManagerTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class PromptingKeyManagerTest
extends KeyManagerTestSuite<PromptingKeyManager<?>> {

    @Override
    protected PromptingKeyManager<?> newKeyManager() {
        return new PromptingKeyManager<>(new TestView<>());
    }
}
