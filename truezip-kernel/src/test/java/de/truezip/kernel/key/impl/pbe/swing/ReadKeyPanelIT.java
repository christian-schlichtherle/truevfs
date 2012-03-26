/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key.impl.pbe.swing;

import de.truezip.kernel.key.param.AesPbeParameters;
import static org.junit.Assert.*;
import org.junit.Test;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;

/**
 * @author  Christian Schlichtherle
 */
public final class ReadKeyPanelIT extends KeyPanelTestSuite<ReadKeyPanel> {

    @Override
    protected ReadKeyPanel newKeyPanel() {
        return new ReadKeyPanel();
    }

    @Override
    protected AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }

    @Test
    public void testPasswd() throws InterruptedException {
        final AesPbeParameters param = newPbeParameters();

        // Check default.
        assertTrue(updateParam(param));
        assertEquals(0, param.getPassword().length);
        assertTrue(isBlank(error.getText()));

        final String passwd = "secret";
        new JPasswordFieldOperator(frame).setText(passwd);
        assertTrue(updateParam(param));
        assertEquals(passwd, new String(param.getPassword()));
        assertTrue(isBlank(error.getText()));
    }

    @Test
    public void testChangeKeySelected() {
        assertFalse(panel.isChangeKeySelected());
        assertFalse(new JCheckBoxOperator(frame).isSelected());

        panel.setChangeKeySelected(true);
        assertTrue(panel.isChangeKeySelected());
        assertTrue(new JCheckBoxOperator(frame).isSelected());

        panel.setChangeKeySelected(false);
        assertFalse(panel.isChangeKeySelected());
        assertFalse(new JCheckBoxOperator(frame).isSelected());

        new JCheckBoxOperator(frame).setSelected(true);
        assertTrue(panel.isChangeKeySelected());

        new JCheckBoxOperator(frame).setSelected(false);
        assertFalse(panel.isChangeKeySelected());
    }
}