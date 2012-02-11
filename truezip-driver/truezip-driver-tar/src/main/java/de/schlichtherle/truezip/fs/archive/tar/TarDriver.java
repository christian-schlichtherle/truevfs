/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.COMPRESS;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.socket.*;
import de.schlichtherle.truezip.util.BitField;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.WillNotClose;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

/**
 * An archive driver for Tape Archive files (TAR).
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class TarDriver extends FsCharsetArchiveDriver<TarDriverEntry> {

    /**
     * The default character set for entry names and comments, which is
     * {@code "US-ASCII"}.
     */
    public static final Charset TAR_CHARSET = Charset.forName("US-ASCII");

    private final IOPool<?> ioPool;

    public TarDriver(final IOPoolProvider provider) {
        super(TAR_CHARSET);
        this.ioPool = provider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The implementation in the class {@link TarDriver} returns
     *         {@code true} because when reading a TAR file sequentially,
     *         each TAR entry should &quot;override&quot; any previously read
     *         TAR entry with an equal name.
     */
    @Override
    public boolean getRedundantContentSupport() {
        return true;
    }

    @Override
    protected final IOPool<?> getPool() {
        return ioPool;
    }

    /**
     * Clears {@link FsInputOption#CACHE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public InputSocket<?> getInputSocket(   FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsInputOption> options) {
        return controller.getInputSocket(
                name,
                options.clear(FsInputOption.CACHE));
    }

    /**
     * Sets {@link FsOutputOption#COMPRESS} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<FsOutputOption> options,
                                            @CheckForNull Entry template) {
        return controller.getOutputSocket(name, options.set(COMPRESS), template);
    }

    @Override
    public TarDriverEntry newEntry(
            String name,
            final Type type,
            final Entry template,
            final BitField<FsOutputOption> mknod)
    throws CharConversionException {
        assertEncodable(name);
        name = toZipOrTarEntryName(name, type);
        final TarDriverEntry entry;
        if (template instanceof TarArchiveEntry) {
            entry = newEntry(name, (TarArchiveEntry) template);
        } else {
            entry = newEntry(name);
            if (null != template) {
                entry.setModTime(template.getTime(WRITE));
                entry.setSize(template.getSize(DATA));
            }
        }
        return entry;
    }

    public TarDriverEntry newEntry(String name) {
        return new TarDriverEntry(name);
    }

    public TarDriverEntry newEntry(String name, TarArchiveEntry template) {
        return new TarDriverEntry(name, template);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TarDriver} acquires an input
     * stream from the given socket and forwards the call to
     * {@link #newTarInputShop}.
     */
    @Override
    public TarInputShop newInputShop(FsModel model, InputSocket<?> input)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        final InputStream in = input.newInputStream();
        try {
            return newTarInputShop(model, in);
        } finally {
            in.close();
        }
    }

    protected TarInputShop newTarInputShop(
            FsModel model,
            @WillCloseWhenClosed InputStream in)
    throws IOException {
        assert null != model;
        return new TarInputShop(this, in);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TarDriver} acquires an output
     * stream from the given socket, forwards the call to
     * {@link #newTarOutputShop} and wraps the result in a new
     * {@link FsMultiplexedOutputShop}.
     */
    @Override
    public OutputShop<TarDriverEntry> newOutputShop(
            FsModel model,
            OutputSocket<?> output,
            InputShop<TarDriverEntry> source)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        final OutputStream out = output.newOutputStream();
        try {
            return new FsMultiplexedOutputShop<TarDriverEntry>(
                    newTarOutputShop(model, out, (TarInputShop) source),
                    getPool());
        } catch (IOException ex) {
            try {
                out.close();
            } catch (IOException ex2) {
                throw (IOException) ex2.initCause(ex);
            }
            throw ex;
        }
    }

    protected TarOutputShop newTarOutputShop(
            FsModel model,
            OutputStream out,
            @CheckForNull @WillNotClose TarInputShop source)
    throws IOException {
        assert null != model;
        return new TarOutputShop(this, out);
    }
}
