/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.key.passwd.swing;

import de.schlichtherle.key.*;

import java.awt.*;
import java.lang.reflect.*;

import javax.swing.*;

import junit.framework.*;
import junit.framework.Test;

import org.netbeans.jemmy.*;
import org.netbeans.jemmy.operators.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.1
 */
public class AesKeyStrengthPanelTest extends TestCase {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private AesKeyStrengthPanel instance;
    private JFrame frame;
    private JFrameOperator frameOp;
    
    public AesKeyStrengthPanelTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                instance = new AesKeyStrengthPanel();
                frame = new JFrame();
                frame.getContentPane().add(instance);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });

        frameOp = new JFrameOperator(); // wait for JFrame
    }

    protected void tearDown() throws Exception {
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
                frame.dispose();
            }
        });
    }

    public static Test suite() throws Exception {
        // THIS DOESN'T WORK RELIABLY!
        // Increase timeout values to see what's going on.
        // Otherwise everything happens very fast.
        /*try {
            JemmyProperties.getCurrentTimeouts().loadDebugTimeouts();
        } catch (IOException failure) {
            throw new UndeclaredThrowableException(failure);
        }*/

        TestSuite suite = new TestSuite(AesKeyStrengthPanelTest.class);
        
        return suite;
    }

    /**
     * Test of get/setKeyStrength method, of class de.schlichtherle.key.passwd.swing.AesKeyStrengthPanel.
     */
    public void testKeyStrength() throws Exception {
        int keyStrength, expResult, selection;

        JComboBoxOperator comboBoxOp = new JComboBoxOperator(frameOp);

        //
        // Check default.
        //

        expResult = AesKeyProvider.KEY_STRENGTH_256;
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        sleep();

        //
        // Set key strength via API and check API and GUI.
        //

        expResult = AesKeyProvider.KEY_STRENGTH_128;
        instance.setKeyStrength(expResult);
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        sleep();

        expResult = AesKeyProvider.KEY_STRENGTH_192;
        instance.setKeyStrength(expResult);
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        sleep();

        expResult = AesKeyProvider.KEY_STRENGTH_256;
        instance.setKeyStrength(expResult);
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        sleep();

        expResult = AesKeyProvider.KEY_STRENGTH_256;
        try {
            instance.setKeyStrength(AesKeyProvider.KEY_STRENGTH_128 - 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        //sleep();

        expResult = AesKeyProvider.KEY_STRENGTH_256;
        try {
            instance.setKeyStrength(AesKeyProvider.KEY_STRENGTH_256 + 1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        //sleep();

        //
        // Set key strength via GUI and check API and GUI.
        //

        expResult = AesKeyProvider.KEY_STRENGTH_128;
        comboBoxOp.setSelectedIndex(expResult);
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        sleep();

        expResult = AesKeyProvider.KEY_STRENGTH_192;
        comboBoxOp.setSelectedIndex(expResult);
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        sleep();

        expResult = AesKeyProvider.KEY_STRENGTH_256;
        comboBoxOp.setSelectedIndex(expResult);
        keyStrength = instance.getKeyStrength();
        assertEquals(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertEquals(expResult, selection);
        sleep();
    };
    
    private static void sleep() {
        //new QueueTool().waitEmpty(500); // doesn't always update the screen!
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }
}
