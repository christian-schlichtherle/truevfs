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
import static de.schlichtherle.truezip.fs.FsUriModifier.*;
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
    private static final URI DOT = URI.create(".");
    private static final URI DOT_DOT = URI.create("..");

    private final TArchiveDetector detector;
    private final Splitter splitter = new Splitter(SEPARATOR_CHAR, false);
    private FsPath root;
    private final UriBuilder uri = new UriBuilder(true);

    TScanner(TArchiveDetector detector) {
        assert null != detector;
        this.detector = detector;
    }

    FsPath toFsPath(FsMountPoint parent, URI member) {
        return toFsPath(new FsPath(parent, ROOT), member);
    }

    FsPath toFsPath(FsPath parent, URI member) {
        //assert !member.isAbsolute();
        assert !member.isOpaque();
        member = member.normalize();
        try {
            String memberPath;
            while ((memberPath = member.getRawPath()).startsWith(DOT_DOT_SEPARATOR)) {
                parent = parent(parent);
                member = new UriBuilder(member, true)
                        .path(memberPath.substring(3))
                        .getUri();
            }
            if ("..".equals(memberPath))
                return parent(parent);
            /*if ("".equals(memberPath))
                return parent;*/
            final String memberAuthority = member.getRawAuthority();
            final URI parentUri = parent.toUri();
            if (null != memberAuthority
                    && !parentUri.isOpaque()
                    && null == parentUri.getRawAuthority())
                this.root = new FsPath(
                        new UriBuilder(parentUri, true)
                            .authority(memberAuthority)
                            .getUri());
            else
                this.root = parent;
            this.uri.path(memberPath).query(member.getRawQuery());
            return scan(memberPath);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static FsPath parent(FsPath path)
    throws URISyntaxException {
        while (true) {
            FsMountPoint pmp = path.getMountPoint();
            FsEntryName  pen = path.getEntryName();
            if (pen.isRoot()) {
                if (null == pmp)
                    throw new IllegalArgumentException("An empty path has no parent.");
                path = pmp.getPath();
                if (null == path)
                    return new FsPath(pmp.toUri().resolve(DOT_DOT));
                continue;
            } else {
                pen = new FsEntryName(pen.toUri().resolve(DOT), CANONICALIZE);
                if (pen.isRoot() && null != pmp) {
                    path = pmp.getPath();
                    if (null == path)
                        return new FsPath(pmp.toUri(), CANONICALIZE);
                }
                return new FsPath(pmp, pen);
            }
        }
    }

    private FsPath scan(final String path) throws URISyntaxException {
        splitter.split(path);
        final String parent = splitter.getParentPath();
        final FsEntryName member = FsEntryName
                .create(uri.path(splitter.getMemberName()).getUri());
        final FsPath parentPath = null != parent ? scan(parent) : root;
        URI parentUri;
        FsPath memberPath;
        if (member.isRoot() || (parentUri = parentPath.toUri()).isOpaque()) {
            memberPath = parentPath.resolve(member);
        } else {
            final String parentUriPath = parentUri.getRawPath();
            if (!parentUriPath.endsWith(SEPARATOR))
                parentUri = new UriBuilder(parentUri, true)
                        .path(parentUriPath + SEPARATOR_CHAR)
                        .getUri();
            memberPath = new FsPath(FsMountPoint.create(parentUri), member);
        }
        final FsScheme scheme = detector.getScheme(member.toString());
        if (null != scheme)
            memberPath = new FsPath(    FsMountPoint.create(scheme, memberPath),
                                        ROOT);
        return memberPath;
    }
}
