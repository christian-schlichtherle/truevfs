/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.mock;

import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriverTestSuite;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MockArchiveDriverTest
extends FsCharsetArchiveDriverTestSuite<MockArchiveDriverEntry, MockArchiveDriver> {

    @Override
    protected MockArchiveDriver newArchiveDriver() {
        return new MockArchiveDriver(getTestConfig());
    }

    @Override
    protected String getUnencodableName() {
        return null;
    }
}
