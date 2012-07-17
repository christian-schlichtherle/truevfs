/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.driver.mock;

import de.schlichtherle.truecommons.services.Container;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.truevfs.kernel.spec.FsDriver;
import net.truevfs.kernel.spec.FsScheme;
import net.truevfs.kernel.spec.util.ExtensionSet;
import net.truevfs.kernel.spec.util.HashMaps;

/**
 * Creates maps with a mock driver.
 *
 * @author Christian Schlichtherle
 */
public final class MockDriverMapContainer
implements Container<Map<FsScheme, FsDriver>> {
    private final Map<FsScheme, FsDriver> drivers;

    public MockDriverMapContainer(final String extensions) {
        final Set<String> set = new ExtensionSet(extensions);
        final Map<FsScheme, FsDriver> map = new LinkedHashMap<>(HashMaps.initialCapacity(set.size()));
        final FsDriver driver = new MockDriver();
        for (final String extension : set)
            map.put(FsScheme.create(extension), driver);
        drivers = Collections.unmodifiableMap(map);
    }

    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Map<FsScheme, FsDriver> apply() {
        return drivers;
    }
}
