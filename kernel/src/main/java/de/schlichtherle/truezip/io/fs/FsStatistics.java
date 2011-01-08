/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.DecoratorInputStream;
import de.schlichtherle.truezip.io.DecoratorOutputStream;
import de.schlichtherle.truezip.io.rof.DecoratorReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * Provides statistics for the federated file systems managed by a single file
 * system manager.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public final class FsStatistics {

    private final FsStatisticsManager manager;
    private volatile long read;
    private volatile long written;

    FsStatistics(final FsStatisticsManager manager) {
        this.manager = manager;
    }

    /**
     * Returns the total number of managed federated file systems.
     */
    public int getFileSystemsTotal() {
        return manager.getSize();
    }

    /**
     * Returns the number of managed federated file systems which have been
     * touched and need synchronization by calling
     * {@link FsStatisticsManager#sync}.
     * <p>
     * Note that you should <em>not</em> use the returned value to synchronize
     * conditionally - this is unreliable!
     */
    public int getFileSystemsTouched() {
        int result = 0;
        for (FsController<?> controller : manager)
            if (controller.getModel().isTouched())
                result++;
        return result;
    }

    /**
     * Returns the total number of managed <em>top level</em> federated file
     * systems.
     */
    public int getTopLevelFileSystemsTotal() {
        int result = 0;
        for (FsController<?> controller : manager)
            if (null == controller.getParent().getParent())
                result++;
        return result;
    }

    /**
     * Returns the number of managed <em>top level</em> federated file systems
     * which have been touched and need synchronization by calling
     * {@link FsStatisticsManager#sync}.
     */
    public int getTopLevelFileSystemsTouched() {
        int result = 0;
        for (FsController<?> controller : manager) {
            if (null == controller.getParent().getParent())
                if (controller.getModel().isTouched())
                    result++;
        }
        return result;
    }

    ReadOnlyFile countBytes(ReadOnlyFile rof) {
        assert !isClosed();
        return new CountingReadOnlyFile(rof);
    }

    private final class CountingReadOnlyFile extends DecoratorReadOnlyFile {
        CountingReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public int read() throws IOException {
            int ret = delegate.read();
            if (0 < ret)
                read++;
            return ret;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int ret = delegate.read(b, off, len);
            if (0 < ret)
                read += ret;
            return ret;
        }
    } // CountingReadOnlyFile

    InputStream countBytes(InputStream in) {
        assert !isClosed();
        return new CountingInputStream(in);
    }

    private final class CountingInputStream extends DecoratorInputStream {
        CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int ret = delegate.read();
            if (0 < ret)
                read++;
            return ret;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int ret = delegate.read(b, off, len);
            if (0 < ret)
                read += ret;
            return ret;
        }
    } // CountingInputStream

    /**
     * Returns the total number of bytes read from all managed
     * <em>top level</em> federated file systems, i.e. all managed federated
     * file systems which have a parent file system which is not a member of
     * another parent file system.
     * <p>
     * This method is intended to be used to monitor the progress of the
     * method {@link FsStatisticsManager#sync}.
     */
    public long getTopLevelRead() {
        return read;
    }

    OutputStream countBytes(OutputStream out) {
        assert !isClosed();
        return new CountingOutputStream(out);
    }

    private final class CountingOutputStream extends DecoratorOutputStream {
        CountingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            delegate.write(b);
            written++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            delegate.write(b, off, len);
            written += len;
        }
    } // class CountingOutputStream

    /**
     * Returns the total number of bytes written to all managed
     * <em>top level</em> federated file systems, i.e. all managed federated
     * file systems which have a parent file system which is not a member of
     * another parent file system.
     * <p>
     * This method is intended to be used to monitor the progress of the
     * method {@link FsStatisticsManager#sync}.
     */
    public long getTopLevelWritten() {
        return written;
    }

    /**
     * Returns {@code true} iff this statistics instance has been closed and
     * should not receive any more updates.
     *
     * @return {@code true} iff this statistics instance has been closed and
     *         should not receive any more updates.
     */
    public boolean isClosed() {
        return this != manager.getStatistics();
    }
}
