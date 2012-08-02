/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.kernel.spec.FsEntryName;
import static net.java.truevfs.kernel.spec.FsEntryName.*;
import net.java.truevfs.kernel.spec.FsMountPoint;
import net.java.truevfs.kernel.spec.FsPath;
import net.java.truevfs.kernel.spec.FsScheme;
import static net.java.truevfs.kernel.spec.FsUriModifier.CANONICALIZE;
import static net.java.truevfs.kernel.spec.FsUriModifier.NULL;
import net.java.truecommons.shed.PathSplitter;
import net.java.truecommons.shed.Paths;
import net.java.truecommons.shed.QuotedUriSyntaxException;
import net.java.truecommons.shed.UriBuilder;

/**
 * Scans hierarchical {@link URI}s for prospective archive files with the help
 * of a {@link TArchiveDetector}.
 * <p>
 * Note that this class has no dependencies on other classes in this package,
 * so it could get published as a member of the package
 * {@code net.truevfs.file} instead if required.
 * 
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class TPathScanner {
    static final URI SEPARATOR_URI = URI.create(SEPARATOR);
    static final URI DOT_URI = URI.create(".");
    static final URI DOT_DOT_URI = URI.create("..");
    static final String DOT_DOT_SEPARATOR = ".." + SEPARATOR_CHAR;

    private final TArchiveDetector detector;
    private final PathSplitter splitter = new PathSplitter(SEPARATOR_CHAR, false);
    private FsPath root;
    private String memberQuery;
    private final UriBuilder uri = new UriBuilder();

    /**
     * Constructs a new URI scanner which uses the given
     * {@link TArchiveDetector}.
     * 
     * @param detector the archive detector to use.
     */
    TPathScanner(TArchiveDetector detector) {
        assert null != detector;
        this.detector = detector;
    }

    /**
     * Constructs a new {@link FsPath} from the given {@code parent} and scans
     * the given {@code member} for prospective archive files.
     * <p>
     * {@code member} must not be opaque and must not define a fragment
     * component.
     * A scheme component gets ignored.
     * If an authority component or an absolute path is present, the authority
     * and path components of {@code parent} get discarded.
     * An authority component gets copied to the result.
     * A path component gets normalized and scanned for prospective archive
     * files using the {@link TArchiveDetector} provided to the constructor and
     * rewritten according to the syntax constraints for an {@link FsPath}.
     * {@code ".."} segments at the beginning of the normalized path component
     * are resolved against the scheme specific part of {@code parent}
     * according to the syntax constraints for an {@link FsPath}.
     * A query component is copied to the result.
     * 
     * @param  parent the file system path to use as the parent.
     * @param  member the URI to scan for prospective archive files.
     * @return the file system path combined from the given {@code parent} and
     *         {@code member}, possibly decorated as an opaque URI to address
     *         prospective archive files.
     * @throws IllegalArgumentException if any precondition is violated.
     */
    FsPath scan(FsPath parent, URI member) {
        try {
            member = checkFix(member.normalize());
            String mp;
            while ((mp = member.getPath()).startsWith(DOT_DOT_SEPARATOR)) {
                parent = parent(parent);
                member = new UriBuilder(member)
                        .path(mp.substring(3))
                        .getUri();
            }
            if ("..".equals(mp))
                return parent(parent);
            final int mpl = pathPrefixLength(member);
            if (0 < mpl) {
                final URI pu = parent.toHierarchicalUri().resolve(SEPARATOR_URI);
                final String ma = member.getAuthority();
                final String p = null != ma || mp.startsWith(SEPARATOR)
                        ? mp.substring(0, mpl)
                        : pu.getPath() + mp.substring(0, mpl);
                this.root = new FsPath(
                        new UriBuilder()
                            .scheme(pu.getScheme())
                            .authority(ma)
                            .path(p)
                            .getUri());
                mp = mp.substring(mpl);
            } else {
                this.root = parent;
            }
            this.memberQuery = member.getQuery();
            return scan(mp);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private FsPath scan(final String path) throws URISyntaxException {
        splitter.split(path);
        final String ps = splitter.getParentPath();
        final FsEntryName men;
        final FsPath pp;
        if (null != ps) {
            men = new FsEntryName(
                    uri.path(splitter.getMemberName()).getUri(),
                    NULL);
            pp = scan(ps);
        } else {
            men = new FsEntryName(
                    uri.path(path).query(memberQuery).getUri(),
                    CANONICALIZE);
            pp = root;
        }
        URI ppu;
        FsPath mp;
        if (men.isRoot() || (ppu = pp.toUri()).isOpaque() || !ppu.isAbsolute()) {
            mp = pp.resolve(men);
        } else {
            final String pup = ppu.getPath();
            if (!pup.endsWith(SEPARATOR))
                ppu = new UriBuilder(ppu).path(pup + SEPARATOR_CHAR).getUri();
            mp = new FsPath(new FsMountPoint(ppu), men);
        }
        final FsScheme s = detector.scheme(men.toString());
        if (null != s)
            mp = new FsPath(new FsMountPoint(s, mp), ROOT);
        return mp;
    }

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
     * Returns whether or not the given path is absolute.
     * <p>
     * A path is absolute if it doesn't need to be combined with other path
     * information in order to locate a file.
     *
     * @return Whether or not the given path is absolute.
     */
    static boolean isAbsolute(URI uri) {
        return uri.isAbsolute() || Paths.isAbsolute(
                uri.getSchemeSpecificPart(), SEPARATOR_CHAR);
    }

    /**
     * Returns the parent of the given file system path.
     * 
     * @param  path a file system path.
     * @return The parent file system path.
     * @throws URISyntaxException 
     */
    static @Nullable FsPath parent(FsPath path) throws URISyntaxException {
        FsMountPoint mp = path.getMountPoint();
        FsEntryName  en = path.getEntryName();
        if (en.isRoot()) {
            if (null == mp)
                return null;
            path = mp.getPath();
            if (null != path)
                return parent(path);
            URI mpu = mp.toUri();
            URI pu = mpu.resolve(DOT_DOT_URI);
            if (mpu.getRawPath().length() <= pu.getRawPath().length())
                return null;
            return new FsPath(pu);
        } else {
            URI pu = en.toUri().resolve(DOT_URI);
            en = new FsEntryName(pu, CANONICALIZE);
            return new FsPath(mp, en);
        }
    }
}