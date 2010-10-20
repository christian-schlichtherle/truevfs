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

package de.schlichtherle.truezip.key.passwd.swing;

import de.schlichtherle.truezip.key.AesKeyProvider;
import java.awt.EventQueue;
import java.lang.reflect.UndeclaredThrowableException;
import javax.swing.JFrame;
import junit.framework.TestCase;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class AesKeyStrengthPanelUITest extends TestCase {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private AesKeyStrengthPanel instance;
    private JFrame frame;
    private JFrameOperator frameOp;

    public AesKeyStrengthPanelUITest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        EventQueue.invokeLater(new Runnable() {
            @Override
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

    @Override
    protected void tearDown() throws Exception {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
			public void run() {
                //frame.setVisible(false);
                frame.dispose();
            }
        });
    }

    /**
     * Test of get/setKeyStrength method, of class de.schlichtherle.truezip.key.passwd.swing.AesKeyStrengthPanel.
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
