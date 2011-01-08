/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.net.URISyntaxException;

import static de.schlichtherle.truezip.io.entry.EntryName.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public enum FsUriModifier {

    NONE {
        @Override
        URI modify(URI uri, PostFix fix) throws URISyntaxException {
            if (uri.normalize() != uri)
                throw new URISyntaxException("\"" + uri + "\"", "URI path not in normal form");
            return uri;
        }
    },

    NORMALIZE {
        @Override
        URI modify(URI uri, PostFix fix) throws URISyntaxException {
            return fix.modify(uri.normalize());
        }
    };

    abstract @NonNull URI modify(@NonNull URI uri, @NonNull PostFix fix)
    throws URISyntaxException;

    enum PostFix {
        ENTRY_NAME {
            @Override
            URI modify(URI uri) {
                return uri;
            }
        },

        MOUNT_POINT {
            @Override
            URI modify(URI uri) {
                return uri;
            }
        },

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
        };

        abstract @NonNull URI modify(@NonNull URI uri)
        throws URISyntaxException;
    }
}
