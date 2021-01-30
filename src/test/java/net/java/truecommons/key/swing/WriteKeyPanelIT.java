/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing;

import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;
import javax.swing.JComponent;
import net.java.truecommons.key.spec.common.AesKeyStrength;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class WriteKeyPanelIT extends KeyPanelTestSuite<WriteKeyPanel> {

    public WriteKeyPanelIT() throws IOException, InterruptedException { }

    @Override
    protected WriteKeyPanel newKeyPanel() {
        return new WriteKeyPanel(mock(SwingPromptingPbeParametersView.class));
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
        final JComponent ui = new KeyStrengthPanel<>(AesKeyStrength.values());
        panel.setExtraDataUI(ui);
        frame.pack();
        assertSame(ui, panel.getExtraDataUI());

        new JComboBoxOperator(frame); // find combo box
    }
}
