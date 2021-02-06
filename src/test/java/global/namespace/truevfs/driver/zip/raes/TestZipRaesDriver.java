/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes;

import global.namespace.truevfs.comp.cio.IoBufferPool;
import global.namespace.truevfs.comp.key.api.KeyManagerMap;
import global.namespace.truevfs.comp.key.api.aes.AesPbeParameters;
import global.namespace.truevfs.comp.key.api.prompting.TestView;
import global.namespace.truevfs.comp.zipdriver.TestKeyManagerMap;
import global.namespace.truevfs.kernel.api.FsTestConfig;

/**
 * @author Christian Schlichtherle
 */
public final class TestZipRaesDriver extends SafeZipRaesDriver {

    private final TestKeyManagerMap keyManagerMap = new TestKeyManagerMap();

    @Override
    public IoBufferPool getPool() { return FsTestConfig.get().getPool(); }

    @Override
    public KeyManagerMap getKeyManagerMap() { return keyManagerMap; }

    public TestView<AesPbeParameters> getView() { return keyManagerMap.getView(); }
}
