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

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.io.Paths.Splitter;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class TScanner {
    private static final String DOT_DOT_SEPARATOR = ".." + SEPARATOR_CHAR;
    private static final URI DOT_DOT = URI.create("..");

    private final TArchiveDetector detector;
    private final Splitter splitter = new Splitter(SEPARATOR_CHAR, false);
    private FsPath root;
    private final UriBuilder uri = new UriBuilder(true);

    TScanner(TArchiveDetector detector) {
        assert null != detector;
        this.detector = detector;
    }

    FsPath toPath(URI parent, URI member) {
        assert !parent.isOpaque();
        assert parent == parent.normalize();
        assert !member.isOpaque();
        member = member.normalize();
        try {
            while (member.getRawPath().startsWith(DOT_DOT_SEPARATOR)) {
                parent = parent.resolve(DOT_DOT);
                member = new UriBuilder(member, true).path(member.getRawPath().substring(3)).getUri();
            }
            if ("..".equals(member.getRawPath())) {
                parent = parent.resolve(DOT_DOT);
                member = new UriBuilder(member, true).path(null).getUri();
            }
            final String authority = member.getRawAuthority();
            final String path = member.getRawPath();
            if (null != authority && null == parent.getRawAuthority())
                this.root = FsPath.create(new UriBuilder(parent, true).authority(authority).getUri());
            else
                this.root = FsPath.create(parent);
            this.uri.path(path).query(member.getRawQuery());
            return scan(path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
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
