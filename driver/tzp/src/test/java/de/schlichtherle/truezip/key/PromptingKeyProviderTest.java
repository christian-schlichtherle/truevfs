/*
 * AbstractKeyProviderTest.java
 * JUnit based test
 *
 * Created on 18. Februar 2007, 13:26
 */

package de.schlichtherle.truezip.key;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version @version@
 */
public class PromptingKeyProviderTest {

    private PromptingKeyProvider<Cloneable> instance;

    @Before
    public void setUp() {
        instance = new PromptingKeyProvider<Cloneable>();
    }

    @Test
    public void testKey() {
        assertNull(instance.getKey());
        Cloneable result = new char[0];
        instance.setKey(result);
        assertSame(result, instance.getKey());
        instance.setKey(null);
        assertNull(instance.getKey());
    }
}
