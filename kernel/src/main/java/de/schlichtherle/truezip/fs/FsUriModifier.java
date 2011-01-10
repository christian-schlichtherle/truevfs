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
package de.schlichtherle.truezip.fs;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.entry.EntryName.*;

/**
 * Modifies a URI when parsing an {@link FsPath}, an {@link FsMountPoint} or an
 * {@link FsEntryName}.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public enum FsUriModifier {

    /** The null modifier does nothing but ensure that the URI is normalized. */
    NULL {
        @Override
        URI modify(URI uri, PostFix fix) throws URISyntaxException {
            if (uri.normalize() != uri)
                throw new URISyntaxException("\"" + uri + "\"", "URI path not in normal form");
            return uri;
        }
    },

    /**
     * The canonicalize modifier normalizes a URI and applies a
     * {@link PostFix post-fix} which depends on the class to parse.
     */
    CANONICALIZE {
        @Override
        URI modify(URI uri, PostFix fix) throws URISyntaxException {
            return fix.modify(uri.normalize());
        }
    };

    abstract @NonNull URI modify(@NonNull URI uri, @NonNull PostFix fix)
    throws URISyntaxException;

    /**
     * Post-fixes a URI when it gets
     * {@link FsUriModifier#CANONICALIZE canonicalized}.
     */
    @Immutable
    public enum PostFix {

        /**
         * The post-fix for an {@link FsPath} depends on the URI type.
         * For an opaque URI, nothing is modified.
         * For a hierarchical URI, its path is truncated so that it does not
         * end with a
         * {@value de.schlichtherle.truezip.entry.EntryName#SEPARATOR}
         * separator.
         * In addition, if the path starts with two separators, the substring
         * following until the next separator is moved to the authority part
         * of the URI.
         * This behavior is intended to fix URIs returned by
         * {@link java.io.File#toURI()}.
         */
        PATH {
            @Override
            URI modify(URI uri) throws URISyntaxException {
                if (uri.isOpaque())
                    return uri;

                // Note that we do not limit these fixes to Windows only in order
                // to make this function work identically on all platforms!

                // Move Windows-like UNC host from path to authority.
                if (uri.getRawPath().startsWith(SEPARATOR + SEPARATOR)) {
                    final String s = uri.getPath();
                    final int i = s.indexOf(SEPARATOR_CHAR, 2);
                    if (0 <= i) {
                        uri = new URI(  uri.getScheme(),
                                        s.substring(2, i),
                                        s.substring(i),
                                        uri.getQuery(),
                                        uri.getFragment());
                    }
                }

                // Delete trailing slash separator from directory URI.
                for (String s; (s = uri.getPath()).endsWith(SEPARATOR)
                        && 2 <= s.length()
                        && (':' != s.charAt(s.length() - 2));) {
                    uri = new URI(  uri.getScheme(),
                                    uri.getAuthority(),
                                    s.substring(0, s.length() - 1),
                                    uri.getQuery(),
                                    uri.getFragment());
                }

                return uri;
            }
        },

        /** The post-fix for an {@link FsMountPoint} does nothing. */
        MOUNT_POINT {
            @Override
            URI modify(URI uri) {
                return uri;
            }
        },

        /** The post-fix for an {@link FsEntryName} does nothing. */
        ENTRY_NAME {
            @Override
            URI modify(URI uri) {
                return uri;
            }
        };

        abstract @NonNull URI modify(@NonNull URI uri)
        throws URISyntaxException;
    }
}
