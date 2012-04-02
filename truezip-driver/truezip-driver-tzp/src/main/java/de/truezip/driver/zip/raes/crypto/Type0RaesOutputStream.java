/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto;

import de.truezip.driver.zip.crypto.SICSeekableBlockCipher;
import static de.truezip.driver.zip.raes.crypto.Constants.*;
import de.truezip.kernel.io.LEDataOutputStream;
import de.truezip.kernel.io.Sink;
import de.truezip.key.param.AesKeyStrength;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Random;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.*;
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
 */
@NotThreadSafe
final class Type0RaesOutputStream extends RaesOutputStream {

    /**
     * The iteration count for the derived keys of the cipher, KLAC and MAC.
     */
    final static int ITERATION_COUNT = 2005; // The RAES epoch :-)

    private final SecureRandom shaker = new SecureRandom();

    /** The key strength. */
    private final AesKeyStrength keyStrength;

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

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    Type0RaesOutputStream(final Sink sink, final Type0RaesParameters param)
    throws IOException{
        super(new BufferedBlockCipher(
                new SICSeekableBlockCipher( // or new SICBlockCipher(
                    new AESFastEngine())));

        assert null != sink;
        assert null != param;

        // Init key strength.
        final AesKeyStrength keyStrength = param.getKeyStrength();
        final int keyStrengthOrdinal = keyStrength.ordinal();
        final int keyStrengthBits = keyStrength.getBits();
        final int keyStrengthBytes = keyStrength.getBytes();
        this.keyStrength = keyStrength;

        // Shake the salt.
        final byte[] salt = new byte[keyStrengthBytes];
        shaker.nextBytes(salt);

        // Init digest for key generation and KLAC.
        final Digest digest = new SHA256Digest();
        assert digest.getDigestSize() >= keyStrengthBytes;

        // Init password.
        final char[] pwdChars = param.getWritePassword();
        final byte[] pwdBytes = PBEParametersGenerator.PKCS12PasswordToBytes(pwdChars);
        paranoidWipe(pwdChars);

        // Derive cipher and MAC parameters.
        final PBEParametersGenerator gen = new PKCS12ParametersGenerator(digest);
        gen.init(pwdBytes, salt, ITERATION_COUNT);
        final ParametersWithIV
                aesCtrParam = (ParametersWithIV) gen.generateDerivedParameters(
                    keyStrengthBits, AES_BLOCK_SIZE_BITS);
        final CipherParameters
                sha256HMmacParam = gen.generateDerivedMacParameters(keyStrengthBits);
        paranoidWipe(pwdBytes);

        // Init cipher.
        this.cipher.init(true, aesCtrParam);

        // Init MAC.
        final Mac mac = this.mac = new HMac(digest);
        mac.init(sha256HMmacParam);

        // Init KLAC.
        final Mac klac = this.klac = new HMac(new SHA256Digest()); // cannot reuse digest!
        klac.init(sha256HMmacParam); // resets the digest

        // Update the KLAC with the cipher key.
        // This is actually redundant, but it's part of the spec, so it
        // cannot get changed anymore.
        final byte[] cipherKey = ((KeyParameter) aesCtrParam.getParameters())
                .getKey();
        klac.update(cipherKey, 0, cipherKey.length);

        // Init chain of output streams as Encrypt-then-MAC.
        final OutputStream out = sink.newStream();
        final LEDataOutputStream dos =
                this.dos = out instanceof LEDataOutputStream
                    ? (LEDataOutputStream) out
                    : new LEDataOutputStream(out);
        this.out = new MacOutputStream(dos, mac);

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
    public AesKeyStrength getKeyStrength() {
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