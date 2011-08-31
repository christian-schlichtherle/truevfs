/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import java.io.File;
import static de.schlichtherle.truezip.nio.file.TFileSystemProvider.Parameter.*;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class TestBase<D extends FsArchiveDriver<?>>
extends de.schlichtherle.truezip.file.TestBase<D> {

    protected static final FsMountPoint
            ROOT_DIRECTORY = FsMountPoint.create(URI.create("file:/"));
    protected static final FsMountPoint
            CURRENT_DIRECTORY = FsMountPoint.create(new File("").toURI());
    protected static final String[] NO_MORE = new String[0];

    private @Nullable Map<String, ?> environment;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(ARCHIVE_DETECTOR, super.getArchiveDetector());
        this.environment = map;
    }

    protected final @Nullable Map<String, ?> getEnvironment() {
        return environment;
    }
}
