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

import de.schlichtherle.truezip.key.pbe.AesPbeParameters;
import static org.junit.Assert.*;
import org.junit.Test;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ReadKeyPanelTest extends KeyPanelTestSuite<ReadKeyPanel> {

    @Override
    protected ReadKeyPanel newKeyPanel() {
        return new ReadKeyPanel();
    }

    @Override
    protected AesPbeParameters newPbeParameters() {
        return new AesPbeParameters();
    }

    @Test
    public void testPasswd() {
        final AesPbeParameters param = newPbeParameters();

        // Check default.
        assertTrue(panel.updateParam(param));
        assertEquals(0, param.getPassword().length);
        assertTrue(isBlank(error.getText()));

        String passwd = "secret";
        new JPasswordFieldOperator(frame).setText(passwd);
        assertTrue(panel.updateParam(param));
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
