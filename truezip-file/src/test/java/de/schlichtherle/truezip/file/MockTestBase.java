/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.archive.mock.MockArchiveDriver;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class MockTestBase extends TestBase<MockArchiveDriver> {

    @Override
    protected String getSuffixList() {
        return "mok|mok1|mok2";
    }

    @Override
    protected MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver();
    }
}
