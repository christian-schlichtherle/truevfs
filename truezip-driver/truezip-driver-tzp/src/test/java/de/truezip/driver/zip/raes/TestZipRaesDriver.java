/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.truezip.driver.zip.raes.crypto.param.AesCipherParameters;
import de.truezip.driver.zip.KeyProviderSyncStrategy;
import de.truezip.kernel.key.MockView;
import de.schlichtherle.truezip.cio.IOPoolProvider;

/**
 * @author  Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

    private final MockView<AesCipherParameters> view;

    public TestZipRaesDriver(IOPoolProvider ioPoolProvider) {
        this(ioPoolProvider, newView());
    }

    private TestZipRaesDriver(
            final IOPoolProvider ioPoolProvider,
            final MockView<AesCipherParameters> view) {
        super(ioPoolProvider, new PromptingKeyManagerService(view));
        this.view = view;
    }

    private static MockView<AesCipherParameters> newView() {
        final AesCipherParameters key = new AesCipherParameters();
        key.setPassword("secret".toCharArray());
        final MockView<AesCipherParameters> view = new MockView<AesCipherParameters>();
        view.setKey(key);
        return view;
    }

    public MockView<AesCipherParameters> getView() {
        return view;
    }

    @Override
    public KeyProviderSyncStrategy getKeyProviderSyncStrategy() {
        return KeyProviderSyncStrategy.RESET_UNCONDITIONALLY;
    }
}