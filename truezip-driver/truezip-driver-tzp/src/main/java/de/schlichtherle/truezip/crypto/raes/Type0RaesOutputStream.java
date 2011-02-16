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

import java.security.SecureRandom;
import de.schlichtherle.truezip.crypto.SICSeekableBlockCipher;
import de.schlichtherle.truezip.io.LEDataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

import static de.schlichtherle.truezip.crypto.raes.RaesConstants.*;

/**
 * Writes a type 0 RAES file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
class Type0RaesOutputStream extends RaesOutputStream {

    private static final SecureRandom shaker = new SecureRandom();

    /**
     * The iteration count for the derived keys of the cipher, KLAC and MAC.
     */
    final static int ITERATION_COUNT = 2005; // The RAES epoch :-)

    /** The actual key strength in bits. */
    private int keyStrengthBits;

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
    throws  NullPointerException,
            IllegalArgumentException,
            RaesKeyException,
            IOException{
        super(out, null);

        assert null != out;
        assert null != param;

        // Check parameters (fail fast).
        final char[] passwd = param.getWritePasswd();
        if (null == passwd)
            throw new RaesKeyException();
        final int keyStrength = param.getKeyStrength().ordinal();

        // Init digest for key generation and KLAC.
        final Digest digest = new SHA256Digest();

        // Init key strength info and salt.
        final int keyStrengthBytes = 16 + keyStrength * 8;
        keyStrengthBits = keyStrengthBytes * 8; // key strength in bits
        assert digest.getDigestSize() >= keyStrengthBytes;
        final byte[] salt = new byte[keyStrengthBytes];
        shaker.nextBytes(salt);

        // Init PBE parameters.
        final PBEParametersGenerator gen = new PKCS12ParametersGenerator(digest);
        final byte[] pass = PBEParametersGenerator.PKCS12PasswordToBytes(passwd);
        for (int i = passwd.length; --i >= 0; ) // nullify password parameter
            passwd[i] = 0;

        gen.init(pass, salt, ITERATION_COUNT);
        // Order is important here, because paramGen does not properly
        // reset the digest object!
        final ParametersWithIV
                cipherParam = (ParametersWithIV) gen.generateDerivedParameters(
                    keyStrengthBits, AES_BLOCK_SIZE);
        final CipherParameters
                macParam = gen.generateDerivedMacParameters(keyStrengthBits);
        for (int i = pass.length; --i >= 0; ) // nullify password buffer
            pass[i] = 0;

        // Init cipher.
        final BufferedBlockCipher
                cipher = new BufferedBlockCipher(
                    new SICSeekableBlockCipher(
                        new AESFastEngine()));
        cipher.init(true, cipherParam);

        // Init MAC.
        mac = new HMac(new SHA256Digest());
        mac.init(macParam);

        // Init KLAC.
        klac = new HMac(digest);
        klac.init(macParam); // resets the digest
        final byte[] cipherKey = ((KeyParameter) cipherParam.getParameters())
                .getKey();
        klac.update(cipherKey, 0, cipherKey.length);

        // Init stream chain.
        dos = new LEDataOutputStream(out);
        this.delegate = new MacOutputStream(dos, mac);

        // Write data envelope header.
        dos.writeInt(SIGNATURE);
        dos.writeByte(ENVELOPE_TYPE_0);
        dos.writeByte(keyStrength);
        dos.writeShort(ITERATION_COUNT);
        dos.write(salt);

        // Init start.
        start = dos.size();
        assert ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT + salt.length == start;

        // Finally init the super class cipher.
        this.cipher = cipher;
    }

    @Override
    public int getKeySizeBits() {
        return keyStrengthBits;
    }

    @Override
    public void close() throws IOException {
        // Order is important here!
        if (!closed) {
            closed = true;
            try {
                // Flush partial block to out, if any.
                finish();

                final long trailer = dos.size();

                assert mac.getMacSize() == klac.getMacSize();
                final byte[] buf = new byte[mac.getMacSize()]; // authentication code buffer
                int bufLen;

                // Calculate and write KLAC to data envelope trailer.
                // Please note that we will only use the first half of the
                // authentication code for security reasons.
                final long length = trailer - start; // message length
                klac(klac, length, buf);
                dos.write(buf, 0, buf.length / 2);

                // Calculate and write MAC to data envelope trailer.
                // Again, we will only use the first half of the
                // authentication code for security reasons.
                bufLen = mac.doFinal(buf, 0);
                assert bufLen == buf.length;
                dos.write(buf, 0, buf.length / 2);

                assert dos.size() - trailer == buf.length;
            } finally {
                super.close();
            }
        }
    }
}
