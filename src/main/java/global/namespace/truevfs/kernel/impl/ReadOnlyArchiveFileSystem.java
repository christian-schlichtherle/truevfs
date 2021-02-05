/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl;

import global.namespace.truevfs.commons.cio.Container;
import global.namespace.truevfs.commons.cio.Entry;
import global.namespace.truevfs.commons.shed.BitField;
import global.namespace.truevfs.kernel.api.FsAccessOption;
import global.namespace.truevfs.kernel.api.FsArchiveEntry;
import global.namespace.truevfs.kernel.api.FsNodeName;
import global.namespace.truevfs.kernel.api.FsReadOnlyFileSystemException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static global.namespace.truevfs.commons.cio.Entry.Access.READ;

/**
 * A read-only virtual file system for archive entries.
 * <p>
 * All modifying methods throw a {@link global.namespace.truevfs.kernel.api.FsReadOnlyFileSystemException}.
 *
 * @param <E> The type of the archive entries.
 * @author Christian Schlichtherle
 */
final class ReadOnlyArchiveFileSystem<E extends FsArchiveEntry> extends ArchiveFileSystem<E> {

    private static final BitField<Entry.Access> READ_ONLY = BitField.of(READ);

    private final Supplier<? extends Throwable> cause;

    ReadOnlyArchiveFileSystem(
            final ArchiveModel<E> model,
            final Container<E> archive,
            final Entry rootTemplate,
            final Supplier<? extends Throwable> cause
    ) throws IOException {
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
    ArchiveFileSystem<E>.Make make(BitField<FsAccessOption> options, FsNodeName name, Entry.Type type, Optional<? extends Entry> template) throws IOException {
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
