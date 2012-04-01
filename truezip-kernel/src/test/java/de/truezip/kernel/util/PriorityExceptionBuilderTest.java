/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.truezip.kernel.util;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public class PriorityExceptionBuilderTest extends ExceptionBuilderTestSuite {
    
    public PriorityExceptionBuilderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of update method, of class PriorityExceptionBuilder.
     */
    @Test
    public void testUpdate() {
        System.out.println("update");
        Exception input = null;
        Exception previous = null;
        PriorityExceptionBuilder instance = new PriorityExceptionBuilder();
        Exception expResult = null;
        Exception result = instance.update(input, previous);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    @SuppressWarnings("serial")
    private static class TestException extends Exception {
    } // TestException

    @SuppressWarnings("serial")
    private static class WarningException extends TestException {
    } // WarningException
}
