/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.MockView;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.socket.IOPoolProvider;

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
        this.provider = new PromptingKeyManagerService(view);
        this.view = view;
    }

    private static MockView<AesPbeParameters> newView() {
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("secret".toCharArray());
        final MockView<AesPbeParameters> view = new MockView<AesPbeParameters>();
        view.setKey(key);
        return view;
    }

    public MockView<AesPbeParameters> getView() {
        return view;
    }

    @Override
    protected KeyManagerProvider getKeyManagerProvider() {
        return provider;
    }

    @Override
    public KeyProviderSyncStrategy getKeyProviderSyncStrategy() {
        return KeyProviderSyncStrategy.RESET_UNCONDITIONALLY;
    }
}
