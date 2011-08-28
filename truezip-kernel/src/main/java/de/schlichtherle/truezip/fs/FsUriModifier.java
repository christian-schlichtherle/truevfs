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

import de.schlichtherle.truezip.util.QuotedUriSyntaxException;
import de.schlichtherle.truezip.util.UriBuilder;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.net.URISyntaxException;
import net.jcip.annotations.Immutable;

import static de.schlichtherle.truezip.fs.FsEntryName.*;

/**
 * Modifies a URI when parsing an {@link FsPath}, an {@link FsMountPoint} or an
 * {@link FsEntryName}.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public enum FsUriModifier {

    /**
     * The null modifier does nothing but ensure that the URI path is in normal
     * form.
     */
    NULL {
        @Override
        URI modify(URI uri, PostFix fix) throws URISyntaxException {
            if (uri.normalize() != uri)
                throw new QuotedUriSyntaxException(uri, "URI path not in normal form");
            return uri;
        }
    },

    /**
     * The canonicalize modifier applies a {@link PostFix post-fix} which
     * depends on the class specific URI syntax.
     */
    CANONICALIZE {
        @Override
        URI modify(URI uri, PostFix fix) throws URISyntaxException {
            return fix.modify(uri);
        }
    };

    /**
     * An idempotent function which modifies a URI.
     *
     * @param  uri the URI to modify.
     * @param  fix the post-fix to apply if required.
     * @return the modified URI.
     */
    abstract URI modify(URI uri, PostFix fix) throws URISyntaxException;

    /**
     * Post-fixes a URI when it gets
     * {@link FsUriModifier#CANONICALIZE canonicalized}.
     */
    @Immutable
    public enum PostFix {

        /**
         * The post-fix for an {@link FsPath} depends on the given URI:
         * If the URI is opaque or not absolute or has a fragment component
         * defined, nothing is modified.
         * Otherwise, the following modifications are conducted:
         * <ol>
         * <li>If the URI path component starts with two separators, the
         *     substring following until the next separator is moved to the
         *     authority part of the URI.
         *     This behavior is intended to fix URIs returned by
         *     {@link java.io.File#toURI()}.
         * <li>The URI path component gets truncated so that it does not end
         *     with {@value de.schlichtherle.truezip.entry.EntryName#SEPARATOR}
         *     whereby a trailing separator after a Windows-like drive letter
         *     is preserved.
         * <li>An empty authority component in the scheme specific part gets
         *     truncated.
         * </ol>
         * <p>
         * Note that this fix is not limited to Windows in order to make
         * this function work identically on all platforms.
         */
        PATH {
            @Override
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("ES_COMPARING_STRINGS_WITH_EQ")
            URI modify(URI uri) throws URISyntaxException {
                if (uri.isOpaque()
                        || !uri.isAbsolute()
                        || null != uri.getRawFragment())
                    return uri;
                {
                    String a = uri.getRawAuthority();
                    String p = uri.getRawPath();
                    if (null == a && null != p && p.startsWith(TWO_SEPARATORS)) {
                        int i = p.indexOf(SEPARATOR_CHAR, 2);
                        if (2 <= i) {
                            a = p.substring(2, i);
                            p = p.substring(i);
                        }
                        uri = new UriBuilder(uri, true).authority(a).path(p).getUri();
                    }
                    uri = uri.normalize();
                }
                String s = uri.getScheme();
                String a = uri.getRawAuthority();
                String p = uri.getRawPath(), q = p;
                for (int l; p.endsWith(SEPARATOR)
                        && (   1 <=(l = p.length()) && null == s
                            || 2 <= l && ':' != p.charAt(l - 2)
                            || 3 <= l && !p.startsWith(SEPARATOR)
                            || 4 <  l && p.startsWith(SEPARATOR)
                            || null != a); )
                    p = p.substring(0, l - 1);
                return p != q
                        ? new UriBuilder(uri, true).path(p).getUri()
                        : uri;
            }
        },

        /**
         * The post-fix for an {@link FsMountPoint} just normalizes the given
         * URI.
         */
        MOUNT_POINT {
            @Override
            URI modify(URI uri) {
                return uri.normalize();
            }
        },

        /**
         * The post-fix for an {@link FsEntryName} depends on the given URI:
         * If the URI is absolute or has an authority or a fragment component
         * defined, nothing is modified.
         * Otherwise, the URI path component gets truncated so that it does not
         * start or end with
         * {@value de.schlichtherle.truezip.entry.EntryName#SEPARATOR}.
         */
        ENTRY_NAME {
            @Override
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("ES_COMPARING_STRINGS_WITH_EQ")
            URI modify(URI uri) throws URISyntaxException {
                uri = uri.normalize();
                if (uri.isAbsolute()
                        || null != uri.getRawAuthority()
                        || null != uri.getRawFragment())
                    return uri;
                String p = uri.getRawPath(), q = p;
                while (p.startsWith(SEPARATOR))
                    p = p.substring(1);
                while (p.endsWith(SEPARATOR))
                    p = p.substring(0, p.length() - 1);
                return p == q
                        ? uri
                        : new UriBuilder(uri, true).path(p).getUri();
            }
        };

        /**
         * An idempotent function which modifies the given URI.
         *
         * @param  uri the URI to modify.
         * @return the modified URI.
         */
        abstract URI modify(URI uri) throws URISyntaxException;

        private static final String TWO_SEPARATORS = SEPARATOR + SEPARATOR;
    }
}
