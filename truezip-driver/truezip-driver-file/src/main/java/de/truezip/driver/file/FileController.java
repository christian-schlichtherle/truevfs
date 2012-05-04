/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file;

import static de.truezip.kernel.FsAccessOption.EXCLUSIVE;
import de.truezip.kernel.*;
import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import static de.truezip.kernel.cio.Entry.Access.*;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import static java.nio.file.Files.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A file system controller with a prospective directory in the platform file
 * system as its mount point.
 *
 * @author Christian Schlichtherle
 */
@Immutable
final class FileController extends FsModelController<FsModel>  {

    private final Path target;

    FileController(final FsModel model) {
        super(model);
        if (null != model.getParent())
            throw new IllegalArgumentException();
        this.target = Paths.get(model.getMountPoint().toUri());
    }

    private BasicFileAttributeView getBasicFileAttributeView(Path file) {
        BasicFileAttributeView view = getFileAttributeView(
                file, BasicFileAttributeView.class);
        assert null != view;
        return view;
    }

    @Override
    public FsController<?> getParent() {
        return null;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return false;
    }

    @Override
    public FileEntry stat(
            final BitField<FsAccessOption> options, final FsEntryName name)
    throws IOException {
        final FileEntry entry = new FileEntry(target, name);
        return exists(entry.getPath()) ? entry : null;
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options, final FsEntryName name, final BitField<Access> types)
    throws IOException {
        final Path file = target.resolve(name.getPath());
        final AccessMode[] modes = modes(types);
        file.getFileSystem().provider().checkAccess(file, modes);
    }

    private static AccessMode[] modes(final BitField<Access> types) {
        final EnumSet<AccessMode> modes = EnumSet.noneOf(AccessMode.class);
        for (final Access type : types) {
            switch (type) {
            case READ:
                modes.add(AccessMode.READ);
                break;
            case WRITE:
                modes.add(AccessMode.WRITE);
                break;
            case EXECUTE:
                modes.add(AccessMode.EXECUTE);
                break;
            }
        }
        return modes.toArray(new AccessMode[modes.size()]);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        Path file = target.resolve(name.getPath());
        // Confirmed: There is no equivalent NIO.2 method, e.g. something like
        //   setAttribute(file, "readOnly", Boolean.TRUE, null);
        // is not available!
        if (!file.toFile().setReadOnly())
            if (exists(file))
                throw new AccessDeniedException(file.toString()); // just guessing here
            else
                throw new NoSuchFileException(file.toString());
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options, final FsEntryName name, final Map<Access, Long> times)
    throws IOException {
        final Path file = target.resolve(name.getPath());
        final Map<Access, Long> t = new EnumMap<>(times);
        getBasicFileAttributeView(file).setTimes(
                toFileTime(t.remove(WRITE)),
                toFileTime(t.remove(READ)),
                toFileTime(t.remove(CREATE)));
        return t.isEmpty();
    }

    @Override
    public boolean setTime(
            BitField<FsAccessOption> options, final FsEntryName name, final BitField<Access> types, final long value)
    throws IOException {
        final Path file = target.resolve(name.getPath());
        final FileTime time = FileTime.fromMillis(value);
        getBasicFileAttributeView(file).setTimes(
                types.get(WRITE)  ? time : null,
                types.get(READ)   ? time : null,
                types.get(CREATE) ? time : null);
        return types.clear(WRITE).clear(READ).clear(CREATE).isEmpty();
    }

    @Override
    public InputSocket<?> input(
            BitField<FsAccessOption> options,
            FsEntryName name) {
        return new FileEntry(target, name).input();
    }

    @Override
    public OutputSocket<?> output(
            BitField<FsAccessOption> options,
            FsEntryName name,
            @CheckForNull Entry template) {
        return new FileEntry(target, name).output(options, template);
    }

    @Override
    public void mknod(  final BitField<FsAccessOption> options, final FsEntryName name, final Type type, @CheckForNull
    final Entry template)
    throws IOException {
        final Path file = target.resolve(name.getPath());
        switch (type) {
            case FILE:
                if (options.get(EXCLUSIVE))
                    createFile(file);
                else
                    newOutputStream(file).close();
                break;
            case DIRECTORY:
                createDirectory(file);
                break;
            default:
                throw new IOException(file + " (entry type not supported: " + type + ")");
        }
        if (null != template) {
            getBasicFileAttributeView(file)
                    .setTimes(  toFileTime(template.getTime(WRITE)),
                                toFileTime(template.getTime(READ)),
                                toFileTime(template.getTime(CREATE)));
        }
    }

    private static @Nullable FileTime toFileTime(Long time) {
        return null == time ? null : toFileTime((long) time);
    }

    private static @Nullable FileTime toFileTime(long time) {
        return UNKNOWN == time ? null : FileTime.fromMillis(time);
    }

    @Override
    public void unlink(BitField<FsAccessOption> options, FsEntryName name)
    throws IOException {
        Path file = target.resolve(name.getPath());
        delete(file);
    }

    @Override
    public void sync(BitField<FsSyncOption> options) {
    }
}
