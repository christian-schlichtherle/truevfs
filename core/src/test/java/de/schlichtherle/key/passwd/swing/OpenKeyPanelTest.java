/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

package de.schlichtherle.key.passwd.swing;

import java.io.*;
import java.util.*;

import javax.swing.*;

import junit.framework.*;

import org.netbeans.jemmy.*;
import org.netbeans.jemmy.operators.*;
import org.netbeans.jemmy.util.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.1
 */
public class OpenKeyPanelTest extends TestCase {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private OpenKeyPanel instance;
    private JFrameOperator frame;
    private JLabelOperator errorLabel;
    private final ComponentChooser keyFileChooser
                = new NameComponentChooser("keyFileChooser");
    
    public OpenKeyPanelTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        instance = new OpenKeyPanel();
        frame = showInstanceInFrame();
        errorLabel = findErrorLabel(frame);
    }

    protected void tearDown() throws Exception {
        frame.setVisible(false);
    }

    private JFrameOperator showInstanceInFrame() {
        final JFrame frame = new JFrame();
        frame.getContentPane().add(instance);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return new JFrameOperator();
    }

    public void testResourceID() {
        final String resourceID = "Hello world!";
        instance.setResourceID(resourceID);
        assertEquals(resourceID, instance.getResourceID());
        
        new JTextComponentOperator(frame, resourceID);
    }

    /**
     * Test of setError method, of class de.schlichtherle.key.passwd.swing.OpenKeyPanel.
     */
    public void testSetError() {
        instance.setError("This is a test error message!");
        assertFalse(isBlank(errorLabel.getText()));
        new JTextFieldOperator(frame).typeText("secret");
        assertTrue(isBlank(errorLabel.getText()));

        instance.setError("This is a test error message!");
        assertFalse(isBlank(errorLabel.getText()));
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        new JFileChooserOperator().chooseFile("file");
        assertTrue(isBlank(errorLabel.getText()));
    }

    private JLabelOperator findErrorLabel(final JFrameOperator frame) {
        final String error = "error";
        instance.setError(error);
        final JLabelOperator errorLabel = new JLabelOperator(frame, error);
        ((JFrame) frame.getSource()).pack();
        instance.setError(null);
        return errorLabel;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().length() <= 0;
    }

    /**
     * Test of getOpenKey method, of class de.schlichtherle.key.passwd.swing.OpenKeyPanel.
     */
    public void testPasswd() {
        String passwd;
        Object result;
        
        final JLabelOperator errorLabel = findErrorLabel(frame);

        // Check default.
        result = instance.getOpenKey();
        assertTrue(result instanceof char[]);
        assertTrue(Arrays.equals("".toCharArray(), (char[]) result));
        assertTrue(isBlank(errorLabel.getText()));

        passwd = "secret";
        new JPasswordFieldOperator(frame).setText(passwd);
        result = instance.getOpenKey();
        assertTrue(result instanceof char[]);
        assertTrue(Arrays.equals(passwd.toCharArray(), (char[]) result));
        assertTrue(isBlank(errorLabel.getText()));
    }

    /**
     * Test of getOpenKey method, of class de.schlichtherle.key.passwd.swing.OpenKeyPanel.
     */
    public void testKeyFile() {
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files

        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        new JFileChooserOperator().chooseFile("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!\"$%&/()=?");
        assertTrue(isBlank(errorLabel.getText()));
        assertNull(instance.getOpenKey());
        assertNotNull(errorLabel.getText());

        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        JFileChooserOperator fc = new JFileChooserOperator();
        File[] files = fc.getFiles();
        fc.cancel(); // close file chooser

        for (int i = 0, l = files.length; i < l; i++) {
            final File file = files[i];
            if (!file.isFile())
                continue;

            new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
            fc = new JFileChooserOperator();
            fc.setSelectedFile(file);
            fc.approve(); // close file chooser
            final Object key = instance.getOpenKey();
            if (key != null) {
                assertTrue(key instanceof byte[]);
                assertTrue(isBlank(errorLabel.getText()));
            } else {
                assertFalse(isBlank(errorLabel.getText()));
            }
        }
    }

    /**
     * Test of is/setKeyChangeRequested method, of class de.schlichtherle.key.passwd.swing.OpenKeyPanel.
     */
    public void testKeyChangeRequested() {
        assertFalse(instance.isKeyChangeRequested());
        assertFalse(new JCheckBoxOperator(frame).isSelected());

        instance.setKeyChangeRequested(true);
        assertTrue(instance.isKeyChangeRequested());
        assertTrue(new JCheckBoxOperator(frame).isSelected());

        instance.setKeyChangeRequested(false);
        assertFalse(instance.isKeyChangeRequested());
        assertFalse(new JCheckBoxOperator(frame).isSelected());

        new JCheckBoxOperator(frame).setSelected(true);
        assertTrue(instance.isKeyChangeRequested());

        new JCheckBoxOperator(frame).setSelected(false);
        assertFalse(instance.isKeyChangeRequested());
    }

    /**
     * Test of get/setExtraDataUI method, of class de.schlichtherle.key.passwd.swing.OpenKeyPanel.
     */
    public void testExtraDataUI() {
        final JComponent ui = new AesKeyStrengthPanel();
        instance.setExtraDataUI(ui);
        frame.pack();
        assertSame(ui, instance.getExtraDataUI());
        
        new JComboBoxOperator(frame); // find combo box
    }
}
