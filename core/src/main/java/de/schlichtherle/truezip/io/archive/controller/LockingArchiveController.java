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
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
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
final class LockingArchiveController<AE extends ArchiveEntry>
extends FilterArchiveController<AE> {

    LockingArchiveController(
            ArchiveModel<AE> model,
            ArchiveController<AE> target) {
        super(model, target);
    }

    final ReentrantLock readLock() {
        return getModel().readLock();
    }

    final ReentrantLock writeLock() {
        return getModel().writeLock();
    }

    final void ensureNotReadLockedByCurrentThread(
            final NotWriteLockedByCurrentThreadException ex) {
        getModel().ensureNotReadLockedByCurrentThread(ex);
    }

    @Override
    public CommonInputSocket<? extends CommonEntry> newInputSocket(String path)
    throws IOException {
        try {
            readLock().lock();
            try {
                return new LockingInputSocket(target.newInputSocket(path));
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return new LockingInputSocket(target.newInputSocket(path));
            } finally {
                writeLock().unlock();
            }
        }
    }

    private class LockingInputSocket
    extends FilterInputSocket<CommonEntry> {

        protected LockingInputSocket(
                final CommonInputSocket<? extends CommonEntry> target) {
            super(target);
        }

        @Override
        public CommonEntry getTarget() {
            try {
                readLock().lock();
                try {
                    return target.chain(this).getTarget();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedByCurrentThreadException ex) {
                ensureNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return target.chain(this).getTarget();
                } finally {
                    writeLock().unlock();
                }
            }
        }

        /*@Override
        public CommonEntry getPeerTarget() {
            try {
                readLock().lock();
                try {
                    return target.chain(this).getPeerTarget();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedByCurrentThreadException ex) {
                ensureNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return target.chain(this).getPeerTarget();
                } finally {
                    writeLock().unlock();
                }
            }
        }*/

        @Override
        public InputStream newInputStream() throws IOException {
            try {
                readLock().lock();
                try {
                    return target.chain(this).newInputStream();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedByCurrentThreadException ex) {
                ensureNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return target.chain(this).newInputStream();
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
                    return target.chain(this).newReadOnlyFile();
                } finally {
                    readLock().unlock();
                }
            } catch (NotWriteLockedByCurrentThreadException ex) {
                ensureNotReadLockedByCurrentThread(ex);
                writeLock().lock();
                try {
                    return target.chain(this).newReadOnlyFile();
                } finally {
                    writeLock().unlock();
                }
            }
        }
    }

    @Override
    public CommonOutputSocket<? extends CommonEntry> newOutputSocket(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return new LockingOutputSocket(target.newOutputSocket(path, options));
        } finally {
            writeLock().unlock();
        }
    }

    private class LockingOutputSocket
    extends FilterOutputSocket<CommonEntry> {

        protected LockingOutputSocket(
                final CommonOutputSocket<? extends CommonEntry> target) {
            super(target);
        }

        @Override
        public CommonEntry getTarget() {
            ensureNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return target.chain(this).getTarget();
            } finally {
                writeLock().unlock();
            }
        }

        /*@Override
        public CommonEntry getPeerTarget() {
            ensureNotReadLockedByCurrentThread();
            writeLock().lock();
            try {
                return target.chain(this).getPeerTarget();
            } finally {
                writeLock().unlock();
            }
        }*/

        @Override
        public OutputStream newOutputStream() throws IOException {
            ensureNotReadLockedByCurrentThread(null);
            writeLock().lock();
            try {
                return target.chain(this).newOutputStream();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public Icon getOpenIcon()
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return target.getOpenIcon();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return target.getOpenIcon();
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
                return target.getClosedIcon();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return target.getClosedIcon();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public final boolean isReadOnly()
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return target.isReadOnly();
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return target.isReadOnly();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public final Entry<?> getEntry(final String path)
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return target.getEntry(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return target.getEntry(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public final boolean isReadable(final String path)
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return target.isReadable(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return target.isReadable(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public final boolean isWritable(final String path)
    throws FalsePositiveEntryException {
        try {
            readLock().lock();
            try {
                return target.isWritable(path);
            } finally {
                readLock().unlock();
            }
        } catch (NotWriteLockedByCurrentThreadException ex) {
            ensureNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return target.isWritable(path);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public final void setReadOnly(final String path)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            target.setReadOnly(path);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final void setTime(
            final String path,
            final BitField<Access> types,
            final long value)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            target.setTime(path, types, value);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final void mknod(
            final String path,
            final Type type,
            final CommonEntry template,
            final BitField<IOOption> options)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            target.mknod(path, type, template, options);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public final void unlink(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        ensureNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            target.unlink(path, options);
        } finally {
            writeLock().unlock();
        }
    }
}
