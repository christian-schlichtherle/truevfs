/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.OutputSocket;
import de.schlichtherle.truezip.entry.InputSocket;
import de.schlichtherle.truezip.entry.IOPool;
import de.schlichtherle.truezip.entry.IOPoolProvider;
import de.schlichtherle.truezip.entry.InputService;
import de.schlichtherle.truezip.entry.OutputService;
import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.entry.Entry.Size.DATA;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.entry.MultiplexedOutputService;
import de.schlichtherle.truezip.fs.addr.FsEntryName;
import de.schlichtherle.truezip.fs.option.FsInputOption;
import de.schlichtherle.truezip.fs.option.FsOutputOption;
import static de.schlichtherle.truezip.fs.option.FsOutputOption.COMPRESS;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
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
 * By default, TAR files use the US-ASCII character set for the encoding of
 * entry names.
 * This configuration pretty much constraints the applicability of this file
 * format to North American countries.
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public class TarDriver extends FsCharsetArchiveDriver<TarDriverEntry> {

    /**
     * The default character set for entry names and comments, which is
     * {@code "US-ASCII"}.
     */
    public static final Charset TAR_CHARSET = Charset.forName("US-ASCII");

    private final IOPool<?> ioPool;

    /**
     * Constructs a new TAR driver.
     *
     * @param provider the provider for the I/O buffer pool.
     */
    public TarDriver(final IOPoolProvider provider) {
        super(TAR_CHARSET);
        if (null == (this.ioPool = provider.get()))
            throw new NullPointerException();
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
     * {@link #newTarInputService}.
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public TarInputService newInputService(FsModel model, InputSocket<?> input)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        TarInputService ia = null;
        final InputStream is = input.newInputStream();
        try {
            return ia = newTarInputService(model, is);
        } finally {
            try {
                is.close();
            } catch (final IOException ex) {
                if (null != ia)
                    ia.close();
                throw ex;
            }
        }
    }

    @CreatesObligation
    protected TarInputService newTarInputService(
            FsModel model,
            @WillCloseWhenClosed InputStream in)
    throws IOException {
        assert null != model;
        return new TarInputService(this, in);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TarDriver} acquires an output
     * stream from the given socket, forwards the call to
     * {@link #newTarOutputService} and wraps the result in a new
     * {@link MultiplexedOutputService}.
     */
    @Override
    public OutputService<TarDriverEntry> newOutputService(
            final FsModel model,
            final OutputSocket<?> output,
            final InputService<TarDriverEntry> source)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        final OutputStream os = output.newOutputStream();
        try {
            return new MultiplexedOutputService<TarDriverEntry>(
                    newTarOutputService(model, os, (TarInputService) source),
                    getPool());
        } catch (final IOException ex) {
            os.close();
            throw ex;
        }
    }

    @CreatesObligation
    protected TarOutputService newTarOutputService(
            FsModel model,
            OutputStream out,
            @CheckForNull @WillNotClose TarInputService source)
    throws IOException {
        assert null != model;
        return new TarOutputService(this, out);
    }
}
