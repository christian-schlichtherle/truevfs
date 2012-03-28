/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.key.MockView;
import de.truezip.kernel.cio.IOPoolProvider;
import de.truezip.key.KeyManagerProvider;
import de.truezip.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public final class TestWinZipAesDriver extends ZipDriver {

    private final KeyManagerProvider provider;
    private final MockView<AesPbeParameters> view;

    public TestWinZipAesDriver(IOPoolProvider ioPoolProvider) {
        this(ioPoolProvider, newView());
    }

    private TestWinZipAesDriver(
            final IOPoolProvider ioPoolProvider,
            final MockView<AesPbeParameters> view) {
        super(ioPoolProvider);
        this.provider = new TestKeyManagerService(this.view = view);
    }

    private static MockView<AesPbeParameters> newView() {
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("secret".toCharArray());
        final MockView<AesPbeParameters> view = new MockView<AesPbeParameters>();
        view.setKey(key);
        return view;
    }

    @Override
    protected KeyManagerProvider getKeyManagerProvider() {
        return provider;
    }

    public MockView<AesPbeParameters> getView() {
        return view;
    }
}
