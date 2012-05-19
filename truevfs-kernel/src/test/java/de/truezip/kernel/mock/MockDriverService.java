/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.mock;

import de.truezip.kernel.FsDriver;
import de.truezip.kernel.FsScheme;
import de.truezip.kernel.spi.FsDriverService;
import java.util.Map;

/**
 * A service for the dummy driver.
 *
 * @author Christian Schlichtherle
 */
public final class MockDriverService extends FsDriverService {

    private final Map<FsScheme, FsDriver> drivers;

    public MockDriverService(String extensions) {
        this.drivers = newMap(new Object[][] {
            { extensions, new MockDriver() },
        });
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<FsScheme, FsDriver> getDrivers() {
        return drivers;
    }
}
