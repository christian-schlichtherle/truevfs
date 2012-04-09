/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.tar;

import static de.truezip.kernel.FsAccessOption.COMPRESS;
import de.truezip.kernel.*;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import static de.truezip.kernel.cio.Entry.Size.DATA;
import de.truezip.kernel.cio.Entry.Type;
import de.truezip.kernel.cio.*;
import de.truezip.kernel.util.BitField;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javax.annotation.CheckForNull;
import javax.annotation.WillClose;
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
public class TarDriver extends FsArchiveDriver<TarDriverEntry> {

    /**
     * The character set for entry names and comments, which is the default
     * character set.
     */
    public static final Charset TAR_CHARSET = Charset.defaultCharset();

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
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link TarDriver} acquires an input
     * stream from the given socket and forwards the call to
     * {@link #newTarInputService}.
     */
    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected TarInputService newInputService(
            final FsModel model,
            final InputSocket<?> input)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        TarInputService is = null;
        final @WillClose InputStream in = input.newStream();
        try {
            return is = newTarInputService(model, in);
        } finally {
            try {
                in.close();
            } catch (final Throwable ex) {
                if (null != is) {
                    try {
                        is.close();
                    } catch (final Throwable ex2) {
                        assert !(ex2 instanceof FsControlFlowIOException) : ex;
                        ex.addSuppressed(ex2);
                    }
                }
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
     * {@link MultiplexingOutputService}.
     */
    @Override
    protected OutputService<TarDriverEntry> newOutputService(
            final FsModel model,
            final @CheckForNull @WillNotClose InputService<TarDriverEntry> source,
            final OutputSocket<?> output)
    throws IOException {
        if (null == model)
            throw new NullPointerException();
        final OutputStream out = output.newStream();
        try {
            return new MultiplexingOutputService<>(
                    newTarOutputService(model, out, (TarInputService) source),
                    getIOPool());
        } catch (final Throwable ex) {
            try {
                out.close();
            } catch (final Throwable ex2) {
                assert !(ex2 instanceof FsControlFlowIOException) : ex2;
                ex.addSuppressed(ex2);
            }
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

    /**
     * Clears {@link FsAccessOption#CACHE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected InputSocket<?> getInputSocket(
            FsController<?> controller,
            FsEntryName name,
            BitField<FsAccessOption> options) {
        return controller.getInputSocket(
                name,
                options.clear(FsAccessOption.CACHE));
    }

    /**
     * Sets {@link FsAccessOption#COMPRESS} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    protected OutputSocket<?> getOutputSocket(
            FsController<?> controller,
            FsEntryName name,
            BitField<FsAccessOption> options) {
        return controller.getOutputSocket(name, options.set(COMPRESS), null);
    }

    @Override
    public TarDriverEntry newEntry(
            String name,
            final Type type,
            final Entry template,
            final BitField<FsAccessOption> mknod) {
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
}
