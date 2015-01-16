/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes;

import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.comp.zipdriver.TestKeyManagerMap;
import net.java.truevfs.kernel.spec.FsTestConfig;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.prompting.TestView;

/**
 * @author Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

    private final TestKeyManagerMap
            container = new TestKeyManagerMap();

    @Override
    public IoBufferPool getPool() {
        return FsTestConfig.get().getPool();
    }

    @Override
    public TestKeyManagerMap getKeyManagerMap() {
        return container;
    }

    public TestView<AesPbeParameters> getView() {
        return container.getView();
    }
}
