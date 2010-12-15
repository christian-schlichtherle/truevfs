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
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
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
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ConcurrentArchiveController<E extends ArchiveEntry>
extends FilterArchiveController<E, ArchiveController<? extends E>> {

    public ConcurrentArchiveController(ArchiveController<? extends E> controller) {
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
    public Icon getOpenIcon() throws IOException {
        try {
            readLock().lock();
            try {
                return controller.getOpenIcon();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.getOpenIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        try {
            readLock().lock();
            try {
                return controller.getClosedIcon();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.getClosedIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadOnly() throws IOException {
        try {
            readLock().lock();
            try {
                return controller.isReadOnly();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.isReadOnly();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public ArchiveFileSystemEntry<? extends E> getEntry(FileSystemEntryName name)
    throws IOException {
        try {
            readLock().lock();
            try {
                return controller.getEntry(name);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.getEntry(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadable(FileSystemEntryName name) throws IOException {
        try {
            readLock().lock();
            try {
                return controller.isReadable(name);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.isReadable(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isWritable(FileSystemEntryName name) throws IOException {
        try {
            readLock().lock();
            try {
                return controller.isWritable(name);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.isWritable(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public void setReadOnly(FileSystemEntryName name) throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            controller.setReadOnly(name);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public boolean setTime(FileSystemEntryName name, BitField<Access> types, long value)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return controller.setTime(name, types, value);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public InputSocket<E> getInputSocket(   FileSystemEntryName name,
                                            BitField<InputOption> options) {
        return new Input(controller.getInputSocket(name, options));
    }

    private class Input extends FilterInputSocket<E> {
        Input(InputSocket<? extends E> input) {
            super(input);
        }

        @Override
        public E getLocalTarget() throws IOException {
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
    } // class Input

    @Override
    public OutputSocket<E> getOutputSocket( FileSystemEntryName name,
                                            BitField<OutputOption> options,
                                            Entry template) {
        return new Output(controller.getOutputSocket(name, options, template));
    }

    private class Output extends FilterOutputSocket<E> {
        Output(OutputSocket<? extends E> output) {
            super(output);
        }

        @Override
        public E getLocalTarget() throws IOException {
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
    public boolean mknod(   FileSystemEntryName name,
                            Type type,
                            BitField<OutputOption> options,
                            Entry template)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return controller.mknod(name, type, options, template);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public void unlink(FileSystemEntryName name)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            controller.unlink(name);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public <X extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, X> builder,
                final BitField<SyncOption> options)
    throws X, FileSystemException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            controller.sync(builder, options);
        } finally {
            writeLock().unlock();
        }
    }
}
