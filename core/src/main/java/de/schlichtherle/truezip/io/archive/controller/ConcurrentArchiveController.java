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

import de.schlichtherle.truezip.io.archive.model.NotWriteLockedException;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.FilterInputSocket;
import de.schlichtherle.truezip.io.socket.FilterOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.Icon;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ConcurrentArchiveController<AE extends ArchiveEntry>
extends FilterArchiveController<AE> {

    public ConcurrentArchiveController(ArchiveController<? extends AE> controller) {
        super(controller);
    }

    private ReentrantLock readLock() {
        return getModel().readLock();
    }

    private ReentrantLock writeLock() {
        return getModel().writeLock();
    }

    private void assertNotReadLockedByCurrentThread(NotWriteLockedException ex)
    throws NotWriteLockedException {
        getModel().assertNotReadLockedByCurrentThread(ex);
    }

    @Override
    public Icon getOpenIcon()
    throws FileSystemException {
        try {
            readLock().lock();
            try {
                return getController().getOpenIcon();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return getController().getOpenIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public Icon getClosedIcon()
    throws FileSystemException {
        try {
            readLock().lock();
            try {
                return getController().getClosedIcon();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return getController().getClosedIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadOnly()
    throws FileSystemException {
        try {
            readLock().lock();
            try {
                return getController().isReadOnly();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return getController().isReadOnly();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public ArchiveFileSystemEntry<? extends AE> getEntry(String path)
    throws FileSystemException {
        try {
            readLock().lock();
            try {
                return getController().getEntry(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return getController().getEntry(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadable(String path)
    throws FileSystemException {
        try {
            readLock().lock();
            try {
                return getController().isReadable(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return getController().isReadable(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isWritable(String path)
    throws FileSystemException {
        try {
            readLock().lock();
            try {
                return getController().isWritable(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return getController().isWritable(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public void setReadOnly(String path)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            getController().setReadOnly(path);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public boolean setTime( String path, BitField<Access> types, long value)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return getController().setTime(path, types, value);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public InputSocket<AE> getInputSocket(  String path,
                                            BitField<InputOption> options) {
        return new Input(getController().getInputSocket(path, options));
    }

    private class Input extends FilterInputSocket<AE> {
        Input(InputSocket<? extends AE> input) {
            super(input);
        }

        @Override
        public AE getLocalTarget() throws IOException {
            try {
                readLock().lock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedException ex) {
                assertNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    writeLock().unlock();
                }
            }
        }

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                readLock().lock();
                try {
                    return getBoundSocket().newInputStream();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedException ex) {
                assertNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return getBoundSocket().newInputStream();
                } finally {
                    writeLock().unlock();
                }
            }
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            try {
                readLock().lock();
                try {
                    return getBoundSocket().newReadOnlyFile();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedException ex) {
                assertNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return getBoundSocket().newReadOnlyFile();
                } finally {
                    writeLock().unlock();
                }
            }
        }
    } // class Input

    @Override
    public OutputSocket<AE> getOutputSocket(String path,
                                            BitField<OutputOption> options,
                                            Entry template) {
        return new Output(getController().getOutputSocket(path, options, template));
    }

    private class Output extends FilterOutputSocket<AE> {
        Output(OutputSocket<? extends AE> output) {
            super(output);
        }

        @Override
        public AE getLocalTarget() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return getBoundSocket().getLocalTarget();
            } finally {
                writeLock().unlock();
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            assertNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return getBoundSocket().newOutputStream();
            } finally {
                writeLock().unlock();
            }
        }
    } // class Output

    @Override
    public boolean mknod(   String path,
                            Type type,
                            BitField<OutputOption> options,
                            Entry template)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return getController().mknod(path, type, options, template);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public void unlink(String path)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            getController().unlink(path);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public <E extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, E> builder,
                final BitField<SyncOption> options)
    throws E, FileSystemException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            getController().sync(builder, options);
        } finally {
            writeLock().unlock();
        }
    }
}
