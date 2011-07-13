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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsDecoratingController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsOutputOptions;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * A file system controller which decorates another file system controller in
 * order to provide the original values of selected parameters for its
 * operation in scope.
 * 
 * @see     FsContextModel
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class FsContextController
extends FsDecoratingController< FsContextModel,
                                FsController<? extends FsContextModel>> {

    private static final FsOperationContext
            NULL = new FsOperationContext();
    private static final FsOperationContext
            NONE = new FsOperationContext();
    static {
        NONE.setOutputOptions(FsOutputOptions.NO_OUTPUT_OPTIONS);
    }

    /**
     * Constructs a new operation file system controller.
     *
     * @param controller the decorated file system controller.
     */
    FsContextController(
            @NonNull FsController<? extends FsContextModel> controller) {
        super(controller);
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        getModel().setContext(NULL);
        return delegate.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        getModel().setContext(NULL);
        return delegate.getClosedIcon();
    }

    @Override
    public boolean isReadOnly() throws IOException {
        getModel().setContext(NULL);
        return delegate.isReadOnly();
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        getModel().setContext(NULL);
        return delegate.getEntry(name);
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        getModel().setContext(NULL);
        return delegate.isReadable(name);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        getModel().setContext(NULL);
        return delegate.isWritable(name);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        getModel().setContext(NONE);
        delegate.setReadOnly(name);
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value)
    throws IOException {
        getModel().setContext(NONE);
        return delegate.setTime(name, types, value);
    }

    @Override
    public boolean setTime(FsEntryName name, Map<Access, Long> times)
    throws IOException {
        getModel().setContext(NONE);
        return delegate.setTime(name, times);
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return new Input(name, options);
    }

    private final class Input extends DecoratingInputSocket<Entry> {
        Input(  FsEntryName name,
                BitField<FsInputOption> options) {
            super(delegate.getInputSocket(name, options));
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            getModel().setContext(NULL);
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            getModel().setContext(NULL);
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            getModel().setContext(NULL);
            return getBoundSocket().newReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            getModel().setContext(NULL);
            return getBoundSocket().newInputStream();
        }
    } // Input

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return new Output(name, options, template);
    }

    private final class Output extends DecoratingOutputSocket<Entry> {
        final FsOperationContext operation;

        Output( FsEntryName name,
                BitField<FsOutputOption> options,
                Entry template) {
            super(delegate.getOutputSocket(name, options, template));
            this.operation = makeContext(options);
            
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            getModel().setContext(operation);
            return getBoundSocket().getLocalTarget();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            getModel().setContext(operation);
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            getModel().setContext(operation);
            return getBoundSocket().newOutputStream();
        }
    } // Output

    @Override
    public void mknod(
            @NonNull FsEntryName name,
            @NonNull Type type,
            @NonNull BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        final FsOperationContext operation = makeContext(options);
        getModel().setContext(operation);
        delegate.mknod(name, type, options, template);
    }

    @Override
    public void unlink(FsEntryName name)
    throws IOException {
        getModel().setContext(NONE);
        delegate.unlink(name);
    }

    @Override
    public <X extends IOException>
    void sync(
            @NonNull final BitField<FsSyncOption> options,
            @NonNull final ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
        getModel().setContext(NONE);
        delegate.sync(options, handler);
    }

    /**
     * Returns an operation object holding the given output options.
     * <p>
     * TODO: Consider reusing the created object by mapping it.
     * 
     * @param  options the options for the output operation in scope.
     * @return A JavaBean which encapsulates the given options for the
     *         {@link FsContextController} operation in scope.
     */
    private static FsOperationContext makeContext(BitField<FsOutputOption> options) {
        FsOperationContext operation = new FsOperationContext();
        operation.setOutputOptions(options);
        return operation;
    }
}
