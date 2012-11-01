/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import net.java.truevfs.comp.zipdriver.ZipDriver;
import net.java.truevfs.kernel.spec.TestConfig;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.key.spec.MockView;
import net.java.truevfs.key.spec.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public final class TestWinZipAesDriver extends ZipDriver {

    private final TestKeyManagerContainer
            container = new TestKeyManagerContainer();

    @Override
    public IoBufferPool getPool() {
        return TestConfig.get().getPool();
    }

    @Override
    public TestKeyManagerContainer getKeyManagerMap() {
        return container;
    }

    public MockView<AesPbeParameters> getView() {
        return container.getView();
    }
}
