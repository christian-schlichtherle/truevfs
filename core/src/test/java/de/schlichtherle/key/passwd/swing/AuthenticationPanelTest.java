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

import java.awt.*;
import java.lang.ref.*;
import java.io.*;
import java.util.logging.*;

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
public class AuthenticationPanelTest extends TestCase {

    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }
    private static final Logger logger = Logger.getLogger(AuthenticationPanelTest.class.getName());
    static final File rootDir;


    static {
        if (File.separatorChar == '\\') {
            rootDir = new File("C:\\");
        } else {
            rootDir = new File("/");
        }
    }
    private final ComponentChooser keyFileChooser = new NameComponentChooser("keyFileChooser");

    public AuthenticationPanelTest(String testName) {
        super(testName);
    }

    /**
     * Test of getFileChooser method, of class de.schlichtherle.key.passwd.swing.AuthenticationPanel.
     */
    public void testGetFileChooser() throws InterruptedException {
        JFileChooser fc = AuthenticationPanel.getFileChooser();
        fc.setCurrentDirectory(rootDir);

        assertSame("JFileChooser instance should not yet have been garbage collected!",
                fc, AuthenticationPanel.getFileChooser());

        final Reference ref = new SoftReference(fc);
        fc = null;

        int i = 1;
        try {
            while (true) {
                assertNotNull("JFileChooser should not yet have been garbage collected!",
                        ref.get());
                // Allocate big fat object in order to cause the internal
                // cache for the file chooser to be cleared.
                // If the chunk is finally big enough, an OOME will be thrown.
                final byte[] bfo = new byte[i * 1024 * 1024];
                i <<= 1; // is not reached on OOME!
            }
        } catch (OutOfMemoryError stopCondition) {
        }

        i >>= 1;
        logger.fine("Successfully allocated " + i + " Megabytes heap memory before OOME.");

        // As a side effect of the OOME, the internal SoftReference to the
        // JFileChooser should have been cleared and hence our reference
        // should have been enqueued.
        assertNull("JFileChooser should have been garbage collected",
                ref.get());

        // Now ask for a file chooser again.
        fc = AuthenticationPanel.getFileChooser();
        assertEquals(
                "New JFileChooser needs to have same current directory!",
                rootDir, fc.getCurrentDirectory());
    }

    public void testTabbedPane() {
        final String text = "Hello world!";
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                final AuthenticationPanel instance = new AuthenticationPanel();

                JPanel passwdPanel = null;
                try {
                    instance.setPasswdPanel(passwdPanel);
                    fail("Calling setPasswdPanel(null) should throw an NPE!");
                } catch (NullPointerException npe) {
                }

                passwdPanel = new JPanel();
                passwdPanel.add(new JLabel(text));
                instance.setPasswdPanel(passwdPanel);

                final JFrame frame = new JFrame();
                frame.getContentPane().add(instance);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setVisible(true);
            }
        });

        final JFrameOperator frame = new JFrameOperator();

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        JFileChooserOperator fc = new JFileChooserOperator();
        assertSame(AuthenticationPanel.getFileChooser(), fc.getSource());
        fc.cancel();
        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_PASSWD); // select tab for passwords
        new JLabelOperator(frame, text);
        fc = null;

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_KEY_FILE); // select tab for key files
        new JButtonOperator(frame, keyFileChooser).push(); // open file chooser
        fc = new JFileChooserOperator();
        final File file = new File(rootDir, "test");
        fc.setSelectedFile(file);
        fc.approve();
        JTextFieldOperator tfOp = new JTextFieldOperator(frame);
        assertEquals(file.getPath(), tfOp.getText());
        fc = null;

        new JTabbedPaneOperator(frame).selectPage(AuthenticationPanel.AUTH_PASSWD); // select tab for passwords
        new JLabelOperator(frame, text);

    //frame.close();
    }
}
