/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.zip;

import global.namespace.truevfs.comp.io.IntervalReadOnlyChannel;
import global.namespace.truevfs.comp.io.MutableBuffer;
import global.namespace.truevfs.comp.io.ReadOnlyChannel;
import global.namespace.truevfs.comp.key.spec.common.AesKeyStrength;
import global.namespace.truevfs.comp.key.spec.util.SuspensionPenalty;
import global.namespace.truevfs.comp.zip.crypto.CipherReadOnlyChannel;
import global.namespace.truevfs.comp.zip.crypto.SeekableBlockCipher;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;

import static global.namespace.truevfs.comp.zip.ExtraField.WINZIP_AES_ID;
import static global.namespace.truevfs.comp.zip.WinZipAesOutputStream.*;

/**
 * Decrypts ZIP entry contents according the WinZip AES specification.
 * <p>
 * Note that this channel implements its own virtual position.
 *
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2 (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.gladman.me.uk/cryptography_technology/fileencrypt/">A Password Based File Encyption Utility (Dr. Gladman)</a>
 * @see     <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898: PKCS #5: Password-Based Cryptography Specification Version 2.0 (IETF et al.)</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 */
final class WinZipAesReadOnlyChannel extends ReadOnlyChannel {

    private static final int MAC_SIZE = newMac().getMacSize();

    private static Mac newMac() { return new HMac(new SHA1Digest()); }

    private final ByteBuffer authenticationCode;

    /**
     * The key parameter required to init the SHA-1 Message Authentication
     * Code (HMAC).
     */
    private final KeyParameter sha1MacParam;

    private final ZipEntry entry;

    WinZipAesReadOnlyChannel(
            final SeekableByteChannel channel,
            final WinZipAesEntryParameters param)
    throws IOException {
        super(channel);

        // Init WinZip AES extra field.
        final ZipEntry entry = param.getEntry();
        assert entry.isEncrypted();
        final WinZipAesExtraField
                field = (WinZipAesExtraField) entry.getExtraField(WINZIP_AES_ID);
        if (null == field)
            throw new ZipCryptoException(entry.getName() + " (missing extra field for WinZip AES entry)");

        // Get key strength.
        final AesKeyStrength keyStrength = field.getKeyStrength();
        final int keyStrengthBits = keyStrength.getBits();
        final int keyStrengthBytes = keyStrength.getBytes();

        // Load salt.
        final ByteBuffer salt = MutableBuffer
                .allocate(keyStrengthBytes / 2)
                .load(channel.position(0))
                .buffer();

        // Load password verification value.
        final ByteBuffer passwdVerifier = MutableBuffer
                .allocate(PWD_VERIFIER_BITS / 8)
                .load(channel)
                .buffer();

        // Init MAC and authentication code.
        final MutableBuffer footer = MutableBuffer.allocate(MAC_SIZE / 2);

        // Init start, end and size of encrypted data.
        final long start = channel.position();
        final long end = channel.size() - footer.remaining();
        final long size = end - start;
        if (0 > size) {
            // Wrap an EOFException so that RawReadOnlyChannel can identify this issue.
            throw new ZipCryptoException(entry.getName()
                    + " (false positive WinZip AES entry is too short)",
                    new EOFException());
        }

        // Load authentication code.
        footer.load(channel.position(end));
        if (channel.position() != channel.size()) {
            // This should never happen unless someone is writing to the
            // end of the file concurrently!
            throw new ZipCryptoException(
                    "Expected end of file after WinZip AES authentication code!");
        }
        authenticationCode = footer.buffer();

        // Derive cipher and MAC parameters.
        final PBEParametersGenerator gen = new PKCS5S2ParametersGenerator();
        KeyParameter keyParam;
        ParametersWithIV aesCtrParam;
        KeyParameter sha1MacParam;
        long lastTry = 0; // don't enforce suspension on first prompt!
        do {
            final byte[] passwd = param.getReadPassword(0 != lastTry);
            assert null != passwd;

            gen.init(passwd, salt.array(), ITERATION_COUNT);
            // Here comes the strange part about WinZip AES encryption:
            // Its unorthodox use of the Password-Based Key Derivation
            // Function 2 (PBKDF2) of PKCS #5 V2.0 alias RFC 2898.
            // Yes, the password verifier is only a 16 bit value.
            // So we must use the MAC for password verification, too.
            assert AES_BLOCK_SIZE_BITS <= keyStrengthBits;
            keyParam = (KeyParameter) gen.generateDerivedParameters(
                    2 * keyStrengthBits + PWD_VERIFIER_BITS);
            Arrays.fill(passwd, (byte) 0);

            // Can you believe they "forgot" the nonce in the CTR mode IV?! :-(
            final byte[] ctrIv = new byte[AES_BLOCK_SIZE_BITS / 8];
            aesCtrParam = new ParametersWithIV(
                    new KeyParameter(keyParam.getKey(), 0, keyStrengthBytes),
                    ctrIv); // yes, the IV is an array of zero bytes!
            sha1MacParam = new KeyParameter(
                    keyParam.getKey(),
                    keyStrengthBytes,
                    keyStrengthBytes);

            lastTry = SuspensionPenalty.enforce(lastTry);

            // Verify password.
        } while (!passwdVerifier.equals(ByteBuffer.wrap(
                keyParam.getKey()).position(2 * keyStrengthBytes)));

        // Init parameters and entry for authenticate().
        this.sha1MacParam = sha1MacParam;
        this.entry = entry;

        // Init cipher and channel.
        final SeekableBlockCipher cipher = new WinZipAesCipher();
        cipher.init(false, aesCtrParam);
        this.channel = new CipherReadOnlyChannel(cipher,
                new IntervalReadOnlyChannel(channel.position(start), size));

        // Commit key strength.
        param.setKeyStrength(keyStrength);
    }

    /**
     * Authenticates all encrypted data in this read-only channel.
     * This method can get called multiple times to detect if the file has been
     * tampered with meanwhile.
     *
     * @throws ZipAuthenticationException If the computed MAC does not match
     *         the MAC declared in the WinZip AES entry.
     * @throws IOException On any I/O related issue.
     */
    void authenticate() throws IOException {
        final Mac mac = newMac();
        mac.init(sha1MacParam);
        final byte[] buf = ((CipherReadOnlyChannel) channel).mac(mac);
        if (!authenticationCode.equals(ByteBuffer.wrap(buf, 0, buf.length / 2)))
            throw new ZipAuthenticationException(entry.getName()
                    + " (authenticated WinZip AES entry content has been tampered with)");
    }
}
