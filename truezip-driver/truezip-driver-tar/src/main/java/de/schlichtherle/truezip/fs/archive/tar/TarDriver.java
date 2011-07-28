/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.tar;

import de.schlichtherle.truezip.entry.Entry;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Size.*;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.archive.FsCharsetArchiveDriver;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import net.jcip.annotations.Immutable;

/**
 * An archive driver which builds Tape Archive files (TAR).
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class TarDriver extends FsCharsetArchiveDriver<TarArchiveEntry> {

    /**
     * The default character set for entry names and comments, which is
     * {@code "US-ASCII"}.
     */
    public static final Charset TAR_CHARSET = Charset.forName("US-ASCII");

    private final IOPoolProvider provider;

    public TarDriver(final IOPoolProvider provider) {
        super(TAR_CHARSET);
        if (null == provider)
            throw new NullPointerException();
        this.provider = provider;
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
        return provider.get();
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
    public TarArchiveEntry newEntry(
            String name,
            final Type type,
            final Entry template,
            final BitField<FsOutputOption> mknod)
    throws CharConversionException {
        assertEncodable(name);
        name = toZipOrTarEntryName(name, type);
        final TarArchiveEntry entry;
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
    
    public TarArchiveEntry newEntry(String name) {
        return new TarArchiveEntry(name);
    }

    public TarArchiveEntry newEntry(String name, TarArchiveEntry template) {
        return new TarArchiveEntry(name, template);
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
        final InputStream in = input.newInputStream();
        try {
            return newTarInputShop(model, in);
        } finally {
            in.close();
        }
    }

    protected TarInputShop newTarInputShop(FsModel model, InputStream in)
    throws IOException {
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
    public OutputShop<TarArchiveEntry> newOutputShop(
            FsModel model,
            OutputSocket<?> output,
            InputShop<TarArchiveEntry> source)
    throws IOException {
        final OutputStream out = output.newOutputStream();
        try {
            return new FsMultiplexedOutputShop<TarArchiveEntry>(
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
            FsModel model, OutputStream out, TarInputShop source)
    throws IOException {
        return new TarOutputShop(this, out);
    }
}
