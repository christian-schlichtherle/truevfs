package de.schlichtherle.truezip.crypto;

import java.security.SecureRandom;
import java.util.Random;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class SICSeekableBlockCipherTest {

    @Test
    public void compareModes() {
        BlockCipher engine = new AESEngine();
        int blockSize = engine.getBlockSize();
        BlockCipher ref = new SICBlockCipher(engine); // reference implementation
        BlockCipher uut = new SICSeekableBlockCipher(engine); // unit under test
        PBEParametersGenerator gen = new PKCS5S2ParametersGenerator();
        byte[] salt = new byte[blockSize]; // used as salt and cipher input
        new SecureRandom().nextBytes(salt);
        gen.init("secret".getBytes(), salt, 1);
        ParametersWithIV
                param = (ParametersWithIV) gen.generateDerivedParameters(
                    blockSize * 8,
                    blockSize * 8);

        ref.init(true, param);
        uut.init(true, param);
        assertModes(ref, uut);

        ref.init(false, param);
        uut.init(false, param);
        assertModes(ref, uut);
    }

    private void assertModes(BlockCipher ref, BlockCipher uut) {
        int blockSize = ref.getBlockSize();
        assertThat(uut.getBlockSize(), is(blockSize));
        byte[] input = new byte[blockSize]; // used as salt and cipher input
        new Random().nextBytes(input);
        for (int i = 0; i < 2; i++) {
            byte[] refOutput = new byte[blockSize];
            ref.processBlock(input, 0, refOutput, 0);
            byte[] uutOutput = new byte[blockSize];
            uut.processBlock(input, 0, uutOutput, 0);
            assertThat(uutOutput, equalTo(refOutput));
        }
    }
}
