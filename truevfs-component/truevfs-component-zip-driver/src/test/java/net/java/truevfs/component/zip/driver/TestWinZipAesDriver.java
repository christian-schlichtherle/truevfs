/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.component.zip.driver;

import net.java.truevfs.kernel.spec.TestConfig;
import net.java.truevfs.kernel.spec.cio.IoBufferPool;
import net.java.truevfs.keymanager.spec.MockView;
import net.java.truevfs.keymanager.spec.param.AesPbeParameters;

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
    public TestKeyManagerContainer getKeyManagerContainer() {
        return container;
    }

    public MockView<AesPbeParameters> getView() {
        return container.getView();
    }
}
