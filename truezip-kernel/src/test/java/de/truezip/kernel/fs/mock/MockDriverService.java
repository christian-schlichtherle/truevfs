/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs.mock;

import de.truezip.kernel.fs.FsDriver;
import de.truezip.kernel.fs.addr.FsScheme;
import de.truezip.kernel.fs.spi.FsDriverService;
import java.util.Map;

/**
 * A service for the dummy driver.
 *
 * @author Christian Schlichtherle
 */
public final class MockDriverService extends FsDriverService {

    private final Map<FsScheme, FsDriver> drivers;

    public MockDriverService(String suffixes) {
        this.drivers = newMap(new Object[][] {
            { suffixes, new MockDriver() },
        });
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<FsScheme, FsDriver> get() {
        return drivers;
    }
}
