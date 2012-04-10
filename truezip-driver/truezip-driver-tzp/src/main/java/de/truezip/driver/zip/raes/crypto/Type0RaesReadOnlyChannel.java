/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto;

import de.truezip.driver.zip.crypto.CipherReadOnlyChannel;
import de.truezip.driver.zip.crypto.CtrBlockCipher;
import de.truezip.driver.zip.crypto.SeekableBlockCipher;
import static de.truezip.driver.zip.raes.crypto.Constants.AES_BLOCK_SIZE_BITS;
import static de.truezip.driver.zip.raes.crypto.Constants.TYPE_0_HEADER_LEN_WO_SALT;
import de.truezip.kernel.io.IntervalReadOnlyChannel;
import de.truezip.kernel.io.PowerBuffer;
import de.truezip.key.param.AesKeyStrength;
import de.truezip.key.util.SuspensionPenalty;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.NotThreadSafe;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import static org.bouncycastle.crypto.PBEParametersGenerator.PKCS12PasswordToBytes;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Reads a type 0 RAES file.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class Type0RaesReadOnlyChannel extends RaesReadOnlyChannel {

    /** The key strength. */
    private final AesKeyStrength keyStrength;

    private final ByteBuffer authenticationCode;

    /**
     * The key parameter required to init the SHA-256 Message Authentication
     * Code (HMAC).
     */
    private final KeyParameter sha256MacParam;

    @CreatesObligation
    Type0RaesReadOnlyChannel(
            final Type0RaesParameters param,
            final @WillCloseWhenClosed SeekableByteChannel channel)
    throws IOException {
        assert null != param;
        assert null != channel;

        // Load header data.
        final PowerBuffer header = PowerBuffer
                .allocate(TYPE_0_HEADER_LEN_WO_SALT)
                .littleEndian()
                .load(channel.position(0));
        final int type = header.position(4).getUByte();
        assert 0 == type;

        // Check key size and iteration count
        final int keyStrengthOrdinal = header.getUByte();
        final AesKeyStrength keyStrength;
        try {
            keyStrength = AesKeyStrength.values()[keyStrengthOrdinal];
            assert keyStrength.ordinal() == keyStrengthOrdinal;
        } catch (final ArrayIndexOutOfBoundsException ex) {
            throw new RaesException(
                    "Unknown index for cipher key strength: "
                    + keyStrengthOrdinal);
        }
        final int keyStrengthBytes = keyStrength.getBytes();
        final int keyStrengthBits = keyStrength.getBits();
        this.keyStrength = keyStrength;

        final int iCount = header.getUShort();
        if (1024 > iCount)
            throw new RaesException(
                    "Iteration count must be 1024 or greater, but is "
                    + iCount
                    + "!");

        // Load salt.
        final PowerBuffer salt = PowerBuffer
                .allocate(keyStrengthBytes)
                .load(channel);

        // Init KLAC and footer.
        final Mac klac = new HMac(new SHA256Digest());
        final PowerBuffer footer = PowerBuffer.allocate(klac.getMacSize());

        // Init start, end and size of encrypted data.
        final long start = channel.position();
        final long end = channel.size() - footer.limit();
        final long size = end - start;
        if (0 > size) {
            // Wrap an EOFException so that a caller can identify this issue.
            throw new RaesException("False positive Type 0 RAES file is too short!",
                    new EOFException());
        }

        // Load authentication code.
        footer.load(channel.position(end)).position(footer.limit() / 2);
        if (channel.position() != channel.size()) {
            // This should never happen unless someone is writing to the
            // end of the file concurrently!
            throw new RaesException(
                    "Expected end of file after data envelope trailer!");
        }
        authenticationCode = footer.slice().buffer();
        final ByteBuffer passwdVerifier = footer.flip().buffer();

        // Derive cipher and MAC parameters.
        final PBEParametersGenerator
                gen = new PKCS12ParametersGenerator(new SHA256Digest());
        ParametersWithIV aesCtrParam;
        KeyParameter sha256MacParam;
        byte[] buf;
        long lastTry = 0; // don't enforce suspension on first prompt!
        do {
            final char[] passwd = param.getReadPassword(0 != lastTry);
            assert null != passwd;
            final byte[] pass = PKCS12PasswordToBytes(passwd);
            Arrays.fill(passwd, (char) 0);

            gen.init(pass, salt.array(), iCount);
            aesCtrParam = (ParametersWithIV) gen.generateDerivedParameters(
                    keyStrengthBits, AES_BLOCK_SIZE_BITS);
            sha256MacParam = (KeyParameter) gen.generateDerivedMacParameters(
                    keyStrengthBits);
            Arrays.fill(pass, (byte) 0);

            lastTry = SuspensionPenalty.enforce(lastTry);

            // Compute and verify KLAC.
            klac.init(sha256MacParam);
            
            // Update the KLAC with the cipher key.
            // This is actually redundant, but it's part of the spec, so it
            // cannot get changed anymore.
            final byte[] cipherKey = ((KeyParameter) aesCtrParam.getParameters()).getKey();
            klac.update(cipherKey, 0, cipherKey.length);
            buf = new byte[klac.getMacSize()];
            RaesOutputStream.klac(klac, size, buf);
        } while (!passwdVerifier.equals(ByteBuffer.wrap(buf, 0, buf.length / 2)));

        // Init parameters for authenticate().
        this.sha256MacParam = sha256MacParam;

        // Init cipher and channel.
        final SeekableBlockCipher
                cipher = new CtrBlockCipher(new AESFastEngine());
        cipher.init(false, aesCtrParam);
        this.channel = new CipherReadOnlyChannel(cipher,
                new IntervalReadOnlyChannel(channel.position(start), size));

        // Commit key strength.
        param.setKeyStrength(keyStrength);
    }

    @Override
    public AesKeyStrength getKeyStrength() {
        return keyStrength;
    }

    @Override
    public void authenticate() throws IOException {
        final Mac mac = new HMac(new SHA256Digest());
        mac.init(sha256MacParam);
        final byte[] buf = ((CipherReadOnlyChannel) channel).mac(mac);
        assert buf.length == mac.getMacSize();
        if (!authenticationCode.equals(ByteBuffer.wrap(buf, 0, buf.length / 2)))
            throw new RaesAuthenticationException();
    }
}
