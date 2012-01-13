/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.fs.archive.zip.PromptingKeyManagerService;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.MockView;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.socket.IOPoolProvider;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class TestWinZipAesDriver extends ZipDriver {
    private final KeyManagerProvider provider;

    public TestWinZipAesDriver(
            IOPoolProvider ioPoolProvider,
            MockView<AesPbeParameters> view) {
        super(ioPoolProvider);
        this.provider = new PromptingKeyManagerService(view);
    }

    @Override
    protected KeyManagerProvider getKeyManagerProvider() {
        return provider;
    }
}
