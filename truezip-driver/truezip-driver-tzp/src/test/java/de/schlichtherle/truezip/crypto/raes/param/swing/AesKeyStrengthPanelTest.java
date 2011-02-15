/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.crypto.raes.param.swing;

import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength;
import java.awt.EventQueue;
import javax.swing.JFrame;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;

import static de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class AesKeyStrengthPanelTest {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private AesKeyStrengthPanel instance;
    private JFrame frame;

    @Before
    public void setUp() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                instance = new AesKeyStrengthPanel();
                frame = new JFrame();
                frame.add(instance);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    @Test
    public void testKeyStrength() throws Exception {
        KeyStrength keyStrength, expResult;
        int selection;

        final JComboBoxOperator
                comboBoxOp = new JComboBoxOperator(new JFrameOperator());

        //
        // Check default.
        //

        expResult = BITS_256;
        keyStrength = instance.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        //
        // Set key strength via API and check API and GUI.
        //

        expResult = BITS_128;
        instance.setKeyStrength(expResult);
        keyStrength = instance.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        expResult = BITS_192;
        instance.setKeyStrength(expResult);
        keyStrength = instance.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        expResult = BITS_256;
        instance.setKeyStrength(expResult);
        keyStrength = instance.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        try {
            instance.setKeyStrength(null);
            fail();
        } catch (NullPointerException expected) {
        }
        keyStrength = instance.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        //
        // Set key strength via GUI and check API and GUI.
        //

        expResult = BITS_128;
        comboBoxOp.setSelectedIndex(expResult.ordinal());
        keyStrength = instance.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        expResult = BITS_192;
        comboBoxOp.setSelectedIndex(expResult.ordinal());
        keyStrength = instance.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        expResult = BITS_256;
        comboBoxOp.setSelectedIndex(expResult.ordinal());
        keyStrength = instance.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBoxOp.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);
    };
}
