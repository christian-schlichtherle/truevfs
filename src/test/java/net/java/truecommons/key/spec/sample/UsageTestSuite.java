/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.sample;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import net.java.truecommons.key.spec.KeyManager;
import net.java.truecommons.key.spec.KeyManagerMap;
import net.java.truecommons.key.spec.KeyProvider;
import net.java.truecommons.key.spec.UnknownKeyException;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import net.java.truecommons.key.spec.sl.KeyManagerMapLocator;
import org.junit.Test;

/** @author Christian Schlichtherle */
public abstract class UsageTestSuite {

// START SNIPPET: getKeyManagerMap
    KeyManagerMap getKeyManagerMap() { return KeyManagerMapLocator.SINGLETON; }
// END SNIPPET: getKeyManagerMap

    @Test
    public void testGettingAPasswordForWritingAnEncryptedFile()
    throws IOException, UnknownKeyException {
// START SNIPPET: gettingAPasswordForWritingAnEncryptedFile1
        KeyManager<AesPbeParameters> manager = getKeyManagerMap()
                .manager(AesPbeParameters.class);
// END SNIPPET: gettingAPasswordForWritingAnEncryptedFile1
// START SNIPPET: gettingAPasswordForWritingAnEncryptedFile2
        File file = new File("encrypted").getCanonicalFile();
        KeyProvider<AesPbeParameters> provider = manager.provider(file.toURI());
// END SNIPPET: gettingAPasswordForWritingAnEncryptedFile2
// START SNIPPET: gettingAPasswordForWritingAnEncryptedFile3
        AesPbeParameters param = provider.getKeyForWriting();
// END SNIPPET: gettingAPasswordForWritingAnEncryptedFile3
// START SNIPPET: gettingAPasswordForWritingAnEncryptedFile4
        char[] password = param.getPassword();
        try {
            // Now write the file using the password.
            // [...]
        } finally {
            Arrays.fill(password, (char) 0); // wipe the password memory
        }
// END SNIPPET: gettingAPasswordForWritingAnEncryptedFile4
    }

    @Test
    public void testGettingAPasswordForReadingAnEncryptedFile()
    throws IOException, UnknownKeyException {
// START SNIPPET: gettingAPasswordForReadingAnEncryptedFile
        File file = new File("encrypted").getCanonicalFile();
        KeyProvider<AesPbeParameters> provider = getKeyManagerMap()
                .manager(AesPbeParameters.class)
                .provider(file.toURI());
        boolean invalid = false;
        do {
            AesPbeParameters param = provider.getKeyForReading(invalid);
            char [] password = param.getPassword();
            try {
                // Now read the file and verify the password.
                // [...]
                // new String(char[]) copies the character array, so don't do
                // that in a real application!
                invalid = !"top secret".equals(new String(password));
            } finally {
                Arrays.fill(password, (char) 0); // wipe the password memory
            }
        } while (invalid);
// END SNIPPET: gettingAPasswordForReadingAnEncryptedFile
    }
}
