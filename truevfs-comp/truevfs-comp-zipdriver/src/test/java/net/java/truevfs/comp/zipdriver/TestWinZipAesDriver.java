/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zipdriver;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.prompting.TestView;
import net.java.truevfs.kernel.spec.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class TestWinZipAesDriver extends ZipDriver {

    private final TestKeyManagerMap container = new TestKeyManagerMap();

    @Override
    public IoBufferPool getPool() { return FsTestConfig.get().getPool(); }

    @Override
    public TestKeyManagerMap getKeyManagerMap() { return container; }

    public TestView<AesPbeParameters> getView() { return container.getView(); }
}
