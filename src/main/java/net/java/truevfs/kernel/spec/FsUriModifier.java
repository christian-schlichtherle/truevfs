/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import net.java.truecommons.shed.QuotedUriSyntaxException;
import net.java.truecommons.shed.UriBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import static net.java.truevfs.kernel.spec.FsNodeName.SEPARATOR;
import static net.java.truevfs.kernel.spec.FsNodeName.SEPARATOR_CHAR;

/**
 * Modifies a URI when parsing a file system {@linkplain FsNodePath node path},
 * a {@linkplain FsMountPoint mount point} or an
 * {@linkplain FsNodeName node name}.
 *
 * @author Christian Schlichtherle
 */
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
     * @param uri the URI to modify.
     * @param fix the post-fix to apply if required.
     * @return the modified URI.
     */
    abstract URI modify(URI uri, PostFix fix) throws URISyntaxException;

    /**
     * Post-fixes a URI when it gets
     * {@link FsUriModifier#CANONICALIZE canonicalized}.
     */
    public enum PostFix {

        /**
         * The post-fix for an {@link FsNodePath} depends on the given URI:
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
         *     with {@link FsNodeName#SEPARATOR} whereby a trailing separator
         *     after a Windows-like drive letter is preserved.
         * <li>An empty authority component in the scheme specific part gets
         *     truncated.
         * </ol>
         * <p>
         * Note that this fix is not limited to Windows in order to make
         * this function work identically on all platforms.
         */
        NODE_PATH {
            @Override
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
                        uri = new UriBuilder(true).uri(uri).authority(a).path(p).toUriChecked();
                    }
                    uri = uri.normalize();
                }
                String s = uri.getScheme();
                String a = uri.getRawAuthority();
                String p = uri.getRawPath(), q = p;
                for (int l; p.endsWith(SEPARATOR)
                        && (1 <= (l = p.length()) && null == s
                        || 2 <= l && ':' != p.charAt(l - 2)
                        || 3 <= l && !p.startsWith(SEPARATOR)
                        || 4 < l && p.startsWith(SEPARATOR)
                        || null != a); )
                    p = p.substring(0, l - 1);
                return p == q ? uri : new UriBuilder(true).uri(uri).path(p).toUriChecked();
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
         * The post-fix for an {@link FsNodeName} depends on the given URI:
         * If the URI is absolute or has an authority or a fragment component
         * defined, nothing is modified.
         * Otherwise, the URI path component gets truncated so that it does not
         * start or end with {@link FsNodeName#SEPARATOR}.
         */
        NODE_NAME {
            @Override
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
                return p == q ? uri : new UriBuilder(true).uri(uri).path(p).toUriChecked();
            }
        };

        /**
         * An idempotent function which modifies the given URI.
         *
         * @param uri the URI to modify.
         * @return the modified URI.
         */
        abstract URI modify(URI uri) throws URISyntaxException;

        private static final String TWO_SEPARATORS = SEPARATOR + SEPARATOR;
    }
}
