/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api.common;

import global.namespace.truevfs.comp.key.api.prompting.PromptingKeyProviderTestSuite;

/**
 * @author Christian Schlichtherle
 */
public class AesPbeParametersProviderTest
extends PromptingKeyProviderTestSuite<AesPbeParameters> {

    @Override
    protected AesPbeParameters newParam() { return new AesPbeParameters(); }
}
