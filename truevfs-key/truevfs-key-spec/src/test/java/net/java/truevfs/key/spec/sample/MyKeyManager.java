/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.key.spec.sample;

import net.java.truevfs.key.spec.PromptingKeyManager;
import net.java.truevfs.key.spec.param.AesPbeParameters;

/** @author Christian Schlichtherle */
public class MyKeyManager
extends PromptingKeyManager<AesPbeParameters> {

    public MyKeyManager() { super(new MyPromptingKeyProviderView()); }
}
