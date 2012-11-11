/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.kernel.spec.TestConfig;
import net.java.truevfs.key.spec.common.AesPbeParameters;
import net.java.truevfs.key.spec.prompting.TestView;

/**
 * @author Christian Schlichtherle
 */
public final class TestWinZipAesDriver extends ZipDriver {

    private final TestKeyManagerMap
            container = new TestKeyManagerMap();

    @Override
    public IoBufferPool getPool() {
        return TestConfig.get().getPool();
    }

    @Override
    public TestKeyManagerMap getKeyManagerMap() {
        return container;
    }

    public TestView<AesPbeParameters> getView() {
        return container.getView();
    }
}
