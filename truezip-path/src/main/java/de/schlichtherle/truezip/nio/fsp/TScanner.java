/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.nio.fsp;

import de.schlichtherle.truezip.entry.EntryName;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.util.UriBuilder;
import java.net.URI;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class TScanner {
    private final FsPath root;
    private final TArchiveDetector detector;
    private final Splitter splitter = new Splitter(EntryName.SEPARATOR_CHAR, false);
    private final UriBuilder uri = new UriBuilder();

    TScanner(FsMountPoint root, TArchiveDetector detector) {
        this(new FsPath(root, ROOT), detector);
    }

    TScanner(FsPath parent, TArchiveDetector detector) {
        assert null != parent;
        assert null != detector;
        this.root = parent;
        this.detector = detector;
    }

    FsPath toPath(URI uri) {
        assert !uri.isOpaque();
        uri = uri.normalize();
        final String path = uri.getPath();
        this.uri.path(path).query(uri.getQuery());
        return scan(path);
    }

    private FsPath scan(final String input) {
        splitter.split(input);
        final String parent = splitter.getParentPath();
        final FsEntryName member = FsEntryName
                .create(uri.path(splitter.getMemberName()).toUri());
        FsPath path = (null != parent ? scan(parent) : root).resolve(member);
        final FsScheme scheme = detector.getScheme(member.toString());
        if (null != scheme)
            path = new FsPath(FsMountPoint.create(scheme, path), ROOT);
        return path;
    }
}
