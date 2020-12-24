/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.cio.Container;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsArchiveEntry;
import net.java.truevfs.kernel.spec.FsNodeName;
import net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static net.java.truecommons.cio.Entry.Access.READ;

/**
 * A read-only virtual file system for archive entries.
 * <p>
 * All modifying methods throw a {@link net.java.truevfs.kernel.spec.FsReadOnlyFileSystemException}.
 *
 * @param <E> The type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class ReadOnlyArchiveFileSystem<E extends FsArchiveEntry> extends ArchiveFileSystem<E> {

    private static final BitField<Entry.Access> READ_ONLY = BitField.of(READ);

    private final Supplier<? extends Throwable> cause;

    ReadOnlyArchiveFileSystem(
            final ArchiveModel<E> model,
            final Container<E> archive,
            final Entry rootTemplate,
            final Supplier<? extends Throwable> cause
    ) {
        super(model, archive, rootTemplate);
        this.cause = cause;
    }

    @Override
    void checkAccess(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Entry.Access> types
    ) throws IOException {
        if (!types.isEmpty() && READ_ONLY != types) {
            throw newFsReadOnlyFileSystemException();
        }
        super.checkAccess(options, name, types);
    }

    @Override
    void setReadOnly(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
    }

    @Override
    boolean setTime(BitField<FsAccessOption> options, FsNodeName name, Map<Entry.Access, Long> times) throws IOException {
        throw newFsReadOnlyFileSystemException();
    }

    @Override
    boolean setTime(BitField<FsAccessOption> options, FsNodeName name, BitField<Entry.Access> types, long value) throws IOException {
        throw newFsReadOnlyFileSystemException();
    }

    @Override
    ArchiveFileSystem<E>.Make make(BitField<FsAccessOption> options, FsNodeName name, Entry.Type type, Optional<Entry> template) throws IOException {
        throw newFsReadOnlyFileSystemException();
    }

    @Override
    void unlink(BitField<FsAccessOption> options, FsNodeName name) throws IOException {
        throw newFsReadOnlyFileSystemException();
    }

    private FsReadOnlyFileSystemException newFsReadOnlyFileSystemException() {
        return new FsReadOnlyFileSystemException(getMountPoint(), cause.get());
    }
}
