/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;

/**
 * Drop-in replacement for
 * {@link java.util.zip.ZipOutputStream java.util.zip.ZipOutputStream}.
 * <p>
 * This class starts writing ordinary ZIP32 File Format.
 * It automatically adds ZIP64 extensions if required,
 * i.e. if the file size exceeds 4GB or more than 65535 entries are written.
 * This implies that the class may produce ZIP archive files which cannot
 * be read by older ZIP implementations.
 * <p>
 * If the system property {@code de.schlichtherle.truezip.io.zip.zip64ext}
 * is set to {@code true} (case is ignored),
 * then ZIP64 extensions are always added when writing a ZIP archive file,
 * regardless of its size.
 * This system property is primarily intended for unit testing purposes.
 * During normal operations, it should not be set as many
 * third party tools would not treat the redundant ZIP64 extensions
 * correctly.
 * Note that it's impossible to inhibit ZIP64 extensions.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @see ZipFile
 */
public class ZipOutputStream extends RawZipOutputStream<ZipEntry> {
    
    /**
     * Creates a new ZIP output stream decorating the given output stream,
     * using the {@value de.schlichtherle.truezip.io.zip.RawZipOutputStream#DEFAULT_CHARSET} charset.
     *
     * @throws NullPointerException If {@code out} is {@code null}.
     */
    public ZipOutputStream(final OutputStream out) {
        super(out);
    }

    /**
     * Creates a new ZIP output stream decorating the given output stream.
     *
     * @throws NullPointerException If {@code out} or {@code charset} are
     *         {@code null}.
     * @throws UnsupportedCharsetException If {@code charset} is not supported
     *         by this JVM.
     */
    public ZipOutputStream(final OutputStream out, final String charset) {
        super(out, charset);
    }

    public ZipOutputStream(
            final OutputStream out,
            final String charset,
            final RawZipFile<ZipEntry> appendee)
    throws ZipException {
        super(out, charset, appendee);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    /**
     * Returns a safe enumeration of clones for all entries written so far.
     * This method takes a snapshot of the collection of all entries and
     * enumerates their clones, so concurrent modifications or state changes
     * do not affect this instance, the returned enumeration or the
     * enumerated ZIP entries.
     *
     * @deprecated Use {@link #iterator()} instead.
     */
    @SuppressWarnings("dep-ann")
	@Override
    public synchronized Enumeration<? extends ZipEntry> entries() {
        class CloneEnumeration implements Enumeration<ZipEntry> {
            final Enumeration<? extends ZipEntry> e
                    = Collections.enumeration(Collections.list(
                        ZipOutputStream.super.entries()));

            @Override
			public boolean hasMoreElements() {
                return e.hasMoreElements();
            }

            @Override
			public ZipEntry nextElement() {
                return e.nextElement().clone();
            }
        }
        return new CloneEnumeration();
    }

    /**
     * Returns a safe iteration of clones for all entries written to this ZIP
     * file so far.
     * This method takes a snapshot of the collection of all entries and
     * clones them while iterating, so concurrent modifications or state
     * changes do not affect this instance, the returned enumeration or the
     * enumerated ZIP entries.
     * The iteration does not support element removal.
     */
    @Override
    public synchronized Iterator<ZipEntry> iterator() {
        class EntryIterator implements Iterator<ZipEntry> {
            private final Iterator<ZipEntry> i;

            private EntryIterator() {
                List<ZipEntry> l = new ArrayList<ZipEntry>(ZipOutputStream.super.size());
                Iterator<ZipEntry> si = ZipOutputStream.super.iterator();
                while (si.hasNext())
                    l.add(si.next());
                i = l.iterator();
            }

            @Override
			public boolean hasNext() {
                return i.hasNext();
            }

            @Override
			public ZipEntry next() {
                return i.next().clone();
            }

            @Override
			public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        return new EntryIterator();
    }

    /**
     * Returns a clone of the entry for the given name or {@code null} if no
     * entry with this name exists.
     *
     * @param name the name of the ZIP entry.
     */
    @Override
    public synchronized ZipEntry getEntry(String name) {
        final ZipEntry entry = super.getEntry(name);
        return entry != null ? entry.clone() : null;
    }

    @Override
    public synchronized void setComment(String comment) {
        super.setComment(comment);
    }

    @Override
    public synchronized String getComment() {
        return super.getComment();
    }
    
    @Override
    public synchronized void setLevel(int level) {
        super.setLevel(level);
    }

    @Override
    public synchronized int getLevel() {
        return super.getLevel();
    }

    @Override
    public synchronized int getMethod() {
        return super.getMethod();
    }

    @Override
    public synchronized void setMethod(int method) {
        super.setMethod(method);
    }

    @Override
    public synchronized long length() {
        return super.length();
    }

    @Override
    public synchronized final boolean isBusy() {
        return super.isBusy();
    }

    @Override
    public synchronized void putNextEntry(
            final ZipEntry entry,
            final boolean deflate)
    throws IOException {
        super.putNextEntry(entry, deflate);
    }

    @Override
    public synchronized void write(int b)
    throws IOException {
        super.write(b);
    }

    @Override
    public synchronized void write(final byte[] b, final int off, final int len)
    throws IOException {
        super.write(b, off, len);
    }

    @Override
    public synchronized void closeEntry() throws IOException {
        super.closeEntry();
    }

    @Override
    public synchronized void finish() throws IOException {
        super.finish();
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
    }
}
