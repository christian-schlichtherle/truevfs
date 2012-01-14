/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import static de.schlichtherle.truezip.swing.JemmyUtils.showFrameWith;
import java.io.File;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.*;
import org.netbeans.jemmy.util.NameComponentChooser;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class AuthenticationPanelIT {
    private static final String LABEL_TEXT = "Hello World!";

    private JFrameOperator frame;

    @Before
    public void setUp() throws InterruptedException {
        final JPanel passwdPanel = new JPanel();
        passwdPanel.add(new JLabel(LABEL_TEXT));
        final AuthenticationPanel panel = new AuthenticationPanel();
        panel.setPasswdPanel(passwdPanel);
        frame = showFrameWith(panel);
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
        JFileChooserOperator fc = new FileChooserOperator(frame);
        fc.cancel();
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_PASSWD); // select tab for passwords
        new JLabelOperator(frame, LABEL_TEXT);
        fc = null;

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        fc = new FileChooserOperator(frame);
        final File file = new File("test");
        fc.setSelectedFile(file);
        fc.approve();
        fc.getQueueTool().waitEmpty();
        JTextFieldOperator tf = new JTextFieldOperator(frame);
        assertEquals(file.getPath(), tf.getText());
        fc = null;

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_PASSWD); // select tab for passwords
        new JLabelOperator(frame, LABEL_TEXT);
    }
}
