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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.FilterInputStream;
import de.schlichtherle.truezip.io.FilterOutputStream;
import de.schlichtherle.truezip.io.rof.FilterReadOnlyFile;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides statistics for the file systems managed by a single file system
 * manager.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileSystemStatistics {

    private final FileSystemManager manager;
    private volatile long read;
    private volatile long written;

    FileSystemStatistics(final FileSystemManager provider) {
        this.manager = provider;
    }

    ReadOnlyFile countBytes(ReadOnlyFile rof) {
        return new CountingReadOnlyFile(rof);
    }
    
    private final class CountingReadOnlyFile extends FilterReadOnlyFile {
        CountingReadOnlyFile(ReadOnlyFile rof) {
            super(rof);
        }

        @Override
        public int read() throws IOException {
            int ret = rof.read();
            if (0 < ret)
                read++;
            return ret;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int ret = rof.read(b, off, len);
            if (0 < ret)
                read += ret;
            return ret;
        }
    } // CountingReadOnlyFile

    InputStream countBytes(InputStream in) {
        return new CountingInputStream(in);
    }

    private final class CountingInputStream extends FilterInputStream {
        CountingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int ret = in.read();
            if (0 < ret)
                read++;
            return ret;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int ret = in.read(b, off, len);
            if (0 < ret)
                read += ret;
            return ret;
        }
    } // CountingInputStream

    /**
     * Returns the total number of bytes read from all <em>top level file
     * systems</em> which have been updated by a call to
     * {@link FileSystemManager#sync(URI, ExceptionBuilder, BitField)}.
     * <p>
     * Please note that this method counts only input from top level file
     * systems which have been touched, i.e. archive files which are actually
     * updated and are not contained in other file systems and hence are
     * present in the host file system.
     * <p>
     * This method is intended to be used for progress monitors and is a rough
     * indicator about what is going on inside the TrueZIP API.
     * The return value will be reset automatically where required,
     * so if this value is going to {@code 0} again you know that a knew
     * update cycle has begun.
     * Other than this, you should not rely on its actual value.
     * <p>
     * For an example how to use this please refer to the source
     * code for {@code nzip.ProgressMonitor} in the base package.
     *
     * @see FileSystemManager#sync(URI, ExceptionBuilder, BitField)
     */
    public long getSyncTotalByteCountRead() {
        return read;
    }

    OutputStream countBytes(OutputStream out) {
        return new CountingOutputStream(out);
    }

    private final class CountingOutputStream extends FilterOutputStream {
        CountingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            written += len;
        }
    } // class CountingOutputStream

    /**
     * Returns the total number of bytes written to all <em>top level file
     * systems</em> which have been updated by a call to
     * {@link FileSystemManager#sync(URI, ExceptionBuilder, BitField)}.
     * <p>
     * Please note that this method counts only output to top level file
     * systems which have been touched, i.e. archive files which are actually
     * updated and are not contained in other file systems and hence are
     * present in the host file system.
     * <p>
     * This method is intended to be used for progress monitors and is a rough
     * indicator about what is going on inside the TrueZIP API.
     * The return value will be reset automatically where required,
     * so if this value is going to {@code 0} again you know that a knew
     * update cycle has begun.
     * Other than this, you should not rely on its actual value.
     * <p>
     * For an example how to use this please refer to the source
     * code for {@code nzip.ProgressMonitor} in the base package.
     *
     * @see FileSystemManager#sync(URI, ExceptionBuilder, BitField)
     */
    public long getSyncTotalByteCountWritten() {
        return written;
    }

    /**
     * Returns the total number of file systems processed.
     */
    public int getFileSystemsTotal() {
        return manager.getControllers().size();
    }

    /**
     * Returns the number of file systems which have been touched and
     * need synchronization by calling
     * {@link FileSystemManager#sync(URI, ExceptionBuilder, BitField)}.
     * Note that you should <em>not</em> use the returned value to call this
     * method conditionally - this is unreliable!
     * Instead, you should always call one of those methods unconditionally.
     */
    public int getFileSystemsTouched() {
        int result = 0;
        for (FederatedFileSystemController<?> controller : manager.getControllers())
            if (controller.getModel().isTouched())
                result++;
        return result;
    }

    /**
     * Returns the total number of top level file systems processed.
     */
    public int getTopLevelFileSystemsTotal() {
        int result = 0;
        for (FederatedFileSystemController<?> controller : manager.getControllers())
            if (null == controller.getModel().getParent())
                result++;
        return result;
    }

    /**
     * Returns the number of top level file systems which have been touched and
     * need synchronization by calling
     * {@link FileSystemManager#sync(URI, ExceptionBuilder, BitField)}.
     * Note that you should <em>not</em> use the returned value to call this
     * method conditionally - this is unreliable!
     * Instead, you should always call one of those methods unconditionally.
     */
    public int getTopLevelFileSystemsTouched() {
        int result = 0;
        for (FederatedFileSystemController<?> controller : manager.getControllers()) {
            final FileSystemModel model = controller.getModel();
            if (null == model.getParent() && model.isTouched())
                result++;
        }
        return result;
    }
}
