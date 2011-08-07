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

import de.schlichtherle.truezip.crypto.SICSeekableBlockCipher;
import static de.schlichtherle.truezip.crypto.raes.Constants.*;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.io.LEDataOutputStream;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Random;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.io.MacOutputStream;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Writes a type 0 RAES file.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class Type0RaesOutputStream extends RaesOutputStream {

    /**
     * The iteration count for the derived keys of the cipher, KLAC and MAC.
     */
    final static int ITERATION_COUNT = 2005; // The RAES epoch :-)

    private final SecureRandom shaker = new SecureRandom();

    /** The key strength. */
    private final KeyStrength keyStrength;

    /** The Message Authentication Code (MAC). */
    private Mac mac;

    /** The cipher Key and cipher text Length Authentication Code (KLAC). */
    private Mac klac;

    /**
     * The low level data output stream.
     * Used for writing the header and footer.
     **/
    private LEDataOutputStream dos;

    /** The offset where the encrypted application data starts. */
    private long start;

    /** Whether this stream has been closed or not. */
    private boolean closed;

    Type0RaesOutputStream(
            final OutputStream out,
            final Type0RaesParameters param)
    throws IOException{
        super(out, new BufferedBlockCipher(
                new SICSeekableBlockCipher( // or new SICBlockCipher(
                    new AESFastEngine())));

        assert null != out;
        assert null != param;

        // Init key strength.
        final KeyStrength keyStrength = param.getKeyStrength();
        final int keyStrengthOrdinal = keyStrength.ordinal();
        final int keyStrengthBits = keyStrength.getBits();
        final int keyStrengthBytes = keyStrength.getBytes();
        this.keyStrength = keyStrength;

        // Init digest for key generation and KLAC.
        final Digest digest = new SHA256Digest();
        assert digest.getDigestSize() >= keyStrengthBytes;

        // Init PBE parameters.
        final PBEParametersGenerator gen = new PKCS12ParametersGenerator(digest);
        final char[] pwdChars = param.getWritePassword();
        final byte[] pwdBytes = PBEParametersGenerator.PKCS12PasswordToBytes(pwdChars);
        paranoidWipe(pwdChars);

        // Shake the salt.
        final byte[] salt = new byte[keyStrengthBytes];
        shaker.nextBytes(salt);

        // Derive cipher and MAC parameters.
        gen.init(pwdBytes, salt, ITERATION_COUNT);
        final ParametersWithIV
                cipherParam = (ParametersWithIV) gen.generateDerivedParameters(
                    keyStrengthBits, AES_BLOCK_SIZE_BITS);
        final CipherParameters
                macParam = gen.generateDerivedMacParameters(keyStrengthBits);
        paranoidWipe(pwdBytes);

        // Init cipher.
        this.cipher.init(true, cipherParam);

        // Init MAC.
        final Mac mac = this.mac = new HMac(digest);
        mac.init(macParam);

        // Init KLAC.
        final Mac klac = this.klac = new HMac(new SHA256Digest()); // cannot reuse digest!
        klac.init(macParam); // resets the digest
        final byte[] cipherKey = ((KeyParameter) cipherParam.getParameters())
                .getKey();
        klac.update(cipherKey, 0, cipherKey.length);

        // Reinit chain of output streams as Encrypt-then-MAC.
        final LEDataOutputStream dos =
                this.dos = out instanceof LEDataOutputStream
                    ? (LEDataOutputStream) out
                    : new LEDataOutputStream(out);
        this.delegate = new MacOutputStream(dos, mac);

        // Write data envelope header.
        dos.writeInt(SIGNATURE);
        dos.writeByte(ENVELOPE_TYPE_0);
        dos.writeByte(keyStrengthOrdinal);
        dos.writeShort(ITERATION_COUNT);
        dos.write(salt);

        // Init start.
        this.start = dos.size();
        assert ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT + salt.length == start;
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
    public KeyStrength getKeyStrength() {
        return keyStrength;
    }

    @Override
    protected void finish() throws IOException {
        // Flush partial block to out, if any.
        super.finish();

        final long trailer = dos.size();

        final Mac mac = this.mac;
        assert mac.getMacSize() == klac.getMacSize();
        final byte[] buf = new byte[mac.getMacSize()]; // MAC buffer
        int bufLength;

        // Calculate and write KLAC to data envelope footer.
        // Please note that we will only use the first half of the
        // authentication code for security reasons.
        final long length = trailer - start; // message length
        klac(klac, length, buf);
        dos.write(buf, 0, buf.length / 2);

        // Calculate and write MAC to data envelope footer.
        // Again, we will only use the first half of the
        // authentication code for security reasons.
        bufLength = mac.doFinal(buf, 0);
        assert bufLength == buf.length;
        dos.write(buf, 0, buf.length / 2);

        assert dos.size() - trailer == buf.length;
    }
}
