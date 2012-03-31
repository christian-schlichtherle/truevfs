/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import static de.truezip.kernel.cio.Entry.Access.WRITE;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.FsCharsetArchiveDriver;
import de.truezip.kernel.FsController;
import de.truezip.kernel.FsModel;
import de.truezip.kernel.addr.FsEntryName;
import de.truezip.kernel.option.AccessOption;
import static de.truezip.kernel.option.AccessOption.COMPRESS;
import de.truezip.kernel.util.BitField;
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
    public final IOPool<?> getIOPool() {
        return ioPool;
    }

    /**
     * Clears {@link AccessOption#CACHE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public InputSocket<?> getInputSocket(   FsController<?> controller,
                                            FsEntryName name,
                                            BitField<AccessOption> options) {
        return controller.getInputSocket(
                name,
                options.clear(AccessOption.CACHE));
    }

    /**
     * Sets {@link AccessOption#COMPRESS} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public OutputSocket<?> getOutputSocket( FsController<?> controller,
                                            FsEntryName name,
                                            BitField<AccessOption> options,
                                            @CheckForNull Entry template) {
        return controller.getOutputSocket(name, options.set(COMPRESS), template);
    }

    @Override
    public TarDriverEntry newEntry(
            String name,
            final Type type,
            final Entry template,
            final BitField<AccessOption> mknod)
    throws CharConversionException {
        checkEncodable(name);
        name = normalize(name, type);
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
                    getIOPool());
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
