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

import de.schlichtherle.truezip.key.pbe.swing.AuthenticationPanel;
import java.io.File;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JFileChooserOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.netbeans.jemmy.util.NameComponentChooser;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class AuthenticationPanelTest extends JemmyUtils {
    private static final String LABEL_TEXT = "Hello World!";
	
    private JFrameOperator frame;
	
    @Before
    public void setUp() {
        final JPanel passwdPanel = new JPanel();
        passwdPanel.add(new JLabel(LABEL_TEXT));
        final AuthenticationPanel panel = new AuthenticationPanel();
        panel.setPasswdPanel(passwdPanel);
        frame = showInNewFrame(panel);
    }

    @After
    public void tearDown() {
        frame.dispose();
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    @Test
    public void testTabbedPane() {
        final ComponentChooser
                keyFileChooser = new NameComponentChooser("keyFileChooser");
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        JFileChooserOperator fc = new FileChooserOperator();
        fc.cancel();
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_PASSWD); // select tab for passwords
        new JLabelOperator(frame, LABEL_TEXT);
        fc = null;

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        fc = new FileChooserOperator();
        final File file = new File("test");
        fc.setSelectedFile(file);
        new QueueTool().waitEmpty();
        fc.approve();
        new QueueTool().waitEmpty();
        JTextFieldOperator tf = new JTextFieldOperator(frame);
        assertEquals(file.getPath(), tf.getText());
        fc = null;

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_PASSWD); // select tab for passwords
        new JLabelOperator(frame, LABEL_TEXT);
    }
}
