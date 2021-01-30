/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.common;

import net.java.truecommons.key.spec.prompting.PromptingKeyProviderTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class AesPbeParametersProviderTest
extends PromptingKeyProviderTestSuite<AesPbeParameters> {

    @Override
    protected AesPbeParameters newParam() { return new AesPbeParameters(); }
}
