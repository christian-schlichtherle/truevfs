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
package de.schlichtherle.truezip.crypto.raes;

import de.schlichtherle.truezip.key.AesKeyProvider;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.UnknownKeyException;
import java.net.URI;

/**
 * An adapter which retrieves {@link RaesParameters} by using the
 * {@link KeyManager}.
 * The {@link KeyManager} usually prompts the user via a pluggable user
 * interface.
 * <p>
 * According to the current state of RAES, only password based encryption
 * is supported. The adapter pattern allows this class to be changed to
 * support other encryption and authentication schemes in the future without
 * requiring to change the client code.
 * <p>
 * Implementation note: Of course this class could implement
 * {@link Type0RaesParameters} directly.
 * But then this interface and all its methods would be public.
 * However, it is anticipated that with the advent of additional parameter
 * interfaces for new RAES types, the explicit implementation of interfaces
 * would limit this classes ability to implement preferences for certain
 * RAES types.
 * Now, implementing the {@link RaesParametersAgent} interface
 * allows us to control the search for suitable RAES parameters according
 * to the <em>user's</em> preferences, whereas any direct implementation
 * of these interfaces would put us at the mercy of {@link RaesOutputStream}!
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class KeyManagerRaesParameters implements RaesParametersAgent {

    private final KeyManager manager;
    private final URI resource;

    /**
     * Constructs a new set of default RAES parameters.
     *
     * @param resource the non-{@code null} absolute, hierarchical and normalized
     *        URI of the RAES file.
     */
    public KeyManagerRaesParameters(URI resource) {
        if (!resource.isAbsolute())
            throw new IllegalArgumentException();
        this.manager = KeyManager.getInstance();
        this.resource = resource;
    }

    @Override
    public RaesParameters getParameters(Class<? extends RaesParameters> type) {
        return new Type0();
    }

    /**
     * An adapter which presents the KeyManager's {@code KeyProvider}
     * interface as {@code Type0RaesParameters}.
     */
    private class Type0 implements Type0RaesParameters {
        @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
        public char[] getCreatePasswd() throws RaesKeyException {
            final KeyProvider<?> provider = manager
                    .getKeyProvider(resource, (Class) AesKeyProvider.class);
            try {
                final Object key = provider.getCreateKey();
                if (key instanceof byte[])
                    return PKCS12BytesToChars((byte[]) key);
                else
                    return (char[]) key;
            } catch (UnknownKeyException failure) {
                throw new RaesKeyException(failure);
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
        public char[] getOpenPasswd() throws RaesKeyException {
            final KeyProvider<?> provider = manager
                    .getKeyProvider(resource, (Class) AesKeyProvider.class);
            try {
                final Object key = provider.getOpenKey();
                if (key instanceof byte[])
                    return PKCS12BytesToChars((byte[]) key);
                else
                    return (char[]) key;
            } catch (UnknownKeyException failure) {
                throw new RaesKeyException(failure);
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
        public void invalidOpenPasswd() {
            final KeyProvider<?> provider = manager
                    .getKeyProvider(resource, (Class) AesKeyProvider.class);
            provider.invalidOpenKey();
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
        public int getKeyStrength() {
            // Don't cache the key manager!
            final KeyProvider<?> provider = manager
                    .getKeyProvider(resource, (Class) AesKeyProvider.class);
            if (provider instanceof AesKeyProvider) {
                return ((AesKeyProvider<?>) provider).getKeyStrength();
            } else {
                return KEY_STRENGTH_256; // default for incompatible key providers.
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
        public void setKeyStrength(int keyStrength) {
            final KeyProvider<?> provider = manager
                    .getKeyProvider(resource, (Class) AesKeyProvider.class);
            if (provider instanceof AesKeyProvider) {
                ((AesKeyProvider<?>) provider).setKeyStrength(keyStrength);
            }
        }
    }

    static {
        // This check avoids a mapping function.
        assert Type0RaesParameters.KEY_STRENGTH_128 == AesKeyProvider.KEY_STRENGTH_128;
        assert Type0RaesParameters.KEY_STRENGTH_192 == AesKeyProvider.KEY_STRENGTH_192;
        assert Type0RaesParameters.KEY_STRENGTH_256 == AesKeyProvider.KEY_STRENGTH_256;
    }

    /**
     * This is the inverse of the Unicode character to byte conversion
     * algorithm specified in PKCS12.
     * It is used to convert the initial 512 bytes of a key file into a
     * pseudo-Unicode character array which is then used as a password.
     */
    private static char[] PKCS12BytesToChars(final byte[] bytes) {
        // Do not use the following - it would omit a byte order sequence
        // and cannot decode all characters.
        // return new String(buf, 0, n, "UTF-16BE").toCharArray();

        // Decode the characters from UTF-16BE, so that the byte order
        // is preserved when the char array is later again translated
        // to a byte array again according to PKCS #12.
        int len = bytes.length;
        len >>= 1;
        char[] chars = new char[len];
        for (int i = 0, off = 0; i < len; i++)
            chars[i] = (char) (bytes[off++] << 8 | bytes[off++] & 0xFF); // attention!

        return chars;
    }
}
