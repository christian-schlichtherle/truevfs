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

import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.input.FilterInputSocket;
import de.schlichtherle.truezip.io.socket.output.FilterOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.Icon;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class LockingArchiveController extends ArchiveController {

    private final ArchiveController controller;

    LockingArchiveController(ArchiveModel model, ArchiveController controller) {
        super(model);
        this.controller = controller;
    }

    ReentrantLock readLock() {
        return getModel().readLock();
    }

    ReentrantLock writeLock() {
        return getModel().writeLock();
    }

    void ensureNotReadLockedByCurrentThread(
            NotWriteLockedByCurrentThreadException ex) {
        getModel().ensureNotReadLockedByCurrentThread(ex);
    }

    @Override
    public Icon getOpenIcon()
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return controller.getOpenIcon();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.getOpenIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public Icon getClosedIcon()
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return controller.getClosedIcon();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.getClosedIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadOnly()
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return controller.isReadOnly();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.isReadOnly();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public Entry<?> getEntry(final String path)
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return controller.getEntry(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.getEntry(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadable(final String path)
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return controller.isReadable(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.isReadable(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isWritable(final String path)
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return controller.isWritable(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return controller.isWritable(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public void setReadOnly(final String path)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            controller.setReadOnly(path);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public void setTime(
            final String path,
            final BitField<Access> types,
            final long value)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            controller.setTime(path, types, value);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public CommonInputSocket<?> newInputSocket(String path)
    throws IOException {
        try {
            readLock().lock();
            try {
                return new InputSocket(controller.newInputSocket(path));
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return new InputSocket(controller.newInputSocket(path));
            } finally {
                writeLock().unlock();
            }
        }
    }

    private class InputSocket
    extends FilterInputSocket<CommonEntry> {

        protected InputSocket(
                final CommonInputSocket<? extends CommonEntry> target) {
            super(target);
        }

        @Override
        public CommonEntry getTarget() {
            try {
                readLock().lock();
                try {
                    return input.share(this).getTarget();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedByCurrentThreadException ex) {
                ensureNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return input.share(this).getTarget();
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
                    return input.share(this).newInputStream();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedByCurrentThreadException ex) {
                ensureNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return input.share(this).newInputStream();
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
                    return input.share(this).newReadOnlyFile();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedByCurrentThreadException ex) {
                ensureNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return input.share(this).newReadOnlyFile();
                } finally {
                    writeLock().unlock();
                }
            }
        }
    }

    @Override
    public CommonOutputSocket<?> newOutputSocket(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return new OutputSocket(controller.newOutputSocket(path, options));
        } finally {
            writeLock().unlock();
        }
    }

    private class OutputSocket
    extends FilterOutputSocket<CommonEntry> {

        protected OutputSocket(
                final CommonOutputSocket<? extends CommonEntry> target) {
            super(target);
        }

        @Override
        public CommonEntry getTarget() {
            ensureNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return output.share(this).getTarget();
            } finally {
                writeLock().unlock();
            }
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            ensureNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return output.share(this).newOutputStream();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public void mknod(
            final String path,
            final Type type,
            final CommonEntry template,
            final BitField<OutputOption> options)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            controller.mknod(path, type, template, options);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public void unlink(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            controller.unlink(path, options);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public void sync(ArchiveSyncExceptionBuilder builder, BitField<SyncOption> options)
    throws ArchiveSyncException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            controller.sync(builder, options);
        } finally {
            writeLock().unlock();
        }
    }
}
