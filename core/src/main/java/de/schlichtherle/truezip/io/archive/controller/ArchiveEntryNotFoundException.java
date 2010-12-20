/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.filesystem.concurrent.ConcurrentFileSystemModel;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.io.filesystem.MountPoint;
import de.schlichtherle.truezip.io.filesystem.Path;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Indicates that an <i>archive entry</i> does not exist or is not accessible.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ArchiveEntryNotFoundException extends FileNotFoundException {

    private static final long serialVersionUID = 2972350932856838564L;

    private final Path path;

    ArchiveEntryNotFoundException(
            final ConcurrentFileSystemModel model,
            final FileSystemEntryName name,
            final String msg) {
        super(msg);
        assert name != null;
        assert msg != null;
        this.path = model.resolvePath(name);
    }

    ArchiveEntryNotFoundException(
            final ConcurrentFileSystemModel model,
            final FileSystemEntryName name,
            final IOException cause) {
        super(cause == null ? null : cause.toString());
        assert name != null;
        super.initCause(cause);
        this.path = model.resolvePath(name);
    }

    @Override
    public String getLocalizedMessage() {
        final String msg = getMessage();
        return msg != null
                ? new StringBuilder(path.toString()).append(" (").append(msg).append(")").toString()
                : path.toString();
    }
}
