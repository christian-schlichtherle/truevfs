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
package de.schlichtherle.truezip.io.fs;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.DecoratorInputSocket;
import de.schlichtherle.truezip.io.socket.DecoratorOutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import javax.swing.Icon;

/**
 * Implements a chain of responsibility in order to resolve
 * {@link FSFalsePositiveException}s thrown by the prospective file system
 * provided to its {@link #FSFederationController constructor}.
 * Whenever the controller for the prospective file system throws a
 * {@link FSFalsePositiveException}, the method call is delegated to the
 * controller for its parent file system in order to resolve the requested
 * operation.
 * <p>
 * This class is thread-safe if and only if the decorated file system
 * controller and its parent file system controller are thread-safe.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
final class FSFederationController
extends FSDecoratorController<FSModel, FSController<?>> {

    private volatile FSPath path;

    /**
     * Constructs a new federated file system controller.
     *
     * @param controller the decorated file system controller.
     */
    FSFederationController(final @NonNull FSController<?> controller) {
        super(controller);
        assert null != getParent();
    }

    private FSEntryName resolveParent(FSEntryName name) {
        return getPath().resolve(name).getEntryName();
    }

    private FSPath getPath() {
        return null != path ? path : (path = getModel().getMountPoint().getPath());
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        try {
            return delegate.getOpenIcon();
        } catch (FSFalsePositiveException ex) {
            return getParent().getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        try {
            return delegate.getClosedIcon();
        } catch (FSFalsePositiveException ex) {
            return getParent().getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        try {
            return delegate.isReadOnly();
        } catch (FSFalsePositiveException ex) {
            return getParent().isReadOnly();
        }
    }

    @Override
    public FSEntry getEntry(FSEntryName name) throws IOException {
        try {
            return delegate.getEntry(name);
        } catch (FSFalsePositiveException ex) {
            return getParent().getEntry(resolveParent(name));
        }
    }

    @Override
    public boolean isReadable(FSEntryName name) throws IOException {
        try {
            return delegate.isReadable(name);
        } catch (FSFalsePositiveException ex) {
            return getParent().isReadable(resolveParent(name));
        }
    }

    @Override
    public boolean isWritable(FSEntryName name) throws IOException {
        try {
            return delegate.isWritable(name);
        } catch (FSFalsePositiveException ex) {
            return getParent().isWritable(resolveParent(name));
        }
    }

    @Override
    public void setReadOnly(FSEntryName name) throws IOException {
        try {
            delegate.setReadOnly(name);
        } catch (FSFalsePositiveException ex) {
            getParent().setReadOnly(resolveParent(name));
        }
    }

    @Override
    public boolean setTime(FSEntryName name, BitField<Access> types, long value)
    throws IOException {
        try {
            return delegate.setTime(name, types, value);
        } catch (FSFalsePositiveException ex) {
            return getParent().setTime(resolveParent(name), types, value);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FSEntryName name,
            final BitField<FSInputOption> options) {
        return new Input(name, options);
    }

    private class Input extends DecoratorInputSocket<Entry> {
        final FSEntryName name;
        final BitField<FSInputOption> options;

        Input(final FSEntryName name, final BitField<FSInputOption> options) {
            super(delegate.getInputSocket(name, options));
            this.name = name;
            this.options = options;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FSFalsePositiveException ex) {
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
            } catch (FSFalsePositiveException ex) {
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
            } catch (FSFalsePositiveException ex) {
                return getParent()
                        .getInputSocket(resolveParent(name), options)
                        .bind(this)
                        .newInputStream();
            }
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            FSEntryName name,
            BitField<FSOutputOption> options,
            Entry template) {
        return new Output(name, options, template);
    }

    private class Output extends DecoratorOutputSocket<Entry> {
        final FSEntryName name;
        final BitField<FSOutputOption> options;
        final Entry template;

        Output( final FSEntryName name,
                final BitField<FSOutputOption> options,
                final Entry template) {
            super(delegate.getOutputSocket(name, options, template));
            this.name = name;
            this.options = options;
            this.template = template;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FSFalsePositiveException ex) {
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
            } catch (FSFalsePositiveException ex) {
                return getParent()
                        .getOutputSocket(resolveParent(name), options, template)
                        .bind(this)
                        .newOutputStream();
            }
        }
    } // class Output

    @Override
    public void mknod(
            @NonNull FSEntryName name,
            @NonNull Type type,
            @NonNull BitField<FSOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        try {
            delegate.mknod(name, type, options, template);
        } catch (FSFalsePositiveException ex) {
            getParent().mknod(resolveParent(name), type, options, template);
        }
    }

    @Override
    public void unlink(FSEntryName name) throws IOException {
        try {
            delegate.unlink(name);
        } catch (FSFalsePositiveException ex) {
            getParent().unlink(resolveParent(name));
        }
    }

    @Override
    public <X extends IOException>
    void sync(
            @NonNull final BitField<FSSyncOption> options,
            @NonNull final ExceptionHandler<? super FSSyncException, X> handler)
    throws X, FSException {
        try {
            delegate.sync(options, handler);
        } catch (FSFalsePositiveException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }
}
