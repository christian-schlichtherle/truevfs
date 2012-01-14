/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import static de.schlichtherle.truezip.crypto.param.AesKeyStrength.*;
import static de.schlichtherle.truezip.swing.JemmyUtils.showFrameWith;
import org.junit.After;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class KeyStrengthPanelIT {
    private KeyStrengthPanel<AesKeyStrength> panel;
    private JFrameOperator frame;

    @Before
    public void setUp() throws InterruptedException {
        panel = new KeyStrengthPanel<AesKeyStrength>(AesKeyStrength.values());
        frame = showFrameWith(panel);
    }

    @After
    public void tearDown() throws Exception {
        frame.dispose();
    }

    @Test
    public void testKeyStrength() throws Exception {
        AesKeyStrength keyStrength, expResult;
        int selection;

        final JComboBoxOperator comboBox = new JComboBoxOperator(frame);

        //
        // Check default.
        //

        expResult = BITS_128;
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
