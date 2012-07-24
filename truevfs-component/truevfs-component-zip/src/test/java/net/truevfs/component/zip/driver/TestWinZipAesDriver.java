/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.component.zip.driver;

import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.keymanager.spec.MockView;
import net.truevfs.keymanager.spec.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public final class TestWinZipAesDriver extends ZipDriver {

    private final TestKeyManagerContainer service = new TestKeyManagerContainer();

    @Override
    public IoBufferPool<? extends IoBuffer<?>> getPool() {
        return TestConfig.get().getPool();
    }

    @Override
    public TestKeyManagerContainer getKeyManagerContainer() {
        return service;
    }

    public MockView<AesPbeParameters> getView() {
        return service.getView();
    }
}
