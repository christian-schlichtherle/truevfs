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
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.DecoratingInputSocket;
import de.schlichtherle.truezip.socket.DecoratingOutputSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.JSE7;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import javax.swing.Icon;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * A concurrent file system controller which decorates another file system
 * controller in order to provide read/write lock features for multi-threaded
 * access by its clients.
 * 
 * @see     FsConcurrentModel
 * @see     FsNotSyncedException
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsSyncController<
        M extends FsModel,
        C extends FsController<? extends M>>
extends FsDecoratingController<M, C> {

    private static final BitField<FsSyncOption>
            AUTO_SYNC_OPTIONS = BitField.of(WAIT_CLOSE_INPUT,
                                            WAIT_CLOSE_OUTPUT);

    private static final SyncSocketFactory
            SYNC_SOCKET_FACTORY = JSE7.AVAILABLE
                ? SyncSocketFactory.NIO2
                : SyncSocketFactory.OIO;

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated concurrent file system controller.
     */
    public FsSyncController(C controller) {
        super(controller);
    }

    private void autoSync() throws FsSyncException {
        sync(AUTO_SYNC_OPTIONS);
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        try {
            return delegate.getOpenIcon();
        } catch (FsNotSyncedException ex) {
            autoSync();
            return delegate.getOpenIcon();
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        try {
            return delegate.getClosedIcon();
        } catch (FsNotSyncedException ex) {
            autoSync();
            return delegate.getClosedIcon();
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        try {
            return delegate.isReadOnly();
        } catch (FsNotSyncedException ex) {
            autoSync();
            return delegate.isReadOnly();
        }
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        try {
            return delegate.getEntry(name);
        } catch (FsNotSyncedException ex) {
            autoSync();
            return delegate.getEntry(name);
        }
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        try {
            return delegate.isReadable(name);
        } catch (FsNotSyncedException ex) {
            autoSync();
            return delegate.isReadable(name);
        }
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        try {
            return delegate.isWritable(name);
        } catch (FsNotSyncedException ex) {
            autoSync();
            return delegate.isWritable(name);
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        try {
            delegate.setReadOnly(name);
        } catch (FsNotSyncedException ex) {
            autoSync();
            delegate.setReadOnly(name);
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        try {
            return delegate.setTime(name, times, options);
        } catch (FsNotSyncedException ex) {
            autoSync();
            return delegate.setTime(name, times, options);
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        try {
            return delegate.setTime(name, types, value, options);
        } catch (FsNotSyncedException ex) {
            autoSync();
            return delegate.setTime(name, types, value, options);
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FsEntryName name,
                                            BitField<FsInputOption> options) {
        return SYNC_SOCKET_FACTORY.newInputSocket(this,
                delegate.getInputSocket(name, options));
    }

    @Override
    public OutputSocket<?> getOutputSocket( FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            Entry template) {
        return SYNC_SOCKET_FACTORY.newOutputSocket(this,
                delegate.getOutputSocket(name, options, template));
    }

    @Override
    public void mknod(
            @NonNull FsEntryName name,
            @NonNull Type type,
            @NonNull BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        try {
            delegate.mknod(name, type, options, template);
        } catch (FsNotSyncedException ex) {
            autoSync();
            delegate.mknod(name, type, options, template);
        }
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        try {
            delegate.unlink(name, options);
        } catch (FsNotSyncedException ex) {
            autoSync();
            delegate.unlink(name, options);
        }
    }

    @Immutable
    private enum SyncSocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController<?, ?> controller,
                    InputSocket<?> input) {
                return controller.new SyncInputSocket(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController<?, ?> controller,
                    OutputSocket<?> output) {
                return controller.new SyncOutputSocket(output);
            }
        },

        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController<?, ?> controller,
                    InputSocket<?> input) {
                return controller.new Nio2SyncInputSocket(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController<?, ?> controller,
                    OutputSocket<?> output) {
                return controller.new Nio2SyncOutputSocket(output);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsSyncController<?, ?> controller,
                InputSocket <?> input);
        
        abstract OutputSocket<?> newOutputSocket(
                FsSyncController<?, ?> controller,
                OutputSocket <?> output);
    } // SyncSocketFactory

    private final class Nio2SyncInputSocket
    extends SyncInputSocket {
        Nio2SyncInputSocket(InputSocket<?> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            try {
                return getBoundSocket().newSeekableByteChannel();
            } catch (FsNotSyncedException ex) {
                autoSync();
                return getBoundSocket().newSeekableByteChannel();
            }
        }
    } // Nio2SyncInputSocket

    private class SyncInputSocket
    extends DecoratingInputSocket<Entry> {
        SyncInputSocket(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FsNotSyncedException ex) {
                autoSync();
                return getBoundSocket().getLocalTarget();
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            try {
                return getBoundSocket().newReadOnlyFile();
            } catch (FsNotSyncedException ex) {
                autoSync();
                return getBoundSocket().newReadOnlyFile();
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                return getBoundSocket().newInputStream();
            } catch (FsNotSyncedException ex) {
                autoSync();
                return getBoundSocket().newInputStream();
            }
        }
    } // SyncInputSocket

    private final class Nio2SyncOutputSocket
    extends SyncOutputSocket {
        Nio2SyncOutputSocket(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            try {
                return getBoundSocket().newSeekableByteChannel();
            } catch (FsNotSyncedException ex) {
                autoSync();
                return getBoundSocket().newSeekableByteChannel();
            }
        }
    } // Nio2SyncOutputSocket

    private class SyncOutputSocket
    extends DecoratingOutputSocket<Entry> {
        SyncOutputSocket(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                return getBoundSocket().getLocalTarget();
            } catch (FsNotSyncedException ex) {
                autoSync();
                return getBoundSocket().getLocalTarget();
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            try {
                return getBoundSocket().newOutputStream();
            } catch (FsNotSyncedException ex) {
                autoSync();
                return getBoundSocket().newOutputStream();
            }
        }
    } // SyncOutputSocket
}
