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

/**
 * An adapter which retrieves {@link RaesParameters} by using a
 * {@link KeyManager}.
 * <p>
 * According to the requirements of RAES, only password based encryption
 * is supported. The adapter pattern allows this class to be changed to
 * support other encryption and authentication schemes in future versions
 * without requiring to change the client code.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public final class KeyManagerRaesParameters implements RaesParametersProvider {

    private final KeyManager<AesCipherParameters> manager;
    private final URI resource;

    /**
     * Equivalent to
     * {@link #KeyManagerRaesParameters(KeyManager, URI) new KeyManagerRaesParameters(provider.get(AesCipherParameters.class), resource)}.
     */
    public KeyManagerRaesParameters(
            final KeyManagerProvider provider,
            final URI resource) {
        this(provider.get(AesCipherParameters.class), resource);
    }

    /**
     * Constructs new RAES parameters using the given key manager.
     *
     * @param resource the absolute URI of the RAES file.
     * @throws IllegalArgumentException if {@code resource} is not absolute.
     */
    public KeyManagerRaesParameters(
            final KeyManager<AesCipherParameters> manager,
            final URI resource) {
        if (null == manager)
            throw new NullPointerException();
        if (!resource.isAbsolute())
            throw new IllegalArgumentException();
        this.manager = manager;
        this.resource = resource;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P extends RaesParameters> P get(Class<P> type) {
        return type.isAssignableFrom(Type0.class) ? (P) new Type0() : null;
    }

    /**
     * An adapter which presents the KeyManager's {@code KeyProvider}
     * interface as {@code Type0RaesParameters}.
     */
    private class Type0 implements Type0RaesParameters {
        private AesCipherParameters param;

        @Override
        public char[] getWritePassword() throws RaesKeyException {
            final KeyProvider<AesCipherParameters>
                    provider = manager.getKeyProvider(resource);
            try {
                return (param = provider.getWriteKey()).getPassword();
            } catch (UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public char[] getReadPassword(boolean invalid) throws RaesKeyException {
            final KeyProvider<AesCipherParameters>
                    provider = manager.getKeyProvider(resource);
            try {
                return (param = provider.getReadKey(invalid)).getPassword();
            } catch (UnknownKeyException ex) {
                throw new RaesKeyException(ex);
            }
        }

        @Override
        public KeyStrength getKeyStrength() {
            if (null == param)
                throw new IllegalStateException("getWritePassword() must get called first!");
            return param.getKeyStrength();
        }

        @Override
        public void setKeyStrength(KeyStrength keyStrength) {
            if (null == param)
                throw new IllegalStateException("getReadPassword(boolean) must get called first!");
            final KeyProvider<AesCipherParameters>
                    provider = manager.getKeyProvider(resource);
            param.setKeyStrength(keyStrength);
            provider.setKey(param);
        }
    }
}
