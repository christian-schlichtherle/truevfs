/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.nio.file;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.UNKNOWN;
import static de.schlichtherle.truezip.entry.EntryName.SEPARATOR;
import de.schlichtherle.truezip.fs.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.EXCLUSIVE;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ControlFlowException;
import static java.io.File.separatorChar;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
 * @since  TrueZIP 7.2
 * @author Christian Schlichtherle
 */
@Immutable
final class FileController extends FsAbstractController<FsModel>  {

    private static final String TWO_SEPARATORS = SEPARATOR + SEPARATOR;

    private final Path target;

    FileController(final FsModel model) {
        super(model);
        if (null != model.getParent()) throw new IllegalArgumentException();
        URI uri = model.getMountPoint().toUri();
        if ('\\' == separatorChar && null != uri.getRawAuthority()) {
            try {
                // Postfix: Move Windows UNC host from authority to path
                // component because the File class can't deal with this.
                // Note that the authority parameter must not be null and that
                // you cannot use the UriBuilder class - using either of these
                // would result in the authority property of the new URI object
                // being equal to the original value again.
                // Note that the use of the buggy URI constructor is authorized
                // for this case!
                // See http://java.net/jira/browse/TRUEZIP-288 .
                uri = new URI(  uri.getScheme(), "",
                                TWO_SEPARATORS + uri.getAuthority() + uri.getPath(),
                                uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        }
        this.target = Paths.get(uri);
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
            BitField<FsOutputOption> options)
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
            final long value, BitField<FsOutputOption> options)
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
            BitField<FsInputOption> options) {
        return new FileEntry(target, name).getInputSocket();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new FileEntry(target, name).getOutputSocket(options, template);
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
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
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        Path file = target.resolve(name.getPath());
        delete(file);
    }

    @Override
    public void sync(final BitField<FsSyncOption> options)
    throws FsSyncException, ControlFlowException {
    }
}
