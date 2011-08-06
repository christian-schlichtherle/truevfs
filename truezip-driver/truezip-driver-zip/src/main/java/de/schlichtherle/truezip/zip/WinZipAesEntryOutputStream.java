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
import de.schlichtherle.truezip.crypto.SICSeekableBlockCipher;
import de.schlichtherle.truezip.crypto.param.KeyStrength;
import de.schlichtherle.truezip.io.LEDataOutputStream;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.io.MacOutputStream;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Encrypts ZIP entry contents according the WinZip AES specification.
 * 
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2</a>
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

    /**
     * The low level data output stream.
     * Used for writing the header and footer.
     **/
    private LEDataOutputStream dos;

    WinZipAesEntryOutputStream(
            final LEDataOutputStream out,
            final WinZipAesEntryParameters param) {
        super(out, new BufferedBlockCipher(
                new SICSeekableBlockCipher( // or new SICBlockCipher(
                    new AESFastEngine())));
        assert null != out;
        assert null != param;
        this.param = param;
    }

    void start() throws IOException {
        final WinZipAesEntryParameters param = this.param;

        // Init key strength.
        final KeyStrength keyStrength = param.getKeyStrength();
        final int keyStrengthBits = keyStrength.getBits();
        final int keyStrengthBytes = keyStrength.getBytes();

        // Init PBE parameters.
        final PBEParametersGenerator gen = new PKCS5S2ParametersGenerator();
        final char[] pwdChars = param.getWritePassword();
        final byte[] pwdBytes = PBEParametersGenerator.PKCS5PasswordToBytes(pwdChars);
        paranoidWipe(pwdChars);

        // Shake the salt.
        final byte[] salt = new byte[keyStrengthBytes / 2];
        shaker.nextBytes(salt);

        // Derive cipher and MAC parameters.
        gen.init(pwdBytes, salt, ITERATION_COUNT);
        final ParametersWithIV
                cipherParam = (ParametersWithIV) gen.generateDerivedParameters(
                    keyStrengthBits, AES_BLOCK_SIZE_BITS + PWD_VERIFIER_BITS);
        final CipherParameters
                macParam = gen.generateDerivedMacParameters(keyStrengthBits);
        paranoidWipe(pwdBytes);

        // Init cipher.
        this.cipher.init(true, cipherParam);

        // Init MAC.
        final Mac mac = this.mac = new HMac(new SHA1Digest());
        mac.init(macParam);

        // Reinit chain of output streams as Encrypt-then-MAC.
        final LEDataOutputStream dos =
                this.dos = (LEDataOutputStream) this.delegate;
        this.delegate = new MacOutputStream(dos, mac);

        // Write header.
        dos.write(salt);
        writePasswordVerifier(cipherParam);
    }

    private void writePasswordVerifier(ParametersWithIV cipherParam)
    throws IOException {
        dos.write(  cipherParam.getIV(),
                    AES_BLOCK_SIZE_BITS / 8,
                    PWD_VERIFIER_BITS / 8);
    }

    /** Wipe the given array. */
    private void paranoidWipe(final byte[] passwd) {
        shaker.nextBytes(passwd);
    }

    /** Wipe the given array. */
    private void paranoidWipe(final char[] passwd) {
        final Random rng = shaker;
        for (int i = passwd.length; --i >= 0; )
            passwd[i] = (char) rng.nextInt();
    }

    @Override
    public void finish() throws IOException {
        // Flush partial block to out, if any.
        super.finish();

        // Calculate and write MAC to footer.
        final byte[] buf = new byte[mac.getMacSize()]; // MAC buffer
        final int bufLength = mac.doFinal(buf, 0);
        assert bufLength == buf.length;
        dos.write(buf, 0, bufLength / 2);
    }
}
