/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
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
 * A decorating file system controller which performs a
 * {@link FsController#sync(BitField) sync} operation on the
 * file system if and only if any decorated file system controller throws an
 * {@link FsNotSyncedException}.
 * 
 * @param   <M> The type of the file system model shared by the decorator chain
 *          of file system controllers.
 * @see     FsNotSyncedException
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public final class FsSyncController<M extends FsModel>
extends FsDecoratingController<M, FsController<? extends M>> {

    private static final SyncSocketFactory
            SYNC_SOCKET_FACTORY = JSE7.AVAILABLE
                ? SyncSocketFactory.NIO2
                : SyncSocketFactory.OIO;

    /**
     * Constructs a new file system sync controller.
     *
     * @param controller the decorated concurrent file system controller.
     */
    public FsSyncController(FsController<? extends M> controller) {
        super(controller);
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        while (true) {
            try {
                return delegate.getOpenIcon();
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        while (true) {
            try {
                return delegate.getClosedIcon();
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        while (true) {
            try {
                return delegate.isReadOnly();
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public FsEntry getEntry(FsEntryName name)
    throws IOException {
        while (true) {
            try {
                return delegate.getEntry(name);
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isReadable(name);
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        while (true) {
            try {
                return delegate.isWritable(name);
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        while (true) {
            try {
                delegate.setReadOnly(name);
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                return delegate.setTime(name, times, options);
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                return delegate.setTime(name, types, value, options);
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
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
        while (true) {
            try {
                delegate.mknod(name, type, options, template);
                return;
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        while (true) {
            try {
                delegate.unlink(name, options);
                return;
            } catch (FsNotSyncedException ex) {
                delegate.sync(SYNC);
            }
        }
    }

    @Immutable
    private enum SyncSocketFactory {
        OIO() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController<?> controller,
                    InputSocket<?> input) {
                return controller.new Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController<?> controller,
                    OutputSocket<?> output) {
                return controller.new Output(output);
            }
        },

        NIO2() {
            @Override
            InputSocket<?> newInputSocket(
                    FsSyncController<?> controller,
                    InputSocket<?> input) {
                return controller.new Nio2Input(input);
            }

            @Override
            OutputSocket<?> newOutputSocket(
                    FsSyncController<?> controller,
                    OutputSocket<?> output) {
                return controller.new Nio2Output(output);
            }
        };

        abstract InputSocket<?> newInputSocket(
                FsSyncController<?> controller,
                InputSocket <?> input);
        
        abstract OutputSocket<?> newOutputSocket(
                FsSyncController<?> controller,
                OutputSocket <?> output);
    } // SyncSocketFactory

    private final class Nio2Input
    extends Input {
        Nio2Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newSeekableByteChannel();
                } catch (FsNotSyncedException ex) {
                    delegate.sync(SYNC);
                }
            }
        }
    } // Nio2Input

    private class Input
    extends DecoratingInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (FsNotSyncedException ex) {
                    delegate.sync(SYNC);
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newReadOnlyFile();
                } catch (FsNotSyncedException ex) {
                    delegate.sync(SYNC);
                }
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newInputStream();
                } catch (FsNotSyncedException ex) {
                    delegate.sync(SYNC);
                }
            }
        }
    } // Input

    private final class Nio2Output
    extends Output {
        Nio2Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newSeekableByteChannel();
                } catch (FsNotSyncedException ex) {
                    delegate.sync(SYNC);
                }
            }
        }
    } // Nio2Output

    private class Output
    extends DecoratingOutputSocket<Entry> {
        Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().getLocalTarget();
                } catch (FsNotSyncedException ex) {
                    delegate.sync(SYNC);
                }
            }
        }

        @Override
        public Entry getPeerTarget() throws IOException {
            // Same implementation as super class, but makes stack trace nicer.
            return getBoundSocket().getPeerTarget();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            while (true) {
                try {
                    return getBoundSocket().newOutputStream();
                } catch (FsNotSyncedException ex) {
                    delegate.sync(SYNC);
                }
            }
        }
    } // Output
}
