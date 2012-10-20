/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import net.java.truecommons.io.DecoratingOutputStream;
import net.java.truecommons.io.LittleEndianOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.comp.zip.crypto.CipherOutputStream;
import net.java.truevfs.key.spec.param.KeyStrength;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.io.MacOutputStream;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Encrypts ZIP entry contents according the WinZip AES specification.
 *
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2 (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.gladman.me.uk/cryptography_technology/fileencrypt/">A Password Based File Encyption Utility (Dr. Gladman)</a>
 * @see     <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898: PKCS #5: Password-Based Cryptography Specification Version 2.0 (IETF et al.)</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class WinZipAesOutputStream extends DecoratingOutputStream {

    /**
     * The iteration count for the derived keys of the cipher, KLAC and MAC.
     */
    static final int ITERATION_COUNT = 1000;

    /**
     * The block size of the Advanced Encryption Specification (AES) Algorithm
     * in bits ({@value #AES_BLOCK_SIZE_BITS}).
     */
    static final int AES_BLOCK_SIZE_BITS = 128;

    static final int PWD_VERIFIER_BITS = 16;

    private final WinZipAesEntryParameters param;

    /** The Message Authentication Code (MAC). */
    private Mac mac;

    /**
     * The low level data output stream.
     * Used for writing the header and footer.
     **/
    private LittleEndianOutputStream leos;

    WinZipAesOutputStream(
            final WinZipAesEntryParameters param,
            final LittleEndianOutputStream leos)
    throws IOException {
        super(leos);
        assert null != leos;
        assert null != param;
        this.param = param;

        // Init key strength.
        final KeyStrength keyStrength = param.getKeyStrength();
        final int keyStrengthBits = keyStrength.getBits();
        final int keyStrengthBytes = keyStrength.getBytes();

        // Shake the salt.
        final byte[] salt = new byte[keyStrengthBytes / 2];
        new SecureRandom().nextBytes(salt);

        // Init password.
        final byte[] passwd = param.getWritePassword();

        // Derive cipher and MAC parameters.
        final PBEParametersGenerator gen = new PKCS5S2ParametersGenerator();
        gen.init(passwd, salt, ITERATION_COUNT);
        // Here comes the strange part about WinZip AES encryption:
        // Its unorthodox use of the Password-Based Key Derivation
        // Function 2 (PBKDF2) of PKCS #5 V2.0 alias RFC 2898.
        // Yes, the password verifier is only a 16 bit value.
        // So we must use the MAC for password verification, too.
        assert AES_BLOCK_SIZE_BITS <= keyStrengthBits;
        final KeyParameter keyParam =
                (KeyParameter) gen.generateDerivedParameters(
                    2 * keyStrengthBits + PWD_VERIFIER_BITS);
        Arrays.fill(passwd, (byte) 0); // must not wipe before generator use!

        // Can you believe they "forgot" the nonce in the CTR mode IV?! :-(
        final byte[] ctrIv = new byte[AES_BLOCK_SIZE_BITS / 8];
        final ParametersWithIV aesCtrParam = new ParametersWithIV(
                new KeyParameter(keyParam.getKey(), 0, keyStrengthBytes),
                ctrIv); // yes, the IV is an array of zero bytes!
        final KeyParameter sha1HMacParam = new KeyParameter(
                keyParam.getKey(),
                keyStrengthBytes,
                keyStrengthBytes);

        // Init cipher and stream.
        final BufferedBlockCipher
                cipher = new BufferedBlockCipher(new WinZipAesCipher());
        cipher.init(true, aesCtrParam);

        // Init MAC.
        final Mac mac = this.mac = new HMac(new SHA1Digest());
        mac.init(sha1HMacParam);

        // Reinit chain of output streams as Encrypt-then-MAC.
        this.leos = leos;
        this.out = new CipherOutputStream(cipher,
                new MacOutputStream(leos, mac));

        // Write header.
        leos.write(salt);
        writePasswordVerifier(keyParam);
    }

    private void writePasswordVerifier(KeyParameter keyParam)
    throws IOException {
        this.leos.write(
                keyParam.getKey(),
                2 * param.getKeyStrength().getBytes(),
                PWD_VERIFIER_BITS / 8);
    }

    void finish() throws IOException {
        // Flush partial block to out, if any.
        ((CipherOutputStream) out).finish();

        // Calculate and write MAC to footer.
        final Mac mac = this.mac;
        final byte[] buf = new byte[mac.getMacSize()]; // MAC buffer
        final int bufLength = mac.doFinal(buf, 0);
        assert bufLength == buf.length;
        this.leos.write(buf, 0, bufLength / 2);
    }
}
