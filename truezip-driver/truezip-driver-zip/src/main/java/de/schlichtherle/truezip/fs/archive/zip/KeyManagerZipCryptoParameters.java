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

import java.nio.charset.Charset;
import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.key.KeyManager;
import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.key.KeyProvider;
import de.schlichtherle.truezip.key.UnknownKeyException;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import de.schlichtherle.truezip.zip.WinZipAesParameters;
import de.schlichtherle.truezip.zip.ZipCryptoParameters;
import de.schlichtherle.truezip.zip.ZipCryptoParametersProvider;
import de.schlichtherle.truezip.zip.ZipKeyException;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import net.jcip.annotations.ThreadSafe;
import org.bouncycastle.crypto.PBEParametersGenerator;
import static org.bouncycastle.crypto.PBEParametersGenerator.*;

/**
 * An adapter which provides {@link ZipCryptoParameters} by using a
 * {@link KeyManagerProvider}.
 * <p>
 * The current implementation supports only {@link WinZipAesParameters}.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public class KeyManagerZipCryptoParameters
implements ZipCryptoParametersProvider {

    protected final KeyManagerProvider provider;

    /** The resource URI of the ZIP file. */
    protected final URI zip;

    /**
     * The character set used for encoding entry names and the file comment
     * in the ZIP file.
     */
    protected final Charset charset;

    /**
     * Constructs ZIP crypto parameters for the given ZIP output shop and ZIP
     * driver.
     *
     * @param  output the ZIP output shop.
     * @param  driver the ZIP driver.
     */
    public KeyManagerZipCryptoParameters(
            final ZipOutputShop output,
            final ZipDriver driver) {
        this.provider = driver.getKeyManagerProvider();
        this.zip = driver.mountPointUri(output.getModel());
        this.charset = output.getRawCharset();
    }

    /**
     * {@inheritDoc}
     * <p>
     * If {@code type} is assignable from {@link WinZipAesParameters}, then a
     * {@link KeyManager} for {@link AesPbeParameters} will get used which
     * is obtained from the {@link KeyManagerProvider provider} which has been
     * provided to the constructor.
     * <p>
     * Otherwise, {@code null} gets returned.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <P extends ZipCryptoParameters> P get(Class<P> type) {
        if (type.isAssignableFrom(WinZipAesParameters.class))
            return (P) new WinZipAes();
        return null;
    }

    /**
     * A template method to return the URI for looking up a
     * {@link KeyProvider} for {@link AesPbeParameters} by using a
     * {@link KeyManager}.
     * <p>
     * The implementation in the class {@code KeyManagerZipCryptoParameters}
     * ignores the given entry name and just returns the value of the field
     * {@link #zip} in order to lookup the same key provider for all entries in
     * a ZIP file.
     * 
     * @param  name the entry name.
     * @return The URI for looking up a {@link KeyProvider} for
     *         {@link AesPbeParameters} by using a {@link KeyManager}.
     */
    protected URI resource(String name) {
        return zip;
    }

    /**
     * A template method to derive password bytes from the given password
     * characters and the given entry name.
     * Typically, only the given password characters should get encoded to
     * form the result.
     * Optionally, the given entry name can be mixed into the result for
     * authentication.
     * Note that this would then form a part of the password used for writing
     * or reading encrypted entries, however.
     * A third party which is expected to successfully read or write encrypted
     * entries would have to agree on the same password bytes derivation scheme.
     * <p>
     * The implementation in the class {@code KeyManagerZipCryptoParameters}
     * ignores the given entry name and just encodes the given password
     * characters using
     * {@link PBEParametersGenerator#PKCS5PasswordToBytes(char[])}.
     * <p>
     * A reasonable alternative implementation could encode the given password
     * characters using the value of the field {@link #charset}, which is the
     * character set actually used to encode entry names and comments in the
     * ZIP file, e.g. UTF-8 or CP437.
     * 
     * @see    WinZipAesParameters#getWritePassword(String)
     *         Discussion Of Alternative Character Set Encodings
     * @param  characters the password characters to encode.
     * @param  name the entry name for optional mixing into the result.
     * @return The derived password bytes.
     * @see    <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898: PKCS #5: Password-Based Cryptography Specification Version 2.0</a> (IETF et al.)
     */
    protected byte[] password(char[] characters, String name) {
        return PKCS5PasswordToBytes(characters);
    }

    /**
     * Adapts a {@code KeyProvider} for {@link  AesPbeParameters} obtained
     * from the {@link #manager} to {@code WinZipAesParameters}.
     */
    private class WinZipAes implements WinZipAesParameters {
        final KeyManager<AesPbeParameters>
                manager = provider.get(AesPbeParameters.class);

        @Override
        public byte[] getWritePassword(final String name)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(resource(name));
            try {
                return password(provider.getWriteKey().getPassword(), name);
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
        }

        @Override
        public byte[] getReadPassword(final String name, final boolean invalid)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(resource(name));
            try {
                return password(provider.getReadKey(invalid).getPassword(), name);
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
        }

        @Override
        public AesKeyStrength getKeyStrength(final String name)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(resource(name));
            try {
                return provider.getWriteKey().getKeyStrength();
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
        }

        @Override
        public void setKeyStrength( final String name,
                                    final AesKeyStrength keyStrength)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.getKeyProvider(resource(name));
            final AesPbeParameters param;
            try {
                param = provider.getReadKey(false);
            } catch (UnknownKeyException ex) {
                throw new ZipKeyException(ex);
            }
            param.setKeyStrength(keyStrength);
            provider.setKey(param);
        }
    } // WinZipAes
}
