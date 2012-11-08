/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes;

import net.java.truevfs.comp.zipdriver.TestKeyManagerMap;
import net.java.truevfs.kernel.spec.TestConfig;
import net.java.truecommons.cio.IoBufferPool;
import net.java.truevfs.key.spec.prompting.TestView;
import net.java.truevfs.key.spec.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

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
