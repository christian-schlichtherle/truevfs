/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsFederationManagerTest extends FsManagerTestSuite {

    @Override
    protected FsManager newManager() {
        return new FsDefaultManager();
    }
}
