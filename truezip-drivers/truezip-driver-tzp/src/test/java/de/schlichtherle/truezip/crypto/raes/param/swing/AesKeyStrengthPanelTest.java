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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;

import static de.schlichtherle.truezip.crypto.raes.Type0RaesParameters.KeyStrength.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class AesKeyStrengthPanelTest extends JemmyUtils {
    private AesKeyStrengthPanel panel;
    private JFrameOperator frame;

    @Before
    public void setUp() {
        panel = new AesKeyStrengthPanel();
        frame = showInNewFrame(panel);
    }

    @After
    public void tearDown() throws Exception {
        frame.dispose();
    }

    @Test
    public void testKeyStrength() throws Exception {
        KeyStrength keyStrength, expResult;
        int selection;

        final JComboBoxOperator comboBox = new JComboBoxOperator(frame);

        //
        // Check default.
        //

        expResult = BITS_256;
        keyStrength = panel.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBox.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        //
        // Set key strength via API and check API and GUI.
        //

        expResult = BITS_128;
        panel.setKeyStrength(expResult);
        keyStrength = panel.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBox.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        expResult = BITS_192;
        panel.setKeyStrength(expResult);
        keyStrength = panel.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBox.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        expResult = BITS_256;
        panel.setKeyStrength(expResult);
        keyStrength = panel.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBox.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        try {
            panel.setKeyStrength(null);
            fail();
        } catch (NullPointerException expected) {
        }
        keyStrength = panel.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBox.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        //
        // Set key strength via GUI and check API and GUI.
        //

        expResult = BITS_128;
        comboBox.setSelectedIndex(expResult.ordinal());
        keyStrength = panel.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBox.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        expResult = BITS_192;
        comboBox.setSelectedIndex(expResult.ordinal());
        keyStrength = panel.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBox.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);

        expResult = BITS_256;
        comboBox.setSelectedIndex(expResult.ordinal());
        keyStrength = panel.getKeyStrength();
        assertSame(expResult, keyStrength);
        selection = comboBox.getSelectedIndex();
        assertSame(expResult.ordinal(), selection);
    };
}
