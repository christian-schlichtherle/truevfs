/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.fs.archive.zip.KeyProviderSyncStrategy;
import de.schlichtherle.truezip.key.MockView;
import de.schlichtherle.truezip.socket.IOPoolProvider;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
