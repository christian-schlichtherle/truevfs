/*
 * Copyright (c) 2012 Schlichtherle IT Services.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Schlichtherle IT Services - initial API and implementation and/or initial documentation
 */
package net.java.truevfs.access;

import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truecommons.shed.Paths;
import net.java.truecommons.shed.QuotedUriSyntaxException;
import net.java.truecommons.shed.UriBuilder;
import static net.java.truevfs.kernel.spec.FsNodeName.*;

/**
 * Utility methods for {@link URI}s which represent file system path names.
 * 
 * @author Christian Schlichtherle
 */
@ThreadSafe
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

    static URI checkFix(final URI uri) throws URISyntaxException {
        if (uri.isOpaque())
            throw new QuotedUriSyntaxException(uri, "Opaque URI");
        if (null != uri.getFragment())
            throw new QuotedUriSyntaxException(uri, "Fragment component defined");
        return fixChecked(uri);
    }

    static URI fixUnchecked(final URI uri) {
        return uri.isOpaque() ? uri : fixChecked(uri);
    }

    private static URI fixChecked(final URI uri) {
        final String ssp = uri.getSchemeSpecificPart();
        final String a = uri.getAuthority();
        if (null == ssp // URI bug: null == new URI("foo").resolve(new URI("..")).getRawSchemeSpecificPart()
                || null == a && ssp.startsWith(SEPARATOR + SEPARATOR)) // empty authority
            return new UriBuilder(uri).toUri();
        return uri;
    }

    /**
     * Returns whether or not the given {@code uri} represents an absolute path.
     * A URI represents an absolute path if it doesn't need to be combined with
     * other path information in order to locate a file.
     *
     * @param  uri the URI to check.
     * @return Whether or not the given URI represents an absolute path.
     */
    static boolean isAbsolutePath(URI uri) {
        return uri.isAbsolute() || Paths.isAbsolute(
                uri.getSchemeSpecificPart(), SEPARATOR_CHAR);
    }

    private TUriHelper() { }
}
