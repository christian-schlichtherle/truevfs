/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se;

import de.schlichtherle.truevfs.kernel.se.ArchiveManager;
import net.truevfs.kernel.FsManager;
import net.truevfs.kernel.FsManagerTestSuite;
import net.truevfs.kernel.util.Link.Type;

/**
 * @author Christian Schlichtherle
 */
public class ArchiveManagerTest extends FsManagerTestSuite {

    @Override
    protected FsManager newManager(Type type) {
        return new ArchiveManager(type);
    }
}
