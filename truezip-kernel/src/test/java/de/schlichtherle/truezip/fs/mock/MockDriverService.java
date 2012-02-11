/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.mock;

import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.fs.spi.FsDriverService;
import java.util.Map;

/**
 * A service for the dummy driver.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class MockDriverService extends FsDriverService {

    private final Map<FsScheme, FsDriver> drivers;

    public MockDriverService(String suffixes) {
        this.drivers = newMap(new Object[][] {
            { suffixes, new MockDriver() },
        });
    }

    @Override
    public Map<FsScheme, FsDriver> get() {
        return drivers;
    }
}
