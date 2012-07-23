/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.raes;

import net.truevfs.driver.zip.TestKeyManagerContainer;
import net.truevfs.kernel.spec.TestConfig;
import net.truevfs.kernel.spec.cio.IoBuffer;
import net.truevfs.kernel.spec.cio.IoBufferPool;
import net.truevfs.keymanager.spec.MockView;
import net.truevfs.keymanager.spec.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

    private final TestKeyManagerContainer
            container = new TestKeyManagerContainer();

    @Override
    public IoBufferPool<? extends IoBuffer<?>> getPool() {
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
