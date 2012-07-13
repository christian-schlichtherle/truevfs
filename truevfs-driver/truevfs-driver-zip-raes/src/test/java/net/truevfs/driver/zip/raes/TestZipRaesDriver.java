/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes;

import net.truevfs.driver.zip.TestKeyManagerProvider;
import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.key.MockView;
import net.truevfs.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

    private final TestKeyManagerProvider service = new TestKeyManagerProvider();

    @Override
    public IoBufferPool<? extends IoBuffer<?>> getPool() {
        return TestConfig.get().getPool();
    }
    
    @Override
    public TestKeyManagerProvider getKeyManagerProvider() {
        return service;
    }

    public MockView<AesPbeParameters> getView() {
        return service.getView();
    }
}
