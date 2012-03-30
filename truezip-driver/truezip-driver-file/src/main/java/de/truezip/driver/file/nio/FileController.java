/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.nio;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import static de.truezip.kernel.cio.Entry.Access.*;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsModel;
import de.truezip.kernel.fs.FsSyncException;
import de.truezip.kernel.fs.addr.FsEntryName;
import de.truezip.kernel.fs.option.FsAccessOption;
import static de.truezip.kernel.fs.option.FsAccessOption.EXCLUSIVE;
import de.truezip.kernel.fs.option.FsSyncOption;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.EnumMap;
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
final class FileController extends FsController<FsModel>  {

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
    public FileEntry getEntry(FsEntryName name) throws IOException {
        FileEntry entry = new FileEntry(target, name);
        return exists(entry.getPath()) ? entry : null;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        Path file = target.resolve(name.getPath());
        return Files.isReadable(file);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        Path file = target.resolve(name.getPath());
        return Files.isWritable(file);
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        Path file = target.resolve(name.getPath());
        return Files.isExecutable(file);
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
                throw new FileNotFoundException(file.toString());
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final Map<Access, Long> times,
            BitField<FsAccessOption> options)
    throws IOException {
        final Path file = target.resolve(name.getPath());
        final Map<Access, Long> t = new EnumMap<Access, Long>(times);
        getBasicFileAttributeView(file).setTimes(
                toFileTime(t.remove(WRITE)),
                toFileTime(t.remove(READ)),
                toFileTime(t.remove(CREATE)));
        return t.isEmpty();
    }

    @Override
    public boolean setTime(
            final FsEntryName name,
            final BitField<Access> types,
            final long value, BitField<FsAccessOption> options)
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
    public InputSocket<?> getInputSocket(
            FsEntryName name,
            BitField<FsAccessOption> options) {
        return new FileEntry(target, name).getInputSocket();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsAccessOption> options,
            @CheckForNull Entry template) {
        return new FileEntry(target, name).getOutputSocket(options, template);
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsAccessOption> options,
                        final @CheckForNull Entry template)
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
    public void unlink(FsEntryName name, BitField<FsAccessOption> options)
    throws IOException {
        Path file = target.resolve(name.getPath());
        delete(file);
    }

    @Override
    public <X extends IOException>
    void sync(  BitField<FsSyncOption> options,
                ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
    }
}