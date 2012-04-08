/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.driver.zip.crypto.CipherReadOnlyChannel;
import de.truezip.driver.zip.crypto.SeekableBlockCipher;
import static de.truezip.driver.zip.io.ExtraField.WINZIP_AES_ID;
import static de.truezip.driver.zip.io.WinZipAesEntryOutputStream.*;
import de.truezip.kernel.io.IntervalReadOnlyChannel;
import de.truezip.kernel.io.PowerBuffer;
import de.truezip.kernel.util.ArrayUtils;
import de.truezip.key.param.AesKeyStrength;
import de.truezip.key.util.SuspensionPenalty;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Decrypts ZIP entry contents according the WinZip AES specification.
 * 
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2 (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.gladman.me.uk/cryptography_technology/fileencrypt/">A Password Based File Encyption Utility (Dr. Gladman)</a>
 * @see     <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898: PKCS #5: Password-Based Cryptography Specification Version 2.0 (IETF et al.)</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class WinZipAesEntryReadOnlyChannel extends CipherReadOnlyChannel {

    private final PowerBuffer authenticationCode;

    /**
     * The key parameter required to init the SHA-1 Message Authentication
     * Code (HMAC).
     */
    private final KeyParameter sha1MacParam;

    private final ZipEntry entry;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    WinZipAesEntryReadOnlyChannel(
            final @WillCloseWhenClosed SeekableByteChannel channel,
            final WinZipAesEntryParameters param)
    throws IOException {
        super(channel);

        // Init WinZip AES extra field.
        final ZipEntry entry = param.getEntry();
        assert entry.isEncrypted();
        final WinZipAesEntryExtraField
                field = (WinZipAesEntryExtraField) entry.getExtraField(WINZIP_AES_ID);
        if (null == field)
            throw new ZipCryptoException(entry.getName() + " (missing extra field for WinZip AES entry)");

        // Get key strength.
        final AesKeyStrength keyStrength = field.getKeyStrength();
        final int keyStrengthBits = keyStrength.getBits();
        final int keyStrengthBytes = keyStrength.getBytes();

        // Load salt.
        final PowerBuffer salt = PowerBuffer
                .allocate(keyStrengthBytes / 2)
                .load(channel.position(0));
        
        // Load password verification value.
        final PowerBuffer passwdVerifier = PowerBuffer
                .allocate(PWD_VERIFIER_BITS / 8)
                .load(channel);

        // Init MAC and authentication code.
        final Mac mac = new HMac(new SHA1Digest());
        this.authenticationCode = PowerBuffer.allocate(mac.getMacSize() / 2);

        // Init start, end and length of encrypted data.
        final long start = channel.position();
        final long end = channel.size() - this.authenticationCode.limit();
        final long size = end - start;
        if (size < 0) {
            // Wrap an EOFException so that RawReadOnlyChannel can identify this issue.
            throw new ZipCryptoException(entry.getName()
                    + " (false positive WinZip AES entry is too short)",
                    new EOFException());
        }

        // Load authentication code.
        this.authenticationCode.load(channel.position(end));
        if (channel.position() < channel.size()) {
            // This should never happen unless someone is writing to the
            // end of the file concurrently!
            throw new ZipCryptoException(
                    "Expected end of file after WinZip AES authentication code!");
        }

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
            paranoidWipe(passwd);

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
        } while (!ArrayUtils.equals(
                keyParam.getKey(), 2 * keyStrengthBytes,
                passwdVerifier.array(), 0,
                PWD_VERIFIER_BITS / 8));

        // Init parameters and entry for authenticate().
        this.sha1MacParam = sha1MacParam;
        this.entry = param.getEntry();

        // Init channel interval and cipher.
        this.channel = new IntervalReadOnlyChannel(channel.position(start), size);
        final SeekableBlockCipher cipher = new WinZipAesCipher();
        cipher.init(false, aesCtrParam);
        init(cipher);

        // Commit key strength.
        param.setKeyStrength(keyStrength);
    }

    /** Wipe the given array. */
    private void paranoidWipe(final byte[] pwd) {
        for (int i = pwd.length; --i >= 0; )
            pwd[i] = 0;
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
        final Mac mac = new HMac(new SHA1Digest());
        mac.init(sha1MacParam);
        final ByteBuffer buf = computeMac(mac);
        assert buf.limit() == mac.getMacSize();
        if (!ArrayUtils.equals(buf.array(), 0, authenticationCode.array(), 0, authenticationCode.limit()))
            throw new ZipAuthenticationException(entry.getName()
                    + " (authenticated WinZip AES entry content has been tampered with)");
    }
}