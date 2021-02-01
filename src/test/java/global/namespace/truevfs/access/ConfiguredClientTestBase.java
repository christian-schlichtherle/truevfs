/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.service.wight.core.ServiceLocator;
import global.namespace.truevfs.comp.shed.ExtensionSet;
import global.namespace.truevfs.kernel.api.*;
import global.namespace.truevfs.kernel.api.spi.FsManagerDecorator;
import global.namespace.truevfs.kernel.api.spi.FsManagerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @param <D> the type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class ConfiguredClientTestBase<D extends FsArchiveDriver<?>>
        extends FsArchiveDriverTestBase<D> {

    /**
     * Controls if each test case should use its own file system manager.
     * If set to {@code true}, then a new file system manager gets created for
     * each test.
     * This is required to run both all test classes and all test methods in
     * parallel.
     * If this property is disabled, then only all test methods can be run
     * in parallel.
     * See configuration of the maven-failsafe-plugin in the top level POM.
     */
    private static final boolean ISOLATE_FS_MANAGER = Boolean.getBoolean(
            ConfiguredClientTestBase.class.getPackage().getName() + ".isolateFsManager");

    static {
        LoggerFactory
                .getLogger(FsArchiveDriverTestBase.class)
                .debug("Isolate file system manager: {}", ISOLATE_FS_MANAGER);
    }

    protected static final long TIMEOUT_MILLIS = 50;
    protected static final FsMountPoint ROOT_DIRECTORY = FsMountPoint
            .create(URI.create("file:/"));
    protected static final FsMountPoint CURRENT_DIRECTORY = FsMountPoint
            .create(new File("").toURI());
    protected static final String[] NO_STRINGS = new String[0];
    private static final String ARCHIVE_DETECTOR = "archiveDetector";

    private static volatile Supplier<FsManager> managerFactory;

    private static FsManager newManager() {
        final Supplier<FsManager> f = ConfiguredClientTestBase.managerFactory;
        return (null != f ? f : (ConfiguredClientTestBase.managerFactory =
                new ServiceLocator().provider(FsManagerFactory.class, FsManagerDecorator.class))).get();
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
        // issues in case this invariant is violated.
        // See http://truevfs.java.net/truevfs-access/usage.html#Third_Party_Access
        if (ISOLATE_FS_MANAGER) {
            config.setManager(newManager());
        }
        final TArchiveDetector detector = new TArchiveDetector(getExtensionList(), Optional.of(getArchiveDriver()));
        environment = Collections.singletonMap(ARCHIVE_DETECTOR, detector);
        config.setArchiveDetector(detector);
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

    protected final String getExtension() {
        return "." + getScheme();
    }

    protected final TConfig getConfig() {
        return config;
    }

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    protected final Map<String, ?> getEnvironment() {
        return environment;
    }

    protected final FsController controller(FsNodePath nodePath) {
        return getConfig()
                .getManager()
                .controller(
                        getConfig().getArchiveDetector(),
                        nodePath.getMountPoint().get());
    }
}
