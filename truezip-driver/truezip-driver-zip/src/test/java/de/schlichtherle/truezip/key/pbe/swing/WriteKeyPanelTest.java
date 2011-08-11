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
public final class WriteKeyPanelTest extends KeyPanelTestSuite<WriteKeyPanel> {

    @Override
    protected WriteKeyPanel newKeyPanel() {
        return new WriteKeyPanel();
    }

    @Override
    protected AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }

    @Test
    public void testPasswd() {
        final AesPbeParameters param = newPbeParameters();

        // Check default.
        assertFalse(panel.updateParam(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(error.getText()));

        // Enter mismatching passwords.
        new JPasswordFieldOperator(frame, 0).setText("foofoofoo");
        new JPasswordFieldOperator(frame, 1).setText("barbarbar");
        assertFalse(panel.updateParam(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(error.getText()));

        // Enter matching passwords, too short.
        String passwd = "secret7"; // 7 chars is too short
        new JPasswordFieldOperator(frame, 0).setText(passwd);
        new JPasswordFieldOperator(frame, 1).setText(passwd);
        assertFalse(panel.updateParam(param));
        assertNull(param.getPassword());
        assertFalse(isBlank(error.getText()));

        // Enter matching passwords, long enough.
        passwd = "secret78"; // min 8 chars is OK
        new JPasswordFieldOperator(frame, 0).setText(passwd);
        new JPasswordFieldOperator(frame, 1).setText(passwd);
        assertTrue(panel.updateParam(param));
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
