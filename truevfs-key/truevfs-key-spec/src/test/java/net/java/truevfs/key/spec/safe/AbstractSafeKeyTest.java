package net.java.truevfs.key.spec.safe;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import static java.nio.charset.StandardCharsets.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class AbstractSafeKeyTest {

    private TestKey key = new TestKey();

    @Before
    public void before() {
        assertNull(key.getSecret());
    }

    @Test
    public void testSetKeyMakesAProtectiveCopy() {
        final ByteBuffer b1 = UTF_8.encode("föo");
        key.setSecret(b1);
        assertTrue(UTF_8.newEncoder().encode(CharBuffer.wrap("bär"), b1, true).isUnderflow());
        final ByteBuffer b2 = key.getSecret();
        assertThat(b2, is(not(b1)));
    }

    @Test
    public void testGetKeyMakesAProtectiveCopy() {
        key.setSecret(UTF_8.encode("föo"));
        final ByteBuffer b1 = key.getSecret();
        final ByteBuffer b2 = key.getSecret();
        assertNotSame(b1, b2);
        assertEquals(b1, b2);
    }

    @Test
    public void testCloneMakesAProtectiveCopyOfTheKey() {
        final ByteBuffer secret = UTF_8.encode("föo");
        key.setSecret(secret); // copies byte buffer
        final TestKey clone = key.clone();
        key.setSecret(null); // clears byte buffer
        assertThat(clone.getSecret(), is(secret));
    }

    private static class TestKey
    extends AbstractSafeKey<TestKey, TestKeyStrength> { }
}
