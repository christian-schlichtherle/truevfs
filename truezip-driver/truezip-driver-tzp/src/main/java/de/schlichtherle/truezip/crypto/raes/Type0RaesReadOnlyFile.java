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
import de.schlichtherle.truezip.crypto.SeekableBlockCipher;
import static de.schlichtherle.truezip.crypto.raes.RaesConstants.*;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.ArrayHelper;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
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
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
class Type0RaesReadOnlyFile extends RaesReadOnlyFile {

    /**
     * The minimum delay between subsequent attempts to authenticate a key
     * in milliseconds.
     */
    private static final long MIN_KEY_RETRY_DELAY = 3 * 1000;

    /** The key strength. */
    private final KeyStrength keyStrength;

    /**
     * The parameters required to init the Message Authentication Code (MAC).
     */
    private final CipherParameters macParam;

    /**
     * The footer of the data envelope containing the authentication codes.
     */
    private final byte[] footer;

    Type0RaesReadOnlyFile(
            final ReadOnlyFile rof,
            final Type0RaesParameters param)
    throws IOException {
        super(rof);

        assert null != rof;
        assert null != param;

        // Load header data.
        final byte[] header = new byte[ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT];
        final long fileLength = rof.length();
        rof.seek(0);
        rof.readFully(header);

        // Check key size and iteration count
        final int keyStrengthOrdinal = readUByte(header, 5);
        final KeyStrength keyStrength;
        try {
            keyStrength = KeyStrength.values()[keyStrengthOrdinal];
            assert keyStrength.ordinal() == keyStrengthOrdinal;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new RaesException(
                    "Unknown index for cipher key strength: "
                    + keyStrengthOrdinal
                    + "!",
                    ex);
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

        // Init start of encrypted data.
        final long start = header.length + keyStrengthBytes;

        // Load salt.
        final byte[] salt = new byte[keyStrengthBytes];
        rof.readFully(salt);

        // Init digest for key generation and KLAC.
        final Digest digest = new SHA256Digest();

        // Load footer data
        footer = new byte[digest.getDigestSize()];
        final long end = fileLength - footer.length;
        rof.seek(end);
        rof.readFully(footer);
        if (-1 != this.delegate.read()) {
            // This should never happen unless someone is writing to the
            // end of the file concurrently!
            throw new RaesException(
                    "Expected end of file after data envelope trailer!");
        }

        // Init encrypted data length.
        final long length = fileLength - footer.length - start;

        // Init PBE parameters.
        final PBEParametersGenerator gen = new PKCS12ParametersGenerator(digest);
        ParametersWithIV cipherParam;
        CipherParameters macParam;
        long lastTry = 0; // don't enforce suspension on first prompt!
        for (boolean invalid = false; ; invalid = true) {
            final char[] passwd = param.getReadPassword(invalid);
            if (null == passwd) // safety first!
                throw new RaesKeyException();
            final byte[] pass = PKCS12PasswordToBytes(passwd);
            for (int i = passwd.length; --i >= 0; ) // nullify password parameter
                passwd[i] = 0;

            gen.init(pass, salt, iCount);
            cipherParam = (ParametersWithIV) gen.generateDerivedParameters(
                    keyStrengthBits, AES_BLOCK_SIZE);
            macParam = gen.generateDerivedMacParameters(keyStrengthBits);
            for (int i = pass.length; --i >= 0; ) // nullify password buffer
                pass[i] = 0;

            // Init and verify KLAC.
            final Mac klac = new HMac(digest);
            klac.init(macParam);
            final byte[] cipherKey = ((KeyParameter) cipherParam.getParameters()).getKey();
            klac.update(cipherKey, 0, cipherKey.length);
            final byte[] buf = new byte[klac.getMacSize()];
            RaesOutputStream.klac(klac, length, buf);
            digest.reset(); // klac.doFinal(...) doesn't do this!

            lastTry = enforceSuspensionPenalty(lastTry);

            if (ArrayHelper.equals(footer, 0, buf, 0, buf.length / 2))
                break;
        }

        param.setKeyStrength(keyStrength);

        this.macParam = macParam;

        // Init cipher.
        final SeekableBlockCipher cipher = new SICSeekableBlockCipher(new AESFastEngine());
        cipher.init(false, cipherParam);

        init(cipher, start, length);
    }

    @SuppressWarnings("SleepWhileHoldingLock")
    private static long enforceSuspensionPenalty(final long last) {
        long delay;
        InterruptedException interrupted = null;
        while ((delay = System.currentTimeMillis() - last) < MIN_KEY_RETRY_DELAY) {
            try {
                Thread.sleep(MIN_KEY_RETRY_DELAY - delay);
            } catch (InterruptedException ex) {
                interrupted = ex;
            }
        }
        if (interrupted != null)
            Thread.currentThread().interrupt();
        return last + delay;
    }

    @Override
    public KeyStrength getKeyStrength() {
        return keyStrength;
    }

    @Override
    public void authenticate() throws RaesAuthenticationException, IOException {
        final Mac mac = new HMac(new SHA256Digest());
        mac.init(macParam);

        final byte[] buf = computeMac(mac);

        if (!ArrayHelper.equals(footer, footer.length / 2, buf, 0, buf.length / 2))
            throw new RaesAuthenticationException();
    }
}
