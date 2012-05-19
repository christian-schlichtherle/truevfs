/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip;

import net.truevfs.kernel.cio.IOPool;
import net.truevfs.kernel.cio.IOPoolProvider;
import net.truevfs.key.MockView;
import net.truevfs.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public final class TestWinZipAesDriver extends ZipDriver {

    private final IOPoolProvider ioPoolProvider;
    private final TestKeyManagerProvider service;

    public TestWinZipAesDriver(final IOPoolProvider ioPoolProvider) {
        this.ioPoolProvider = ioPoolProvider;
        this.service = new TestKeyManagerProvider();
    }

    @Override
    public IOPool<?> getIOPool() {
        return ioPoolProvider.getIOPool();
    }

    @Override
    public TestKeyManagerProvider getKeyManagerProvider() {
        return service;
    }

    public MockView<AesPbeParameters> getView() {
        return service.getView();
    }
}
