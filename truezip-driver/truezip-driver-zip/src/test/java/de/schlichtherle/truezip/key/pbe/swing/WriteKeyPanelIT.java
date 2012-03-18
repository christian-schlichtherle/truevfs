/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import javax.swing.JComponent;
import static org.junit.Assert.*;
import org.junit.Test;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class WriteKeyPanelIT extends KeyPanelTestSuite<WriteKeyPanel> {

    @Override
    protected WriteKeyPanel newKeyPanel() {
        return new WriteKeyPanel();
    }

    @Override
    protected AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }

    @Test
    public void testPasswd() throws InterruptedException {
        final AesPbeParameters param = newPbeParameters();

        // Check default.
        assertFalse(updateParam(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(error.getText()));

        // Enter mismatching passwords.
        new JPasswordFieldOperator(frame, 0).setText("foofoofoo");
        new JPasswordFieldOperator(frame, 1).setText("barbarbar");
        assertFalse(updateParam(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(error.getText()));

        // Enter matching passwords, too short.
        String passwd = "secret7"; // 7 chars is too short
        new JPasswordFieldOperator(frame, 0).setText(passwd);
        new JPasswordFieldOperator(frame, 1).setText(passwd);
        assertFalse(updateParam(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(error.getText()));

        // Enter matching passwords, long enough.
        passwd = "secret78"; // min 8 chars is OK
        new JPasswordFieldOperator(frame, 0).setText(passwd);
        new JPasswordFieldOperator(frame, 1).setText(passwd);
        assertTrue(updateParam(param));
        assertEquals(passwd, new String(param.getPassword()));
        assertTrue(isBlank(error.getText()));
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testExtraDataUI() {
        final JComponent ui = new KeyStrengthPanel<AesKeyStrength>(AesKeyStrength.values());
        panel.setExtraDataUI(ui);
        frame.pack();
        assertSame(ui, panel.getExtraDataUI());

        new JComboBoxOperator(frame); // find combo box
    }
}
