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

import de.schlichtherle.truezip.crypto.CipherReadOnlyFile;
import de.schlichtherle.truezip.crypto.SeekableBlockCipher;
import de.schlichtherle.truezip.crypto.SuspensionPenalty;
import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.util.ArrayHelper;
import static de.schlichtherle.truezip.zip.WinZipAesExtraField.*;
import static de.schlichtherle.truezip.zip.WinZipAesEntryOutputStream.*;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Arrays;
import net.jcip.annotations.NotThreadSafe;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * Decrypts ZIP entry contents according the WinZip AES specification.
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
final class WinZipAesReadOnlyFile extends CipherReadOnlyFile {

    WinZipAesReadOnlyFile(
            final ReadOnlyFile rof,
            final WinZipAesEntryParameters param)
    throws IOException {
        super(rof);

        // Init WinZip AES extra field.
        final ZipEntry entry = param.getEntry();
        assert entry.isEncrypted();
        final WinZipAesExtraField
                field = (WinZipAesExtraField) entry.getExtraField(WINZIP_AES_ID);

        // Init key strength.
        final AesKeyStrength keyStrength = field.getKeyStrength();
        final int keyStrengthBits = keyStrength.getBits();
        final int keyStrengthBytes = keyStrength.getBytes();

        // Init read only file.
        rof.seek(0);
        final long fileLength = rof.length();

        // Load salt.
        final byte[] salt = new byte[keyStrengthBytes / 2];
        rof.readFully(salt);
        
        // Load password verification value.
        final byte[] passwdVerifier = new byte[PWD_VERIFIER_BITS / 8];
        rof.readFully(passwdVerifier);

        // Init start of encrypted data.
        final long start = salt.length + passwdVerifier.length;

        // Init MAC and load authentication code.
        final Mac mac = new HMac(new SHA1Digest());
        final byte[] authenticationCode = new byte[mac.getMacSize() / 2];
        {
            final long end = fileLength - authenticationCode.length;
            rof.seek(end);
            rof.readFully(authenticationCode);
            if (-1 != rof.read()) {
                // This should never happen unless someone is writing to the
                // end of the file concurrently!
                throw new ZipCryptoException(
                        "Expected end of file after authentication code!");
            }
        }

        // Init encrypted data length.
        final long length = fileLength - authenticationCode.length - start;

        // Derive cipher and MAC parameters.
        final PBEParametersGenerator gen = new PKCS5S2ParametersGenerator();
        ParametersWithIV aesCtrParam;
        CipherParameters sha1HMacParam;
        long lastTry = 0; // don't enforce suspension on first prompt!
        for (boolean invalid = false; ; invalid = true) {
            final byte[] passwd = param.getReadPassword(invalid);
            assert null != passwd;

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
            paranoidWipe(passwd);

            // Can you believe they "forgot" the nonce in the CTR mode IV?! :-(
            final byte[] ctrIv = new byte[AES_BLOCK_SIZE_BITS / 8];
            aesCtrParam = new ParametersWithIV(
                    new KeyParameter(keyParam.getKey(), 0, keyStrengthBytes),
                    ctrIv); // yes, the IV is an array of zero bytes!
            sha1HMacParam = new KeyParameter(
                    keyParam.getKey(),
                    keyStrengthBytes,
                    keyStrengthBytes);

            lastTry = SuspensionPenalty.enforce(lastTry);

            // Verify password.
            if (ArrayHelper.equals(
                    keyParam.getKey(), 2 * keyStrengthBytes,
                    passwdVerifier, 0,
                    PWD_VERIFIER_BITS / 2))
                break;
        }

        // Init cipher.
        final SeekableBlockCipher cipher = new WinZipAesCipher();
        cipher.init(false, aesCtrParam);
        init(cipher, start, length);

        // Authenticate.
        mac.init(sha1HMacParam);
        authenticate(mac, authenticationCode, entry.getName());

        // Commit parameters.
        param.setKeyStrength(keyStrength);
    }

    /** Wipe the given array. */
    private void paranoidWipe(final byte[] pwd) {
        for (int i = pwd.length; --i >= 0; )
            pwd[i] = 0;
    }

    /**
     * Authenticates all encrypted data in this read only file.
     * It is safe to call this method multiple times to detect if the file
     * has been tampered with meanwhile.
     * <p>
     * This method is called from the constructor.
     *
     * @throws ZipAuthenticationException If the computed MAC does not match
     *         the MAC declared in the WinZip AES entry.
     * @throws IOException On any I/O related issue.
     */
    private void authenticate(
            final Mac mac,
            final byte[] authenticationCode,
            final String name)
    throws IOException {
        final byte[] buf = computeMac(mac);
        assert buf.length == mac.getMacSize();
        if (!ArrayHelper.equals(buf, 0, authenticationCode, 0, authenticationCode.length / 2))
            throw new ZipAuthenticationException(name
                    + " (authenticated entry content has been tampered with)");
    }
}
