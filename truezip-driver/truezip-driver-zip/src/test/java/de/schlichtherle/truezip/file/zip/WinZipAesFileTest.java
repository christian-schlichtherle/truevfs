/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFileTestSuite;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.key.MockView;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class WinZipAesFileTest extends TFileTestSuite<TestWinZipAesDriver> {

    private @Nullable MockView<AesPbeParameters> view;

    @Override
    protected String getSuffixList() {
        return "zip";
    }

    @Override
    protected TestWinZipAesDriver newArchiveDriver() {
        return new TestWinZipAesDriver(IO_POOL_PROVIDER, view);
    }

    @Override
    public void setUp() throws Exception {
        this.view = new MockView<AesPbeParameters>();
        super.setUp();
        final TConfig config = TConfig.get();
        config.setOutputPreferences(config.getOutputPreferences().set(ENCRYPT));
        final AesPbeParameters key = new AesPbeParameters();
        key.setPassword("secret".toCharArray());
        view.setKey(key);
    }
}
