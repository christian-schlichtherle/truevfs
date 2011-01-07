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
package de.schlichtherle.truezip.io.fs.concurrency;

import de.schlichtherle.truezip.io.fs.FSSyncException;
import de.schlichtherle.truezip.io.fs.FSSyncOption;
import de.schlichtherle.truezip.io.fs.FSOutputOption;
import de.schlichtherle.truezip.io.fs.FSInputOption;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.fs.FSController;
import de.schlichtherle.truezip.io.fs.FSEntry;
import de.schlichtherle.truezip.io.fs.FSEntryName;
import de.schlichtherle.truezip.io.fs.FSException;
import de.schlichtherle.truezip.io.fs.FSDecoratorController;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.DecoratorInputSocket;
import de.schlichtherle.truezip.io.socket.DecoratorOutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * Supports multiple concurrent reader threads.
 * 
 * @see     FSConcurrencyModel
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class FSConcurrencyController
extends FSDecoratorController<  FSConcurrencyModel,
                                FSController<? extends FSConcurrencyModel>> {

    private volatile ReadLock readLock;
    private volatile WriteLock writeLock;

    /**
     * Constructs a new concurrent file system controller.
     *
     * @param controller the decorated file system controller.
     */
    public FSConcurrencyController(
            @NonNull FSController<? extends FSConcurrencyModel> controller) {
        super(controller);
    }

    private ReadLock readLock() {
        return null != readLock ? readLock : (readLock = getModel().readLock());
    }

    private WriteLock writeLock() {
        return null != writeLock ? writeLock : (writeLock = getModel().writeLock());
    }

    private void assertNotReadLockedByCurrentThread(FSNotWriteLockedException ex)
    throws FSNotWriteLockedException {
        getModel().assertNotReadLockedByCurrentThread(ex);
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.getOpenIcon();
            } finally {
                readLock().unlock();
            }
        } catch (FSNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.getOpenIcon();
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
                return delegate.getClosedIcon();
            } finally {
                readLock().unlock();
            }
        } catch (FSNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.getClosedIcon();
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
                return delegate.isReadOnly();
            } finally {
                readLock().unlock();
            }
        } catch (FSNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.isReadOnly();
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public FSEntry getEntry(FSEntryName name)
    throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.getEntry(name);
            } finally {
                readLock().unlock();
            }
        } catch (FSNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.getEntry(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isReadable(FSEntryName name) throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.isReadable(name);
            } finally {
                readLock().unlock();
            }
        } catch (FSNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.isReadable(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public boolean isWritable(FSEntryName name) throws IOException {
        try {
            readLock().lock();
            try {
                return delegate.isWritable(name);
            } finally {
                readLock().unlock();
            }
        } catch (FSNotWriteLockedException ex) {
            assertNotReadLockedByCurrentThread(ex);
            writeLock().lock();
            try {
                return delegate.isWritable(name);
            } finally {
                writeLock().unlock();
            }
        }
    }

    @Override
    public void setReadOnly(FSEntryName name) throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            delegate.setReadOnly(name);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public boolean setTime(FSEntryName name, BitField<Access> types, long value)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            return delegate.setTime(name, types, value);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public InputSocket<?> getInputSocket(   FSEntryName name,
                                            BitField<FSInputOption> options) {
        return new Input(delegate.getInputSocket(name, options));
    }

    private class Input extends DecoratorInputSocket<Entry> {
        Input(InputSocket<?> input) {
            super(input);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            try {
                readLock().lock();
                try {
                    return getBoundSocket().getLocalTarget();
                } finally {
                    readLock().unlock();
                }
            } catch (FSNotWriteLockedException ex) {
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
            } catch (FSNotWriteLockedException ex) {
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
            } catch (FSNotWriteLockedException ex) {
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
    public OutputSocket<?> getOutputSocket( FSEntryName name,
                                            BitField<FSOutputOption> options,
                                            Entry template) {
        return new Output(delegate.getOutputSocket(name, options, template));
    }

    private class Output extends DecoratorOutputSocket<Entry> {
        Output(OutputSocket<?> output) {
            super(output);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
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
    public void mknod(
            @NonNull FSEntryName name,
            @NonNull Type type,
            @NonNull BitField<FSOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            delegate.mknod(name, type, options, template);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public void unlink(FSEntryName name)
    throws IOException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            delegate.unlink(name);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public <X extends IOException>
    void sync(
            @NonNull final BitField<FSSyncOption> options,
            @NonNull final ExceptionHandler<? super FSSyncException, X> handler)
    throws X, FSException {
        assertNotReadLockedByCurrentThread(null);
        writeLock().lock();
        try {
            delegate.sync(options, handler);
        } finally {
            writeLock().unlock();
        }
    }
}
