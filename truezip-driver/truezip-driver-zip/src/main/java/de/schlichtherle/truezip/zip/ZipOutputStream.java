/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.fs.archive.FsArchiveFileSystem;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Writes a ZIP file.
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
 *
 * @see     ZipFile
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
public class ZipOutputStream extends RawZipOutputStream<ZipEntry> {

    /**
     * The number of entries which can be additionally accomodated by
     * the internal hash map without resizing it, which is {@value}.
     * When a new ZIP file is created, this constant is used in order to
     * compute the initial capacity of the internal hash map.
     * When an existing ZIP file is appended to, this constant is added to the
     * number of entries in order to compute the initial capacity of the
     * internal hash map.
     * 
     * @since  TrueZIP 7.3
     */
    public static final int OVERHEAD_SIZE = FsArchiveFileSystem.OVERHEAD_SIZE;

    /**
     * The default character set used for entry names and comments in ZIP files.
     * This is {@code "UTF-8"} for compatibility with Sun's JDK implementation.
     */
    public static final Charset DEFAULT_CHARSET = Constants.DEFAULT_CHARSET;

    private static final ZipOutputStreamParameters DEFAULT_PARAM
            = new DefaultZipOutputStreamParameters(DEFAULT_CHARSET);

    private @CheckForNull ZipCryptoParameters cryptoParameters;

    /**
     * Constructs a ZIP output stream which decorates the given output stream
     * using the {@code "UTF-8"} charset.
     * 
     * @param  out The output stream to write the ZIP file to.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ZipOutputStream(@WillCloseWhenClosed OutputStream out) {
        super(out, null, DEFAULT_PARAM);
    }

    /**
     * Constructs a ZIP output stream which decorates the given output stream
     * using the given charset.
     *
     * @param  out The output stream to write the ZIP file to.
     * @param  charset the character set to use.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ZipOutputStream(@WillCloseWhenClosed OutputStream out, Charset charset) {
        super(out, null, new DefaultZipOutputStreamParameters(charset));
    }

    /**
     * Constructs a ZIP output stream which decorates the given output stream
     * and appends to the given ZIP file.
     * 
     * @param  out The output stream to write the ZIP file to.
     *         If {@code appendee} is not {@code null}, then this must be set
     *         up so that it appends to the same ZIP file from which
     *         {@code appendee} is reading.
     * @param  appendee the ZIP file to append to.
     *         This may already be closed.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public ZipOutputStream(@WillCloseWhenClosed OutputStream out, ZipFile appendee) {
        super(out, appendee, DEFAULT_PARAM);
        if (null == appendee)
            throw new NullPointerException();
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
    public Iterator<ZipEntry> iterator() {
        final class EntryIterator implements Iterator<ZipEntry> {
            final Iterator<ZipEntry> i;

            EntryIterator() {
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
        } // class EntryIterator

        return new EntryIterator();
    }

    /**
     * Returns a clone of the entry for the given name or {@code null} if no
     * entry with this name exists.
     *
     * @param name the name of the ZIP entry.
     */
    @Override
    public ZipEntry getEntry(String name) {
        final ZipEntry entry = super.getEntry(name);
        return entry != null ? entry.clone() : null;
    }

    @Override
    public @Nullable ZipCryptoParameters getCryptoParameters() {
        return cryptoParameters;
    }

    /**
     * Sets the parameters for encryption or authentication of entries.
     * <p>
     * Note that only {@link WinZipAesParameters WinZip AES encryption} is
     * currently supported.
     * 
     * @param cryptoParameters the parameters for encryption or authentication
     *        of entries.
     * @since TrueZIP 7.3
     */
    public void setCryptoParameters(
            final @CheckForNull ZipCryptoParameters cryptoParameters) {
        this.cryptoParameters = cryptoParameters;
    }
}