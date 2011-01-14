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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;

/**
 * A decorator for a read only file.
 * <p>
 * Note that subclasses of this class may implement their own virtual file
 * pointer.
 * Thus, if you would like to access the underlying {@code ReadOnlyFile}
 * again after you have finished working with the
 * {@code FilteredReadOnlyFile}, you should synchronize its file pointer like
 * this:
 * <pre>
 *     ReadOnlyFile rof = new SimpleReadOnlyFile(new File("HelloWorld.java"));
 *     try {
 *         ReadOnlyFile frof = new FilteredReadOnlyFile(rof);
 *         try {
 *             // Do any file input on brof here...
 *             frof.seek(1);
 *         } finally {
 *             // Synchronize the file pointers.
 *             rof.seek(frof.getFilePointer());
 *         }
 *         // This assertion would fail if we hadn't done the file pointer
 *         // synchronization!
 *         assert rof.getFilePointer() == 1;
 *     } finally {
 *         rof.close();
 *     }
 * </pre>
 * This does not apply to this base class, however.
 * <p>
 * Subclasses implemententing their own virtual file pointer should add a note
 * referring to this classes Javadoc like this:
 * <blockquote>
 * <b>Note:</b> This class implements its own virtual file pointer.
 * Thus, if you would like to access the underlying {@code ReadOnlyFile}
 * again after you have finished working with an instance of this class,
 * you should synchronize their file pointers using the pattern as described
 * in {@link DecoratingReadOnlyFile}.
 * </blockquote>
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public abstract class DecoratingReadOnlyFile extends AbstractReadOnlyFile {

    /** The nullable decorated read only file. */
    @Nullable
    protected ReadOnlyFile delegate;

    /**
     * Creates a new instance of {@code DecoratingReadOnlyFile},
     * which filters the given read only file.
     */
    protected DecoratingReadOnlyFile(ReadOnlyFile rof) {
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
}
