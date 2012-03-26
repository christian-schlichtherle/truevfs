/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.kernel.cio.IOPoolProvider;
import de.truezip.kernel.key.KeyManagerProvider;
import de.truezip.kernel.key.impl.MockView;
import de.truezip.kernel.key.impl.spi.PromptingKeyManagerService;
import de.truezip.kernel.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public final class TestWinZipAesDriver extends ZipDriver {

    private final KeyManagerProvider provider;

    public TestWinZipAesDriver(IOPoolProvider ioPoolProvider) {
        this(ioPoolProvider, newView());
    }

    private TestWinZipAesDriver(
            final IOPoolProvider ioPoolProvider,
            final MockView<AesPbeParameters> view) {
        super(ioPoolProvider);
        this.provider = new PromptingKeyManagerService(view);
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

    @Override
    public PromptingKeyProviderSyncStrategy getKeyProviderSyncStrategy() {
        return PromptingKeyProviderSyncStrategy.RESET_UNCONDITIONALLY;
    }
}
