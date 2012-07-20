/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes;

import net.truevfs.driver.zip.TestKeyManagerContainer;
import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.keymgr.spec.MockView;
import net.truevfs.keymgr.spec.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

    private final TestKeyManagerContainer service = new TestKeyManagerContainer();

    @Override
    public IoBufferPool<? extends IoBuffer<?>> getPool() {
        return TestConfig.get().getPool();
    }
    
    @Override
    public TestKeyManagerContainer getKeyManagerProvider() {
        return service;
    }

    public MockView<AesPbeParameters> getView() {
        return service.getView();
    }
}
