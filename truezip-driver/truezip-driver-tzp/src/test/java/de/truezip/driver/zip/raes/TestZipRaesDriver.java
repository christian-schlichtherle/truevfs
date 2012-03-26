/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.truezip.driver.zip.PromptingKeyProviderSyncStrategy;
import de.truezip.kernel.cio.IOPoolProvider;
import de.truezip.kernel.key.impl.MockView;
import de.truezip.kernel.key.impl.spi.PromptingKeyManagerService;
import de.truezip.kernel.key.param.AesPbeParameters;

/**
 * @author Christian Schlichtherle
 */
public class TestZipRaesDriver extends SafeZipRaesDriver {

    private final MockView<AesPbeParameters> view;

    public TestZipRaesDriver(IOPoolProvider ioPoolProvider) {
        this(ioPoolProvider, newView());
    }

    private TestZipRaesDriver(
            final IOPoolProvider ioPoolProvider,
            final MockView<AesPbeParameters> view) {
        super(ioPoolProvider, new PromptingKeyManagerService(view));
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
    public PromptingKeyProviderSyncStrategy getKeyProviderSyncStrategy() {
        return PromptingKeyProviderSyncStrategy.RESET_UNCONDITIONALLY;
    }
}