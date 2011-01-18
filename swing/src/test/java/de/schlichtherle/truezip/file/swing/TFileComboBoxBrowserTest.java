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
package de.schlichtherle.truezip.file.swing;

import de.schlichtherle.truezip.file.TFile;
import java.awt.event.KeyEvent;
import java.io.File;
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
import org.junit.Test;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JTextComponentOperator;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class TFileComboBoxBrowserTest {
    static {
        JemmyProperties.setCurrentOutput(TestOut.getNullOutput()); // shut up!
    }

    private static final Logger logger
            = Logger.getLogger(TFileComboBoxBrowserTest.class.getName());

    @Test
    public void testDirectory() {
        final TFileComboBoxBrowser browser = new TFileComboBoxBrowser();
        final File cur = new TFile(".");
        File dir;

        dir = browser.getDirectory();
        assertEquals(cur, dir);

        browser.setDirectory(cur);
        dir = browser.getDirectory();
        assertSame(cur, dir);

        browser.setDirectory(null);
        dir = browser.getDirectory();
        assertEquals(cur, dir);
    }

    @Test
    public void testState4Update() {
        final TFileComboBoxBrowser browser = new TFileComboBoxBrowser();
        try {
            browser.update("");
            fail("Calling the previous method should throw a NullPointerException!");
        } catch (NullPointerException expected) {
        }

        final JComboBox combo = new JComboBox();
        browser.setComboBox(combo);
        browser.update("");
    }

    @Test
    public void testAutoCompletion() throws IOException {
        File dir = new File(".");
        assertAutoCompletion(dir);
        assertAutoCompletion(dir.getCanonicalFile());

        dir = new TFile(".");
        assertAutoCompletion(dir);
        assertAutoCompletion(dir.getCanonicalFile());
    }

    private void assertAutoCompletion(final File dir) {
        final String[] entries = dir.list();
        if (entries == null || entries.length == 0) {
            logger.warning("Current directory does not contain any files - skipping test!");
            return;
        }

        final JComboBox combo = new JComboBox();
        final TFileComboBoxBrowser browser = new TFileComboBoxBrowser();
        browser.setDirectory(dir);

        for (int i = 0; i < entries.length; i++) {
            final String entry = entries[i];
            for (int j = 0; j < entry.length(); j++)
                assertAutoCompletion(browser, combo, entry.substring(0, j));
        }
    }

    private static void assertAutoCompletion(
            final TFileComboBoxBrowser browser,
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
        final String[] entries = new TFile(".").list(new FilenameFilter() {
            final int l = initials.length();

            @Override
			public boolean accept(File dir, String name) {
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

    @Test
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
