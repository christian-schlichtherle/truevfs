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
import de.schlichtherle.truezip.crypto.SuspensionPenalty;
import static de.schlichtherle.truezip.crypto.raes.Constants.*;
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
final class Type0RaesReadOnlyFile extends RaesReadOnlyFile {

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

        assert null != param;

        // Init read only file.
        rof.seek(0);
        final long fileLength = rof.length();

        // Load header data.
        final byte[] header = new byte[ENVELOPE_TYPE_0_HEADER_LEN_WO_SALT];
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

        // Init start of encrypted data.
        final long start = header.length + salt.length;

        // Init KLAC.
        final Mac klac = new HMac(new SHA256Digest());

        // Load footer data.
        {
            this.footer = new byte[klac.getMacSize()];
            final long end = fileLength - this.footer.length;
            rof.seek(end);
            rof.readFully(this.footer);
            if (-1 != rof.read()) {
                // This should never happen unless someone is writing to the
                // end of the file concurrently!
                throw new RaesException(
                        "Expected end of file after data envelope trailer!");
            }
        }

        // Init encrypted data length.
        final long length = fileLength - this.footer.length - start;

        // Derive cipher and MAC parameters.
        final PBEParametersGenerator
                gen = new PKCS12ParametersGenerator(new SHA256Digest());
        ParametersWithIV aesCtrParam;
        CipherParameters sha256HMacParam;
        long lastTry = 0; // don't enforce suspension on first prompt!
        for (boolean invalid = false; ; invalid = true) {
            final char[] passwd = param.getReadPassword(invalid);
            assert null != passwd;
            final byte[] pass = PKCS12PasswordToBytes(passwd);
            for (int i = passwd.length; --i >= 0; ) // nullify password parameter
                passwd[i] = 0;

            gen.init(pass, salt, iCount);
            aesCtrParam = (ParametersWithIV) gen.generateDerivedParameters(
                    keyStrengthBits, AES_BLOCK_SIZE_BITS);
            sha256HMacParam = gen.generateDerivedMacParameters(keyStrengthBits);
            paranoidWipe(pass);

            // Verify KLAC.
            klac.init(sha256HMacParam);
            final byte[] cipherKey = ((KeyParameter) aesCtrParam.getParameters()).getKey();
            klac.update(cipherKey, 0, cipherKey.length);
            final byte[] buf = new byte[klac.getMacSize()];
            RaesOutputStream.klac(klac, length, buf);

            lastTry = SuspensionPenalty.enforce(lastTry);

            if (ArrayHelper.equals(this.footer, 0, buf, 0, buf.length / 2))
                break;
        }

        // Init cipher.
        final SeekableBlockCipher
                cipher = new SICSeekableBlockCipher(new AESFastEngine());
        cipher.init(false, aesCtrParam);
        init(cipher, start, length);

        this.macParam = sha256HMacParam;

        // Commit parameters.
        param.setKeyStrength(keyStrength);
    }

    /** Wipe the given array. */
    private void paranoidWipe(final byte[] pwd) {
        for (int i = pwd.length; --i >= 0; )
            pwd[i] = 0;
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
        assert buf.length == mac.getMacSize();
        if (!ArrayHelper.equals(buf, 0, footer, footer.length / 2, footer.length / 2))
            throw new RaesAuthenticationException();
    }
}
