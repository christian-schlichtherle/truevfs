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
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import de.schlichtherle.truezip.util.SuffixSet;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.junit.After;
import org.junit.Before;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class TestBase<D extends FsArchiveDriver<?>> {

    private @Nullable D driver;
    private @Nullable TArchiveDetector detector;

    protected abstract String getSuffixList();

    protected FsScheme getScheme() {
        return FsScheme.create(new SuffixSet(getSuffixList()).iterator().next());
    }

    protected final String getSuffix() {
        return "." + getScheme();
    }

    protected abstract D newArchiveDriver();

    protected final @Nullable D getArchiveDriver() {
        return driver;
    }

    protected final @Nullable TArchiveDetector getArchiveDetector() {
        return detector;
    }

    @Before
    public void setUp() throws Exception {
        final D driver = newArchiveDriver();
        final TArchiveDetector detector = new TArchiveDetector(
                getSuffixList(), driver);
        final TConfig config = TConfig.push();
        config.setLenient(true);
        config.setArchiveDetector(detector);
        this.driver = driver;
        this.detector = detector;
    }

    @After
    public void tearDown() throws Exception {
        TConfig.pop();
    }
}
