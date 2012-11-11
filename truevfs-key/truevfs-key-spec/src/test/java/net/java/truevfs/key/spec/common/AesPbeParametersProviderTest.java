/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.common;

import net.java.truevfs.key.spec.common.AesPbeParameters;
import net.java.truevfs.key.spec.prompting.PromptingKeyProviderTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class AesPbeParametersProviderTest
extends PromptingKeyProviderTestSuite<AesPbeParameters> {

    @Override
    protected AesPbeParameters newParam() { return new AesPbeParameters(); }
}
