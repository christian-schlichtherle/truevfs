/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFileTestBase;
import static de.schlichtherle.truezip.fs.FsOutputOption.ENCRYPT;
import de.schlichtherle.truezip.key.MockView;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import java.io.IOException;
import javax.annotation.Nullable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class WinZipAesFileIT extends TFileTestBase<TestWinZipAesDriver> {

    private @Nullable MockView<AesPbeParameters> view;

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver(IOPoolProvider provider) {
        return new TestWinZipAesDriver(provider, view);
    }

    @Override
    public void setUp() throws IOException {
        this.view = new MockView<AesPbeParameters>();
        super.setUp();
        final TConfig config = TConfig.get();
        config.setOutputPreferences(config.getOutputPreferences().set(ENCRYPT));
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("secret".toCharArray());
        view.setKey(key);
    }
}
