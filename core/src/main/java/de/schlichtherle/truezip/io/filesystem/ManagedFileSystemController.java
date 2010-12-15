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
import java.lang.reflect.UndeclaredThrowableException;
import javax.swing.Icon;

/**
 * Implements a chain of responsibility in order to resolveAbsolute
 * {@link FalsePositiveException}s thrown by the prospective file system
 * provided to its {@link #ManagedFileSystemController constructor}.
 * Whenever the controller for the prospective file system throws a
 * {@link FalsePositiveException}, the method call is delegated to the
 * controller for its parent file system in order to resolveAbsolute the requested
 * operation.
 * As a desired side effect, it also adapts the controller for the prospective
 * file system to a controller for a component file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ManagedFileSystemController
extends FilterFileSystemController<Entry, FileSystemController<?>>
implements FileSystemController<Entry> {

    ManagedFileSystemController(final FileSystemController<?> controller) {
        super(controller);
        assert null != getParent();
    }

    private FileSystemEntryName resolveParent(FileSystemEntryName name) {
        return getModel().resolveParent(name);
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        try {
            return controller.getOpenIcon();
        } catch (FalsePositiveException ex) {
            return getParent().getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        try {
            return controller.getClosedIcon();
        } catch (FalsePositiveException ex) {
            return getParent().getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        try {
            return controller.isReadOnly();
        } catch (FalsePositiveException ex) {
            return getParent().isReadOnly();
        }
    }

    @Override
    public FileSystemEntry<?> getEntry(FileSystemEntryName name) throws IOException {
        try {
            return controller.getEntry(name);
        } catch (FalsePositiveException ex) {
            return getParent().getEntry(resolveParent(name));
        }
    }

    @Override
    public boolean isReadable(FileSystemEntryName name) throws IOException {
        try {
            return controller.isReadable(name);
        } catch (FalsePositiveException ex) {
            return getParent().isReadable(resolveParent(name));
        }
    }

    @Override
    public boolean isWritable(FileSystemEntryName name) throws IOException {
        try {
            return controller.isWritable(name);
        } catch (FalsePositiveException ex) {
            return getParent().isWritable(resolveParent(name));
        }
    }

    @Override
    public void setReadOnly(FileSystemEntryName name) throws IOException {
        try {
            controller.setReadOnly(name);
        } catch (FalsePositiveException ex) {
            getParent().setReadOnly(resolveParent(name));
        }
    }

    @Override
    public boolean setTime(FileSystemEntryName name, BitField<Access> types, long value)
    throws IOException {
        try {
            return controller.setTime(name, types, value);
        } catch (FalsePositiveException ex) {
            return getParent().setTime(resolveParent(name), types, value);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FileSystemEntryName name,
            final BitField<InputOption> options) {
        return new Input(name, options);
    }

    private class Input extends FilterInputSocket<Entry> {
        final FileSystemEntryName name;
        final BitField<InputOption> options;

        Input(final FileSystemEntryName name, final BitField<InputOption> options) {
            super(controller.getInputSocket(name, options));
            this.name = name;
            this.options = options;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getInputSocket(resolveParent(name), options)
                        .bind(this)
                        .getLocalTarget();
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            try {
                return getBoundSocket().newReadOnlyFile();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getInputSocket(resolveParent(name), options)
                        .bind(this)
                        .newReadOnlyFile();
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                return getBoundSocket().newInputStream();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getInputSocket(resolveParent(name), options)
                        .bind(this)
                        .newInputStream();
            }
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            FileSystemEntryName name,
            BitField<OutputOption> options,
            Entry template) {
        return new Output(name, options, template);
    }

    private class Output extends FilterOutputSocket<Entry> {
        final FileSystemEntryName name;
        final BitField<OutputOption> options;
        final Entry template;

        Output( final FileSystemEntryName name,
                final BitField<OutputOption> options,
                final Entry template) {
            super(controller.getOutputSocket(name, options, template));
            this.name = name;
            this.options = options;
            this.template = template;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FalsePositiveException ex) {
                return getParent()
                        .getOutputSocket(resolveParent(name), options, template)
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
                        .getOutputSocket(resolveParent(name), options, template)
                        .bind(this)
                        .newOutputStream();
            }
        }
    } // class Output

    @Override
    public boolean mknod(   FileSystemEntryName name,
                            Entry.Type type,
                            BitField<OutputOption> options,
                            Entry template)
    throws IOException {
        try {
            return controller.mknod(name, type, options, template);
        } catch (FalsePositiveException ex) {
            return getParent().mknod(resolveParent(name), type, options, template);
        }
    }

    @Override
    public void unlink(FileSystemEntryName name) throws IOException {
        try {
            controller.unlink(name);
        } catch (FalsePositiveException ex) {
            getParent().unlink(resolveParent(name));
        }
    }

    @Override
    public <X extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, X> builder,
                final BitField<SyncOption> options)
    throws X, FileSystemException {
        try {
            controller.sync(builder, options);
        } catch (FalsePositiveException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }
}
