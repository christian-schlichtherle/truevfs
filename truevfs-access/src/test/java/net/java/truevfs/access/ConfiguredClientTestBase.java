/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import net.java.truecommons.services.Factory;
import net.java.truecommons.services.Locator;
import net.java.truecommons.shed.ExtensionSet;
import net.java.truevfs.kernel.spec.FsArchiveDriver;
import net.java.truevfs.kernel.spec.FsArchiveDriverTestBase;
import net.java.truevfs.kernel.spec.FsManager;
import net.java.truevfs.kernel.spec.FsMountPoint;
import net.java.truevfs.kernel.spec.FsScheme;
import net.java.truevfs.kernel.spec.sl.FsManagerLocator;
import net.java.truevfs.kernel.spec.spi.FsManagerDecorator;
import net.java.truevfs.kernel.spec.spi.FsManagerFactory;

/**
 * @param  <D> the type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class ConfiguredClientTestBase<D extends FsArchiveDriver<?>>
extends FsArchiveDriverTestBase<D> {

    protected static final long TIMEOUT_MILLIS = 50;
    protected static final FsMountPoint ROOT_DIRECTORY = FsMountPoint
            .create(URI.create("file:/"));
    protected static final FsMountPoint CURRENT_DIRECTORY = FsMountPoint
            .create(new File("").toURI());
    protected static final String[] NO_STRINGS = new String[0];
    private static final String ARCHIVE_DETECTOR = "archiveDetector";

    private static volatile Factory<FsManager> managerFactory;

    private static FsManager newManager() {
        final Factory<FsManager> f =
                ConfiguredClientTestBase.managerFactory;
        return (null != f ? f : (ConfiguredClientTestBase.managerFactory =
                    new Locator(FsManagerLocator.class)
                       .factory(FsManagerFactory.class, FsManagerDecorator.class))
                ).get();
    }

    private TConfig config;
    private Map<String, ?> environment;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        config = TConfig.open();
        // Using a private file system manager would normally violate the third
        // party access constraints, but in this context it's safe because no
        // two test methods should ever access the same archive file(s) except
        // when performing a sync of all mounted file systems.
        // Mind that a sync should always succeed (unless there's an issue in
        // the parent file system) und must not confuse other threads about the
        // state of the synced archive file(s).
        // So the default value 'false' helps to identify potential isolation
        // issues in case this invariant is not met.
        // See http://truevfs.java.net/truevfs-access/usage.html#Third_Party_Access
        if (ISOLATE_FS_MANAGER) config.setManager(newManager());
        final TArchiveDetector detector = new TArchiveDetector(getExtensionList(), getArchiveDriver());
        environment = Collections.singletonMap(ARCHIVE_DETECTOR, detector);
        config.setDetector(detector);
        config.setLenient(true);
    }

    @Override
    public void tearDown() {
        try {
            config.close();
        } finally {
            super.tearDown();
        }
    }

    protected abstract String getExtensionList();

    protected final FsScheme getScheme() {
        return FsScheme.create(new ExtensionSet(getExtensionList()).iterator().next());
    }

    protected final String getExtension() { return "." + getScheme(); }

    protected final TArchiveDetector getDetector() {
        return config.getDetector();
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    protected final Map<String, ?> getEnvironment() { return environment; }
}
