/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive.zip;

import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.UnknownKeyException;
import de.schlichtherle.truezip.zip.WinZipAesParameters;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import de.schlichtherle.truezip.zip.ZipCryptoParametersProvider;
import de.schlichtherle.truezip.zip.ZipKeyException;
import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import net.jcip.annotations.ThreadSafe;

/**
 * An adapter which retrieves {@link ZipCryptoParameters} by using a
 * {@link KeyManager} for {@link AesPbeParameters}.
 * <p>
 * The current implementation supports only {@link WinZipAesParameters}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class KeyManagerZipCryptoParameters implements ZipCryptoParametersProvider {

    private final KeyManager<AesPbeParameters> manager;
    private final URI zip;

    /**
     * Constructs ZIP crypto parameters using the given key manager provider.
     *
     * @param  provider the key manager provider.
     * @param  zip the absolute URI of the ZIP file.
     * @throws IllegalArgumentException if {@code zip} is not an absolute URI.
     */
    public KeyManagerZipCryptoParameters(
            final KeyManagerProvider provider,
            final URI zip) {
        if (!zip.isAbsolute())
            throw new IllegalArgumentException();
        this.manager = provider.get(AesPbeParameters.class);
        this.zip = zip;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P extends ZipCryptoParameters> P get(Class<P> type) {
        return type.isAssignableFrom(WinZipAes.class) ? (P) new WinZipAes() : null;
    }

    /**
     * Returns the URI for looking up a {@link KeyProvider} for
     * {@link AesPbeParameters} by using a {@link KeyManager}.
     * 
     * @param  zip the absolute URI of the ZIP file.
     * @param  name the ZIP entry name.
     * @return The URI for looking up a {@link KeyProvider} for
     *         {@link AesPbeParameters} by using a {@link KeyManager}.
     */
    protected URI toResource(URI zip, String name) {
        return zip;
    }

    /**
     * Adapts a {@code KeyProvider} for {@link  AesPbeParameters} obtained
     * from the {@link manager} to {@code WinZipAesParameters}.
     */
    private class WinZipAes implements WinZipAesParameters {
        @Override
        public char[] getWritePassword(final String name)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(toResource(zip, name));
            try {
                return provider.getWriteKey().getPassword();
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
        }

        @Override
        public char[] getReadPassword(final String name, final boolean invalid)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(toResource(zip, name));
            try {
                return provider.getReadKey(invalid).getPassword();
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
        }

        @Override
        public AesKeyStrength getKeyStrength(final String name) {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(toResource(zip, name));
            try {
                return provider.getWriteKey().getKeyStrength();
            } catch (UnknownKeyException ex) {
                throw new IllegalStateException("getWritePassword(String) must get called first!", ex);
            }
        }

        @Override
        public void setKeyStrength( final String name,
                                    final AesKeyStrength keyStrength) {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(toResource(zip, name));
            final AesPbeParameters param;
            try {
                param = provider.getReadKey(false);
            } catch (UnknownKeyException ex) {
                throw new IllegalStateException("getReadPassword(boolean) must get called first!", ex);
            }
            param.setKeyStrength(keyStrength);
            provider.setKey(param);
        }
    } // WinZipAes
}
