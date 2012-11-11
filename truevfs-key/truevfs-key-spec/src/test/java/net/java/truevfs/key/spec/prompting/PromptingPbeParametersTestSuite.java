/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.prompting;

import net.java.truevfs.key.spec.AbstractSecretKeyTestSuite;
import net.java.truevfs.key.spec.KeyStrength;

/**
 * @author Christian Schlichtherle
 */
public abstract class PromptingPbeParametersTestSuite<
        P extends AbstractPromptingPbeParameters<P, S>,
        S extends KeyStrength>
extends AbstractSecretKeyTestSuite<P> {

    protected abstract P newParam();

    @Override
    protected final P newKey() {
        final P param = newParam();
        param.setChangeRequested(true);
        final S[] strengths = param.getAllKeyStrengths();
        param.setKeyStrength(strengths[strengths.length - 1]);
        // param.setPassword("töp secret".toCharArray()); // transient!
        return param;
    }
}
