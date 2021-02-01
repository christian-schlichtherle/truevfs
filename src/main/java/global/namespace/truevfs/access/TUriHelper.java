/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access;

import global.namespace.truevfs.comp.shed.Paths;
import global.namespace.truevfs.comp.shed.QuotedUriSyntaxException;
import global.namespace.truevfs.comp.shed.UriBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import static global.namespace.truevfs.kernel.api.FsNodeName.SEPARATOR;
import static global.namespace.truevfs.kernel.api.FsNodeName.SEPARATOR_CHAR;

/**
 * Utility functions for {@link URI}s which represent file system path names.
 *
 * @author Christian Schlichtherle
 */
final class TUriHelper {
    static final URI SEPARATOR_URI = URI.create(SEPARATOR);
    static final URI DOT_URI = URI.create(".");
    static final URI DOT_DOT_URI = URI.create("..");

    static int pathPrefixLength(final URI uri) {
        final String ssp = uri.getSchemeSpecificPart();
        final String a = uri.getAuthority();
        final int al = null == a ? 0 : 2 + a.length();
        final int pl = Paths.prefixLength(ssp, SEPARATOR_CHAR, true) - al;
        return pl >= 0 ? pl : Paths.prefixLength(uri.getPath(), SEPARATOR_CHAR, false);
    }

    static URI check(final URI uri) throws URISyntaxException {
        if (null != uri.getFragment())
            throw new QuotedUriSyntaxException(uri, "Fragment component defined");
        return uri;
    }

    /**
     * Eventually recreates the given URI to work around
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7198297">http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7198297</a>:
     * <pre>
     * {@code assert null == new URI("x/").resolve("..").getSchemeSpecificPart();}
     * </pre>
     *
     * @param  uri the URI to fix.
     * @return A fixed URI or {@code uri} if it doesn't need fixing.
     */
    static URI fix(final URI uri) {
        final String ssp = uri.getSchemeSpecificPart();
        final String a = uri.getAuthority();
        // Workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7198297 :
        // assert null == new URI("foo/").resolve(new URI("..")).getRawSchemeSpecificPart();
        if (null == ssp
                || null == a && ssp.startsWith(SEPARATOR + SEPARATOR)) // empty authority
            return new UriBuilder().uri(uri).toUriUnchecked();
        return uri;
    }

    /**
     * Returns whether or not the given {@code uri} has an absolute path.
     * A URI has an absolute path if it doesn't need to be combined with
     * other path information in order to locate a file.
     *
     * @param  uri the URI to test.
     * @return Whether or not the given URI has an absolute path.
     */
    static boolean hasAbsolutePath(URI uri) {
        return !uri.isOpaque() && Paths.isAbsolute(
                uri.getSchemeSpecificPart(), SEPARATOR_CHAR);
    }

    private TUriHelper() { }
}
