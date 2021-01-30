/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.swing;

import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.key.spec.common.AesPbeParameters;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JPasswordFieldOperator;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class ReadKeyPanelIT extends KeyPanelTestSuite<ReadKeyPanel> {

    public ReadKeyPanelIT() throws IOException, InterruptedException { }

    @Override
    protected ReadKeyPanel newKeyPanel() {
        return new ReadKeyPanel(mock(SwingPromptingPbeParametersView.class));
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

        final String passwd = "top secret";
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
