/*
 * AbstractKeyProviderTest.java
 * JUnit based test
 *
 * Created on 18. Februar 2007, 13:26
 */

package de.schlichtherle.truezip.key;

import java.util.Arrays;
import junit.framework.TestCase;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class AbstractKeyProviderTest extends TestCase {

    public AbstractKeyProviderTest(String testName) {
        super(testName);
    }

    /**
     * Test of cloneKey method, of class truevfs.key.AbstractKeyProvider.
     */
    public void testClone() {
        Cloneable[] keys = {
            "test".getBytes(),
            "test".toCharArray(),
            new short[] { 0, 1, 2 },
            new int[] { 0, 1, 2 },
            new long[] { 0L, 1L, 2L },
            new float[] { 0F, 1F, 2F },
            new double[] { 0D, 1D, 2D },
            new boolean[] { false, true, false },
            new Object[] { new Object(), new Object(), new Object() },
            new Byte[] { 0, 1, 2 },
            new Character[] { 0, 1, 2 },
            new Short[] { 0, 1, 2 },
            new Integer[] { 0, 1, 2 },
            new Long[] { 0L, 1L, 2L },
            new Float[] { 0F, 1F, 2F },
            new Double[] { 0D, 1D, 2D },
            new Boolean[] { false, true, false },
            new CloneMe(),
        };

        for (final Cloneable key : keys) {
            testClone(key, AbstractKeyProvider.clone(key));
            testClone(  AbstractKeyProvider.clone(key),
                        AbstractKeyProvider.clone(key));
        }
    }

    private static class CloneMe implements Cloneable {
        private final int rnd = (int) Math.random();

        @Override
        public CloneMe clone() {
            try {
                return (CloneMe) super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CloneMe && rnd == ((CloneMe) o).rnd;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + this.rnd;
            return hash;
        }
    }

    public void testClone(final Cloneable key, final Cloneable clone) {
        assertNotSame(key, clone);
        if (key instanceof byte[])
            assertTrue(Arrays.equals((byte[]) key, (byte[]) clone));
        else if (key instanceof char[])
            assertTrue(Arrays.equals((char[]) key, (char[]) clone));
        else if (key instanceof short[])
            assertTrue(Arrays.equals((short[]) key, (short[]) clone));
        else if (key instanceof int[])
            assertTrue(Arrays.equals((int[]) key, (int[]) clone));
        else if (key instanceof long[])
            assertTrue(Arrays.equals((long[]) key, (long[]) clone));
        else if (key instanceof float[])
            assertTrue(Arrays.equals((float[]) key, (float[]) clone));
        else if (key instanceof double[])
            assertTrue(Arrays.equals((double[]) key, (double[]) clone));
        else if (key instanceof boolean[])
            assertTrue(Arrays.equals((boolean[]) key, (boolean[]) clone));
        else if (key instanceof Object[])
            assertTrue(Arrays.equals((Object[]) key, (Object[]) clone));
        else
            assertEquals(key, clone);
    }
}
