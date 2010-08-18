/*
 * AbstractKeyProviderTest.java
 * JUnit based test
 *
 * Created on 18. Februar 2007, 13:26
 */

package de.schlichtherle.key;

import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 * @version @version@
 */
public class PromptingKeyProviderTest extends TestCase {

    private PromptingKeyProvider<Cloneable> instance;

    public PromptingKeyProviderTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() {
        instance = new PromptingKeyProvider<Cloneable>();
    }

    /**
     * Test of getKey method, of class truevfs.key.AbstractKeyProvider.
     */
    public void testKey() {
        assertNull(instance.getKey());
        Cloneable result = new char[0];
        instance.setKey(result);
        assertSame(result, instance.getKey());
        instance.setKey(null);
        assertNull(instance.getKey());
    }
}
