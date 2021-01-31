/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec.mock;

import net.java.truecommons.shed.ExtensionSet;
import net.java.truecommons.shed.HashMaps;
import net.java.truevfs.kernel.spec.FsDriver;
import net.java.truevfs.kernel.spec.FsScheme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Creates maps with a mock driver.
 *
 * @author Christian Schlichtherle
 */
public final class MockDriverMapContainer implements Supplier<Map<FsScheme, ? extends FsDriver>> {

    private final Map<FsScheme, ? extends FsDriver> drivers;

    public MockDriverMapContainer(final String extensions) {
        final Set<String> set = new ExtensionSet(extensions);
        final Map<FsScheme, FsDriver> map = new LinkedHashMap<>(HashMaps.initialCapacity(set.size()));
        final FsDriver driver = new MockDriver();
        set.forEach(extension -> map.put(FsScheme.create(extension), driver));
        drivers = Collections.unmodifiableMap(map);
    }

    @Override
    public Map<FsScheme, ? extends FsDriver> get() {
        return drivers;
    }
}
