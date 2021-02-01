/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.truevfs.comp.cio.Entry;
import global.namespace.truevfs.comp.cio.Entry.Access;
import global.namespace.truevfs.comp.cio.Entry.Type;
import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.cio.OutputSocket;
import global.namespace.truevfs.comp.shed.BitField;
import global.namespace.truevfs.kernel.spec.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import static java.io.File.separatorChar;
import static java.nio.file.Files.*;
import static global.namespace.truevfs.comp.cio.Entry.Access.*;
import static global.namespace.truevfs.comp.cio.Entry.UNKNOWN;
import static global.namespace.truevfs.kernel.spec.FsAccessOption.EXCLUSIVE;
import static global.namespace.truevfs.kernel.spec.FsNodeName.SEPARATOR;

/**
 * A file system controller with a prospective directory in the platform file
 * system as its mount point.
 *
 * @author Christian Schlichtherle
 */
final class FileController extends FsAbstractController {

    private static final String TWO_SEPARATORS = SEPARATOR + SEPARATOR;

    private final Path target;

    FileController(final FsModel model) {
        super(model);
        if (model.getParent().isPresent()) {
            throw new IllegalArgumentException();
        }
        URI uri = model.getMountPoint().getUri();
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
                uri = new URI(uri.getScheme(), "",
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
    public Optional<? extends FsController> getParent() {
        return Optional.empty();
    }

    @Override
    public Optional<? extends FileNode> node(
            final BitField<FsAccessOption> options,
            final FsNodeName name
    ) throws IOException {
        final FileNode entry = new FileNode(target, name);
        return exists(entry.getPath()) ? Optional.of(entry) : Optional.empty();
    }

    @Override
    public void checkAccess(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Access> types
    ) throws IOException {
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
        return modes.toArray(new AccessMode[0]);
    }

    @Override
    public void setReadOnly(
            final BitField<FsAccessOption> options,
            final FsNodeName name
    ) throws IOException {
        Path file = target.resolve(name.getPath());
        // Confirmed: There is no equivalent NIO.2 method, e.g. something like
        //   setAttribute(file, "readOnly", Boolean.TRUE, null);
        // is not available!
        if (!file.toFile().setReadOnly()) {
            throw exists(file)
                    ? new AccessDeniedException(file.toString()) // just guessing here
                    : new NoSuchFileException(file.toString());
        }
    }

    @Override
    public boolean setTime(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final Map<Access, Long> times
    ) throws IOException {
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
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final BitField<Access> types,
            final long value)
            throws IOException {
        final Path file = target.resolve(name.getPath());
        final FileTime time = FileTime.fromMillis(value);
        getBasicFileAttributeView(file).setTimes(
                types.get(WRITE) ? time : null,
                types.get(READ) ? time : null,
                types.get(CREATE) ? time : null);
        return types.clear(WRITE).clear(READ).clear(CREATE).isEmpty();
    }

    @Override
    public InputSocket<?> input(
            BitField<FsAccessOption> options,
            FsNodeName name) {
        return new FileNode(target, name).input(options);
    }

    @Override
    public OutputSocket<?> output(
            BitField<FsAccessOption> options,
            FsNodeName name,
            Optional<? extends Entry> template) {
        return new FileNode(target, name).output(options, template);
    }

    @Override
    public void make(
            final BitField<FsAccessOption> options,
            final FsNodeName name,
            final Type type,
            final Optional<? extends Entry> template)
            throws IOException {
        final Path file = target.resolve(name.getPath());
        switch (type) {
            case FILE:
                if (options.get(EXCLUSIVE)) createFile(file);
                else newOutputStream(file).close();
                break;
            case DIRECTORY:
                /*if (options.get(CREATE_PARENTS))
                    createDirectories(file);
                else*/
                createDirectory(file);
                break;
            default:
                throw new IOException(file + " (entry type not supported: " + type + ")");
        }
        if (template.isPresent()) {
            final Entry t = template.get();
            getBasicFileAttributeView(file)
                    .setTimes(toFileTime(t.getTime(WRITE)),
                            toFileTime(t.getTime(READ)),
                            toFileTime(t.getTime(CREATE)));
        }
    }

    private static @Nullable
    FileTime toFileTime(Long time) {
        return null == time ? null : toFileTime((long) time);
    }

    private static @Nullable
    FileTime toFileTime(long time) {
        return UNKNOWN == time ? null : FileTime.fromMillis(time);
    }

    @Override
    public void unlink(
            final BitField<FsAccessOption> options,
            final FsNodeName name)
            throws IOException {
        Path file = target.resolve(name.getPath());
        delete(file);
    }

    @Override
    public void sync(BitField<FsSyncOption> options) {
    }
}
