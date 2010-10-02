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
package de.schlichtherle.truezip.key.passwd.swing;

import java.awt.EventQueue;
import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import junit.framework.TestCase;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JFileChooserOperator;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.netbeans.jemmy.util.NameComponentChooser;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class AuthenticationPanelUITest extends TestCase {

    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private static final Logger logger = Logger.getLogger(
            AuthenticationPanelUITest.class.getName());

    static final File rootDir;
    static {
        if (File.separatorChar == '\\') {
            rootDir = new File("C:\\");
        } else {
            rootDir = new File("/");
        }
    }
    private final ComponentChooser keyFileChooser
            = new NameComponentChooser("keyFileChooser");

    public AuthenticationPanelUITest(String testName) {
        super(testName);
    }

    public void testGetFileChooser() throws InterruptedException {
        final ReferenceQueue<JFileChooser> queue = new ReferenceQueue<JFileChooser>();
        final PhantomReference<JFileChooser> ref = initQueue(queue, rootDir);

        byte[] bfo;
        int i = 1;
        while (null == queue.poll()) {
                // Allocate big fat object in order to cause the internal
                // cache for the file chooser to be cleared.
            try {
                bfo = new byte[i * 1024 * 1024];
                i++; // is not reached on OOME!
            } catch (OutOfMemoryError oome) {
                //oome.printStackTrace();
                // SoftReferences (which are used by getFileChooser) are
                // guaranteed to be cleared BEFORE an OOME is thrown.
                // However, the additional chunk of memory we are requesting
                // may be too big, in which case the reference is first cleared
                // and then immediately an OOME is thrown.
                // When the SoftReference has been cleared, the referent is
                // made eligible for finalization.
                // Run the finalization now and assert this:
                System.runFinalization();
                assertNotNull(queue.poll()); // JFileChooser has been finalized
                break;
            }

            // Release memory again in order to allow the JVM to operate
            // normally.
            bfo = null;

            System.gc();
        }
        assert null == queue.poll(); // previous poll() has removed the reference
        logger.log(Level.FINE, "Successfully allocated {0} megabytes before JFileChooser was discarded.", i);

        // Now ask for a file chooser again.
        JFileChooser fc = AuthenticationPanel.getFileChooser();
        assertEquals(
                "Newly instantiated JFileChooser needs to have same current directory as previous instance!",
                rootDir, fc.getCurrentDirectory());
    }

    private PhantomReference<JFileChooser> initQueue(
            final ReferenceQueue<JFileChooser> queue,
            final File dir) {
        JFileChooser fc = AuthenticationPanel.getFileChooser();
        fc.setCurrentDirectory(dir);

        final PhantomReference<JFileChooser> ref
                = new PhantomReference<JFileChooser>(fc, queue);

        fc = null;
        System.gc();

        fc = AuthenticationPanel.getFileChooser();
        fc.setCurrentDirectory(dir);

        fc = null;
        System.gc();

        assertNull(
                "Initial JFileChooser instance should not yet have been thrown away!",
                queue.poll());

        return ref;
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
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

        frame.dispose();
    }
}
