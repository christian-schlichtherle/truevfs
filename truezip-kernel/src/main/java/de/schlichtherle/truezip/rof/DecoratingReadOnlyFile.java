/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.rof;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * An abstract decorator for a read only file.
 * <p>
 * Note that sub-classes of this class may implement their own virtual file
 * pointer.
 * Thus, if you would like to use the decorated read only file again after
 * you have finished using the decorating read only file, then you should not
 * assume a particular position of the file pointer of the decorated read only
 * file.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public abstract class DecoratingReadOnlyFile extends AbstractReadOnlyFile {

    /** The nullable decorated read only file. */
    protected @Nullable ReadOnlyFile delegate;

    /**
     * Constructs a new decorating read only file.
     *
     * @param rof the nullable read only file to decorate.
     */
    protected DecoratingReadOnlyFile(final @Nullable ReadOnlyFile rof) {
        this.delegate = rof;
    }

    @Override
    public long length() throws IOException {
        return delegate.length();
    }

    @Override
    public long getFilePointer() throws IOException {
        return delegate.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        delegate.seek(pos);
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(']')
                .toString();
    }
}
