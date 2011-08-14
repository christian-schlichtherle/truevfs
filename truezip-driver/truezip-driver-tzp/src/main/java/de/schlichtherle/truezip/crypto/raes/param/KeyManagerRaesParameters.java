/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.crypto.raes.param;

import de.schlichtherle.truezip.crypto.raes.RaesKeyException;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.raes.RaesParametersProvider;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.UnknownKeyException;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import net.jcip.annotations.ThreadSafe;

/**
 * An adapter which provides {@link RaesParameters} by using a
 * {@link KeyManager} for {@link AesCipherParameters}.
 * <p>
 * The current implementation supports only {@link Type0RaesParameters}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
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
    @SuppressWarnings("unchecked")
    public <P extends RaesParameters> P get(Class<P> type) {
        if (type.isAssignableFrom(Type0RaesParameters.class))
            return (P) new Type0();
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
        public KeyStrength getKeyStrength()
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
        public void setKeyStrength(final KeyStrength keyStrength)
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
