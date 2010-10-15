/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import java.net.URI;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.io.filesystem.FileSystemModel;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import java.io.OutputStream;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.InputStream;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.io.entry.FilterCommonEntry;
import java.util.Set;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.SPECIAL;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * A prospective archive controller is a facade which adapts a chain of
 * archive controller decorators and utilizes a file system controller for
 * its enclosing archive file in order to implement a chain of responsibility
 * for resolving {@link FalsePositiveException}s.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ProspectiveArchiveController<AE extends ArchiveEntry>
implements FileSystemController<CommonEntry> {

    private final ArchiveController<AE> controller;
    private final FileSystemController<?> enclController;
    private final String enclPath;

    ProspectiveArchiveController(   final URI mountPoint,
                                    final ArchiveDriver<AE> driver,
                                    FileSystemController<?> enclController) {
        if (null == enclController)
            enclController = new HostFileSystemController(
                    mountPoint.resolve(".."));
        final SyncScheduler syncScheduler = new SyncScheduler();
        final ArchiveModel model = new ArchiveModel(
                enclController.getModel(), mountPoint, syncScheduler);
        // TODO: Support append strategy.
        this.controller = new LockingArchiveController<AE>(
                new CachingArchiveController<AE>(
                    new UpdatingArchiveController<AE>(
                        enclController, model, driver)));
        this.enclController = enclController;
        this.enclPath = enclController
                .getModel()
                .getMountPoint()
                .relativize(controller.getModel().getMountPoint())
                .getPath();
    }

    private class SyncScheduler implements TouchListener {
        @Override
        public void setTouched(boolean touched) {
            Controllers.scheduleSync(touched ? STRONG : WEAK, ProspectiveArchiveController.this);
        }
    }

    /** Returns the file system controller for the enclosing file system. */
    private FileSystemController<?> getEnclController() {
        return enclController;
    }

    /**
     * Resolves the given relative {@code path} against the relative path of
     * this controller's archive file within its enclosing file system.
     */
    private String getEnclPath(String path) {
        return isRoot(path)
                ? cutTrailingSeparators(enclPath, SEPARATOR_CHAR)
                : enclPath + path;
    }

    private ArchiveController<?> getController() {
        return controller;
    }

    @Override
    public FileSystemModel getModel() {
        return getController().getModel();
    }

    boolean isTouched() {
        return getController().isTouched();
    }

    <E extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, E> builder,
                BitField<SyncOption> options)
    throws E {
        try {
            getController().sync(builder, options);
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public Icon getOpenIcon() {
        try {
            return getController().getOpenIcon();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return null;
            return getEnclController().getOpenIcon();
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public Icon getClosedIcon() {
        try {
            return getController().getClosedIcon();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return null;
            return getEnclController().getClosedIcon();
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            return getController().isReadOnly();
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return true;
            return getEnclController().isReadOnly();
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public FileSystemEntry getEntry(String path) {
        try {
            return getController().getEntry(path);
        } catch (FalsePositiveException ex) {
            final FileSystemEntry entry = getEnclController()
                    .getEntry(getEnclPath(path));
            if (ex.isTransient())
                return null == entry
                        ? null
                        : new SpecialFileEntry<CommonEntry>(
                            entry instanceof Entry<?>
                                ? ((Entry<?>) entry).getTarget()
                                : entry);
            return entry;
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    private static final class SpecialFileEntry<CE extends CommonEntry>
    extends FilterCommonEntry<CE>
    implements Entry<CE> {
        SpecialFileEntry(CE entry) {
            super(entry);
        }

        @Override
        public Type getType() {
            return SPECIAL;
        }

        @Override
        public Set<String> getMembers() {
            return null;
        }

        @Override
        public CE getTarget() {
            return entry;
        }
    }

    @Override
    public boolean isReadable(String path) {
        try {
            return getController().isReadable(path);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return false;
            return getEnclController().isReadable(getEnclPath(path));
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean isWritable(String path) {
        try {
            return getController().isWritable(path);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                return false;
            return getEnclController().isWritable(getEnclPath(path));
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        try {
            getController().setReadOnly(path);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            getEnclController().setReadOnly(getEnclPath(path));
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        try {
            return getController().setTime(path, types, value);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController()
                    .setTime(getEnclPath(path), types, value);
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final String path,
            final BitField<InputOption> options)
    throws IOException, NotWriteLockedException {
        try {
            return new Input(path, options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController()
                    .getInputSocket(getEnclPath(path), options);
        }
    }

    /**
     * This class may be actually unused because archive controllers usually
     * detect false positive archive files eagerly, i.e. when a socket is
     * acquired, rather than lazily, i.e. when a socket is used.
     */
    private class Input extends FilterInputSocket<CommonEntry> {
        final String path;
        final BitField<InputOption> options;

        Input(  final String path, final BitField<InputOption> options)
        throws IOException, FalsePositiveException, NotWriteLockedException {
            super(getController().getInputSocket(path, options));
            this.path = path;
            this.options = options;
        }

        @Override
        public CommonEntry getLocalTarget()
        throws IOException, FalsePositiveException, NotWriteLockedException {
            try {
                return getInputSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                return getEnclController()
                        .getInputSocket(getEnclPath(path), options)
                        .getLocalTarget();
            }
        }

        @Override
        public InputStream newInputStream()
        throws IOException, FalsePositiveException, NotWriteLockedException {
            try {
                return getInputSocket().newInputStream();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                return getEnclController()
                        .getInputSocket(getEnclPath(path), options)
                        .newInputStream();
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile()
        throws IOException, FalsePositiveException, NotWriteLockedException {
            try {
                return getInputSocket().newReadOnlyFile();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                return getEnclController()
                        .getInputSocket(getEnclPath(path), options)
                        .newReadOnlyFile();
            }
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        try {
            return new Output(path, options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController()
                    .getOutputSocket(getEnclPath(path), options);
        }
    }

    /**
     * This class may be actually unused because archive controllers usually
     * detect false positive archive files eagerly, i.e. when a socket is
     * acquired, rather than lazily, i.e. when a socket is used.
     */
    private class Output extends FilterOutputSocket<CommonEntry> {
        final String path;
        final BitField<OutputOption> options;

        Output(final String path, final BitField<OutputOption> options)
        throws IOException, FalsePositiveException, NotWriteLockedException {
            super(getController().getOutputSocket(path, options));
            this.path = path;
            this.options = options;
        }

        @Override
        public CommonEntry getLocalTarget()
        throws IOException, FalsePositiveException, NotWriteLockedException {
            try {
                return getOutputSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                return getEnclController()
                        .getOutputSocket(getEnclPath(path), options)
                        .getLocalTarget();
            }
        }

        @Override
        public OutputStream newOutputStream()
        throws IOException, FalsePositiveException, NotWriteLockedException {
            try {
                return getOutputSocket().newOutputStream();
            } catch (FalsePositiveException ex) {
                if (ex.isTransient())
                    throw ex.getCause();
                return getEnclController()
                        .getOutputSocket(getEnclPath(path), options)
                        .newOutputStream();
            }
        }
    } // class Output

    @Override
    public boolean mknod(   String path,
                            Type type,
                            CommonEntry template,
                            BitField<OutputOption> options)
    throws IOException {
        try {
            return getController().mknod(path, type, template, options);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            return getEnclController()
                    .mknod(getEnclPath(path), type, template, options);
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public void unlink(String path)
    throws IOException {
        try {
            getController().unlink(path);
        } catch (FalsePositiveException ex) {
            if (ex.isTransient())
                throw ex.getCause();
            getEnclController().unlink(getEnclPath(path));
        } catch (NotWriteLockedException ex) {
            throw new AssertionError(ex);
        }
    }
}
