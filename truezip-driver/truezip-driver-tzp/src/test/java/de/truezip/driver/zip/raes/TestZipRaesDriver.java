/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.truezip.driver.zip.TestKeyManagerProvider;
import de.truezip.kernel.cio.IOPool;
import de.truezip.kernel.cio.IOPoolProvider;
import de.truezip.key.MockView;
import de.truezip.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

    private final IOPoolProvider ioPoolProvider;
    private final TestKeyManagerProvider service;

    public TestZipRaesDriver(final IOPoolProvider ioPoolProvider) {
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