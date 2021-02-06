/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.mock;

import global.namespace.truevfs.comp.util.ExtensionSet;
import global.namespace.truevfs.comp.util.HashMaps;
import global.namespace.truevfs.kernel.api.FsDriver;
import global.namespace.truevfs.kernel.api.FsScheme;

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
public final class MockDriverMapProvider implements Supplier<Map<FsScheme, ? extends FsDriver>> {

    private final Map<FsScheme, ? extends FsDriver> drivers;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public MockDriverMapProvider(final String extensions) {
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
