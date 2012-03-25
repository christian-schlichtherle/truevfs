/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto;

import de.truezip.driver.zip.crypto.SICSeekableBlockCipher;
import de.truezip.driver.zip.crypto.SeekableBlockCipher;
import de.truezip.driver.zip.crypto.SuspensionPenalty;
import static de.truezip.driver.zip.raes.crypto.Constants.AES_BLOCK_SIZE_BITS;
import static de.truezip.driver.zip.raes.crypto.Constants.ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT;
import de.truezip.driver.zip.raes.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.ArrayHelper;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.EOFException;
import java.io.IOException;
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
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class Type0RaesReadOnlyFile extends RaesReadOnlyFile {

    /** The key strength. */
    private final AesKeyStrength keyStrength;

    /**
     * The key parameter required to init the SHA-256 Message Authentication
     * Code (HMAC).
     */
    private final KeyParameter sha256MacParam;

    /**
     * The footer of the data envelope containing the authentication codes.
     */
    private final byte[] footer;

    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    Type0RaesReadOnlyFile(
            final @WillCloseWhenClosed ReadOnlyFile rof,
            final Type0RaesParameters param)
    throws IOException {
        super(rof);

        assert null != param;

        // Init read only file.
        rof.seek(0);
        final long fileLength = rof.length();

        // Load header data.
        final byte[] header = new byte[ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT];
        rof.readFully(header);

        // Check key size and iteration count
        final int keyStrengthOrdinal = readUByte(header, 5);
        final AesKeyStrength keyStrength;
        try {
            keyStrength = AesKeyStrength.values()[keyStrengthOrdinal];
            assert keyStrength.ordinal() == keyStrengthOrdinal;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new RaesException(
                    "Unknown index for cipher key strength: "
                    + keyStrengthOrdinal);
        }
        final int keyStrengthBytes = keyStrength.getBytes();
        final int keyStrengthBits = keyStrength.getBits();
        this.keyStrength = keyStrength;

        final int iCount = readUShort(header, 6);
        if (1024 > iCount)
            throw new RaesException(
                    "Iteration count must be 1024 or greater, but is "
                    + iCount
                    + "!");

        // Load salt.
        final byte[] salt = new byte[keyStrengthBytes];
        rof.readFully(salt);

        // Init KLAC and footer.
        final Mac klac = new HMac(new SHA256Digest());
        this.footer = new byte[klac.getMacSize()];

        // Init start, end and length of encrypted data.
        final long start = header.length + salt.length;
        final long end = fileLength - this.footer.length;
        final long length = end - start;
        if (length < 0) {
            // Wrap an EOFException so that a caller can identify this issue.
            throw new RaesException("False positive Type 0 RAES file is too short!",
                    new EOFException());
        }

        // Load footer data.
        rof.seek(end);
        rof.readFully(this.footer);
        if (-1 != rof.read()) {
            // This should never happen unless someone is writing to the
            // end of the file concurrently!
            throw new RaesException(
                    "Expected end of file after data envelope trailer!");
        }

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
            for (int i = passwd.length; --i >= 0; ) // nullify password parameter
                passwd[i] = 0;

            gen.init(pass, salt, iCount);
            aesCtrParam = (ParametersWithIV) gen.generateDerivedParameters(
                    keyStrengthBits, AES_BLOCK_SIZE_BITS);
            sha256MacParam = (KeyParameter) gen.generateDerivedMacParameters(
                    keyStrengthBits);
            paranoidWipe(pass);

            lastTry = SuspensionPenalty.enforce(lastTry);

            // Compute and verify KLAC.
            klac.init(sha256MacParam);
            
            // Update the KLAC with the cipher key.
            // This is actually redundant, but it's part of the spec, so it
            // cannot get changed anymore.
            final byte[] cipherKey = ((KeyParameter) aesCtrParam.getParameters()).getKey();
            klac.update(cipherKey, 0, cipherKey.length);
            buf = new byte[klac.getMacSize()];
            RaesOutputStream.klac(klac, length, buf);
        } while (!ArrayHelper.equals(this.footer, 0, buf, 0, buf.length / 2));

        // Init parameters for authenticate().
        this.sha256MacParam = sha256MacParam;

        // Init cipher.
        final SeekableBlockCipher
                cipher = new SICSeekableBlockCipher(new AESFastEngine());
        cipher.init(false, aesCtrParam);
        init(cipher, start, length);

        // Commit key strength to parameters.
        param.setKeyStrength(keyStrength);
    }

    /** Wipe the given array. */
    private void paranoidWipe(final byte[] pwd) {
        for (int i = pwd.length; --i >= 0; )
            pwd[i] = 0;
    }

    @Override
    public AesKeyStrength getKeyStrength() {
        return keyStrength;
    }

    @Override
    public void authenticate() throws IOException {
        final Mac mac = new HMac(new SHA256Digest());
        mac.init(sha256MacParam);
        final byte[] buf = computeMac(mac);
        assert buf.length == mac.getMacSize();
        if (!ArrayHelper.equals(buf, 0, footer, footer.length / 2, footer.length / 2))
            throw new RaesAuthenticationException();
    }
}