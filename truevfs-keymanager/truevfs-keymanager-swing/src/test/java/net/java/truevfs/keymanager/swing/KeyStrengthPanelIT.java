/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.keymanager.swing;

import net.java.truevfs.keymanager.swing.KeyStrengthPanel;
import net.java.truevfs.keymanager.spec.param.AesKeyStrength;
import static net.java.truevfs.keymanager.spec.param.AesKeyStrength.*;
import net.java.truevfs.keymanager.swing.util.JemmyUtils;
import org.junit.After;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JFrameOperator;

/**
 * @author  Christian Schlichtherle
 */
public final class KeyStrengthPanelIT extends JemmyUtils {
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