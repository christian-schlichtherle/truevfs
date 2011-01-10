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
package de.schlichtherle.truezip.file.swing;

import de.schlichtherle.truezip.file.swing.FileComboBoxBrowser;
import de.schlichtherle.truezip.file.File;
import java.awt.event.KeyEvent;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import junit.framework.TestCase;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileComboBoxBrowserTest extends TestCase {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private static final Logger logger
            = Logger.getLogger(FileComboBoxBrowserTest.class.getName());

    public FileComboBoxBrowserTest(String testName) {
        super(testName);
    }

    /**
     * Test of directory property, of class de.schlichtherle.truezip.io.swing.FileComboBoxBrowser.
     */
    public void testDirectory() {
        final FileComboBoxBrowser browser = new FileComboBoxBrowser();
        final java.io.File cur = new File(".");
        java.io.File dir;

        dir = browser.getDirectory();
        assertEquals(cur, dir);

        browser.setDirectory(cur);
        dir = browser.getDirectory();
        assertSame(cur, dir);

        browser.setDirectory(null);
        dir = browser.getDirectory();
        assertEquals(cur, dir);
    }

    /**
     * Test of update method, of class de.schlichtherle.truezip.io.swing.FileComboBoxBrowser.
     */
    public void testState4Update() {
        final FileComboBoxBrowser browser = new FileComboBoxBrowser();
        try {
            browser.update("");
            fail("Calling the previous method should throw a NullPointerException!");
        } catch (NullPointerException expected) {
        }

        final JComboBox combo = new JComboBox();
        browser.setComboBox(combo);
        browser.update("");
    }

    public void testAutoCompletion() throws IOException {
        java.io.File dir = new java.io.File(".");
        testAutoCompletion(dir);
        testAutoCompletion(dir.getCanonicalFile());

        dir = new File(".");
        testAutoCompletion(dir);
        testAutoCompletion(dir.getCanonicalFile());
    }

    public void testAutoCompletion(final java.io.File dir) {
        final String[] entries = dir.list();
        if (entries == null || entries.length == 0) {
            logger.warning("Current directory does not contain any files - skipping test!");
            return;
        }

        final JComboBox combo = new JComboBox();
        final FileComboBoxBrowser browser = new FileComboBoxBrowser();
        browser.setDirectory(dir);

        for (int i = 0; i < entries.length; i++) {
            final String entry = entries[i];
            for (int j = 0; j < entry.length(); j++)
                testAutoCompletion(browser, combo, entry.substring(0, j));
        }
    }

    private static void testAutoCompletion(
            final FileComboBoxBrowser browser,
            final JComboBox combo,
            final String initials) {
        browser.setComboBox(null); // reset
        combo.removeAllItems(); // reset
        combo.setEditable(true);
        combo.setSelectedItem(initials);
        browser.setComboBox(combo); // initialize
        assertEquals(filter(initials), asList(combo));
    }

    private static List<String> filter(final String initials) {
        final String[] entries = new File(".").list(new FilenameFilter() {
            final int l = initials.length();

            @Override
			public boolean accept(java.io.File dir, String name) {
                if (name.length() >= l)
                    return initials.equalsIgnoreCase(name.substring(0, l));
                else
                    return false;
            }
        });
        Arrays.sort(entries, Collator.getInstance());
        return Arrays.asList(entries);
    }

    private static List<Object> asList(final JComboBox combo) {
        final List<Object> list = new LinkedList<Object>();
        final ComboBoxModel model = combo.getModel();
        for (int i = 0, l = model.getSize(); i < l; i++)
            list.add(model.getElementAt(i));
        return list;
    }

    // TODO: This is far from being a comprehensive test.
    public void testGUI() throws InterruptedException, InvocationTargetException {
        FileComboBoxPanel.main(new String[] { "." });

        final JFrameOperator frame = new JFrameOperator();
        final JTextComponentOperator tc0 = new JTextComponentOperator(frame, 0);
        final JTextComponentOperator tc1 = new JTextComponentOperator(frame, 1);

        // Type a character in tc0, then check that it's appearing in tc1.
        tc0.typeText("?");
        assertEquals("?", tc1.getText());

        // Clear character in tc1, ensure that its cleared in tc0
        tc1.clearText();
        assertEquals("", tc0.getText());

        // Select first element in list of tc0 (entry in current directory),
        // if any, and check its appearance in tc1.
        tc0.pressKey(KeyEvent.VK_DOWN);
        final String child = tc0.getText();
        assertEquals(child, tc1.getText());

        frame.dispose();
    }
}
