/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.comp.shed.ExtensionSet;
import global.namespace.truevfs.it.base.FsArchiveDriverTestBase;
import global.namespace.truevfs.kernel.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * @param <D> the type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class ConfiguredClientTestBase<D extends FsArchiveDriver<?>> extends FsArchiveDriverTestBase<D> {

    protected static final long TIMEOUT_MILLIS = 50;
    protected static final FsMountPoint ROOT_DIRECTORY = FsMountPoint.create(URI.create("file:/"));
    protected static final FsMountPoint CURRENT_DIRECTORY = FsMountPoint.create(new File("").toURI());
    protected static final String[] NO_STRINGS = new String[0];
    private static final String ARCHIVE_DETECTOR = "archiveDetector";

    private TConfig config;
    private Map<String, ?> environment;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        config = TConfig.open();
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
        return getConfig().getManager().controller(getConfig().getArchiveDetector(), nodePath.getMountPoint().get());
    }
}
