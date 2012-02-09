/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import static org.junit.Assert.*;
import org.junit.Test;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
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
