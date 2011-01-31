/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * Implements a chain of responsibility in order to resolve
 * {@link FsFalsePositiveException}s thrown by the prospective file system
 * provided to its {@link #FsFederatingController constructor}.
 * Whenever the controller for the prospective file system throws a
 * {@link FsFalsePositiveException}, the method call is delegated to the
 * controller for its parent file system in order to resolve the requested
 * operation.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
final class FsFederatingController
extends FsDecoratingController<FsModel, FsController<?>> {

    private volatile FsPath path;

    /**
     * Constructs a new file system federating controller.
     *
     * @param controller the decorated file system controller.
     */
    FsFederatingController(final @NonNull FsController<?> controller) {
        super(controller);
        assert null != getParent();
    }

    private FsEntryName resolveParent(FsEntryName name) {
        return getPath().resolve(name).getEntryName();
    }

    private FsPath getPath() {
        return null != path ? path : (path = getModel().getMountPoint().getPath());
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        try {
            return delegate.getOpenIcon();
        } catch (FsFalsePositiveException ex) {
            return getParent().getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        try {
            return delegate.getClosedIcon();
        } catch (FsFalsePositiveException ex) {
            return getParent().getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        try {
            return delegate.isReadOnly();
        } catch (FsFalsePositiveException ex) {
            return getParent().isReadOnly();
        }
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        try {
            return delegate.getEntry(name);
        } catch (FsFalsePositiveException ex) {
            return getParent().getEntry(resolveParent(name));
        }
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        try {
            return delegate.isReadable(name);
        } catch (FsFalsePositiveException ex) {
            return getParent().isReadable(resolveParent(name));
        }
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        try {
            return delegate.isWritable(name);
        } catch (FsFalsePositiveException ex) {
            return getParent().isWritable(resolveParent(name));
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        try {
            delegate.setReadOnly(name);
        } catch (FsFalsePositiveException ex) {
            getParent().setReadOnly(resolveParent(name));
        }
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value)
    throws IOException {
        try {
            return delegate.setTime(name, types, value);
        } catch (FsFalsePositiveException ex) {
            return getParent().setTime(resolveParent(name), types, value);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(
            final FsEntryName name,
            final BitField<FsInputOption> options) {
        return new Input(name, options);
    }

    private class Input extends DecoratingInputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsInputOption> options;

        Input(final FsEntryName name, final BitField<FsInputOption> options) {
            super(delegate.getInputSocket(name, options));
            this.name = name;
            this.options = options;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FsFalsePositiveException ex) {
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
            } catch (FsFalsePositiveException ex) {
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
            } catch (FsFalsePositiveException ex) {
                return getParent()
                        .getInputSocket(resolveParent(name), options)
                        .bind(this)
                        .newInputStream();
            }
        }
    } // class Input

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            Entry template) {
        return new Output(name, options, template);
    }

    private class Output extends DecoratingOutputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsOutputOption> options;
        final Entry template;

        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
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
            } catch (FsFalsePositiveException ex) {
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
            } catch (FsFalsePositiveException ex) {
                return getParent()
                        .getOutputSocket(resolveParent(name), options, template)
                        .bind(this)
                        .newOutputStream();
            }
        }
    } // class Output

    @Override
    public void mknod(
            @NonNull FsEntryName name,
            @NonNull Type type,
            @NonNull BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        try {
            delegate.mknod(name, type, options, template);
        } catch (FsFalsePositiveException ex) {
            getParent().mknod(resolveParent(name), type, options, template);
        }
    }

    @Override
    public void unlink(FsEntryName name) throws IOException {
        try {
            delegate.unlink(name);
        } catch (FsFalsePositiveException ex) {
            getParent().unlink(resolveParent(name));
        }
    }
}
