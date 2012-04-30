/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import de.truezip.driver.zip.raes.crypto.RaesKeyException;
import de.truezip.driver.zip.raes.crypto.RaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesParametersProvider;
import de.truezip.driver.zip.raes.crypto.Type0RaesParameters;
import de.truezip.key.KeyManager;
import de.truezip.key.KeyManagerProvider;
import de.truezip.key.KeyProvider;
import de.truezip.key.UnknownKeyException;
import de.truezip.key.param.AesKeyStrength;
import de.truezip.key.param.AesPbeParameters;
import java.net.URI;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An adapter which provides {@link RaesParameters} by using a
 * {@link KeyManager} for {@link AesPbeParameters}.
 * <p>
 * The current implementation supports only {@link Type0RaesParameters}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class KeyManagerRaesParameters
implements RaesParametersProvider {

    /** The key manager for accessing RAES encrypted data. */
    protected final KeyManager<AesPbeParameters> manager;

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
        this(provider.keyManager(AesPbeParameters.class), raes);
    }

    /**
     * Constructs new RAES parameters.
     *
     * @param  manager the key manager for accessing RAES encrypted data.
     * @param  raes the resource URI of the RAES file.
     */
    public KeyManagerRaesParameters(
            final KeyManager<AesPbeParameters> manager,
            final URI raes) {
        this.manager = Objects.requireNonNull(manager);
        this.raes = Objects.requireNonNull(raes);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If {@code type} is assignable from {@link Type0RaesParameters}, then the
     * {@link KeyManager} for {@link AesPbeParameters} will getKeyManager used which
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
     * Adapts a {@code KeyProvider} for {@link  AesPbeParameters} obtained
     * from the {@link #manager} to {@code Type0RaesParameters}.
     */
    private class Type0 implements Type0RaesParameters {
        @Override
        public char[] getWritePassword()
        throws RaesKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.make(raes);
            try {
                return provider.getWriteKey().getPassword();
            } catch (final UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public char[] getReadPassword(final boolean invalid)
        throws RaesKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.make(raes);
            try {
                return provider.getReadKey(invalid).getPassword();
            } catch (final UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public AesKeyStrength getKeyStrength()
        throws RaesKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.make(raes);
            try {
                return provider.getWriteKey().getKeyStrength();
            } catch (final UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public void setKeyStrength(final AesKeyStrength keyStrength)
        throws RaesKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.make(raes);
            final AesPbeParameters param;
            try {
                param = provider.getReadKey(false);
            } catch (final UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
            param.setKeyStrength(keyStrength);
            provider.setKey(param);
        }
    } // Type0
}
