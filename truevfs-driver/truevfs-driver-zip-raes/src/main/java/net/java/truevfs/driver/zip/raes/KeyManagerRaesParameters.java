/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes;

import java.net.URI;
import java.util.Objects;
import javax.annotation.concurrent.ThreadSafe;
import net.java.truevfs.driver.zip.raes.crypto.RaesKeyException;
import net.java.truevfs.driver.zip.raes.crypto.RaesParameters;
import net.java.truevfs.driver.zip.raes.crypto.RaesParametersProvider;
import net.java.truevfs.driver.zip.raes.crypto.Type0RaesParameters;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.KeyManagerMap;
import net.java.truecommons.key.spec.KeyProvider;
import net.java.truecommons.key.spec.UnknownKeyException;
import net.java.truecommons.key.spec.common.AesKeyStrength;
import net.java.truecommons.key.spec.common.AesPbeParameters;

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
     * @param  container the container of the key manager for accessing RAES
     *         encrypted data.
     * @param  raes the absolute URI of the RAES file.
     */
    public KeyManagerRaesParameters(
            final KeyManagerMap container,
            final URI raes) {
        this(container.manager(AesPbeParameters.class), raes);
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

        private volatile KeyProvider<AesPbeParameters> provider;

        private KeyProvider<AesPbeParameters> provider() {
            final KeyProvider<AesPbeParameters> p = provider;
            return null != p ? p : (provider = manager.provider(raes));
        }

        @Override
        public char[] getPasswordForWriting()
        throws RaesKeyException {
            try {
                return provider().getKeyForWriting().getPassword();
            } catch (final UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public char[] getPasswordForReading(final boolean invalid)
        throws RaesKeyException {
            try {
                return provider().getKeyForReading(invalid).getPassword();
            } catch (final UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public AesKeyStrength getKeyStrength()
        throws RaesKeyException {
            try {
                return provider().getKeyForWriting().getKeyStrength();
            } catch (final UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public void setKeyStrength(final AesKeyStrength keyStrength)
        throws RaesKeyException {
            final KeyProvider<AesPbeParameters> p = provider();
            final AesPbeParameters k;
            try {
                k = p.getKeyForReading(false);
            } catch (final UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
            k.setKeyStrength(keyStrength);
            p.setKey(k);
        }
    } // Type0
}
