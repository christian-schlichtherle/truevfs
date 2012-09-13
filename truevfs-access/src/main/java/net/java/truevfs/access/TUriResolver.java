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
import net.java.truecommons.shed.QuotedUriSyntaxException;
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
final class TUriResolver {
    private static final String DOT_DOT_SEPARATOR = ".." + SEPARATOR_CHAR;

    private final TArchiveDetector detector;
    private final PathSplitter splitter = new PathSplitter(SEPARATOR_CHAR, false);
    private FsNodePath root;
    private String memberQuery;
    private final UriBuilder builder = new UriBuilder();

    /**
     * Constructs a new URI resolver which uses the given
     * {@link TArchiveDetector} to resolve for archive files.
     * 
     * @param detector the archive detector to use for scanning.
     */
    TUriResolver(TArchiveDetector detector) {
        assert null != detector;
        this.detector = detector;
    }

    /**
     * Scans the given {@code uri} for prospective archive files and resolves
     * it against the given {@code base} file system node path.
     * <p>
     * {@code uri} must not be opaque and must not define a fragment component.
     * If it defines a scheme component, it must match the scheme component of
     * the hierarchical URI of {@code base}.
     * If an authority component or an absolute path is present, the authority
     * and path components of {@code base} get discarded.
     * An authority component gets copied to the result.
     * A path component gets normalized and scanned for prospective archive
     * files using the {@link TArchiveDetector} provided to the constructor and
     * rewritten according to the syntax constraints for an {@link FsNodePath}.
     * {@code ".."} segments at the beginning of the normalized path component
     * are resolved against the scheme specific part of {@code base}
     * according to the syntax constraints for file system node paths.
     * No {@code ".."} segments may remain after resolving.
     * A query component is copied to the result.
     * 
     * @param  base the base file system node path for resolving.
     * @param  uri the URI to resolve for prospective archive files.
     * @return the file system node path combined from the given {@code base}
     *         and {@code uri}, possibly decorated as an opaque URI to address
     *         prospective archive files.
     * @throws IllegalArgumentException if any precondition is violated.
     */
    FsNodePath resolve(FsNodePath base, URI uri) {
        if (uri.isAbsolute())
            if (!uri.getScheme().equals(base.getHierarchicalUri().getScheme()))
                throw new IllegalArgumentException();
        try {
            uri = checkAndFix(uri.normalize());
            String path = uri.getPath();
            for (   int max;
                    1 < (max = Math.min(path.length(), DOT_DOT_SEPARATOR.length())) &&
                    DOT_DOT_SEPARATOR.startsWith(path.substring(0, max));
                    ) {
                base = parent(base);
                uri = new UriBuilder(uri)
                        .path(path = path.substring(max))
                        .getUri();
                if (null == base)
                    throw new QuotedUriSyntaxException(uri,
                            "Illegal start of path component");
            }
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
            return resolve(path);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private FsNodePath resolve(final String path) throws URISyntaxException {
        splitter.split(path);
        final String pp = splitter.getParentPath();
        final FsNodeName nn;
        final FsNodePath np;
        if (null != pp) {
            nn = new FsNodeName(
                    builder.path(splitter.getMemberName()).getUri(),
                    NULL);
            np = resolve(pp);
        } else {
            nn = new FsNodeName(
                    builder.path(path).query(memberQuery).getUri(),
                    CANONICALIZE);
            np = root;
        }
        URI npu;
        FsNodePath rnp;
        if (nn.isRoot() || (npu = np.getUri()).isOpaque() || !npu.isAbsolute()) {
            rnp = np.resolve(nn);
        } else {
            final String npup = npu.getPath();
            if (!npup.endsWith(SEPARATOR))
                npu = new UriBuilder(npu).path(npup + SEPARATOR_CHAR).getUri();
            rnp = new FsNodePath(new FsMountPoint(npu), nn);
        }
        final FsScheme s = detector.scheme(nn.toString());
        if (null != s) rnp = new FsNodePath(new FsMountPoint(s, rnp), ROOT);
        return rnp;
    }

    /**
     * Returns the nullable parent of the given file system node path.
     * 
     * @param  path a file system node path.
     * @return The parent file system node path or null if {@code path} does
     *         not name a parent.
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
