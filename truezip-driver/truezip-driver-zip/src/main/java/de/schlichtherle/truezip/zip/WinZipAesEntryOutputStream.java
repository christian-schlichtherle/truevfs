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
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.crypto.CipherOutputStream;
import de.schlichtherle.truezip.crypto.param.KeyStrength;
import de.schlichtherle.truezip.io.LEDataOutputStream;
import static de.schlichtherle.truezip.zip.WinZipAesExtraField.*;
import static de.schlichtherle.truezip.zip.ZipEntry.*;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.security.SecureRandom;
import net.jcip.annotations.NotThreadSafe;
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
 * @since   TrueZIP 7.3
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2 (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.gladman.me.uk/cryptography_technology/fileencrypt/">A Password Based File Encyption Utility (Dr. Gladman)</a>
 * @see     <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898: PKCS #5: Password-Based Cryptography Specification Version 2.0 (IETF et al.)</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class WinZipAesEntryOutputStream extends CipherOutputStream {

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

    private final SecureRandom shaker = new SecureRandom();

    private final WinZipAesEntryParameters param;

    /** The Message Authentication Code (MAC). */
    private Mac mac;

    private boolean modifiedEntry;

    /**
     * The low level data output stream.
     * Used for writing the header and footer.
     **/
    private LEDataOutputStream dos;

    WinZipAesEntryOutputStream(
            final LEDataOutputStream out,
            final WinZipAesEntryParameters param)
    throws ZipKeyException {
        super(out, new BufferedBlockCipher(new WinZipAesCipherMode()));
        assert null != out;
        assert null != param;
        this.param = param;

        modifyEntry();
    }

    /**
     * Modify the compressed size and the CRC-32 of the entry if required.
     */
    private void modifyEntry() throws ZipKeyException {
        if (this.modifiedEntry)
            return;
        final WinZipAesEntryParameters param = this.param;
        final ZipEntry entry = param.getEntry();
        long csize = entry.getCompressedSize();
        if (UNKNOWN == csize) {
            assert UNKNOWN == entry.getCrc();
            return;
        }
        assert UNKNOWN != entry.getCrc();
        this.modifiedEntry = true;
        csize += param.getKeyStrength().getBytes() / 2 // salt value
                + 2   // password verification value
                + 10; // authentication code
        entry.setCompressedSize64(csize);
        final WinZipAesExtraField
                ef = (WinZipAesExtraField) entry.getExtraField(WINZIP_AES_ID);
        if (VV_AE_2 == ef.getVendorVersion())
            entry.setCrc32(0);
    }

    void start() throws IOException {
        final WinZipAesEntryParameters param = this.param;

        // Init key strength.
        final KeyStrength keyStrength = param.getKeyStrength();
        final int keyStrengthBits = keyStrength.getBits();
        final int keyStrengthBytes = keyStrength.getBytes();

        // Shake the salt.
        final byte[] salt = new byte[keyStrengthBytes / 2];
        shaker.nextBytes(salt);

        // Init password.
        final byte[] pwdBytes = param.getWritePassword();

        // Derive cipher and MAC parameters.
        // Here comes the strange part about WinZip AES encryption:
        // Its unorthodox use of the Password-Based Key Derivation Function 2
        // (PBKDF2) of PKCS #5 V2.0 alias RFC 2898.
        final PBEParametersGenerator gen = new PKCS5S2ParametersGenerator();
        gen.init(pwdBytes, salt, ITERATION_COUNT);
        assert AES_BLOCK_SIZE_BITS <= keyStrengthBits;
        // Yes, the password verifier is only a 16 bit value.
        // So we must use the MAC for verification.
        final KeyParameter keyParam =
                (KeyParameter) gen.generateDerivedParameters(
                    2 * keyStrengthBits + PWD_VERIFIER_BITS);
        paranoidWipe(pwdBytes); // must not wipe before generator use!

        // Can you believe they "forgot" the nonce in the CTR mode IV?! :-(
        // This is why the WinZip AES specification should be considered
        // "broken"!
        final byte[] ctrIv = new byte[AES_BLOCK_SIZE_BITS / 8];
        final ParametersWithIV aesCtrParam = new ParametersWithIV(
                new KeyParameter(keyParam.getKey(), 0, keyStrengthBytes),
                ctrIv); // yes, the IV is an array of zero bytes!
        final KeyParameter sha1HMacParam = new KeyParameter(
                keyParam.getKey(),
                keyStrengthBytes,
                keyStrengthBytes);

        // Init cipher.
        final BufferedBlockCipher cipher = this.cipher;
        cipher.init(true, aesCtrParam);

        // Init MAC.
        final Mac mac = this.mac = new HMac(new SHA1Digest());
        mac.init(sha1HMacParam);

        // Reinit chain of output streams as Encrypt-then-MAC.
        final LEDataOutputStream dos =
                this.dos = (LEDataOutputStream) this.delegate;
        this.delegate = new MacOutputStream(dos, mac);

        // Write header.
        dos.write(salt);
        writePasswordVerifier(keyParam);
    }

    private void writePasswordVerifier(KeyParameter keyParam)
    throws IOException {
        dos.write(  keyParam.getKey(),
                    2 * param.getKeyStrength().getBytes(),
                    PWD_VERIFIER_BITS / 8);
    }

    /** Wipe the given array. */
    private void paranoidWipe(final byte[] passwd) {
        shaker.nextBytes(passwd);
    }

    @Override
    protected void finish() throws IOException {
        // Flush partial block to out, if any.
        super.finish();

        // Calculate and write MAC to footer.
        final Mac mac = this.mac;
        final byte[] buf = new byte[mac.getMacSize()]; // MAC buffer
        final int bufLength = mac.doFinal(buf, 0);
        assert bufLength == buf.length;
        dos.write(buf, 0, bufLength / 2);

        modifyEntry();
    }
}
