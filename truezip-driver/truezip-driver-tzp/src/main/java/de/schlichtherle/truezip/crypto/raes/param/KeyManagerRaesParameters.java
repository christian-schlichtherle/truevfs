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
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerService;
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

    private final KeyManagerService service;
    private final URI resource;

    /**
     * Constructs a new set of default RAES parameters.
     *
     * @param resource the non-{@code null} absolute, hierarchical and normalized
     *        URI of the RAES file.
     */
    public KeyManagerRaesParameters(final KeyManagerService service,
                                    final URI resource) {
        if (null == service)
            throw new NullPointerException();
        if (!resource.isAbsolute())
            throw new IllegalArgumentException();
        this.service = service;
        this.resource = resource;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P extends RaesParameters> P getParameters(Class<P> type) {
        return type.isAssignableFrom(Type0.class) ? (P) new Type0() : null;
    }

    /**
     * An adapter which presents the KeyManager's {@code KeyProvider}
     * interface as {@code Type0RaesParameters}.
     */
    private class Type0 implements Type0RaesParameters {
        private AesCipherParameters param;

        @Override
        public KeyStrength getKeyStrength() {
            assert null != param : "getCreatePasswd() must get called first!";
            return param.getKeyStrength();
        }

        @Override
        public char[] getCreatePasswd() throws RaesKeyException {
            final KeyProvider<? extends AesCipherParameters> provider = service
                    .getManager(AesCipherParameters.class)
                    .getKeyProvider(resource);
            try {
                return (param = provider.getCreateKey()).getPassword();
            } catch (UnknownKeyException failure) {
                throw new RaesKeyException(failure);
            }
        }

        @Override
        public char[] getOpenPasswd(boolean invalid) throws RaesKeyException {
            final KeyProvider<? extends AesCipherParameters> provider = service
                    .getManager(AesCipherParameters.class)
                    .getKeyProvider(resource);
            try {
                return provider.getOpenKey(invalid).getPassword();
            } catch (UnknownKeyException failure) {
                throw new RaesKeyException(failure);
            }
        }
    }
}
