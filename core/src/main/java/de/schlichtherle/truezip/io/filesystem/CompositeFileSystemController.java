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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Icon;

/**
 * A composite file system controller implements a chain of responsibility
 * in order to resolve {@link FalsePositiveException}s thrown by the
 * prospective file system provided to its
 * {@link #CompositeFileSystemController constructor}.
 * Whenever the controller for the prospective file system throws a
 * {@link FalsePositiveException}, the method call is delegated to the
 * controller for its parent file system in order to resolve the requested
 * operation.
 * As a desired side effect, it also adapts the controller for the prospective
 * file system to a controller for a component file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class CompositeFileSystemController
extends AbstractFileSystemController<Entry>
implements ComponentFileSystemController<Entry> {

    private final FileSystemController<?> prospect;

    CompositeFileSystemController(final FileSystemController<?> prospect) {
        if (null == prospect.getParent())
            throw new IllegalArgumentException();
        this.prospect = prospect;
    }

    private FileSystemController<?> getProspect() {
        return prospect;
    }

    @Override
    public FileSystemModel getModel() {
        return getProspect().getModel();
    }

    @Override
    public ComponentFileSystemController<?> getParent() {
        return getProspect().getParent();
    }

    private String parentPath(String path) {
        return getModel().parentPath(path);
    }

    @Override
    public Icon getOpenIcon() {
        try {
            return getProspect().getOpenIcon();
        } catch (FalsePositiveException ex) {
            return getParent().getOpenIcon();
        } catch (FileSystemException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public Icon getClosedIcon() {
        try {
            return getProspect().getClosedIcon();
        } catch (FalsePositiveException ex) {
            return getParent().getClosedIcon();
        } catch (FileSystemException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            return getProspect().isReadOnly();
        } catch (FalsePositiveException ex) {
            return getParent().isReadOnly();
        } catch (FileSystemException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public FileSystemEntry<?> getEntry(String path) {
        try {
            return getProspect().getEntry(path);
        } catch (FalsePositiveException ex) {
            return getParent().getEntry(parentPath(path));
        } catch (FileSystemException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean isReadable(String path) {
        try {
            return getProspect().isReadable(path);
        } catch (FalsePositiveException ex) {
            return getParent().isReadable(parentPath(path));
        } catch (FileSystemException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public boolean isWritable(String path) {
        try {
            return getProspect().isWritable(path);
        } catch (FalsePositiveException ex) {
            return getParent().isWritable(parentPath(path));
        } catch (FileSystemException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        try {
            getProspect().setReadOnly(path);
        } catch (FalsePositiveException ex) {
            getParent().setReadOnly(parentPath(path));
        }
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        try {
            return getProspect().setTime(path, types, value);
        } catch (FalsePositiveException ex) {
            return getParent().setTime(parentPath(path), types, value);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final String path,
            final BitField<InputOption> options) {
        return new Input(path, options);
    }

    private class Input extends FilterInputSocket<Entry> {
        final String path;
        final BitField<InputOption> options;

        Input(final String path, final BitField<InputOption> options) {
            super(getProspect().getInputSocket(path, options));
            this.path = path;
            this.options = options;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getInputSocket(parentPath(path), options)
                        .bind(this)
                        .getLocalTarget();
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                return getBoundSocket().newInputStream();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getInputSocket(parentPath(path), options)
                        .bind(this)
                        .newInputStream();
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            try {
                return getBoundSocket().newReadOnlyFile();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getInputSocket(parentPath(path), options)
                        .bind(this)
                        .newReadOnlyFile();
            }
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            String path,
            BitField<OutputOption> options,
            Entry template) {
        return new Output(path, options, template);
    }

    private class Output extends FilterOutputSocket<Entry> {
        final String path;
        final BitField<OutputOption> options;
        final Entry template;

        Output( final String path,
                final BitField<OutputOption> options,
                final Entry template) {
            super(getProspect().getOutputSocket(path, options, template));
            this.path = path;
            this.options = options;
            this.template = template;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getOutputSocket(parentPath(path), options, template)
                        .bind(this)
                        .getLocalTarget();
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            try {
                return getBoundSocket().newOutputStream();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getOutputSocket(parentPath(path), options, template)
                        .bind(this)
                        .newOutputStream();
            }
        }
    } // class Output

    @Override
    public boolean mknod(   String path,
                            Entry.Type type,
                            BitField<OutputOption> options,
                            Entry template)
    throws IOException {
        try {
            return getProspect().mknod(path, type, options, template);
        } catch (FalsePositiveException ex) {
            return getParent().mknod(parentPath(path), type, options, template);
        }
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public void unlink(String path)
    throws IOException {
        try {
            getProspect().unlink(path);
        } catch (FalsePositiveException ex) {
            getParent().unlink(parentPath(path));
        }
    }

    @Override
    public <E extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, E> builder,
                final BitField<SyncOption> options)
    throws E {
        try {
            getProspect().sync(builder, options);
        } catch (FileSystemException ex) {
            throw new AssertionError(ex);
        }
    }
}
