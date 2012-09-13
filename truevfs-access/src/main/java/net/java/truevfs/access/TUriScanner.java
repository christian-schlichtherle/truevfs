/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.shed.PathSplitter;
import net.java.truecommons.shed.UriBuilder;
import static net.java.truevfs.access.TUriHelper.*;
import net.java.truevfs.kernel.spec.FsMountPoint;
import net.java.truevfs.kernel.spec.FsNodeName;
import static net.java.truevfs.kernel.spec.FsNodeName.*;
import net.java.truevfs.kernel.spec.FsNodePath;
import net.java.truevfs.kernel.spec.FsScheme;
import static net.java.truevfs.kernel.spec.FsUriModifier.*;

/**
 * Scans {@link URI}s for prospective archive files and resolves them against
 * base {@link FsNodePath}s.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class TUriScanner {
    private static final String DOT_DOT_SEPARATOR = ".." + SEPARATOR_CHAR;

    private final TArchiveDetector detector;
    private final PathSplitter splitter = new PathSplitter(SEPARATOR_CHAR, false);
    private FsNodePath root;
    private String memberQuery;
    private final UriBuilder uri = new UriBuilder();

    /**
     * Constructs a new URI scanner which uses the given
     * {@link TArchiveDetector}.
     * 
     * @param detector the archive detector to use.
     */
    TUriScanner(TArchiveDetector detector) {
        assert null != detector;
        this.detector = detector;
    }

    /**
     * Scans the given {@code uri} for prospective archive files and resolves
     * it against the given {@code base} file system node path.
     * <p>
     * {@code uri} must not be opaque and must not define a fragment component.
     * A scheme component gets ignored.
     * If an authority component or an absolute path is present, the authority
     * and path components of {@code base} get discarded.
     * An authority component gets copied to the result.
     * A path component gets normalized and scanned for prospective archive
     * files using the {@link TArchiveDetector} provided to the constructor and
     * rewritten according to the syntax constraints for an {@link FsNodePath}.
     * {@code ".."} segments at the beginning of the normalized path component
     * are resolved against the scheme specific part of {@code base}
     * according to the syntax constraints for file system node paths.
     * A query component is copied to the result.
     * 
     * @param  base the base file system node path for resolving.
     * @param  uri the URI to scan for prospective archive files.
     * @return the file system node path combined from the given {@code base}
     *         and {@code uri}, possibly decorated as an opaque URI to address
     *         prospective archive files.
     * @throws IllegalArgumentException if any precondition is violated.
     */
    FsNodePath scan(FsNodePath base, URI uri) {
        try {
            uri = checkAndFix(uri.normalize());
            String path;
            while ((path = uri.getPath()).startsWith(DOT_DOT_SEPARATOR)) {
                base = parent(base);
                uri = new UriBuilder(uri)
                        .path(path.substring(3))
                        .getUri();
            }
            if ("..".equals(path)) return parent(base);
            final int ppl = pathPrefixLength(uri);
            if (0 < ppl) {
                final URI baseUri = base.getHierarchicalUri().resolve(SEPARATOR_URI);
                final String authority = uri.getAuthority();
                final String rootPath = null != authority || path.startsWith(SEPARATOR)
                        ? path.substring(0, ppl)
                        : baseUri.getPath() + path.substring(0, ppl);
                root = new FsNodePath(
                        new UriBuilder()
                            .scheme(baseUri.getScheme())
                            .authority(authority)
                            .path(rootPath)
                            .getUri());
                path = path.substring(ppl);
            } else {
                root = base;
            }
            memberQuery = uri.getQuery();
            return scan(path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private FsNodePath scan(final String path) throws URISyntaxException {
        splitter.split(path);
        final String ps = splitter.getParentPath();
        final FsNodeName men;
        final FsNodePath pp;
        if (null != ps) {
            men = new FsNodeName(
                    uri.path(splitter.getMemberName()).getUri(),
                    NULL);
            pp = scan(ps);
        } else {
            men = new FsNodeName(
                    uri.path(path).query(memberQuery).getUri(),
                    CANONICALIZE);
            pp = root;
        }
        URI ppu;
        FsNodePath mp;
        if (men.isRoot() || (ppu = pp.getUri()).isOpaque() || !ppu.isAbsolute()) {
            mp = pp.resolve(men);
        } else {
            final String pup = ppu.getPath();
            if (!pup.endsWith(SEPARATOR))
                ppu = new UriBuilder(ppu).path(pup + SEPARATOR_CHAR).getUri();
            mp = new FsNodePath(new FsMountPoint(ppu), men);
        }
        final FsScheme s = detector.scheme(men.toString());
        if (null != s) mp = new FsNodePath(new FsMountPoint(s, mp), ROOT);
        return mp;
    }

    /**
     * Returns the parent of the given file system node path.
     * 
     * @param  path a file system node path.
     * @return The parent file system node path.
     * @throws URISyntaxException 
     */
    static @Nullable FsNodePath parent(FsNodePath path)
    throws URISyntaxException {
        FsMountPoint mp = path.getMountPoint();
        FsNodeName  en = path.getNodeName();
        if (en.isRoot()) {
            if (null == mp) return null;
            path = mp.getPath();
            if (null != path) return parent(path);
            URI mpu = mp.getUri();
            URI pu = mpu.resolve(DOT_DOT_URI);
            if (mpu.getRawPath().length() <= pu.getRawPath().length())
                return null;
            return new FsNodePath(pu);
        } else {
            URI pu = en.getUri().resolve(DOT_URI);
            en = new FsNodeName(pu, CANONICALIZE);
            return new FsNodePath(mp, en);
        }
    }
}
