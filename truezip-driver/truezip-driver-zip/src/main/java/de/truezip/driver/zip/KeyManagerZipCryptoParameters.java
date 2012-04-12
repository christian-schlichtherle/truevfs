/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip;

import de.truezip.driver.zip.io.*;
import de.truezip.kernel.FsModel;
import de.truezip.key.KeyManager;
import de.truezip.key.KeyManagerProvider;
import de.truezip.key.KeyProvider;
import de.truezip.key.UnknownKeyException;
import de.truezip.key.param.AesKeyStrength;
import de.truezip.key.param.AesPbeParameters;
import java.net.URI;
import java.nio.charset.Charset;
import javax.annotation.concurrent.ThreadSafe;
import org.bouncycastle.crypto.PBEParametersGenerator;
import static org.bouncycastle.crypto.PBEParametersGenerator.PKCS5PasswordToBytes;

/**
 * An adapter which provides {@link ZipCryptoParameters} by using a
 * {@link KeyManagerProvider}.
 * <p>
 * The current implementation supports only {@link WinZipAesParameters}.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class KeyManagerZipCryptoParameters
implements ZipParametersProvider, ZipCryptoParameters {

    /** The ZIP driver which has been provided to the constructor. */
    private final ZipDriver driver;

    /** The file system model which has been provided to the constructor. */
    private final FsModel model;

    /**
     * The character set which has been provided to the constructor and is
     * used for encoding entry names and the file comment in the ZIP file .
     */
    protected final Charset charset;

    /**
     * Constructs new ZIP crypto parameters.
     *
     * @param driver the ZIP driver.
     * @param model the file system model.
     * @param charset the character set which is used for encoding entry names
     *        and the file comment in the ZIP file.
     */
    public KeyManagerZipCryptoParameters(
            final ZipDriver driver,
            final FsModel model,
            final Charset charset) {
        if (null == (this.driver = driver))
            throw new NullPointerException();
        if (null == (this.model = model))
            throw new NullPointerException();
        if (null == (this.charset = charset))
            throw new NullPointerException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * If {@code type} is assignable from {@link WinZipAesParameters}, then a
     * {@link KeyManager} for {@link AesPbeParameters} will getKeyManager used which
     * is obtained from the {@link KeyManagerProvider provider} which has been
     * provided to the constructor.
     * <p>
     * Otherwise, {@code null} gets returned.
     */
    @Override
    public <P extends ZipParameters> P get(Class<P> type) {
        if (type.isAssignableFrom(WinZipAesParameters.class))
            return type.cast(new WinZipAes());
        return null;
    }

    /**
     * A template method to derive password bytes from the given password
     * characters and the given entry name.
     * Typically, only the given password characters should getKeyManager encoded to
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
     * @see    <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898: PKCS #5: Password-Based Cryptography Specification Version 2.0 (IETF et al.)</a>
     */
    protected byte[] password(char[] characters, String name) {
        return PKCS5PasswordToBytes(characters);
    }

    private <K> KeyManager<K> keyManager(Class<K> type) {
        return driver.getKeyManagerProvider().getKeyManager(type);
    }

    private URI resourceUri(String name) {
        return driver.resourceUri(model, name);
    }

    /**
     * Adapts a {@code KeyProvider} for {@link  AesPbeParameters} obtained
     * from the {@link #keyManager} to {@code WinZipAesParameters}.
     */
    private class WinZipAes implements WinZipAesParameters {
        final KeyManager<AesPbeParameters>
                manager = keyManager(AesPbeParameters.class);

        @Override
        public byte[] getWritePassword(final String name)
        throws ZipKeyException {
            final KeyProvider<AesPbeParameters>
                    provider = manager.make(resourceUri(name));
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
                    provider = manager.make(resourceUri(name));
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
                    provider = manager.make(resourceUri(name));
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
                    provider = manager.make(resourceUri(name));
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