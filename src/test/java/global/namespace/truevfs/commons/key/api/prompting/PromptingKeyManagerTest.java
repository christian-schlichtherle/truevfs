/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.key.api.prompting;

import global.namespace.truevfs.commons.key.api.KeyManagerTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class PromptingKeyManagerTest extends KeyManagerTestSuite<PromptingKeyManager<?>> {

    @SuppressWarnings("unchecked")
    @Override
    protected PromptingKeyManager<?> newKeyManager() {
        return new PromptingKeyManager(new TestView<>());
    }
}
