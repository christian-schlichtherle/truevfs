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
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.crypto.raes.RaesOutputStream;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesReadOnlyFile;
import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.crypto.raes.param.KeyManagerRaesParameters;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.archive.FsMultiplexedOutputShop;
import de.schlichtherle.truezip.fs.archive.zip.JarArchiveEntry;
import de.schlichtherle.truezip.fs.archive.zip.JarDriver;
import de.schlichtherle.truezip.fs.archive.zip.OptionOutputSocket;
import de.schlichtherle.truezip.fs.archive.zip.ZipArchiveEntry;
import de.schlichtherle.truezip.fs.archive.zip.ZipInputShop;
import de.schlichtherle.truezip.fs.archive.zip.ZipOutputShop;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.PromptingKeyProvider;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.IOPool;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.InputShop;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.LazyOutputSocket;
import de.schlichtherle.truezip.socket.OutputShop;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import net.jcip.annotations.Immutable;

/**
 * An abstract archive driver which builds RAES encrypted ZIP files
 * and optionally authenticates the cipher data of the input archive files
 * presented to it.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public abstract class ZipRaesDriver extends JarDriver {

    private final KeyManagerProvider keyManagerProvider;

    /**
     * Constructs a new RAES encrypted ZIP file driver.
     *
     * @param ioPoolProvider the I/O pool service to use for temporary data.
     * @param keyManagerProvider the key manager service.
     */
    public ZipRaesDriver(   IOPoolProvider ioPoolProvider,
                            final KeyManagerProvider keyManagerProvider) {
        super(ioPoolProvider);
        if (null == keyManagerProvider)
            throw new NullPointerException();
        this.keyManagerProvider = keyManagerProvider;
    }

    /**
     * Returns the key provider sync strategy,
     * which is {@link KeyProviderSyncStrategy#RESET_CANCELLED_KEY}.
     *
     * @return The key provider sync strategy.
     */
    protected KeyProviderSyncStrategy getKeyProviderSyncStrategy() {
        return KeyProviderSyncStrategy.RESET_CANCELLED_KEY;
    }

    /**
     * Constructs a new abstract ZIP.RAES driver which
     * uses the given byte
     * size to trigger verification of the Message Authentication Code (MAC).
     * Note that the given parameter only affects the authentication of the
     * <em>cipher text</em> in input archives - the <em>cipher key</em> and
     * <em>file length</em> are always authenticated with RAES.
     *
     * Returns the value of the property {@code authenticationTrigger}.
     * If the size of an input file is smaller than or equal to this value,
     * the Message Authentication Code (MAC) for the entire
     * <em>cipher text</em> is computed and verified in order to authenticate
     * the file.
     * Otherwise, only the <em>cipher key</em> and the <em>file length</em>
     * get authenticated.
     * <p>
     * Consequently, if the value of this property is set to a negative value,
     * the cipher text gets <em>never</em> verified, and if set to
     * {@link Long#MAX_VALUE}, the cipher text gets <em>always</em>
     * authenticated.
     *
     * @return The value of the property {@code authenticationTrigger}.
     */
    protected abstract long getAuthenticationTrigger();

    @Override
    public final FsController<?>
    newController(FsModel model, FsController<?> parent) {
        return new ZipRaesController(
                super.newController(model, parent), this);
    }

    /**
     * Creates a new {@link JarArchiveEntry}, enforcing that the data gets
     * {@code DEFLATED} when written, even if copying data from a
     * {@code STORED} source entry.
     * This feature strengthens the security of the authentication process.
     */
    @Override
    public final JarArchiveEntry
    newEntry(   final String path,
                final Type type,
                final @CheckForNull Entry template,
                final BitField<FsOutputOption> mknod)
    throws CharConversionException {
        // Enforce deflation to strengthen the authentication security level.
        return super.newEntry(path, type, template, mknod.set(COMPRESS));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in {@link ZipRaesDriver} calls
     * {@link #getRaesParameters}, with which it initializes a new
     * {@link RaesReadOnlyFile}.
     * Next, if the gross file length of the archive is smaller than or equal
     * to the authentication trigger, the MAC authentication on the cipher
     * text is performed.
     * Finally, the {@link RaesReadOnlyFile} is passed on to the super
     * class implementation.
     */
    @Override
    public final InputShop<ZipArchiveEntry>
    newInputShop(   final FsModel model,
                    final InputSocket<?> input)
    throws IOException {
        final ReadOnlyFile rof = input.newReadOnlyFile();
        try {
            final RaesReadOnlyFile rrof = RaesReadOnlyFile.getInstance(
                    rof, getRaesParameters(model));
            if (rof.length() <= getAuthenticationTrigger()) { // compare rof, not rrof!
                // Note: If authentication fails, this is reported through some
                // sort of IOException, not a FileNotFoundException!
                // This allows the client to treat the tampered archive like an
                // ordinary file which may be read, written or deleted.
                rrof.authenticate();
            }
            return newInputShop(rrof);
        } catch (IOException ex) {
            rof.close();
            throw ex;
        }
    }

    /**
     * Sets {@link FsOutputOption#STORE} in {@code options} before
     * forwarding the call to {@code controller}.
     */
    @Override
    public final OptionOutputSocket getOutputSocket(
            final FsController<?> controller,
            final FsEntryName name,
            BitField<FsOutputOption> options,
            final @CheckForNull Entry template) {
        options = options.clear(GROW);
        // Leave FsOutputOption.COMPRESS untouched - the driver shall be given
        // opportunity to apply its own preferences to sort out such a conflict.
        BitField<FsOutputOption> options2 = options.set(STORE);
        return new OptionOutputSocket(
                controller.getOutputSocket(name, options2, template),
                options); // use modified options!
    }

    @Override
    protected OutputShop<ZipArchiveEntry> newOutputShop(
            final FsModel model,
            final OptionOutputSocket output,
            final @CheckForNull ZipInputShop source)
    throws IOException {
        final OutputStream out = new LazyOutputSocket<Entry>(output)
                .newOutputStream();
        try {
            final RaesOutputStream ros = RaesOutputStream.getInstance(
                    out, getRaesParameters(model));
            return newOutputShop(ros, source);
        } catch (IOException ex) {
            out.close();
            throw ex;
        }
    }

    /**
     * Returns the {@link RaesParameters} for the given file system model.
     * 
     * @param  model the file system model.
     * @return The {@link RaesParameters} for the given file system model.
     */
    final RaesParameters getRaesParameters(FsModel model) {
        return new KeyManagerRaesParameters(
                getKeyManager(),
                toMountPointResource(model));
    }

    final KeyManager<AesCipherParameters> getKeyManager() {
        return keyManagerProvider.get(AesCipherParameters.class);
    }

    /**
     * Returns a URI which represents the mount point of the given model as a
     * resource URI for looking up a {@link KeyProvider}.
     * Note that this URI needs to be matched exactly when setting a password
     * programmatically!
     * <p>
     * The implementation in the class {@link ZipRaesDriver} returns the
     * expression {@code model.getMountPoint().toHierarchicalUri()}
     * in order to improve the readability of the URI in comparison to the
     * expression {@code model.getMountPoint().toUri()}.
     * 
     * @param  model the file system model.
     * @return A URI representing the file system model's mount point.
     * @see    <a href="http://java.net/jira/browse/TRUEZIP-72">#TRUEZIP-72</a>
     */
    protected URI toMountPointResource(FsModel model) {
        return model.getMountPoint().toHierarchicalUri();
    }

    /**
     * Defines strategies for updating a key provider once a RAES encrypted
     * ZIP file has been successfully synchronized.
     */
    public enum KeyProviderSyncStrategy {

        /**
         * Calls {@link PromptingKeyProvider#resetCancelledKey}
         * if and only if the given provider is a {@link PromptingKeyProvider}.
         */
        RESET_CANCELLED_KEY {
            @Override
            public void sync(KeyProvider<?> provider) {
                if (provider instanceof PromptingKeyProvider<?>)
                    ((PromptingKeyProvider<?>) provider).resetCancelledKey();
            }
        },

        /**
         * Calls {@link PromptingKeyProvider#resetUnconditionally}
         * if and only if the given provider is a {@link PromptingKeyProvider}.
         */
        RESET_UNCONDITIONALLY {
            @Override
            public void sync(KeyProvider<?> provider) {
                if (provider instanceof PromptingKeyProvider<?>)
                    ((PromptingKeyProvider<?>) provider).resetUnconditionally();
            }
        };

        /**
         * This method is called upon a call to
         * {@link ZipRaesController#sync} after a successful
         * synchronization of a RAES encrypted ZIP file.
         *
         * @param provider the key provider for the RAES encrypted ZIP file
         *        which has been successfully synchronized.
         */
        public abstract void sync(KeyProvider<?> provider);
    } // KeyProviderSyncStrategy
}
