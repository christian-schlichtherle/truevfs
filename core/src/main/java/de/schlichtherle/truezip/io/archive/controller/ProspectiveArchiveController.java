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

import de.schlichtherle.truezip.io.filesystem.CompositeFileSystemModel;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.filesystem.AbstractFileSystemController;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import java.net.URI;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.OutputStream;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.InputStream;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.filesystem.CompositeFileSystemController;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;
import static de.schlichtherle.truezip.util.Link.Type.STRONG;
import static de.schlichtherle.truezip.util.Link.Type.WEAK;

/**
 * A prospective archive controller is a composite file system facade which
 * adapts a chain of archive controller decorators and utilizes a (composite)
 * file system controller for its parent archive file in order to implement a
 * chain of responsibility for resolving {@link FalsePositiveException}s.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ProspectiveArchiveController<AE extends ArchiveEntry>
extends     AbstractFileSystemController<CommonEntry            >
implements  CompositeFileSystemController<CommonEntry           > {

    private final ArchiveController<AE> controller;
    private final FileSystemController<?> parentController;
    private final String parentPath;

    ProspectiveArchiveController(   final URI mountPoint,
                                    final ArchiveDriver<AE> driver,
                                    final FileSystemController<?> parentController) {
        assert null != mountPoint;
        assert null != driver;
        assert null != parentController;
        final SyncScheduler syncScheduler = new SyncScheduler();
        final ArchiveModel model = new ArchiveModel(
                parentController.getModel(), mountPoint, syncScheduler);
        this.controller = driver.newController(model, parentController);
        this.parentController = parentController;
        this.parentPath = parentController
                .getModel()
                .getMountPoint()
                .relativize(mountPoint)
                .getPath();
    }

    private class SyncScheduler implements TouchListener {
        @Override
        public void setTouched(boolean touched) {
            Controllers.scheduleSync(touched ? STRONG : WEAK, ProspectiveArchiveController.this);
        }
    }

    /** Returns the file system controller for the parent file system. */
    private FileSystemController<?> getParentController() {
        return parentController;
    }

    /**
     * Resolves the given relative {@code path} against the relative path of
     * this controller's target archive file within its parent file system.
     */
    private String getParentPath(String path) {
        return isRoot(path)
                ? cutTrailingSeparators(parentPath, SEPARATOR_CHAR)
                : parentPath + path;
    }

    private ArchiveController<AE> getController() {
        return controller;
    }

    @Override
    public CompositeFileSystemModel getModel() {
        return getController().getModel();
    }

    /*boolean isTouched() {
        return getController().isTouched();
    }*/

    @Override
    public <E extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, E> builder,
                final BitField<SyncOption> options)
    throws E {
        try {
            getController().sync(builder, options);
        } catch (ArchiveException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public Icon getOpenIcon() {
        try {
            return getController().getOpenIcon();
        } catch (FalsePositiveException ex) {
            return getParentController().getOpenIcon();
        } catch (ArchiveException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public Icon getClosedIcon() {
        try {
            return getController().getClosedIcon();
        } catch (FalsePositiveException ex) {
            return getParentController().getClosedIcon();
        } catch (ArchiveException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            return getController().isReadOnly();
        } catch (FalsePositiveException ex) {
            return getParentController().isReadOnly();
        } catch (ArchiveException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public FileSystemEntry<?> getEntry(String path) {
        try {
            return getController().getEntry(path);
        } catch (FalsePositiveException ex) {
            return getParentController().getEntry(getParentPath(path));
        } catch (ArchiveException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean isReadable(String path) {
        try {
            return getController().isReadable(path);
        } catch (FalsePositiveException ex) {
            return getParentController().isReadable(getParentPath(path));
        } catch (ArchiveException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean isWritable(String path) {
        try {
            return getController().isWritable(path);
        } catch (FalsePositiveException ex) {
            return getParentController().isWritable(getParentPath(path));
        } catch (ArchiveException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        try {
            getController().setReadOnly(path);
        } catch (FalsePositiveException ex) {
            getParentController().setReadOnly(getParentPath(path));
        }
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        try {
            return getController().setTime(path, types, value);
        } catch (FalsePositiveException ex) {
            return getParentController().setTime(getParentPath(path), types, value);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final String path,
            final BitField<InputOption> options) {
        return new Input(path, options);
    }

    private class Input extends FilterInputSocket<CommonEntry> {
        final String path;
        final BitField<InputOption> options;

        Input(final String path, final BitField<InputOption> options) {
            super(getController().getInputSocket(path, options));
            this.path = path;
            this.options = options;
        }

        @Override
        public CommonEntry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                return getParentController()
                        .getInputSocket(getParentPath(path), options)
                        .bind(this)
                        .getLocalTarget();
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                return getBoundSocket().newInputStream();
            } catch (FalsePositiveException ex) {
                return getParentController()
                        .getInputSocket(getParentPath(path), options)
                        .bind(this)
                        .newInputStream();
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            try {
                return getBoundSocket().newReadOnlyFile();
            } catch (FalsePositiveException ex) {
                return getParentController()
                        .getInputSocket(getParentPath(path), options)
                        .bind(this)
                        .newReadOnlyFile();
            }
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            String path,
            BitField<OutputOption> options,
            CommonEntry template) {
        return new Output(path, options, template);
    }

    private class Output extends FilterOutputSocket<CommonEntry> {
        final String path;
        final BitField<OutputOption> options;
        final CommonEntry template;

        Output( final String path,
                final BitField<OutputOption> options,
                final CommonEntry template) {
            super(getController().getOutputSocket(path, options, template));
            this.path = path;
            this.options = options;
            this.template = template;
        }

        @Override
        public CommonEntry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                return getParentController()
                        .getOutputSocket(getParentPath(path), options, template)
                        .bind(this)
                        .getLocalTarget();
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            try {
                return getBoundSocket().newOutputStream();
            } catch (FalsePositiveException ex) {
                return getParentController()
                        .getOutputSocket(getParentPath(path), options, template)
                        .bind(this)
                        .newOutputStream();
            }
        }
    } // class Output

    @Override
    public boolean mknod(   String path,
                            CommonEntry.Type type,
                            BitField<OutputOption> options,
                            CommonEntry template)
    throws IOException {
        try {
            return getController().mknod(path, type, options, template);
        } catch (FalsePositiveException ex) {
            return getParentController().mknod(getParentPath(path), type, options, template);
        }
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public void unlink(String path)
    throws IOException {
        try {
            getController().unlink(path);
        } catch (FalsePositiveException ex) {
            getParentController().unlink(getParentPath(path));
        }
    }
}
