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
import de.schlichtherle.truezip.util.SuffixSet;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.logging.Level.*;
import java.util.logging.Logger;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TFileSystemProvider extends FileSystemProvider {

    private static final int NUM_PROVIDERS = 30; // match number of SCHEMEXX classes here!
    private static final String[] SCHEMES;
    static {
        final Logger logger = Logger.getLogger(
                TFileSystemProvider.class.getName(),
                TFileSystemProvider.class.getName());
        final SuffixSet set = new SuffixSet(TArchiveDetector.ALL.toString());
        final int total = set.size();
        final List<String> subset = new ArrayList<>(set)
                .subList(0, Math.min(total, NUM_PROVIDERS));
        SCHEMES = subset.toArray(new String[subset.size()]);
        final boolean changed;
        if (total < NUM_PROVIDERS) {
            changed = set.retainAll(subset.subList(0, NUM_PROVIDERS % total));
            assert changed;
            logger.log(CONFIG, "too_few", new Object[] { total, NUM_PROVIDERS, set.toString() });
        } else if (NUM_PROVIDERS < total) {
            changed = set.removeAll(subset);
            assert changed;
            logger.log(WARNING, "too_many", new Object[] { total, NUM_PROVIDERS, set.toString() });
        }
    }

    private final String scheme;

    TFileSystemProvider(final int scheme) {
        this.scheme = SCHEMES[scheme % SCHEMES.length];
    }

    protected TFileSystemProvider(final String scheme) {
        if (null == TArchiveDetector.ALL.getScheme("." + scheme))
            throw new IllegalArgumentException();
        this.scheme = scheme;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        return TFileSystem.SINGLETON;
    }

    @Override
    public Path getPath(URI uri) {
        return new TPath(uri);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static class SCHEME00 extends TFileSystemProvider {
        public SCHEME00() { super(0); }
    }

    public static class SCHEME01 extends TFileSystemProvider {
        public SCHEME01() { super(1); }
    }

    public static class SCHEME02 extends TFileSystemProvider {
        public SCHEME02() { super(2); }
    }

    public static class SCHEME03 extends TFileSystemProvider {
        public SCHEME03() { super(3); }
    }

    public static class SCHEME04 extends TFileSystemProvider {
        public SCHEME04() { super(4); }
    }

    public static class SCHEME05 extends TFileSystemProvider {
        public SCHEME05() { super(5); }
    }

    public static class SCHEME06 extends TFileSystemProvider {
        public SCHEME06() { super(6); }
    }

    public static class SCHEME07 extends TFileSystemProvider {
        public SCHEME07() { super(7); }
    }

    public static class SCHEME08 extends TFileSystemProvider {
        public SCHEME08() { super(8); }
    }

    public static class SCHEME09 extends TFileSystemProvider {
        public SCHEME09() { super(9); }
    }

    public static class SCHEME10 extends TFileSystemProvider {
        public SCHEME10() { super(10); }
    }

    public static class SCHEME11 extends TFileSystemProvider {
        public SCHEME11() { super(11); }
    }

    public static class SCHEME12 extends TFileSystemProvider {
        public SCHEME12() { super(12); }
    }

    public static class SCHEME13 extends TFileSystemProvider {
        public SCHEME13() { super(13); }
    }

    public static class SCHEME14 extends TFileSystemProvider {
        public SCHEME14() { super(14); }
    }

    public static class SCHEME15 extends TFileSystemProvider {
        public SCHEME15() { super(15); }
    }

    public static class SCHEME16 extends TFileSystemProvider {
        public SCHEME16() { super(16); }
    }

    public static class SCHEME17 extends TFileSystemProvider {
        public SCHEME17() { super(17); }
    }

    public static class SCHEME18 extends TFileSystemProvider {
        public SCHEME18() { super(18); }
    }

    public static class SCHEME19 extends TFileSystemProvider {
        public SCHEME19() { super(19); }
    }

    public static class SCHEME20 extends TFileSystemProvider {
        public SCHEME20() { super(20); }
    }

    public static class SCHEME21 extends TFileSystemProvider {
        public SCHEME21() { super(21); }
    }

    public static class SCHEME22 extends TFileSystemProvider {
        public SCHEME22() { super(22); }
    }

    public static class SCHEME23 extends TFileSystemProvider {
        public SCHEME23() { super(23); }
    }

    public static class SCHEME24 extends TFileSystemProvider {
        public SCHEME24() { super(24); }
    }

    public static class SCHEME25 extends TFileSystemProvider {
        public SCHEME25() { super(25); }
    }

    public static class SCHEME26 extends TFileSystemProvider {
        public SCHEME26() { super(26); }
    }

    public static class SCHEME27 extends TFileSystemProvider {
        public SCHEME27() { super(27); }
    }

    public static class SCHEME28 extends TFileSystemProvider {
        public SCHEME28() { super(28); }
    }

    public static class SCHEME29 extends TFileSystemProvider {
        public SCHEME29() { super(29); }
    }
}
