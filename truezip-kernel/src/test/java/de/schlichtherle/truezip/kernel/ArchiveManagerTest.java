/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsManager;
import de.truezip.kernel.FsManagerTestSuite;
import de.truezip.kernel.util.Link.Type;

/**
 * @author Christian Schlichtherle
 */
public class ArchiveManagerTest extends FsManagerTestSuite {

    @Override
    protected FsManager newManager(Type type) {
        return new ArchiveManager(type);
    }
}