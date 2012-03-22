/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.util.Link.Type;

/**
 * @author  Christian Schlichtherle
 */
public class FsDefaultManagerTest extends FsManagerTestSuite {

    @Override
    protected FsManager newManager(Type type) {
        return new FsDefaultManager(type);
    }
}