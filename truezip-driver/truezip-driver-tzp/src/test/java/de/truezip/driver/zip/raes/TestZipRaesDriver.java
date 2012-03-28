/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.truezip.driver.zip.TestKeyManagerService;
import de.truezip.kernel.cio.IOPoolProvider;
import de.truezip.key.MockView;
import de.truezip.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

    private final TestKeyManagerService service;

    public TestZipRaesDriver(final IOPoolProvider ioPoolProvider) {
        super(ioPoolProvider);
        this.service = new TestKeyManagerService();
    }

    @Override
    protected TestKeyManagerService getKeyManagerProvider() {
        return service;
    }

    public MockView<AesPbeParameters> getView() {
        return service.getView();
    }
}