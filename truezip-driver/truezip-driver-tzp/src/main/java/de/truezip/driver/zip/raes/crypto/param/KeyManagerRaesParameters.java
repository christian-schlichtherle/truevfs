/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto.param;

import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.UnknownKeyException;
import de.truezip.driver.zip.raes.crypto.RaesKeyException;
import de.truezip.driver.zip.raes.crypto.RaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesParametersProvider;
import de.truezip.driver.zip.raes.crypto.Type0RaesParameters;
import java.net.URI;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An adapter which provides {@link RaesParameters} by using a
 * {@link KeyManager} for {@link AesCipherParameters}.
 * <p>
 * The current implementation supports only {@link Type0RaesParameters}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class KeyManagerRaesParameters
implements RaesParametersProvider {

    /** The key manager for accessing RAES encrypted data. */
    protected final KeyManager<AesCipherParameters> manager;

    /** The resource URI of the RAES file. */
    protected final URI raes;

    /**
     * Constructs RAES parameters using the given key manager provider.
     *
     * @param  provider the provider for the key manager for accessing RAES
     *         encrypted data.
     * @param  raes the absolute URI of the RAES file.
     */
    public KeyManagerRaesParameters(
            final KeyManagerProvider provider,
            final URI raes) {
        this(provider.get(AesCipherParameters.class), raes);
    }

    /**
     * Constructs new RAES parameters.
     *
     * @param  manager the key manager for accessing RAES encrypted data.
     * @param  raes the resource URI of the RAES file.
     */
    public KeyManagerRaesParameters(
            final KeyManager<AesCipherParameters> manager,
            final URI raes) {
        if (null == manager || null == raes)
            throw new NullPointerException();
        this.manager = manager;
        this.raes = raes;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If {@code type} is assignable from {@link Type0RaesParameters}, then the
     * {@link KeyManager} for {@link AesCipherParameters} will get used which
     * has been provided to the constructor.
     * <p>
     * Otherwise, {@code null} gets returned.
     */
    @Override
    public <P extends RaesParameters> P get(Class<P> type) {
        if (type.isAssignableFrom(Type0RaesParameters.class))
            return type.cast(new Type0());
        return null;
    }

    /**
     * Adapts a {@code KeyProvider} for {@link  AesCipherParameters} obtained
     * from the {@link #manager} to {@code Type0RaesParameters}.
     */
    private class Type0 implements Type0RaesParameters {
        @Override
        public char[] getWritePassword()
        throws RaesKeyException {
            final KeyProvider<AesCipherParameters>
                    provider = manager.getKeyProvider(raes);
            try {
                return provider.getWriteKey().getPassword();
            } catch (UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public char[] getReadPassword(final boolean invalid)
        throws RaesKeyException {
            final KeyProvider<AesCipherParameters>
                    provider = manager.getKeyProvider(raes);
            try {
                return provider.getReadKey(invalid).getPassword();
            } catch (UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public AesKeyStrength getKeyStrength()
        throws RaesKeyException {
            final KeyProvider<AesCipherParameters>
                    provider = manager.getKeyProvider(raes);
            try {
                return provider.getWriteKey().getKeyStrength();
            } catch (UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public void setKeyStrength(final AesKeyStrength keyStrength)
        throws RaesKeyException {
            final KeyProvider<AesCipherParameters>
                    provider = manager.getKeyProvider(raes);
            final AesCipherParameters param;
            try {
                param = provider.getReadKey(false);
            } catch (UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
            param.setKeyStrength(keyStrength);
            provider.setKey(param);
        }
    } // Type0
}
